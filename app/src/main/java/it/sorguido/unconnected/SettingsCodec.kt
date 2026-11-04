package it.sorguido.unconnected

import it.sorguido.unconnected.domain.*
import org.json.JSONArray
import org.json.JSONObject

/** Strict schema parsing: damage never becomes a valid empty (block-all) schedule. */
object SettingsCodec {
    private fun JSONObject.strictInt(key: String): Int = get(key) as? Int ?: error("Invalid integer type")
    private fun JSONObject.strictBoolean(key: String): Boolean = get(key) as? Boolean ?: error("Invalid boolean type")
    private fun JSONObject.strictLong(key: String): Long = when (val value = get(key)) {
        is Long -> value
        is Int -> value.toLong()
        else -> error("Invalid integer type")
    }
    const val MAX_BYTES = 128 * 1024
    /** Highest schema written by this build; reading accepts every schema from 1 up to it. */
    const val CURRENT_SCHEMA = 3

    /** Target is optional so callers can pre-check other fields before touching the target. */
    private fun numberObject(number: NumberRule): JSONObject = JSONObject().apply {
        put("id", number.id); put("label", number.label); put("enabled", number.enabled)
        when (val target = number.target) {
            is ExactTarget -> put("e164", target.canonical)
            is MaskTarget -> put("mask", target.canonical)
        }
    }

    private fun parseNumber(it: JSONObject, canonicalize: (NumberTarget) -> String?): NumberRule {
        val id = it.getString("id")
        val label = it.getString("label")
        val enabled = it.strictBoolean("enabled")
        val hasE164 = it.has("e164")
        val hasMask = it.has("mask")
        // Exactly one typed field: masks never ride inside an E.164 container.
        if (hasE164 == hasMask) error("Number needs exactly one of e164/mask")
        val target: NumberTarget = if (hasE164) {
            val raw = it.getString("e164")
            if (raw.contains('*')) error("Asterisks are not allowed in e164")
            ExactTarget(raw)
        } else {
            val raw = it.getString("mask")
            val star = raw.indexOf('*')
            if (star < 0 || !raw.substring(star).all { character -> character == '*' } ||
                raw.take(star).contains('*') || !Numbers.canonicalForm.matches(raw.take(star))
            ) error("Malformed mask")
            MaskTarget(raw.take(star), raw.length - star)
        }
        // Reject persisted targets that normalize to a different value: never silently repair storage.
        require(canonicalize(target) == target.canonical) {
            "Stored target is not canonical for the selected region"
        }
        return NumberRule(id, label, target, enabled)
    }

    fun decode(raw: String): Settings? = try {
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_BYTES)
        val json = JSONObject(raw)
        val schema = json.strictInt("schema")
        require(schema in 1..CURRENT_SCHEMA)
        // Old snapshots migrate with no timer. Never reinterpret an unversioned timer field.
        require(schema == 3 || !json.has("timer"))
        val numbers = json.getJSONArray("numbers")
        val windows = json.getJSONArray("windows")
        require(numbers.length() <= 200 && windows.length() <= 100)
        val region = json.getString("region")
        val result = Settings(
            enabled = json.strictBoolean("enabled"),
            region = region,
            emptyScheduleConfirmed = json.strictBoolean("emptyScheduleConfirmed"),
            timer = if (schema < 3) null else {
                require(json.has("timer"))
                if (json.isNull("timer")) null else json.getJSONObject("timer").let {
                    BlockTimer(it.strictLong("startedWallMillis"), it.strictLong("startedElapsedMillis"),
                        it.strictLong("durationMillis"), it.strictInt("bootCount"),
                        if (!it.has("anchor")) null else it.getJSONObject("anchor").let { anchor ->
                            TimerAnchor(anchor.strictInt("bootCount"), anchor.strictLong("elapsedMillis"),
                                anchor.strictLong("remainingMillis"))
                        })
                }
            },
            // schema=1 documents carry only "e164" and migrate losslessly to exact targets.
            numbers = (0 until numbers.length()).map { i -> parseNumber(numbers.getJSONObject(i)) { Numbers.canonicalTarget(it, region) } },
            windows = (0 until windows.length()).map { i -> windows.getJSONObject(i).let { window ->
                val days = window.getJSONArray("days")
                require(days.length() in 0..7)
                val selected = (0 until days.length()).map { days.get(it) as? Int ?: error("Invalid day type") }
                require(selected.distinct().size == selected.size)
                TimeWindow(window.getString("id"), window.getString("label"),
                    selected.toSet(),
                    window.strictInt("startMinute"), window.strictInt("endMinute"))
            } }
        )
        // A typed but logically invalid weekly window must not disable an independent timer.
        // Keep it for repair; Rules gates the schedule branch separately and the UI flags it.
        result.takeIf(Rules::isValidGeneral)
    } catch (_: Exception) { null }

    fun encode(settings: Settings): String = JSONObject().apply {
        val hasMasks = settings.numbers.any { it.target is MaskTarget }
        put("schema", if (settings.timer != null) CURRENT_SCHEMA else if (hasMasks) 2 else 1)
        settings.timer?.let { timer -> put("timer", JSONObject().apply {
            put("startedWallMillis", timer.startedWallMillis)
            put("startedElapsedMillis", timer.startedElapsedMillis)
            put("durationMillis", timer.durationMillis)
            put("bootCount", timer.bootCount)
            timer.anchor?.let { anchor -> put("anchor", JSONObject().apply {
                put("bootCount", anchor.bootCount)
                put("elapsedMillis", anchor.elapsedMillis)
                put("remainingMillis", anchor.remainingMillis)
            }) }
        }) }
        put("enabled", settings.enabled); put("region", settings.region)
        put("emptyScheduleConfirmed", settings.emptyScheduleConfirmed)
        put("numbers", JSONArray().apply { settings.numbers.forEach { number -> put(numberObject(number)) } })
        put("windows", JSONArray().apply { settings.windows.forEach { window -> put(JSONObject().apply {
            put("id", window.id); put("label", window.label); put("days", JSONArray(window.days.sorted()))
            put("startMinute", window.startMinute); put("endMinute", window.endMinute)
        }) } })
    }.toString()
}

package it.sorguido.unconnected

import it.sorguido.unconnected.domain.*
import org.json.JSONArray
import org.json.JSONObject

/** Strict schema parsing: damage never becomes a valid empty (block-all) schedule. */
object SettingsCodec {
    private fun JSONObject.strictInt(key: String): Int = get(key) as? Int ?: error("Invalid integer type")
    private fun JSONObject.strictBoolean(key: String): Boolean = get(key) as? Boolean ?: error("Invalid boolean type")
    const val MAX_BYTES = 128 * 1024
    fun decode(raw: String): Settings? = try {
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_BYTES)
        val json = JSONObject(raw)
        require(json.strictInt("schema") == 1)
        val numbers = json.getJSONArray("numbers")
        val windows = json.getJSONArray("windows")
        require(numbers.length() <= 200 && windows.length() <= 100)
        val result = Settings(
            enabled = json.strictBoolean("enabled"), region = json.getString("region"),
            emptyScheduleConfirmed = json.strictBoolean("emptyScheduleConfirmed"),
            numbers = (0 until numbers.length()).map { i -> numbers.getJSONObject(i).let {
                NumberRule(it.getString("id"), it.getString("label"), it.getString("e164"), it.strictBoolean("enabled"))
            } },
            windows = (0 until windows.length()).map { i -> windows.getJSONObject(i).let { window ->
                val days = window.getJSONArray("days")
                require(days.length() in 1..7)
                val selected = (0 until days.length()).map { days.get(it) as? Int ?: error("Invalid day type") }
                require(selected.distinct().size == selected.size)
                TimeWindow(window.getString("id"), window.getString("label"),
                    selected.toSet(),
                    window.strictInt("startMinute"), window.strictInt("endMinute"))
            } }
        )
        result.takeIf(Rules::isValid)
    } catch (_: Exception) { null }

    fun encode(settings: Settings): String = JSONObject().apply {
        put("schema", 1); put("enabled", settings.enabled); put("region", settings.region)
        put("emptyScheduleConfirmed", settings.emptyScheduleConfirmed)
        put("numbers", JSONArray().apply { settings.numbers.forEach { number -> put(JSONObject().apply {
            put("id", number.id); put("label", number.label); put("e164", number.e164); put("enabled", number.enabled)
        }) } })
        put("windows", JSONArray().apply { settings.windows.forEach { window -> put(JSONObject().apply {
            put("id", window.id); put("label", window.label); put("days", JSONArray(window.days.sorted()))
            put("startMinute", window.startMinute); put("endMinute", window.endMinute)
        }) } })
    }.toString()
}

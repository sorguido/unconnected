package it.sorguido.unconnected

import it.sorguido.unconnected.domain.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SettingsCodecTest {
    private val sample = Settings(
        enabled = true,
        region = "US",
        numbers = listOf(NumberRule("one", "Example", "+12025550101"),
            NumberRule("two", "Example 2", "+12025550102", enabled = false)),
        windows = listOf(TimeWindow("night", "Night shift", setOf(1, 3, 7), 1320, 360),
            TimeWindow("split", "Afternoon", setOf(2, 4), 780, 1020)),
    )
    private fun json() = JSONObject(SettingsCodec.encode(sample))

    @Test fun roundTripPreservesCompleteSnapshot() {
        assertEquals(sample, SettingsCodec.decode(SettingsCodec.encode(sample)))
        assertEquals(Settings(), SettingsCodec.decode(SettingsCodec.encode(Settings())))
    }

    @Test fun emptyScheduleConfirmationIsPersistedExplicitly() {
        listOf(false, true).forEach { confirmed ->
            val settings = sample.copy(windows = emptyList(), emptyScheduleConfirmed = confirmed)
            assertEquals(settings, SettingsCodec.decode(SettingsCodec.encode(settings)))
        }
    }

    @Test fun malformedOrOversizedContentNeverBecomesDefaults() {
        listOf("", "{", "null", "[]", "{}", "x".repeat(SettingsCodec.MAX_BYTES + 1)).forEach {
            assertNull(SettingsCodec.decode(it))
        }
        val oversizedUnicode = json().put("padding", "é".repeat(SettingsCodec.MAX_BYTES / 2))
        assertNull(SettingsCodec.decode(oversizedUnicode.toString()))
    }

    @Test fun everyRequiredFieldMustBePresent() {
        listOf("schema", "enabled", "region", "emptyScheduleConfirmed", "numbers", "windows").forEach { field ->
            val missing = json().apply { remove(field) }
            assertNull("Missing $field", SettingsCodec.decode(missing.toString()))
        }
        listOf("id", "label", "e164", "enabled").forEach { field ->
            val missing = json().apply { getJSONArray("numbers").getJSONObject(0).remove(field) }
            assertNull("Missing number $field", SettingsCodec.decode(missing.toString()))
        }
        listOf("id", "label", "days", "startMinute", "endMinute").forEach { field ->
            val missing = json().apply { getJSONArray("windows").getJSONObject(0).remove(field) }
            assertNull("Missing window $field", SettingsCodec.decode(missing.toString()))
        }
    }

    @Test fun rejectsTypeCoercionAndFractionalMinutes() {
        val malformed = listOf(
            json().put("schema", "1"),
            json().put("schema", 1.5),
            json().put("enabled", "true"),
            json().put("emptyScheduleConfirmed", "true"),
            json().put("region", JSONObject.NULL),
            json().apply { getJSONArray("numbers").getJSONObject(0).put("enabled", "true") },
            json().apply { getJSONArray("windows").getJSONObject(0).put("startMinute", 1320.5) },
            json().apply { getJSONArray("windows").getJSONObject(0).put("endMinute", "360") },
            json().apply { getJSONArray("windows").getJSONObject(0).put("days", JSONArray().put(1.5)) },
            json().apply { getJSONArray("windows").getJSONObject(0).put("days", JSONArray().put("1")) },
            json().apply { getJSONArray("windows").getJSONObject(0).put("days", JSONArray().put(1).put(1)) },
        )
        malformed.forEachIndexed { index, value ->
            assertNull("Coerced variant $index", SettingsCodec.decode(value.toString()))
        }
    }

    @Test fun rejectsInvalidStateAndUnknownSchema() {
        assertNull(SettingsCodec.decode(json().put("schema", SettingsCodec.CURRENT_SCHEMA + 1).toString())) // unknown future schema
        assertNull(SettingsCodec.decode(json().put("schema", 0).toString()))
        assertNull(SettingsCodec.decode(json().put("region", "ZZ").toString()))
        assertNull(SettingsCodec.decode(SettingsCodec.encode(sample.copy(numbers = sample.numbers + sample.numbers[0]))))
        val invalidSchedule = sample.copy(windows = listOf(sample.windows[0].copy(days = emptySet())))
        assertEquals(invalidSchedule, SettingsCodec.decode(SettingsCodec.encode(invalidSchedule)))
        assertFalse(Rules.isValid(invalidSchedule))
        assertTrue(Rules.isValidGeneral(invalidSchedule))
        assertNull(SettingsCodec.decode(SettingsCodec.encode(sample.copy(windows = List(101) { sample.windows[0].copy(id = "$it") }))))
        assertNull(SettingsCodec.decode(SettingsCodec.encode(sample.copy(numbers = List(201) { sample.numbers[0].copy(id = "$it") }))))
    }

    // ---- schema 1 (v1.0.0) compatibility and schema 2 masks ----

    /**
     * The exact JSON shape written by the released v1.0.0 app, with fictional numbers only.
     * Built by concatenating raw strings: a Kotlin raw string keeps '\' literal, so line
     * continuations would corrupt the fixture.
     */
    private val v1Json = """{"schema":1,"enabled":true,"region":"US","emptyScheduleConfirmed":false,""" +
        """"numbers":[{"id":"one","label":"Example","e164":"+12025550101","enabled":true},""" +
        """{"id":"two","label":"Example 2","e164":"+12025550102","enabled":false}],""" +
        """"windows":[{"id":"night","label":"Night shift","days":[1,3,7],"startMinute":1320,"endMinute":360},""" +
        """{"id":"split","label":"Afternoon","days":[2,4],"startMinute":780,"endMinute":1020}]}"""

    /**
     * Structural JSON comparison: object key order is an implementation detail of JSONObject,
     * the content is not. Written against the API shared by android.jar and the org.json test jar.
     */
    private fun jsonEquals(expected: Any?, actual: Any?): Boolean = when {
        expected is JSONObject && actual is JSONObject ->
            expected.length() == actual.length() &&
                expected.keys().asSequence().all { key ->
                    actual.has(key) && jsonEquals(expected.opt(key), actual.opt(key))
                }
        expected is JSONArray && actual is JSONArray ->
            expected.length() == actual.length() &&
                (0 until expected.length()).all { jsonEquals(expected.get(it), actual.get(it)) }
        else -> expected == actual
    }

    private fun assertSameJson(expected: String, actual: String) =
        assertTrue("JSON differs.\nexpected: $expected\nactual:   $actual",
            jsonEquals(JSONObject(expected), JSONObject(actual)))

    @Test fun legacySchemaOneUpgradesLosslesslyToTheSameSnapshot() {
        val decoded = SettingsCodec.decode(v1Json)
        assertEquals(sample, decoded)
        decoded!!.numbers.forEach { assertTrue(it.target is ExactTarget) }
        // Re-encoding an all-exact snapshot stays on schema 1 with identical content, so a v1.0.0
        // reader that only understands "e164" keeps working after the upgrade.
        val reencoded = SettingsCodec.encode(decoded)
        assertEquals(1, JSONObject(reencoded).getInt("schema"))
        assertSameJson(v1Json, reencoded)
    }

    @Test fun schemaTwoRoundTripsMasksAndPreservesEveryOtherField() {
        val mask = NumberRule("milan", "Fictional office",
            NumberMasks.parse("02 12345***", "IT")!!, enabled = false)
        val settings = Settings(enabled = true, region = "IT", numbers = listOf(mask),
            windows = sample.windows, emptyScheduleConfirmed = true)
        val encoded = SettingsCodec.encode(settings)
        assertEquals(2, JSONObject(encoded).getInt("schema"))
        assertEquals("+390212345***", JSONObject(encoded).getJSONArray("numbers")
            .getJSONObject(0).getString("mask"))
        assertEquals(settings, SettingsCodec.decode(encoded))
    }

    @Test fun masksOfDifferentWidthAreStoredAndReadBackUnchanged() {
        // Narrower and wider variants of the same prefix are distinct valid rules, not damage.
        listOf("+390212345*" to 1, "+390212345**" to 2, "+390212345***" to 3, "+390212345****" to 4)
            .forEach { (stored, wildcards) ->
                val settings = Settings(region = "IT", numbers = listOf(NumberRule("m", "", MaskTarget("+390212345", wildcards))))
                val encoded = SettingsCodec.encode(settings)
                assertEquals(stored, JSONObject(encoded).getJSONArray("numbers").getJSONObject(0).getString("mask"))
                assertEquals(settings, SettingsCodec.decode(encoded))
            }
    }

    @Test fun corruptMaskFieldsFailOpenInsteadOfBecomingBlockAll() {
        val base = SettingsCodec.encode(Settings(region = "IT",
            numbers = listOf(NumberRule("m", "", NumberMasks.parse("02 12345***", "IT")!!))))
        fun variant(mutate: JSONObject.() -> Unit): String =
            JSONObject(base).apply { mutate(getJSONArray("numbers").getJSONObject(0)) }.toString()
        val corrupted = listOf(
            variant { put("mask", "0212345***") },          // national spelling in storage
            variant { put("mask", "+3902*2345***") },       // internal wildcard
            variant { put("mask", "+390212345?") },         // wrong wildcard character
            variant { put("mask", "+390212345*****") },     // more wildcards than MAX_WILDCARDS
            variant { put("mask", "+99912345***") },        // unassigned country code
            variant { put("e164", "+390212345000") },       // e164 and mask together
            variant { remove("mask") },                     // no target at all
            variant { put("e164", "+390212345***") },       // asterisks inside e164
        )
        corrupted.forEachIndexed { index, json ->
            assertNull("Corrupt mask variant $index must decode to null", SettingsCodec.decode(json))
        }
        // The untouched document still decodes: the variants above are copies, not mutations of it.
        assertEquals(1, SettingsCodec.decode(base)!!.numbers.size)
    }

    @Test fun storedMaskWithSilentlyRepairedTrunkPrefixIsRejected() {
        // Fictitious Ofcom London range. libphonenumber removes the redundant 0 after +44,
        // but stored canonical targets must never be accepted with a different meaning.
        val nonCanonical = MaskTarget("+4402079460", 3)
        assertEquals("+442079460***", Numbers.canonicalTarget(nonCanonical, "GB"))
        val settings = Settings(region = "GB",
            numbers = listOf(NumberRule("invalid-prefix", "Example", nonCanonical)))
        assertFalse(Rules.isValid(settings))
        assertNull(SettingsCodec.decode(SettingsCodec.encode(settings)))

        // The properly canonicalized form remains valid and round-trips without loss.
        val canonical = NumberMasks.parse("02079460***", "GB")!!
        assertEquals("+442079460***", canonical.canonical)
        val validSettings = settings.copy(numbers = listOf(NumberRule("valid-prefix", "Example", canonical)))
        assertTrue(Rules.isValid(validSettings))
        assertEquals(validSettings, SettingsCodec.decode(SettingsCodec.encode(validSettings)))
    }

}

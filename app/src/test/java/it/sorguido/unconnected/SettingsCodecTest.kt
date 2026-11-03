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
        assertNull(SettingsCodec.decode(json().put("schema", 3).toString())) // unknown future schema
        assertNull(SettingsCodec.decode(json().put("schema", 0).toString()))
        assertNull(SettingsCodec.decode(json().put("region", "ZZ").toString()))
        assertNull(SettingsCodec.decode(SettingsCodec.encode(sample.copy(numbers = sample.numbers + sample.numbers[0]))))
        assertNull(SettingsCodec.decode(SettingsCodec.encode(sample.copy(windows = listOf(sample.windows[0].copy(days = emptySet()))))))
        assertNull(SettingsCodec.decode(SettingsCodec.encode(sample.copy(windows = List(101) { sample.windows[0].copy(id = "$it") }))))
        assertNull(SettingsCodec.decode(SettingsCodec.encode(sample.copy(numbers = List(201) { sample.numbers[0].copy(id = "$it") }))))
    }

    // ---- schema 1 (v1.0.0) compatibility and schema 2 masks ----

    /** The exact JSON shape written by the released v1.0.0 app, with fictional numbers only. */
    private val v1Json = """{"schema":1,"enabled":true,"region":"US","emptyScheduleConfirmed":false,\
"numbers":[{"id":"one","label":"Example","e164":"+12025550101","enabled":true},\
{"id":"two","label":"Example 2","e164":"+12025550102","enabled":false}],\
"windows":[{"id":"night","label":"Night shift","days":[1,3,7],"startMinute":1320,"endMinute":360},\
{"id":"split","label":"Afternoon","days":[2,4],"startMinute":780,"endMinute":1020}]}"""

    @Test fun legacySchemaOneUpgradesLosslesslyToTheSameSnapshot() {
        val decoded = SettingsCodec.decode(v1Json)
        assertEquals(sample, decoded)
        assertNotNull(decoded)
        decoded!!.numbers.forEach { assertTrue(it.target is ExactTarget) }
        // Re-encoding an all-exact snapshot stays on schema 1: untouched bytes for old readers.
        assertEquals(1, JSONObject(SettingsCodec.encode(decoded)).getInt("schema"))
        assertEquals(v1Json.replace(" ",""), SettingsCodec.encode(decoded).replace(" ",""))
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

    @Test fun corruptMaskFieldsFailOpenInsteadOfBecomingBlockAll() {
        val base = JSONObject(SettingsCodec.encode(Settings(region = "IT",
            numbers = listOf(NumberRule("m", "", NumberMasks.parse("02 12345***", "IT")!!)))))
        val firstNumber = base.getJSONArray("numbers").getJSONObject(0)
        val corrupted = listOf(
            JSONObject(base.toString()).apply { getJSONArray("numbers").getJSONObject(0).put("mask", "+390212345**") }, // different width
            JSONObject(base.toString()).apply { getJSONArray("numbers").getJSONObject(0).put("mask", "0212345***") },   // national spelling in storage
            JSONObject(base.toString()).apply { getJSONArray("numbers").getJSONObject(0).put("mask", "+3902*2345***") }, // internal wildcard
            JSONObject(base.toString()).apply { getJSONArray("numbers").getJSONObject(0).put("mask", "+390212345?") },   // wrong wildcard char
            JSONObject(base.toString()).apply { getJSONArray("numbers").getJSONObject(0).put("mask", "+390212345****") },// too wide to canonicalize
            JSONObject(base.toString()).apply { getJSONArray("numbers").getJSONObject(0).put("e164", "+390212345000") }, // e164 + mask together
            JSONObject(base.toString()).apply { getJSONArray("numbers").getJSONObject(0).remove("mask") },               // no target at all
            JSONObject(base.toString()).apply { getJSONArray("numbers").getJSONObject(0).put("e164", "+390212345***") }, // asterisks inside e164
        )
        corrupted.forEachIndexed { index, json ->
            assertNull("Corrupt mask variant $index must decode to null", SettingsCodec.decode(json.toString()))
        }
        firstNumber.remove("mask"); firstNumber.put("mask", "+390212345***")
        assertEquals(1, SettingsCodec.decode(base.toString())!!.numbers.size)
    }
}

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
        assertNull(SettingsCodec.decode(json().put("schema", 2).toString()))
        assertNull(SettingsCodec.decode(json().put("region", "ZZ").toString()))
        assertNull(SettingsCodec.decode(SettingsCodec.encode(sample.copy(numbers = sample.numbers + sample.numbers[0]))))
        assertNull(SettingsCodec.decode(SettingsCodec.encode(sample.copy(windows = listOf(sample.windows[0].copy(days = emptySet()))))))
        assertNull(SettingsCodec.decode(SettingsCodec.encode(sample.copy(windows = List(101) { sample.windows[0].copy(id = "$it") }))))
        assertNull(SettingsCodec.decode(SettingsCodec.encode(sample.copy(numbers = List(201) { sample.numbers[0].copy(id = "$it") }))))
    }
}

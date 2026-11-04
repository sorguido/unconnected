package it.sorguido.unconnected

import it.sorguido.unconnected.domain.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.time.ZonedDateTime

class TimerPersistenceTest {
    private val reading = TimerReading(1_800_000_000_000L, 10_000L, 12)
    private val timer = BlockTimers.start(60, reading)!!
    private val settings = Settings(numbers = listOf(NumberRule("one", "Example", "+12025550101")), timer = timer)
    private val inside = ZonedDateTime.parse("2026-11-02T12:00:00+01:00[Europe/Rome]")

    @Test fun schemaThreeRoundTripsAllTimerAnchorsAndSurvivesRecreation() {
        val encoded = SettingsCodec.encode(settings)
        assertEquals(3, JSONObject(encoded).getInt("schema"))
        val recreated = SettingsCodec.decode(encoded)!!
        assertEquals(settings, recreated)
        assertTrue(Rules.shouldBlock(recreated, "+12025550101", inside, true, reading))
        assertEquals(35_000L, BlockTimers.remainingMillis(recreated.timer, reading.copy(elapsedMillis = 35_000)))
        assertEquals(25_000L, BlockTimers.remainingMillis(recreated.timer,
            reading.copy(wallMillis = reading.wallMillis + 35_000, elapsedMillis = 5000, bootCount = 13)))
    }

    @Test fun legacySchemasMigrateWithoutActivatingTimerAndKeepMasks() {
        for (masked in listOf(false, true)) {
            val legacy = settings.copy(timer = null, enabled = true,
                numbers = if (masked) listOf(NumberRule("range", "", NumberMasks.parse("+120255501**", "US")!!)) else settings.numbers)
            val oldRaw = SettingsCodec.encode(legacy)
            assertEquals(if (masked) 2 else 1, JSONObject(oldRaw).getInt("schema"))
            assertFalse(JSONObject(oldRaw).has("timer"))
            val migrated = SettingsCodec.decode(oldRaw)!!
            assertEquals(legacy, migrated)
            assertNull(migrated.timer)
            assertFalse(Rules.shouldBlock(migrated, "+12025550101", inside, true, reading))
            assertEquals(legacy.copy(timer = timer), SettingsCodec.decode(SettingsCodec.encode(migrated.copy(timer = timer))))
        }
    }

    @Test fun stoppingTimerPreservesLegacyCompatibleSnapshotAndConfirmation() {
        val started = settings.copy(enabled = true, windows = emptyList(), emptyScheduleConfirmed = true)
        val stopped = started.copy(timer = null)
        assertEquals(stopped, SettingsCodec.decode(SettingsCodec.encode(stopped)))
        assertEquals(1, JSONObject(SettingsCodec.encode(stopped)).getInt("schema"))
        assertTrue(stopped.emptyScheduleConfirmed)
    }

    @Test fun corruptOrMissingTimerFieldsFailOpen() {
        val timerFields = listOf("startedWallMillis", "startedElapsedMillis", "durationMillis", "bootCount")
        for (field in timerFields) {
            val missing = JSONObject(SettingsCodec.encode(settings)).apply { getJSONObject("timer").remove(field) }
            assertNull("Missing timer $field", SettingsCodec.decode(missing.toString()))
            for (bad in listOf("123", 1.5, JSONObject.NULL, -1L, Long.MAX_VALUE)) {
                val corrupt = JSONObject(SettingsCodec.encode(settings)).apply { getJSONObject("timer").put(field, bad) }
                assertNull("Corrupt timer $field=$bad", SettingsCodec.decode(corrupt.toString()))
            }
        }
        val missingObject = JSONObject(SettingsCodec.encode(settings)).apply { remove("timer") }
        assertNull(SettingsCodec.decode(missingObject.toString()))
        val nullObject = JSONObject(SettingsCodec.encode(settings)).put("timer", JSONObject.NULL)
        assertNull(SettingsCodec.decode(nullObject.toString())!!.timer)
        for (schema in 1..2) assertNull(SettingsCodec.decode(JSONObject(SettingsCodec.encode(settings)).put("schema", schema).toString()))
    }

    @Test fun malformedGeneralRulesStillInvalidateTheWholeDocumentWithTimer() {
        assertNull(SettingsCodec.decode(SettingsCodec.encode(settings.copy(region = "ZZ"))))
        assertNull(SettingsCodec.decode(SettingsCodec.encode(settings.copy(numbers = settings.numbers + settings.numbers))))
        val invalidType = JSONObject(SettingsCodec.encode(settings)).put("enabled", "false")
        assertNull(SettingsCodec.decode(invalidType.toString()))
    }

    @Test fun invalidTypedWeeklyScheduleIsRetainedWhileTimerWorks() {
        val invalid = settings.copy(enabled = true, windows = listOf(TimeWindow("bad", "", setOf(0), -1, 2000)))
        val decoded = SettingsCodec.decode(SettingsCodec.encode(invalid))!!
        assertEquals(invalid, decoded)
        assertTrue(Rules.shouldBlock(decoded, "+12025550101", inside, true, reading))
        assertFalse(Rules.shouldBlock(decoded.copy(timer = null), "+12025550101", inside, true, reading))
    }

    private class MemoryDisk(var document: ByteArray? = null) : SettingsStorage {
        var failWrite = false
        var failRead = false
        var writes = 0
        override fun read(): ByteArray? {
            if (failRead) throw IOException("Simulated read failure")
            return document?.copyOf()
        }
        override fun writeAtomic(bytes: ByteArray) {
            if (failWrite) throw IOException("Simulated atomic failure")
            document = bytes.copyOf()
            writes++
        }
    }

    @Test fun cleanInstallStartsWithBothFunctionsOff() {
        val defaults = SettingsStore(MemoryDisk()).load()!!
        assertFalse(defaults.enabled)
        assertNull(defaults.timer)
    }

    @Test fun failedTimerStartPreservesCommittedDocumentAndPausesSessionUntilRetry() {
        val committed = settings.copy(timer = null)
        val disk = MemoryDisk(SettingsCodec.encode(committed).toByteArray())
        val store = SettingsStore(disk)
        disk.failWrite = true
        assertFalse(store.save(settings))
        assertEquals(committed, store.load())
        assertTrue(SettingsStore.sessionPaused.get())
        assertTrue(SettingsStore.sessionPauseFlow.value)
        assertEquals(0, disk.writes)
        disk.failWrite = false
        assertTrue(store.save(settings))
        assertEquals(settings, SettingsStore(MemoryDisk(disk.document)).load())
        assertFalse(SettingsStore.sessionPaused.get())
        assertFalse(SettingsStore.sessionPauseFlow.value)
    }

    @Test fun failedEarlyStopDoesNotClaimTimerStoppedAndRetryCommitsStop() {
        val disk = MemoryDisk(SettingsCodec.encode(settings).toByteArray())
        val store = SettingsStore(disk)
        val revision = SettingsStore.revision.get()
        disk.failWrite = true
        assertFalse(store.save(settings.copy(timer = null)))
        assertEquals(settings, store.load())
        assertTrue(SettingsStore.sessionPaused.get())
        assertTrue(SettingsStore.revision.get() > revision)
        disk.failWrite = false
        assertTrue(store.save(settings.copy(timer = null)))
        assertNull(store.load()!!.timer)
        assertFalse(SettingsStore.sessionPaused.get())
    }

    @Test fun invalidWritesAndReadFailuresNeverBecomeEnabledDefaults() {
        val disk = MemoryDisk(SettingsCodec.encode(settings).toByteArray())
        val store = SettingsStore(disk)
        assertFalse(store.save(settings.copy(region = "ZZ")))
        assertEquals(settings, store.load())
        assertTrue(SettingsStore.sessionPaused.get())
        assertTrue(store.save(settings)) // Leave the process guard healthy for other test classes.
        disk.failRead = true
        assertNull(store.load())
        disk.failRead = false
        disk.document = "{broken".toByteArray()
        assertNull(store.load())
        disk.document = ByteArray(SettingsCodec.MAX_BYTES + 1)
        assertNull(store.load())
    }

    @Test fun timerCanBeSavedWithUnconfirmedEmptyOrInvalidSchedule() {
        val store = SettingsStore(MemoryDisk())
        for (value in listOf(settings.copy(windows = emptyList()),
            settings.copy(windows = listOf(TimeWindow("broken", "", emptySet(), 0, 0))))) {
            assertTrue(store.save(value))
            assertEquals(value, store.load())
        }
    }
    @Test fun rebootAnchorIsDurableAndClockRollbackCannotExtendOrReviveTimer() {
        var now = reading.copy(wallMillis = reading.wallMillis + 35_000, elapsedMillis = 5000, bootCount = 13)
        val disk = MemoryDisk(SettingsCodec.encode(settings).toByteArray())
        val store = SettingsStore(disk) { now }
        val rebased = store.load()!!
        assertEquals(1, disk.writes)
        assertEquals(25_000L, BlockTimers.remainingMillis(rebased.timer, now))
        assertEquals(TimerAnchor(13, 5000, 25_000), rebased.timer!!.anchor)
        now = now.copy(wallMillis = reading.wallMillis + 10_000, elapsedMillis = 15_000)
        val recreated = SettingsStore(disk) { now }.load()!!
        assertEquals(15_000L, BlockTimers.remainingMillis(recreated.timer, now))
        assertEquals(1, disk.writes) // Repeated loads do not poll/write.
        now = now.copy(wallMillis = reading.wallMillis - 100_000, elapsedMillis = 30_000)
        val expired = store.load()!!
        assertEquals(0L, BlockTimers.remainingMillis(expired.timer, now))
        assertEquals(0L, expired.timer!!.anchor!!.remainingMillis)
        now = now.copy(wallMillis = reading.wallMillis + 10_000, elapsedMillis = 35_000)
        val stillExpired = SettingsStore(disk) { now }.load()!!
        assertEquals(0L, BlockTimers.remainingMillis(stillExpired.timer, now))
        assertEquals(2, disk.writes)
    }

    @Test fun observedExpiryAfterRebootIsTombstonedBeforeClockCanRollBack() {
        var now = reading.copy(wallMillis = reading.wallMillis + 65_000, elapsedMillis = 5000, bootCount = 13)
        val disk = MemoryDisk(SettingsCodec.encode(settings).toByteArray())
        val first = SettingsStore(disk) { now }.load()!!
        assertEquals(0L, first.timer!!.anchor!!.remainingMillis)
        now = now.copy(wallMillis = reading.wallMillis + 20_000, elapsedMillis = 6000)
        assertEquals(0L, BlockTimers.remainingMillis(SettingsStore(disk) { now }.load()!!.timer, now))
        now = now.copy(bootCount = 14, elapsedMillis = 1000)
        assertEquals(0L, BlockTimers.remainingMillis(SettingsStore(disk) { now }.load()!!.timer, now))
    }

    @Test fun failedRebootAnchorCommitFailsOpenAndPreservesOriginalDocument() {
        val original = SettingsCodec.encode(settings).toByteArray()
        val disk = MemoryDisk(original.copyOf()).apply { failWrite = true }
        val rebooted = reading.copy(wallMillis = reading.wallMillis + 35_000, elapsedMillis = 5000, bootCount = 13)
        val store = SettingsStore(disk) { rebooted }
        assertNull(store.load())
        assertArrayEquals(original, disk.document)
        assertTrue(SettingsStore.sessionPaused.get())
        disk.failWrite = false
        val recovered = store.load()!!
        assertEquals(25_000L, BlockTimers.remainingMillis(recovered.timer, rebooted))
        assertTrue(SettingsStore.sessionPaused.get()) // Automatic repair cannot undo a failed user stop.
        assertTrue(store.save(recovered.copy(timer = null)))
        assertFalse(SettingsStore.sessionPaused.get())
    }

    @Test fun staleUiSaveCannotOverwriteRebootAnchorOrExpiryTombstone() {
        var now = reading.copy(wallMillis = reading.wallMillis + 35_000, elapsedMillis = 5000, bootCount = 13)
        val disk = MemoryDisk(SettingsCodec.encode(settings).toByteArray())
        val store = SettingsStore(disk) { now }
        val loaded = store.load()!!
        assertNotNull(loaded.timer!!.anchor)
        now = now.copy(wallMillis = reading.wallMillis + 10_000, elapsedMillis = 15_000)
        assertTrue(store.save(settings.copy(enabled = true))) // An older UI snapshot has no anchor.
        var saved = store.load()!!
        assertEquals(loaded.timer.anchor, saved.timer!!.anchor)
        assertEquals(15_000L, BlockTimers.remainingMillis(saved.timer, now))
        now = now.copy(elapsedMillis = 31_000)
        val expired = store.load()!!
        assertEquals(0L, expired.timer!!.anchor!!.remainingMillis)
        assertTrue(store.save(settings.copy(enabled = false)))
        saved = store.load()!!
        assertEquals(0L, saved.timer!!.anchor!!.remainingMillis)
        assertEquals(0L, BlockTimers.remainingMillis(saved.timer, now))
        val genuinelyNewTimer = BlockTimers.start(60, now)!!
        assertTrue(store.save(saved.copy(timer = genuinelyNewTimer)))
        assertEquals(60_000L, BlockTimers.remainingMillis(store.load()!!.timer, now))
    }

    @Test fun repeatedRebootsNeverIncreasePreviouslyAnchoredRemainingDuration() {
        var now = reading.copy(wallMillis = reading.wallMillis + 35_000, elapsedMillis = 5000, bootCount = 13)
        val disk = MemoryDisk(SettingsCodec.encode(settings).toByteArray())
        val first = SettingsStore(disk) { now }.load()!!
        assertEquals(25_000L, BlockTimers.remainingMillis(first.timer, now))
        now = now.copy(wallMillis = reading.wallMillis + 10_000, elapsedMillis = 1000, bootCount = 14)
        val second = SettingsStore(disk) { now }.load()!!
        assertEquals(25_000L, BlockTimers.remainingMillis(second.timer, now))
        now = now.copy(wallMillis = reading.wallMillis + 20_000, elapsedMillis = 11_000)
        assertEquals(15_000L, BlockTimers.remainingMillis(second.timer, now))
    }

    @Test fun corruptPersistedAnchorsAreNeverAccepted() {
        val anchored = settings.copy(timer = timer.copy(anchor = TimerAnchor(13, 5000, 25_000)))
        val raw = SettingsCodec.encode(anchored)
        assertEquals(anchored, SettingsCodec.decode(raw))
        for ((field, value) in listOf("bootCount" to 11, "elapsedMillis" to -1,
            "remainingMillis" to -1, "remainingMillis" to 60_001,
            "remainingMillis" to "25000", "bootCount" to 13.5)) {
            val corrupt = JSONObject(raw).apply { getJSONObject("timer").getJSONObject("anchor").put(field, value) }
            assertNull("Invalid anchor $field=$value", SettingsCodec.decode(corrupt.toString()))
        }
        for (field in listOf("bootCount", "elapsedMillis", "remainingMillis")) {
            val missing = JSONObject(raw).apply { getJSONObject("timer").getJSONObject("anchor").remove(field) }
            assertNull(SettingsCodec.decode(missing.toString()))
        }
    }

}

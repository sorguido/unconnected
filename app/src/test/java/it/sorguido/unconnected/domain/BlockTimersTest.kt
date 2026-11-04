package it.sorguido.unconnected.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.ZonedDateTime

class BlockTimersTest {
    private val clock = TimerReading(1_800_000_000_000L, 10_000L, 12)
    private fun timer(seconds: Long = 60) = BlockTimers.start(seconds, clock)!!
    private val number = "+12025550101" // Reserved fictional number.
    private val rule = NumberRule("one", "Example", number)
    private val inside = ZonedDateTime.parse("2026-11-02T12:00:00+01:00[Europe/Rome]")
    private val outside = inside.withHour(1)
    private val base = Settings(numbers = listOf(rule))
    private fun blocks(settings: Settings, time: ZonedDateTime = inside, at: TimerReading = clock,
                       caller: String? = number, ready: Boolean = true) =
        Rules.shouldBlock(settings, caller, time, ready, at)

    @Test fun completeScheduleTimerTruthTableInsideAndOutsideWindows() {
        for (schedule in listOf(false, true)) for (activeTimer in listOf(false, true)) {
            val settings = base.copy(enabled = schedule, timer = if (activeTimer) timer() else null)
            assertEquals("inside schedule=$schedule timer=$activeTimer", activeTimer, blocks(settings))
            assertEquals("outside schedule=$schedule timer=$activeTimer", activeTimer || schedule, blocks(settings, outside))
        }
    }

    @Test fun expiryIsExactAndRestoresTheCurrentScheduleImmediately() {
        val settings = base.copy(timer = timer(), enabled = true)
        val before = clock.copy(elapsedMillis = clock.elapsedMillis + 59_999)
        val exact = clock.copy(elapsedMillis = clock.elapsedMillis + 60_000)
        assertTrue(blocks(settings, at = before))
        assertFalse(blocks(settings, at = exact))
        assertTrue(blocks(settings, outside, exact))
        assertFalse(blocks(settings.copy(enabled = false), outside, exact))
        assertEquals(1L, BlockTimers.remainingMillis(settings.timer, before))
        assertEquals(0L, BlockTimers.remainingMillis(settings.timer, exact))
        assertEquals(0L, BlockTimers.remainingMillis(settings.timer, exact.copy(elapsedMillis = Long.MAX_VALUE)))
    }

    @Test fun manualStopPreservesScheduleAndNumbers() {
        val active = base.copy(enabled = true, timer = timer())
        val stopped = active.copy(timer = null)
        assertEquals(active.copy(timer = null), stopped)
        assertFalse(blocks(stopped))
        assertTrue(blocks(stopped, outside))
        assertTrue(blocks(active.copy(enabled = false)))
        assertFalse(blocks(active.copy(enabled = false, timer = null), outside))
    }

    @Test fun editsToScheduleDuringTimerTakeEffectWhenTimerEnds() {
        val active = base.copy(enabled = true, timer = timer())
        val modified = active.copy(windows = listOf(TimeWindow("all-night", "", setOf(1), 0, 120)))
        assertTrue(blocks(modified, outside))
        val expired = clock.copy(elapsedMillis = clock.elapsedMillis + 60_000)
        assertFalse(blocks(modified, outside, expired))
        assertTrue(blocks(modified, inside, expired))
        assertEquals(active.timer, modified.timer)
    }

    @Test fun timerDoesNotRequireAConfirmedOrValidWeeklySchedule() {
        val variants = listOf(
            base.copy(windows = emptyList()),
            base.copy(enabled = true, windows = emptyList()),
            base.copy(windows = listOf(TimeWindow("broken", "", emptySet(), 20, 20))),
            base.copy(enabled = true, windows = listOf(TimeWindow("broken", "", setOf(8), -1, 2000))),
        )
        for (settings in variants) {
            assertTrue(Rules.isValidGeneral(settings))
            assertTrue(blocks(settings.copy(timer = timer())))
            assertFalse(blocks(settings))
            assertFalse(blocks(settings.copy(timer = timer()), at = clock.copy(elapsedMillis = 70_000)))
        }
    }

    @Test fun timerNeverBlocksDisabledUnlistedAbsentOrAmbiguousCallers() {
        val active = base.copy(timer = timer())
        assertFalse(blocks(active.copy(numbers = listOf(rule.copy(enabled = false)))))
        assertFalse(blocks(active.copy(numbers = emptyList())))
        for (caller in listOf(null, "", "private", "2025550101", "+12025550102", "+12025550101x4", "tel:$number", "+１２025550101")) {
            assertFalse("Unexpected timer block for $caller", blocks(active, caller = caller))
        }
        val uk = active.copy(numbers = listOf(NumberRule("uk", "", "+442079460000")))
        assertFalse(blocks(uk, caller = "+44 (0)20 7946 0000"))
        assertTrue(blocks(uk, caller = "+44 20 7946 0000"))
    }

    @Test fun masksRetainFixedLengthAndIndividualEnabledStateUnderTimer() {
        val mask = NumberRule("range", "", NumberMasks.parse("+120255501**", "US")!!)
        val settings = base.copy(numbers = listOf(mask), timer = timer())
        assertTrue(blocks(settings))
        assertTrue(blocks(settings, caller = "+12025550199"))
        assertFalse(blocks(settings, caller = "+12025550201"))
        assertFalse(blocks(settings, caller = "+120255501010"))
        assertFalse(blocks(settings.copy(numbers = listOf(mask.copy(enabled = false)))))
    }

    @Test fun timerStillRequiresPermissionsAndGeneralIntegrity() {
        val active = base.copy(timer = timer())
        assertFalse(blocks(active, ready = false))
        for (settings in listOf(active.copy(region = "ZZ"), active.copy(numbers = listOf(rule, rule)),
            active.copy(numbers = listOf(rule.copy(target = ExactTarget("+1202555")))),
            active.copy(timer = timer().copy(durationMillis = Long.MAX_VALUE)))) {
            assertFalse(Rules.isValidGeneral(settings))
            assertFalse(blocks(settings))
        }
    }

    @Test fun durationBoundariesAndExtremeValuesAreValidatedWithoutOverflow() {
        for (duration in listOf(Long.MIN_VALUE, -1L, 0L, BlockTimers.MAX_DURATION_SECONDS + 1, Long.MAX_VALUE)) {
            assertNull("Invalid duration $duration", BlockTimers.start(duration, clock))
        }
        assertEquals(1000L, timer(1).durationMillis)
        assertEquals(359_999_000L, timer(BlockTimers.MAX_DURATION_SECONDS).durationMillis)
        assertNull(BlockTimers.start(1, clock.copy(wallMillis = Long.MAX_VALUE)))
        assertNull(BlockTimers.start(1, clock.copy(elapsedMillis = Long.MAX_VALUE)))
        assertNull(BlockTimers.start(1, clock.copy(wallMillis = 0)))
        assertNull(BlockTimers.start(1, clock.copy(elapsedMillis = -1)))
        assertNull(BlockTimers.start(1, clock.copy(bootCount = null)))
        assertNull(BlockTimers.start(1, clock.copy(bootCount = -1)))
    }

    @Test fun sameBootIgnoresClockChangesAndTimezoneAndIncludesDeepSleep() {
        val active = timer()
        val progressed = clock.copy(elapsedMillis = 25_000)
        assertEquals(45_000L, BlockTimers.remainingMillis(active, progressed))
        assertEquals(45_000L, BlockTimers.remainingMillis(active, progressed.copy(wallMillis = clock.wallMillis - 86_400_000)))
        assertEquals(45_000L, BlockTimers.remainingMillis(active, progressed.copy(wallMillis = clock.wallMillis + 86_400_000)))
        assertEquals(0L, BlockTimers.remainingMillis(active, clock.copy(elapsedMillis = 70_000, wallMillis = clock.wallMillis - 86_400_000)))
    }

    @Test fun processRecreationUsesPersistedAnchorsWithoutAnyUiTicks() {
        val copied = timer().copy()
        assertEquals(25_000L, BlockTimers.remainingMillis(copied, clock.copy(elapsedMillis = 45_000)))
    }

    @Test fun rebootUsesOriginalWallDeadlineAndDoesNotRestartDuration() {
        val afterReboot = clock.copy(wallMillis = clock.wallMillis + 35_000, elapsedMillis = 5000, bootCount = 13)
        assertEquals(25_000L, BlockTimers.remainingMillis(timer(), afterReboot))
        assertEquals(0L, BlockTimers.remainingMillis(timer(), afterReboot.copy(wallMillis = clock.wallMillis + 60_000)))
        assertEquals(0L, BlockTimers.remainingMillis(timer(), afterReboot.copy(wallMillis = clock.wallMillis + 70_000)))
        assertEquals(10_000L, BlockTimers.remainingMillis(timer(), afterReboot.copy(wallMillis = clock.wallMillis + 50_000, bootCount = 14)))
    }

    @Test fun unknownBootOrInconsistentClocksFailOpenWithoutChangingSchedule() {
        val active = timer()
        val variants = listOf(clock.copy(bootCount = null), clock.copy(bootCount = 11),
            clock.copy(elapsedMillis = 9999), clock.copy(elapsedMillis = -1),
            clock.copy(bootCount = 13, wallMillis = clock.wallMillis - 1, elapsedMillis = 0),
            clock.copy(bootCount = 13, wallMillis = clock.wallMillis + 5000, elapsedMillis = 10_000))
        variants.forEach { assertEquals(0L, BlockTimers.remainingMillis(active, it)) }
        assertFalse(blocks(base.copy(timer = active), at = clock.copy(bootCount = null)))
        assertTrue(blocks(base.copy(enabled = true, timer = active), outside, clock.copy(bootCount = null)))
    }

    @Test fun nullAndCorruptTimerNeverBecomeActive() {
        assertEquals(0L, BlockTimers.remainingMillis(null, clock))
        for (invalid in listOf(timer().copy(durationMillis = 0), timer().copy(durationMillis = 1001),
            timer().copy(startedWallMillis = -1), timer().copy(startedElapsedMillis = -1), timer().copy(bootCount = -1))) {
            assertFalse(BlockTimers.isValid(invalid))
            assertEquals(0L, BlockTimers.remainingMillis(invalid, clock))
        }
    }
}

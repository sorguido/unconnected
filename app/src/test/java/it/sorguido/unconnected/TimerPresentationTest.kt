package it.sorguido.unconnected

import org.junit.Assert.*
import org.junit.Test

class TimerPresentationTest {
    @Test fun keypadProducesSecondsMinutesAndHoursWithoutOverflowOrSilentNormalization() {
        assertEquals(1L, timerEntrySeconds("1"))
        assertEquals(59L, timerEntrySeconds("59"))
        assertEquals(60L, timerEntrySeconds("100"))
        assertEquals(3600L, timerEntrySeconds("10000"))
        assertEquals(359999L, timerEntrySeconds("995959"))
        for (invalid in listOf("", "0", "000000", "60", "6000", "1000000", "-1", "1.0", " 1", "１２", "999999")) {
            assertNull(invalid, timerEntrySeconds(invalid))
        }
    }

    @Test fun keypadLeadingZerosDoNotExhaustCapacityAndSeventhDigitCannotChangeDuration() {
        var digits = ""
        for (digit in listOf(0, 0, 1, 2, 3, 4, 5, 6)) digits = appendTimerDigit(digits, digit)
        assertEquals("123456", digits)
        assertEquals("123456", appendTimerDigit(digits, 7))
        assertEquals("123456", appendTimerDigit(digits, -1))
        assertEquals(45296L, timerEntrySeconds(digits))
        assertEquals(5025L, timerEntrySeconds(digits.dropLast(1)))
    }

    @Test fun quickChoicesAreRealValidDurations() {
        for (minutes in listOf(1, 5, 10, 15)) assertEquals(minutes * 60L, timerEntrySeconds((minutes * 100).toString()))
    }

    @Test fun countdownNeverDisplaysZeroBeforeDeadline() {
        assertEquals("00:00:00", timerCountdown(-1))
        assertEquals("00:00:00", timerCountdown(0))
        assertEquals("00:00:01", timerCountdown(1))
        assertEquals("00:01:00", timerCountdown(59999))
        assertEquals("00:01:00", timerCountdown(60000))
        assertEquals("01:00:00", timerCountdown(3599999))
        assertEquals("99:59:59", timerCountdown(359999000))
    }

    @Test fun independentStatusesNeverClaimAllCallsAreAllowedJustBecauseScheduleIsOff() {
        assertEquals(FilterStatus.OFF, filterStatus(false, true, false))
        assertEquals(TimerStatus.ACTIVE, timerStatus(true, true, false))
        assertEquals(TimerStatus.OFF, timerStatus(false, true, false))
        assertEquals(TimerStatus.SETUP_REQUIRED, timerStatus(true, false, false))
        assertEquals(TimerStatus.SESSION_PAUSED, timerStatus(true, true, true))
        assertEquals(TimerStatus.OFF, timerStatus(false, false, true))
        assertEquals(FilterStatus.SCHEDULE_INVALID, filterStatus(true, true, false, false))
        assertEquals(FilterStatus.ACTIVE, filterStatus(true, true, false, true))
    }

    @Test fun savingStopHoldsCommittedTimerCopyThenShowsFailureOrCommittedOff() {
        val committed = timerStatus(true, true, false)
        val paused = timerStatus(true, true, true)
        assertEquals(TimerStatus.ACTIVE, cardStatus(paused, busy = true, held = committed))
        assertEquals(TimerStatus.SESSION_PAUSED, cardStatus(paused, busy = false, held = committed))
        val stopped = timerStatus(false, true, false)
        assertEquals(TimerStatus.ACTIVE, cardStatus(stopped, busy = true, held = committed))
        assertEquals(TimerStatus.OFF, cardStatus(stopped, busy = false, held = committed))
    }
}

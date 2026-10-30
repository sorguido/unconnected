package it.sorguido.unconnected.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class RulesTest {
    // Reserved fictional North American 555-01xx test numbers; no personal data.
    private val first = NumberRule("one", "Example", "+12025550101")
    private val second = NumberRule("two", "Example 2", "+12025550102")
    private val base = Settings(enabled = true, numbers = listOf(first))
    private fun at(value: String) = ZonedDateTime.parse(value)
    private fun monday(time: String) = at("2026-10-26T${time}:00+01:00[Europe/Rome]")
    private fun window(days: Set<Int>, start: Int, end: Int, id: String = "w") =
        TimeWindow(id, "", days, start, end)

    @Test fun defaultInstallAllowsEveryCall() {
        assertFalse(Settings().enabled)
        assertFalse(Rules.shouldBlock(Settings(numbers = listOf(first)), first.e164, monday("01:00"), true))
        assertFalse(Rules.shouldBlock(base.copy(numbers = emptyList()), first.e164, monday("01:00"), true))
    }

    @Test fun workingDayBoundariesAreStartInclusiveEndExclusive() {
        assertFalse(Rules.isAllowed(base.windows, monday("08:29")))
        assertTrue(Rules.isAllowed(base.windows, monday("08:30")))
        assertTrue(Rules.isAllowed(base.windows, monday("17:29")))
        assertFalse(Rules.isAllowed(base.windows, monday("17:30")))
        assertTrue(Rules.isAllowed(base.windows, monday("17:29").plusSeconds(59)))
    }

    @Test fun weeklyRecurrenceAndWeekendRollover() {
        assertFalse(Rules.isAllowed(base.windows, at("2026-10-25T12:00:00+01:00[Europe/Rome]")))
        assertTrue(Rules.isAllowed(base.windows, monday("12:00")))
        assertEquals(monday("08:30"), Rules.nextAllowed(base.windows,
            at("2026-10-23T18:00:00+02:00[Europe/Rome]")))
    }

    @Test fun overnightWindowBelongsToItsStartingDay() {
        val windows = listOf(window(setOf(7), 22 * 60, 6 * 60))
        assertTrue(Rules.isAllowed(windows, at("2026-10-25T22:00:00+01:00[Europe/Rome]")))
        assertTrue(Rules.isAllowed(windows, monday("05:59")))
        assertFalse(Rules.isAllowed(windows, monday("06:00")))
        assertFalse(Rules.isAllowed(windows, monday("22:00")))
    }

    @Test fun splitShiftsAndOverlapsUseUnionOfWindows() {
        val windows = listOf(window(setOf(1), 480, 720, "a"), window(setOf(1), 780, 1020, "b"))
        assertTrue(Rules.isAllowed(windows, monday("11:59")))
        assertFalse(Rules.isAllowed(windows, monday("12:00")))
        assertTrue(Rules.isAllowed(windows, monday("13:00")))
        assertEquals(monday("13:00"), Rules.nextAllowed(windows, monday("12:05")))
        assertTrue(Rules.isAllowed(windows + window(setOf(1), 700, 800, "c"), monday("12:30")))
    }

    @Test fun allUncertainCallerIdentitiesFailOpen() {
        val invalid = listOf(null, "", "private", "anonymous", "2025550101", "0012025550101",
            "tel:+12025550101", "+12025550101;ext=4", "+12025550101x4", "+12025550101,4",
            "+1-800-FLOWERS", "+１２025550101", "+1202555010", "+12025550101#")
        invalid.forEach { assertFalse("Unexpected block: $it", Rules.shouldBlock(base, it, monday("01:00"), true)) }
    }

    @Test fun strictFullNumberMatchAndIndividualEnableState() {
        assertTrue(Rules.shouldBlock(base, first.e164, monday("01:00"), true))
        assertTrue(Rules.shouldBlock(base, "+1 (202) 555-0101", monday("01:00"), true))
        assertFalse(Rules.shouldBlock(base, second.e164, monday("01:00"), true))
        assertFalse(Rules.shouldBlock(base, first.e164, monday("12:00"), true))
        val multiple = base.copy(numbers = listOf(first.copy(enabled = false), second))
        assertFalse(Rules.shouldBlock(multiple, first.e164, monday("01:00"), true))
        assertTrue(Rules.shouldBlock(multiple, second.e164, monday("01:00"), true))
    }

    @Test fun incomingInternationalTrunkPrefixIsNotSilentlyRepaired() {
        // Ofcom reserved London drama range, not a personal number.
        val uk = base.copy(numbers = listOf(NumberRule("uk", "Example UK", "+442079460000")))
        assertTrue(Rules.shouldBlock(uk, "+44 20 7946 0000", monday("01:00"), true))
        assertFalse(Rules.shouldBlock(uk, "+44 (0)20 7946 0000", monday("01:00"), true))
    }

    @Test fun masterAndPermissionsAlwaysOverrideBlocking() {
        assertFalse(Rules.shouldBlock(base.copy(enabled = false), first.e164, monday("01:00"), true))
        assertFalse(Rules.shouldBlock(base, first.e164, monday("01:00"), false))
    }

    @Test fun emptyScheduleRequiresExplicitConfirmation() {
        val empty = base.copy(windows = emptyList())
        assertFalse(Rules.shouldBlock(empty, first.e164, monday("01:00"), true))
        assertTrue(Rules.shouldBlock(empty.copy(emptyScheduleConfirmed = true), first.e164, monday("01:00"), true))
        assertNull(Rules.nextAllowed(emptyList(), monday("01:00")))
    }

    @Test fun corruptConfigurationFailsOpen() {
        val corrupt = listOf(
            base.copy(region = "ZZ"),
            base.copy(numbers = listOf(first, first.copy(id = "duplicate-number"))),
            base.copy(numbers = listOf(first, second.copy(id = first.id))),
            base.copy(numbers = listOf(first.copy(e164 = "2025550101"))),
            base.copy(numbers = listOf(first.copy(label = "x".repeat(81)))),
            base.copy(windows = listOf(window(emptySet(), 400, 500))),
            base.copy(windows = listOf(window(setOf(0), 400, 500))),
            base.copy(windows = listOf(window(setOf(1), 400, 400))),
            base.copy(windows = listOf(window(setOf(1), -1, 500))),
            base.copy(windows = listOf(window(setOf(1), 400, 1440))),
            base.copy(windows = listOf(base.windows.first(), base.windows.first())),
            base.copy(windows = List(101) { window(setOf(1), 400, 500, "$it") }),
            base.copy(numbers = List(201) { first.copy(id = "$it") }),
        )
        corrupt.forEach {
            assertFalse(Rules.isValid(it))
            assertFalse(Rules.shouldBlock(it, first.e164, monday("01:00"), true))
        }
    }

    @Test fun springGapUsesActualLocalTimeAndNextExistentOpening() {
        val windows = listOf(window(setOf(7), 150, 195)) // 02:30–03:15
        val before = at("2026-03-29T01:59:00+01:00[Europe/Rome]")
        val after = at("2026-03-29T03:00:00+02:00[Europe/Rome]")
        assertFalse(Rules.isAllowed(windows, before))
        assertTrue(Rules.isAllowed(windows, after))
        assertEquals(after, Rules.nextAllowed(windows, before))
        val entirelySkipped = listOf(window(setOf(7), 150, 165))
        assertEquals(at("2026-04-05T02:30:00+02:00[Europe/Rome]"),
            Rules.nextAllowed(entirelySkipped, before))
    }

    @Test fun repeatedHourAllowsBothOffsetsAndFindsSecondOpening() {
        val windows = listOf(window(setOf(7), 135, 150))
        val firstOpening = at("2026-10-25T02:15:00+02:00[Europe/Rome]")
        val secondOpening = at("2026-10-25T02:15:00+01:00[Europe/Rome]")
        assertTrue(Rules.isAllowed(windows, firstOpening))
        assertTrue(Rules.isAllowed(windows, secondOpening))
        assertEquals(secondOpening, Rules.nextAllowed(windows, firstOpening.plusMinutes(20)))
    }

    @Test fun fallbackCanReenterAlreadyStartedWindow() {
        val windows = listOf(window(setOf(7), 90, 150)) // 01:30–02:30
        val beforeFallback = at("2026-10-25T02:45:00+02:00[Europe/Rome]")
        assertEquals(at("2026-10-25T02:00:00+01:00[Europe/Rome]"),
            Rules.nextAllowed(windows, beforeFallback))
    }

    @Test fun deviceTimezoneAtCallArrivalControlsSchedule() {
        val rome = monday("09:00")
        val newYork = rome.withZoneSameInstant(ZoneId.of("America/New_York"))
        assertTrue(Rules.isAllowed(base.windows, rome))
        assertFalse(Rules.isAllowed(base.windows, newYork))
        assertEquals(rome, Rules.nextAllowed(base.windows, rome))
    }

    @Test fun invalidScheduleCannotBePresentedAsNextOpening() {
        val invalid = listOf(window(setOf(1), 10, 10))
        assertTrue(Rules.isAllowed(invalid, monday("01:00")))
        assertNull(Rules.nextAllowed(invalid, monday("01:00")))
    }
    @Test fun allowedUntilFindsEndOfOverlappingUnionAndSplitShift() {
        val windows = listOf(window(setOf(1), 480, 720, "morning"),
            window(setOf(1), 700, 800, "overlap"), window(setOf(1), 900, 1020, "afternoon"))
        assertEquals(monday("13:20"), Rules.allowedUntil(windows, monday("09:00")))
        assertEquals(monday("17:00"), Rules.allowedUntil(windows, monday("15:00")))
        assertNull(Rules.allowedUntil(windows, monday("14:00")))
    }

    @Test fun allowedUntilFindsYesterdayOvernightEnd() {
        val windows = listOf(window(setOf(7), 1320, 360))
        assertEquals(monday("06:00"), Rules.allowedUntil(windows, monday("01:00")))
        assertEquals(monday("06:00"), Rules.allowedUntil(windows,
            at("2026-10-25T22:00:00+01:00[Europe/Rome]")))
    }

    @Test fun allowedUntilDistinguishesRepeatedHourIntervals() {
        val windows = listOf(window(setOf(7), 135, 150))
        assertEquals(at("2026-10-25T02:30:00+02:00[Europe/Rome]"),
            Rules.allowedUntil(windows, at("2026-10-25T02:15:00+02:00[Europe/Rome]")))
        assertEquals(at("2026-10-25T02:30:00+01:00[Europe/Rome]"),
            Rules.allowedUntil(windows, at("2026-10-25T02:15:00+01:00[Europe/Rome]")))
        val spanningFallback = listOf(window(setOf(7), 135, 195))
        // The clock moves back to 02:00, outside 02:15–03:15, before reaching 03:15.
        assertEquals(at("2026-10-25T02:00:00+01:00[Europe/Rome]"),
            Rules.allowedUntil(spanningFallback, at("2026-10-25T02:20:00+02:00[Europe/Rome]")))
    }

    @Test fun allowedUntilHandlesSkippedEndInSpringGap() {
        val windows = listOf(window(setOf(7), 90, 150))
        assertEquals(at("2026-03-29T03:00:00+02:00[Europe/Rome]"),
            Rules.allowedUntil(windows, at("2026-03-29T01:45:00+01:00[Europe/Rome]")))
    }

    @Test fun continuouslyAllowedUnionHasNoEnd() {
        val windows = listOf(window((1..7).toSet(), 0, 720, "am"),
            window((1..7).toSet(), 720, 0, "pm"))
        assertNull(Rules.allowedUntil(windows, monday("09:00")))
        assertNull(Rules.allowedUntil(emptyList(), monday("09:00")))
        assertNull(Rules.allowedUntil(listOf(window(setOf(1), 5, 5)), monday("09:00")))
    }

}

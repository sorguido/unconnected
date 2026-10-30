package it.sorguido.unconnected.domain

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.time.LocalDateTime
import java.time.ZonedDateTime

/** All values are immutable snapshots; weekdays use ISO Monday=1 through Sunday=7. */
data class NumberRule(val id: String, val label: String, val e164: String, val enabled: Boolean = true)
data class TimeWindow(
    val id: String,
    val label: String,
    val days: Set<Int>,
    val startMinute: Int,
    val endMinute: Int,
)
data class Settings(
    val enabled: Boolean = false,
    val region: String = "IT",
    val numbers: List<NumberRule> = emptyList(),
    val windows: List<TimeWindow> = listOf(
        TimeWindow("default", "", setOf(1, 2, 3, 4, 5), 8 * 60 + 30, 17 * 60 + 30),
    ),
    val emptyScheduleConfirmed: Boolean = false,
)

object Numbers {
    private val phoneUtil by lazy { PhoneNumberUtil.getInstance() }
    private val lexical = Regex("\\+?[0-9(][0-9 ()\\-.]*")
    private val canonical = Regex("\\+[1-9][0-9]{6,14}")

    fun isSupportedRegion(region: String): Boolean = region in phoneUtil.supportedRegions

    /** No extensions, vanity letters, URI parameters, Unicode digits or guessed suffixes. */
    fun normalize(raw: String, region: String): String? {
        if (raw.length > 80 || !isSupportedRegion(region)) return null
        val input = raw.trim()
        if (!lexical.matches(input) || !validGrouping(input)) return null
        return try {
            val parsed = phoneUtil.parse(input, region)
            if (parsed.hasExtension() || !phoneUtil.isValidNumber(parsed)) null
            else phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
                .takeIf(canonical::matches)
        } catch (_: NumberParseException) {
            null
        }
    }

    private fun validGrouping(input: String): Boolean {
        var inside = false
        var groupDigits = 0
        for (character in input) {
            when (character) {
                '(' -> {
                    if (inside) return false
                    inside = true
                    groupDigits = 0
                }
                ')' -> {
                    if (!inside || groupDigits == 0) return false
                    inside = false
                }
                in '0'..'9' -> if (inside) groupDigits++
                else -> if (inside) return false
            }
        }
        return !inside && input.lastOrNull() !in listOf('-', '.', '(')
    }

    fun isCanonical(raw: String, region: String): Boolean =
        canonical.matches(raw) && normalize(raw, region) == raw
}

object Rules {
    const val MAX_NUMBERS = 200
    const val MAX_WINDOWS = 100
    const val MAX_LABEL_LENGTH = 80
    private fun validId(value: String) = value.isNotBlank() && value.length <= 80

    fun isValidWindow(window: TimeWindow): Boolean =
        validId(window.id) && window.label.length <= MAX_LABEL_LENGTH &&
            window.days.isNotEmpty() && window.days.all { it in 1..7 } &&
            window.startMinute in 0..1439 && window.endMinute in 0..1439 &&
            window.startMinute != window.endMinute

    fun isValid(settings: Settings): Boolean =
        settings.numbers.size <= MAX_NUMBERS && settings.windows.size <= MAX_WINDOWS &&
            Numbers.isSupportedRegion(settings.region) &&
            settings.numbers.all {
                validId(it.id) && it.label.length <= MAX_LABEL_LENGTH &&
                    Numbers.isCanonical(it.e164, settings.region)
            } &&
            settings.numbers.map { it.id }.distinct().size == settings.numbers.size &&
            settings.numbers.map { it.e164 }.distinct().size == settings.numbers.size &&
            validWindows(settings.windows)

    private fun validWindows(windows: List<TimeWindow>): Boolean =
        windows.size <= MAX_WINDOWS && windows.all(::isValidWindow) &&
            windows.map { it.id }.distinct().size == windows.size

    /** Local wall-clock semantics: both instances of a repeated DST hour are allowed. */
    fun isAllowed(windows: List<TimeWindow>, now: ZonedDateTime): Boolean {
        // Invalid schedules are uncertainty, so allow instead of filtering.
        if (!validWindows(windows)) return true
        val day = now.dayOfWeek.value
        val previousDay = if (day == 1) 7 else day - 1
        val minute = now.hour * 60 + now.minute
        return windows.any { window ->
            if (window.startMinute < window.endMinute) {
                day in window.days && minute >= window.startMinute && minute < window.endMinute
            } else {
                (day in window.days && minute >= window.startMinute) ||
                    (previousDay in window.days && minute < window.endMinute)
            }
        }
    }

    /** ready means the required role and permission have both been observed as granted. */
    fun shouldBlock(settings: Settings, caller: String?, now: ZonedDateTime, ready: Boolean): Boolean {
        if (!ready || !settings.enabled || caller == null || !caller.startsWith('+')) return false
        if (!isValid(settings)) return false
        if (settings.windows.isEmpty() && !settings.emptyScheduleConfirmed) return false
        val normalized = Numbers.normalize(caller, settings.region) ?: return false
        // Incoming international IDs must already contain the exact E.164 digits.
        // Parsers may silently repair a redundant national trunk prefix; that is ambiguous.
        val presentedDigits = caller.filter { it == '+' || it in '0'..'9' }
        if (presentedDigits != normalized) return false
        if (settings.numbers.none { it.enabled && it.e164 == normalized }) return false
        return !isAllowed(settings.windows, now)
    }

    /** Next opening at an actual instant, including DST gaps and repeated hours. No scheduler. */
    fun nextAllowed(windows: List<TimeWindow>, now: ZonedDateTime): ZonedDateTime? {
        if (!validWindows(windows) || windows.isEmpty()) return null
        if (isAllowed(windows, now)) return now
        return boundaryCandidates(windows, now).asSequence()
            .filter { it.isAfter(now) && isAllowed(windows, it) }
            .minByOrNull { it.toInstant() }
    }

    /** End of the current union of allowed windows; null if continuously allowed or invalid. */
    fun allowedUntil(windows: List<TimeWindow>, from: ZonedDateTime): ZonedDateTime? {
        if (!validWindows(windows) || windows.isEmpty() || !isAllowed(windows, from)) return null
        return boundaryCandidates(windows, from).asSequence()
            .filter { it.isAfter(from) && !isAllowed(windows, it) }
            .minByOrNull { it.toInstant() }
    }

    private fun boundaryCandidates(windows: List<TimeWindow>, now: ZonedDateTime): List<ZonedDateTime> {
        val zoneRules = now.zone.rules
        val candidates = mutableListOf<ZonedDateTime>()
        fun addLocal(local: LocalDateTime) {
            val offsets = zoneRules.getValidOffsets(local)
            if (offsets.isEmpty()) {
                // A gap may start or end a window at the first existent local minute.
                zoneRules.getTransition(local)?.let { candidates += it.instant.atZone(now.zone) }
            } else offsets.forEach { candidates += ZonedDateTime.ofLocal(local, now.zone, it) }
        }
        // Include yesterday's overnight ends and one full future weekly recurrence.
        for (offset in -1L..8L) {
            val date = now.toLocalDate().plusDays(offset)
            for (window in windows) {
                if (date.dayOfWeek.value !in window.days) continue
                addLocal(date.atTime(window.startMinute / 60, window.startMinute % 60))
                val endDate = if (window.startMinute > window.endMinute) date.plusDays(1) else date
                addLocal(endDate.atTime(window.endMinute / 60, window.endMinute % 60))
            }
        }
        val end = now.plusDays(8).toInstant()
        var transition = zoneRules.nextTransition(now.toInstant())
        while (transition != null && transition.instant.isBefore(end)) {
            candidates += transition.instant.atZone(now.zone)
            transition = zoneRules.nextTransition(transition.instant.plusSeconds(1))
        }
        return candidates.filter { !it.toInstant().isAfter(end) }
    }
}

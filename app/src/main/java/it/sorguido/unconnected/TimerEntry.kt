package it.sorguido.unconnected

import java.util.Locale

/** Six keypad digits in HHMMSS order, without normalizing invalid minutes or seconds. */
internal fun timerEntrySeconds(digits: String): Long? {
    if (digits.length !in 1..6 || digits.any { it !in '0'..'9' }) return null
    val padded = digits.padStart(6, '0')
    val hours = padded.substring(0, 2).toLong()
    val minutes = padded.substring(2, 4).toLong()
    val seconds = padded.substring(4, 6).toLong()
    if (minutes > 59 || seconds > 59) return null
    return (hours * 3600 + minutes * 60 + seconds).takeIf { it > 0 }
}

internal fun timerCountdown(millis: Long): String {
    // Round up: 00:00:00 appears only once the effective deadline has been reached.
    val seconds = millis.coerceAtLeast(0) / 1000 + if (millis > 0 && millis % 1000 != 0L) 1 else 0
    return String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
}

internal fun appendTimerDigit(digits: String, digit: Int): String =
    if (digit in 0..9 && digits.length < 6) (digits + digit).trimStart('0') else digits

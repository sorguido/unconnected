package it.sorguido.unconnected

/** Presentation only: one sampled state supplies both heading and explanation. */
internal enum class FilterStatus(val title: Int, val detail: Int) {
    OFF(R.string.filtering_off, R.string.off_detail),
    ACTIVE(R.string.filtering_on, R.string.on_detail),
    SESSION_PAUSED(R.string.state_paused, R.string.session_paused_detail),
    SETUP_REQUIRED(R.string.state_paused, R.string.paused_detail),
    SCHEDULE_INVALID(R.string.schedule_paused, R.string.schedule_invalid),
}

internal fun filterStatus(
    enabled: Boolean,
    permissionsReady: Boolean,
    sessionPaused: Boolean,
    scheduleValid: Boolean = true,
): FilterStatus = when {
    !enabled -> FilterStatus.OFF
    sessionPaused -> FilterStatus.SESSION_PAUSED
    !permissionsReady -> FilterStatus.SETUP_REQUIRED
    !scheduleValid -> FilterStatus.SCHEDULE_INVALID
    else -> FilterStatus.ACTIVE
}

/** An in-flight save reports itself outside the card, so the committed copy stays until it completes. */
internal fun <T> cardStatus(committed: T, busy: Boolean, held: T): T =
    if (busy) held else committed

internal enum class TimerStatus(val title: Int, val detail: Int) {
    OFF(R.string.timer_off, R.string.timer_off_detail),
    ACTIVE(R.string.timer_active, R.string.timer_active_detail),
    SESSION_PAUSED(R.string.timer_paused, R.string.session_paused_detail),
    SETUP_REQUIRED(R.string.timer_paused, R.string.paused_detail),
}

internal fun timerStatus(active: Boolean, permissionsReady: Boolean, sessionPaused: Boolean): TimerStatus = when {
    !active -> TimerStatus.OFF
    sessionPaused -> TimerStatus.SESSION_PAUSED
    !permissionsReady -> TimerStatus.SETUP_REQUIRED
    else -> TimerStatus.ACTIVE
}

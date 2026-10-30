package it.sorguido.unconnected

/** Presentation only: one sampled state supplies both heading and explanation. */
internal enum class FilterStatus(val title: Int, val detail: Int) {
    OFF(R.string.filtering_off, R.string.off_detail),
    ACTIVE(R.string.filtering_on, R.string.on_detail),
    SESSION_PAUSED(R.string.state_paused, R.string.session_paused_detail),
    SETUP_REQUIRED(R.string.state_paused, R.string.paused_detail),
}

internal fun filterStatus(
    enabled: Boolean,
    permissionsReady: Boolean,
    sessionPaused: Boolean,
): FilterStatus = when {
    !enabled -> FilterStatus.OFF
    sessionPaused -> FilterStatus.SESSION_PAUSED
    !permissionsReady -> FilterStatus.SETUP_REQUIRED
    else -> FilterStatus.ACTIVE
}

/** An in-flight save reports itself outside the card, so the committed copy stays until it completes. */
internal fun cardStatus(committed: FilterStatus, busy: Boolean, held: FilterStatus): FilterStatus =
    if (busy) held else committed

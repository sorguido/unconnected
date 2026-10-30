package it.sorguido.unconnected

/** Presentation only: one sampled state supplies both heading and explanation. */
internal enum class FilterStatus(val title: Int, val detail: Int) {
    SAVING(R.string.state_saving, R.string.saving_detail),
    OFF(R.string.filtering_off, R.string.off_detail),
    ACTIVE(R.string.filtering_on, R.string.on_detail),
    SESSION_PAUSED(R.string.state_paused, R.string.session_paused_detail),
    SETUP_REQUIRED(R.string.state_paused, R.string.paused_detail),
}

internal fun filterStatus(
    enabled: Boolean,
    permissionsReady: Boolean,
    saving: Boolean,
    sessionPaused: Boolean,
): FilterStatus = when {
    saving -> FilterStatus.SAVING
    !enabled -> FilterStatus.OFF
    sessionPaused -> FilterStatus.SESSION_PAUSED
    !permissionsReady -> FilterStatus.SETUP_REQUIRED
    else -> FilterStatus.ACTIVE
}

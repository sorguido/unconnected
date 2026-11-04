package it.sorguido.unconnected.domain

/** Persisted duration and two clock anchors; the visual countdown never controls screening. */
data class BlockTimer(
    val startedWallMillis: Long,
    val startedElapsedMillis: Long,
    val durationMillis: Long,
    val bootCount: Int,
    val anchor: TimerAnchor? = null,
)

/** Re-established atomically after reboot, or zeroed permanently after observed expiry. */
data class TimerAnchor(val bootCount: Int, val elapsedMillis: Long, val remainingMillis: Long)

data class TimerReading(val wallMillis: Long, val elapsedMillis: Long, val bootCount: Int?)

object BlockTimers {
    const val MAX_DURATION_SECONDS = 99L * 3600L + 59L * 60L + 59L
    private const val MAX_DURATION_MILLIS = MAX_DURATION_SECONDS * 1000L

    fun isValid(timer: BlockTimer): Boolean =
        timer.startedWallMillis > 0 && timer.startedElapsedMillis >= 0 && timer.bootCount >= 0 &&
            timer.durationMillis in 1000L..MAX_DURATION_MILLIS && timer.durationMillis % 1000L == 0L &&
            timer.startedWallMillis <= Long.MAX_VALUE - timer.durationMillis &&
            timer.startedElapsedMillis <= Long.MAX_VALUE - timer.durationMillis &&
            (timer.anchor == null || (timer.anchor.bootCount >= timer.bootCount &&
                timer.anchor.elapsedMillis >= 0 && timer.anchor.remainingMillis in 0L..timer.durationMillis &&
                timer.anchor.elapsedMillis <= Long.MAX_VALUE - timer.anchor.remainingMillis))

    fun start(durationSeconds: Long, now: TimerReading): BlockTimer? {
        if (durationSeconds !in 1L..MAX_DURATION_SECONDS || now.bootCount == null) return null
        return BlockTimer(now.wallMillis, now.elapsedMillis, durationSeconds * 1000L, now.bootCount)
            .takeIf(::isValid)
    }

    /**
     * Elapsed realtime includes deep sleep and ignores wall-clock changes within one boot.
     * After reboot use the original wall deadline, only when both clocks remain consistent:
     * time since start must cover this boot's uptime. Uncertain/rolled-back clocks fail open.
     * SettingsStore durably reanchors the first observation after reboot, then uses elapsed
     * realtime again. No receiver, alarm, background polling or expiry wakeup is necessary.
     */
    fun remainingMillis(timer: BlockTimer?, now: TimerReading): Long {
        if (timer == null || !isValid(timer) || now.wallMillis <= 0 || now.elapsedMillis < 0 ||
            now.bootCount == null || now.bootCount < timer.bootCount) return 0
        val sourceBoot = timer.anchor?.bootCount ?: timer.bootCount
        val sourceElapsed = timer.anchor?.elapsedMillis ?: timer.startedElapsedMillis
        val sourceRemaining = timer.anchor?.remainingMillis ?: timer.durationMillis
        if (sourceRemaining == 0L || now.bootCount < sourceBoot) return 0
        val remaining = if (now.bootCount == sourceBoot) {
            if (now.elapsedMillis < sourceElapsed) return 0
            sourceRemaining - (now.elapsedMillis - sourceElapsed)
        } else {
            if (now.wallMillis < timer.startedWallMillis) return 0
            val wallPassed = now.wallMillis - timer.startedWallMillis
            if (wallPassed < now.elapsedMillis) return 0
            minOf(timer.durationMillis - wallPassed, sourceRemaining)
        }
        return remaining.coerceAtLeast(0)
    }

    /** Preserve original identity/deadline while making this boot monotonic across processes. */
    fun reanchor(timer: BlockTimer, now: TimerReading): BlockTimer {
        if (!isValid(timer) || now.bootCount == null || now.bootCount < timer.bootCount ||
            now.elapsedMillis < 0 || now.wallMillis <= 0 || timer.anchor?.remainingMillis == 0L) return timer
        val sourceBoot = timer.anchor?.bootCount ?: timer.bootCount
        if (now.bootCount < sourceBoot) return timer
        val remaining = remainingMillis(timer, now)
        return if (now.bootCount != sourceBoot || remaining == 0L)
            timer.copy(anchor = TimerAnchor(now.bootCount, now.elapsedMillis, remaining)) else timer
    }

    fun sameIdentity(first: BlockTimer, second: BlockTimer): Boolean =
        first.copy(anchor = null) == second.copy(anchor = null)
}

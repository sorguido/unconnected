package it.sorguido.unconnected

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import it.sorguido.unconnected.domain.TimerReading

/** Read-only platform clock sampling; no new permission or device setting change. */
object TimerClock {
    fun sample(): TimerReading = TimerReading(System.currentTimeMillis(), SystemClock.elapsedRealtime(), null)

    fun read(context: Context): TimerReading = identify(context, sample())

    fun identify(context: Context, sampled: TimerReading): TimerReading {
        val bootCount = runCatching {
            Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
                .takeIf { it >= 0 }
        }.getOrNull()
        return sampled.copy(bootCount = bootCount)
    }

    /** Boot identity cannot change while this process survives; avoid a second provider read. */
    fun advance(previous: TimerReading): TimerReading =
        TimerReading(System.currentTimeMillis(), SystemClock.elapsedRealtime(), previous.bootCount)
}

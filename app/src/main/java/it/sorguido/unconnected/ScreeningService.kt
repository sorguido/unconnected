package it.sorguido.unconnected

import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.TelecomManager
import it.sorguido.unconnected.domain.Rules
import java.time.ZonedDateTime
import java.util.concurrent.Executors

class ScreeningService : CallScreeningService() {
    private val main = Handler(Looper.getMainLooper())

    private inner class PendingResponse(private val details: Call.Details) {
        private var answered = false
        val deadline = Runnable { answer(false) }
        fun answer(block: Boolean) {
            if (answered) return
            answered = true
            main.removeCallbacks(deadline)
            respondToCall(details, CallResponse.Builder()
                .setDisallowCall(block).setRejectCall(false).setSkipNotification(block).build())
        }
    }

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) return
        // Budget three seconds for a fail-open response, independently of disk/parser work.
        // Actual Android dispatch timing still requires physical-device verification.
        val response = PendingResponse(callDetails)
        main.postDelayed(response.deadline, 3000)
        val arrived = ZonedDateTime.now()
        val revision = SettingsStore.revision.get()
        val handle = callDetails.handle
        val caller = if (callDetails.handlePresentation == TelecomManager.PRESENTATION_ALLOWED && handle?.scheme == "tel") handle.schemeSpecificPart else null
        worker.execute {
            val block = runCatching {
                val settings = SettingsStore(this).load() ?: return@runCatching false
                Rules.shouldBlock(settings, caller, arrived, ScreeningAccess.ready(this))
            }.getOrDefault(false)
            main.post {
                response.answer(block && !SettingsStore.sessionPaused.get() && revision == SettingsStore.revision.get() && ScreeningAccess.ready(this))
            }
        }
    }

    companion object {
        private val worker = Executors.newSingleThreadExecutor { work -> Thread(work, "screening-decision").apply { isDaemon = true } }
    }
}

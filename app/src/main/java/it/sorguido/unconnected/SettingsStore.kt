package it.sorguido.unconnected

import android.content.Context
import android.util.AtomicFile
import it.sorguido.unconnected.domain.Settings
import it.sorguido.unconnected.domain.Rules
import it.sorguido.unconnected.domain.BlockTimers
import it.sorguido.unconnected.domain.TimerReading
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One bounded atomic document; no contacts queries, call history, network or backups. */
/** The storage contract makes failed atomic saves and retries testable without Android. */
internal interface SettingsStorage {
    /** null means a genuinely absent document; damaged/failed reads must throw. */
    fun read(): ByteArray?
    /** Either commits all bytes or throws while preserving the preceding document. */
    fun writeAtomic(bytes: ByteArray)
}

private class AndroidSettingsStorage(context: Context) : SettingsStorage {
    private val file = AtomicFile(File(context.noBackupFilesDir, "rules.json"))

    override fun read(): ByteArray? = try {
        file.openRead().use { stream ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            while (output.size() <= SettingsCodec.MAX_BYTES) {
                val count = stream.read(buffer, 0, minOf(buffer.size, SettingsCodec.MAX_BYTES + 1 - output.size()))
                if (count < 0) break
                output.write(buffer, 0, count)
            }
            output.toByteArray().also { require(it.size <= SettingsCodec.MAX_BYTES) }
        }
    } catch (error: java.io.FileNotFoundException) {
        if (file.baseFile.exists()) throw error else null
    }

    override fun writeAtomic(bytes: ByteArray) {
        var output: java.io.FileOutputStream? = null
        try {
            output = file.startWrite()
            output.write(bytes)
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }
}

class SettingsStore internal constructor(
    private val storage: SettingsStorage,
    private val clock: (() -> TimerReading)? = null,
) {
    constructor(context: Context) : this(AndroidSettingsStorage(context), { TimerClock.read(context) })

    private fun readSnapshot(): Settings? {
        val bytes = storage.read() ?: return Settings()
        if (bytes.size > SettingsCodec.MAX_BYTES) return null
        return SettingsCodec.decode(String(bytes, Charsets.UTF_8))
    }

    private fun normalized(settings: Settings): Settings {
        val timer = settings.timer ?: return settings
        val now = clock?.invoke() ?: return settings
        return settings.copy(timer = BlockTimers.reanchor(timer, now))
    }

    private fun writeSnapshot(settings: Settings) {
        require(Rules.isValidGeneral(settings))
        val raw = SettingsCodec.encode(settings).toByteArray(Charsets.UTF_8)
        require(raw.size <= SettingsCodec.MAX_BYTES)
        storage.writeAtomic(raw)
    }

    fun load(): Settings? = synchronized(lock) {
        try {
            val original = readSnapshot() ?: return@synchronized null
            val current = normalized(original)
            if (current != original) {
                // Persist a new boot's monotonic anchor before exposing it to any caller/UI.
                // Also retain an observed-expiry tombstone, so wall rollback cannot revive it.
                writeSnapshot(current)
            }
            current
        } catch (_: Exception) {
            // A failed clock-anchor commit must never leave a wall-dependent blocking timer.
            setSessionPaused(true)
            revision.incrementAndGet()
            null
        }
    }

    fun save(settings: Settings): Boolean = synchronized(lock) {
        beginUpdate()
        try {
            if (!Rules.isValidGeneral(settings)) return@synchronized false
            val latest = readSnapshot()
            // User edits carry their loaded timer. Preserve a newer disk anchor/tombstone
            // for the same timer; only explicit stop or a genuinely new start replaces it.
            val candidate = if (settings.timer != null && latest?.timer != null &&
                BlockTimers.sameIdentity(settings.timer, latest.timer)) {
                settings.copy(timer = settings.timer.copy(anchor = latest.timer.anchor))
            } else settings
            writeSnapshot(normalized(candidate))
            setSessionPaused(false)
            true
        } catch (_: Exception) { false }
    }

    companion object {
        private val lock = Any()
        internal val revision = AtomicLong(0)
        val sessionPaused = AtomicBoolean(false)
        private val pauseUpdates = MutableStateFlow(false)
        val sessionPauseFlow = pauseUpdates.asStateFlow()
        // A short publication lock, separate from the disk lock, keeps both readers aligned.
        @Synchronized
        private fun setSessionPaused(paused: Boolean) {
            sessionPaused.set(paused)
            pauseUpdates.value = paused
        }
        fun beginUpdate() {
            setSessionPaused(true)
            revision.incrementAndGet()
        }
    }
}

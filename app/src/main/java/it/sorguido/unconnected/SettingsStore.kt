package it.sorguido.unconnected

import android.content.Context
import android.util.AtomicFile
import it.sorguido.unconnected.domain.Settings
import it.sorguido.unconnected.domain.Rules
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicBoolean

/** One bounded atomic document; no contacts queries, call history, network or backups. */
class SettingsStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "rules.json"))

    fun load(): Settings? = synchronized(lock) {
        try {
            val raw = file.openRead().use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (output.size() <= SettingsCodec.MAX_BYTES) {
                    val count = stream.read(buffer, 0, minOf(buffer.size, SettingsCodec.MAX_BYTES + 1 - output.size()))
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
                val bytes = output.toByteArray()
                if (bytes.size > SettingsCodec.MAX_BYTES) return@synchronized null
                String(bytes, Charsets.UTF_8)
            }
            SettingsCodec.decode(raw)
        } catch (_: java.io.FileNotFoundException) {
            if (file.baseFile.exists()) null else Settings()
        } catch (_: Exception) { null }
    }

    fun save(settings: Settings): Boolean = synchronized(lock) {
        beginUpdate()
        if (!Rules.isValid(settings)) return@synchronized false
        val raw = SettingsCodec.encode(settings).toByteArray(Charsets.UTF_8)
        if (raw.size > SettingsCodec.MAX_BYTES) return@synchronized false
        var output: java.io.FileOutputStream? = null
        try {
            output = file.startWrite()
            output.write(raw)
            file.finishWrite(output)
            sessionPaused.set(false)
            true
        } catch (_: Exception) {
            file.failWrite(output)
            false
        }
    }

    companion object {
        private val lock = Any()
        internal val revision = AtomicLong(0)
        val sessionPaused = AtomicBoolean(false)
        fun beginUpdate() {
            sessionPaused.set(true)
            revision.incrementAndGet()
        }
    }
}

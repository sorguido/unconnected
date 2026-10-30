package it.sorguido.unconnected

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class FilterStatusTest {
    @Test fun savingOffActiveAndRealPausesHaveConsistentCopy() {
        for (enabled in listOf(false, true)) for (permissions in listOf(false, true)) {
            for (paused in listOf(false, true)) {
                assertEquals(FilterStatus.SAVING, filterStatus(enabled, permissions, true, paused))
            }
        }
        assertEquals(FilterStatus.OFF, filterStatus(false, true, false, false))
        assertEquals(FilterStatus.OFF, filterStatus(false, false, false, true))
        assertEquals(FilterStatus.ACTIVE, filterStatus(true, true, false, false))
        assertEquals(FilterStatus.SETUP_REQUIRED, filterStatus(true, false, false, false))
        assertEquals(FilterStatus.SESSION_PAUSED, filterStatus(true, true, false, true))
        assertEquals(R.string.on_detail, FilterStatus.ACTIVE.detail)
        assertEquals(R.string.saving_detail, FilterStatus.SAVING.detail)
        assertEquals(R.string.session_paused_detail, FilterStatus.SESSION_PAUSED.detail)
        assertEquals(R.string.paused_detail, FilterStatus.SETUP_REQUIRED.detail)
    }

    @Test fun reproducesOldCapturedReadinessAcrossChildScopeUpdates() {
        val pause = AtomicBoolean(true)
        val busy = mutableStateOf(true)
        val observer = SnapshotStateObserver { it() }
        val parent = Any()
        val child = Any()
        var parentInvalidated = false
        var childInvalidated = false
        var capturedReady = true
        observer.start()
        try {
            observer.observeReads(parent, { parentInvalidated = true }) { capturedReady = !pause.get() }
            observer.observeReads(child, { childInvalidated = true }) { busy.value; capturedReady }
            pause.set(false)
            busy.value = false
            Snapshot.sendApplyNotifications()
            assertTrue(childInvalidated)
            assertFalse(parentInvalidated)
            assertFalse("Old heading remains paused", capturedReady)
            assertFalse("Old description reads the now-unpaused flag", pause.get())
        } finally { observer.stop(); observer.clear() }
    }

    @Test fun observedPauseClearsAfterBackgroundCompletionWithoutSettingsChangeOrResume() = runBlocking {
        SettingsStore.beginUpdate()
        val observedPause = mutableStateOf(SettingsStore.sessionPauseFlow.value)
        val busy = mutableStateOf(false) // New composition attached during an existing save.
        val observer = SnapshotStateObserver { it() }
        val scope = Any()
        var invalidated = false
        fun status() = filterStatus(true, true, busy.value, observedPause.value)
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            // The same StateFlow -> snapshot State bridge used by collectAsState in Home.
            SettingsStore.sessionPauseFlow.collect { observedPause.value = it }
        }
        observer.start()
        try {
            observer.observeReads(scope, { invalidated = true }) {
                assertEquals(FilterStatus.SESSION_PAUSED, status())
            }
            withContext(Dispatchers.IO) { publishPause(false) }
            yield()
            Snapshot.sendApplyNotifications()
            assertTrue("Save completion must invalidate the status reader", invalidated)
            assertEquals(FilterStatus.ACTIVE, status())
            assertFalse(SettingsStore.sessionPaused.get())
        } finally { collector.cancelAndJoin(); observer.stop(); observer.clear(); publishPause(false) }
    }

    @Test fun repeatedSavesFailureAndRetryKeepPublishedStateInSync() = runBlocking {
        val seen = mutableListOf<Boolean>()
        publishPause(false)
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            SettingsStore.sessionPauseFlow.collect { seen += it }
        }
        try {
            repeat(3) {
                val revision = SettingsStore.revision.get()
                SettingsStore.beginUpdate()
                yield()
                assertTrue(SettingsStore.sessionPaused.get())
                assertTrue(SettingsStore.sessionPauseFlow.value)
                assertEquals(revision + 1, SettingsStore.revision.get())
                assertEquals(FilterStatus.SAVING, filterStatus(true, true, true, true))
                withContext(Dispatchers.IO) { publishPause(false) }
                yield()
                assertFalse(SettingsStore.sessionPaused.get())
                assertEquals(FilterStatus.ACTIVE, filterStatus(true, true, false, SettingsStore.sessionPauseFlow.value))
            }
            SettingsStore.beginUpdate() // A failed save never publishes false.
            yield()
            assertEquals(FilterStatus.SESSION_PAUSED, filterStatus(true, true, false, SettingsStore.sessionPauseFlow.value))
            SettingsStore.beginUpdate() // Retry succeeds; no settings equality/resume dependency.
            withContext(Dispatchers.IO) { publishPause(false) }
            yield()
            assertEquals(FilterStatus.ACTIVE, filterStatus(true, true, false, SettingsStore.sessionPauseFlow.value))
            assertEquals(listOf(false, true, false, true, false, true, false, true, false), seen)
        } finally { collector.cancelAndJoin(); publishPause(false) }
    }

    // Exercise the exact publisher used by save's success branch without Android disk IO.
    // This is not a substitute for a physical save/rotation/UI test.
    private fun publishPause(paused: Boolean) {
        SettingsStore.Companion::class.java.getDeclaredMethod("setSessionPaused", Boolean::class.javaPrimitiveType).apply {
            isAccessible = true
        }.invoke(SettingsStore.Companion, paused)
    }
}

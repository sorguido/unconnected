package it.sorguido.unconnected

import android.app.Activity
import android.database.Cursor
import android.provider.ContactsContract.CommonDataKinds.Phone
import it.sorguido.unconnected.domain.NumberRule
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.concurrent.CancellationException

class ContactPickingTest {
    private val selectedUri = "content://com.android.contacts/data/phones/42"
    private val draft = NumberDraft("Manual draft", "+12025550110")

    @Test fun queriesOnlySelectedPhoneRowAndPrefillsEditableDraft() {
        val row = TestCursor(listOf(listOf("+1 202-555-0111", "Test contact")))
        var queries = 0
        val result = readContactPick(Activity.RESULT_OK, selectedUri) { uri, projection ->
            queries++
            assertEquals(selectedUri, uri)
            assertArrayEquals(arrayOf(Phone.NUMBER, Phone.DISPLAY_NAME), projection)
            row.cursor
        }
        assertEquals(1, queries)
        assertTrue(row.closed)
        assertEquals(NumberDraft("Test contact", "+1 202-555-0111"), applyContactPick(draft, result))
        assertEquals("+12025550111", validateNumberEntry((result as ContactPickResult.Selected).number, "US", emptyList(), null).e164)
        // Selection changes only a draft. Manual corrections still use the same Save validator.
        assertEquals("+12025550112", validateNumberEntry("202-555-0112", "US", emptyList(), null).e164)
    }

    @Test fun cancelNeverQueriesOrReplacesDraft() {
        val result = readContactPick(Activity.RESULT_CANCELED, selectedUri) { _, _ -> error("Must not query") }
        assertEquals(ContactPickResult.Cancelled, result)
        assertSame(draft, applyContactPick(draft, result))
    }

    @Test fun missingResultOrDirectoryUriNeverQueries() {
        val invalid = listOf(null, "", "content://com.android.contacts/contacts/42",
            "content://com.android.contacts/data/phones", "content://com.android.contacts/data",
            "file:///data/42", "content://unexpected.provider/data/42",
            "content://com.android.contacts/data/phones/42?directory=2", "content://com.android.contacts/data/42#other")
        for (uri in invalid) {
            val result = readContactPick(Activity.RESULT_OK, uri) { _, _ -> error("Must not query $uri") }
            assertEquals(ContactPickResult.Failed, result)
            assertSame(draft, applyContactPick(draft, result))
        }
        assertTrue(isPhoneRowUri("content://com.android.contacts/data/42"))
        assertTrue(isPhoneRowUri(selectedUri))
    }

    @Test fun emptyNullAmbiguousAndMissingNumberResultsLeaveDraftUnchangedAndCloseCursor() {
        val cursors = listOf(
            TestCursor(emptyList()), TestCursor(listOf(listOf(null, "Name"))),
            TestCursor(listOf(listOf("  ", "Name"))),
            TestCursor(listOf(listOf("1".repeat(81), "Name"))),
            TestCursor(listOf(listOf("+12025550111", "Name")), numberColumn = false),
            TestCursor(listOf(listOf("+12025550111", "Name"), listOf("+12025550112", "Name"))),
        )
        for (row in cursors) {
            val result = readContactPick(Activity.RESULT_OK, selectedUri) { _, _ -> row.cursor }
            assertEquals(ContactPickResult.Failed, result)
            assertSame(draft, applyContactPick(draft, result))
            assertTrue(row.closed)
        }
        assertEquals(ContactPickResult.Failed, readContactPick(Activity.RESULT_OK, selectedUri) { _, _ -> null })
    }

    @Test fun deniedPermissionAndUnavailableProviderAreRecoverable() {
        for (problem in listOf(SecurityException("denied"), IllegalArgumentException("unavailable"), IllegalStateException("closed"))) {
            val result = readContactPick(Activity.RESULT_OK, selectedUri) { _, _ -> throw problem }
            assertEquals(ContactPickResult.Failed, result)
            assertSame(draft, applyContactPick(draft, result))
        }
        val row = TestCursor(listOf(listOf("+12025550111", "Name")), failRead = true)
        assertEquals(ContactPickResult.Failed, readContactPick(Activity.RESULT_OK, selectedUri) { _, _ -> row.cursor })
        assertTrue(row.closed)
    }

    @Test fun cancellationOfAsyncReadIsNotSwallowed() {
        try {
            readContactPick(Activity.RESULT_OK, selectedUri) { _, _ -> throw CancellationException() }
            fail("Cancelled work must not update the draft")
        } catch (_: CancellationException) { /* expected */ }
    }

    @Test fun absentNameKeepsManualLabelAndLongNamesFitExistingLimit() {
        for (row in listOf(TestCursor(listOf(listOf("+12025550111", null))),
            TestCursor(listOf(listOf("+12025550111", " "))),
            TestCursor(listOf(listOf("+12025550111", "ignored")), nameColumn = false))) {
            val result = readContactPick(Activity.RESULT_OK, selectedUri) { _, _ -> row.cursor }
            assertEquals(NumberDraft(draft.label, "+12025550111"), applyContactPick(draft, result))
        }
        val row = TestCursor(listOf(listOf("+12025550111", "x".repeat(90))))
        assertEquals(80, applyContactPick(draft, readContactPick(Activity.RESULT_OK, selectedUri) { _, _ -> row.cursor }).label.length)
    }

    @Test fun pickedAndManualDuplicatesUseSameNormalizedValidationIncludingDisabledEntries() {
        val existing = listOf(NumberRule("existing", "Test", "+12025550111", false))
        val row = TestCursor(listOf(listOf("+1 (202) 555-0111", "Test")))
        val picked = applyContactPick(draft, readContactPick(Activity.RESULT_OK, selectedUri) { _, _ -> row.cursor })
        assertEquals(R.string.number_duplicate, validateNumberEntry(picked.number, "US", existing, null).error)
        assertEquals(R.string.number_duplicate, validateNumberEntry("2025550111", "US", existing, null).error)
        assertEquals("+12025550111", validateNumberEntry(picked.number, "US", existing, "existing").e164)
        assertEquals("+12025550112", validateNumberEntry("2025550112", "US", existing, null).e164)
    }

    @Test fun invalidPickedNumbersRemainEditableButCannotPassSave() {
        for (raw in listOf("invalid", "112", "+12025550111 ext 4")) {
            val row = TestCursor(listOf(listOf(raw, "Test")))
            val picked = applyContactPick(draft, readContactPick(Activity.RESULT_OK, selectedUri) { _, _ -> row.cursor })
            assertEquals(raw, picked.number)
            assertEquals(R.string.number_invalid, validateNumberEntry(picked.number, "US", emptyList(), null).error)
        }
        assertEquals(R.string.number_invalid, validateNumberEntry("+12025550111", "ZZ", emptyList(), null).error)
    }

    /** Cursor interface fixture: no Android provider, contact database, or device permissions. */
    private class TestCursor(
        private val rows: List<List<String?>>, private val numberColumn: Boolean = true,
        private val nameColumn: Boolean = true, private val failRead: Boolean = false,
    ) {
        var closed = false
        private var position = -1
        val cursor: Cursor = Proxy.newProxyInstance(Cursor::class.java.classLoader, arrayOf(Cursor::class.java)) { _, method, args ->
            when (method.name) {
                "moveToFirst" -> { position = 0; rows.isNotEmpty() }
                "moveToNext" -> { position++; position < rows.size }
                "getColumnIndex" -> when (args!![0]) {
                    Phone.NUMBER -> if (numberColumn) 0 else -1
                    Phone.DISPLAY_NAME -> if (nameColumn) 1 else -1
                    else -> error("Unexpected column")
                }
                "getString" -> if (failRead) throw IllegalStateException("Provider failed") else rows[position][args!![0] as Int]
                "close" -> { closed = true; null }
                else -> error("Unexpected cursor operation: ${method.name}")
            }
        } as Cursor
    }
}

package it.sorguido.unconnected

import android.app.Activity
import android.database.Cursor
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.google.i18n.phonenumbers.PhoneNumberUtil
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.concurrent.CancellationException

class ContactPickingTest {
    private val selectedUri = "content://com.android.contacts/contacts/42"
    private val draft = NumberDraft("Manual draft", "+12025550110")
    private val first = "+12025550111"
    private val second = "+12025550112"
    private val isEnterprise: (Long) -> Boolean = { it >= 1_000_000_000L }

    @Test fun singleNumberQueriesOnlyChosenAggregateAndItsBoundedPhoneRows() {
        val provider = Provider(phoneRows = listOf(phone("+1 (202) 555-0111")))
        val result = provider.pick()
        assertEquals(ContactPickResult.Selected(first, "Test contact"), result)
        assertEquals(NumberDraft("Test contact", first), applyContactPick(draft, result))
        assertEquals(listOf(
            Query("$selectedUri?limit=2", listOf(Contacts._ID, Contacts.DISPLAY_NAME), null, null),
            Query("content://com.android.contacts/data/phones?limit=101",
                listOf(Phone.CONTACT_ID, Phone.NUMBER, Phone.TYPE, Phone.LABEL), "${Phone.CONTACT_ID} = ?", listOf("42")),
        ), provider.queries)
        assertTrue(provider.contact!!.closed)
        assertTrue(provider.phones!!.closed)
    }

    @Test fun distinctNumbersRequireAnExplicitChoiceAndNeverDefaultToFirst() {
        val result = Provider(phoneRows = listOf(phone(first), phone(second, Phone.TYPE_WORK))).pick()
        assertTrue(result is ContactPickResult.Choices)
        val choices = result as ContactPickResult.Choices
        assertEquals(listOf(first, second), choices.numbers.map { it.e164 })
        assertFalse(choices.hasUnusableNumbers)
        assertSame(draft, applyContactPick(draft, choices))
        assertEquals(NumberDraft("Test contact", second), applyContactPick(draft, chooseContactNumber(choices, second)))
        assertSame(draft, applyContactPick(draft, chooseContactNumber(choices, "+12025550113")))
    }

    @Test fun exactDuplicatesProduceOnePrefill() {
        val provider = Provider(phoneRows = List(4) { phone(first) })
        assertEquals(ContactPickResult.Selected(first, "Test contact"), provider.pick())
    }

    @Test fun fourEquivalentItalianRepresentationsKeepOneNumberAndRetainDistinctAlternative() {
        // Synthetic example supplied by libphonenumber metadata, never a device contact.
        val util = PhoneNumberUtil.getInstance()
        val example = util.getExampleNumberForType("IT", PhoneNumberUtil.PhoneNumberType.MOBILE)
        val canonical = util.format(example, PhoneNumberUtil.PhoneNumberFormat.E164)
        val variants = listOf(canonical, "00${canonical.drop(1)}",
            util.format(example, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL),
            util.format(example, PhoneNumberUtil.PhoneNumberFormat.NATIONAL))
        assertEquals(4, variants.distinct().size)
        for (value in variants) assertEquals(value, canonical, normalizeContactNumber(value, "IT"))
        val rows = variants.map { phone(it) }
        assertEquals(ContactPickResult.Selected(canonical, "Test contact"), Provider(phoneRows = rows).pick(region = "IT"))
        val choices = Provider(phoneRows = rows + listOf(phone("+44 20 7946 0111", Phone.TYPE_WORK)))
            .pick(region = "IT") as ContactPickResult.Choices
        assertEquals(listOf(canonical, "+442079460111"), choices.numbers.map { it.e164 })
        assertFalse(choices.hasUnusableNumbers)
    }

    @Test fun equivalentRowsPreserveDistinctTypeAndCustomLabels() {
        val rows = listOf(phone(first), phone("+1 (202) 555-0111", Phone.TYPE_WORK),
            phone(first), phone(first, Phone.TYPE_CUSTOM, "  Desk  "), phone(second, Phone.TYPE_HOME))
        val choices = Provider(phoneRows = rows).pick() as ContactPickResult.Choices
        assertEquals(listOf(ContactNumberLabel(Phone.TYPE_MOBILE, null),
            ContactNumberLabel(Phone.TYPE_WORK, null), ContactNumberLabel(Phone.TYPE_CUSTOM, "Desk")), choices.numbers[0].labels)
        assertEquals(listOf(ContactNumberLabel(Phone.TYPE_HOME, null)), choices.numbers[1].labels)
    }

    @Test fun internationalTrunkRepairsAreNeverMergedWithValidNumber() {
        for (raw in listOf("+44 (0)20 7946 0111", "0044 (0)20 7946 0111")) {
            assertNull(raw, normalizeContactNumber(raw, "IT"))
            val choices = Provider(phoneRows = listOf(phone("+442079460111"), phone(raw))).pick(region = "IT") as ContactPickResult.Choices
            assertEquals(listOf("+442079460111"), choices.numbers.map { it.e164 })
            assertTrue(choices.hasUnusableNumbers)
            assertSame(draft, applyContactPick(draft, choices))
        }
        assertNull(normalizeContactNumber("442079460111", "GB"))
    }

    @Test fun validNationalTrunkAndRegionalInternationalPrefixRemainSupported() {
        assertEquals("+442079460111", normalizeContactNumber("020 7946 0111", "GB"))
        assertEquals("+442079460111", normalizeContactNumber("011 44 20 7946 0111", "US"))
        assertEquals(first, normalizeContactNumber("202-555-0111", "US"))
    }

    @Test fun similarSuffixesAndDifferentCountriesStayDistinct() {
        val choices = Provider(phoneRows = listOf(phone(first), phone(second), phone("+442079460111"))).pick() as ContactPickResult.Choices
        assertEquals(listOf(first, second, "+442079460111"), choices.numbers.map { it.e164 })
    }

    @Test fun invalidAndAmbiguousRowsProduceClearOutcomeWithoutReplacingDraft() {
        for (raw in listOf(null, "", " ", "invalid", "112", "+12025550111 ext 4", "1".repeat(81))) {
            val provider = Provider(phoneRows = listOf(phone(raw)))
            val result = provider.pick()
            assertEquals(raw, ContactPickResult.InvalidNumbers, result)
            assertSame(draft, applyContactPick(draft, result))
            assertTrue(provider.phones!!.closed)
        }
        assertEquals(ContactPickResult.InvalidNumbers, Provider(phoneRows = listOf(phone(first))).pick(region = "ZZ"))
    }

    @Test fun mixedUnusableRowsRetainEveryValidAlternativeAndRequireChoiceWithWarning() {
        val choices = Provider(phoneRows = listOf(phone(first), phone("invalid"), phone(second))).pick() as ContactPickResult.Choices
        assertEquals(listOf(first, second), choices.numbers.map { it.e164 })
        assertTrue(choices.hasUnusableNumbers)
        assertSame(draft, applyContactPick(draft, choices))
    }

    @Test fun contactWithoutPhoneNumbersLeavesManualDraftAvailable() {
        val provider = Provider(phoneRows = emptyList())
        assertEquals(ContactPickResult.Empty, provider.pick())
        assertTrue(provider.phones!!.closed)
        assertSame(draft, applyContactPick(draft, ContactPickResult.Empty))
    }

    @Test fun cancellationAtEitherStageLeavesDraftAndNeverQueriesOnNativeCancel() {
        val provider = Provider()
        val cancelled = provider.pick(resultCode = Activity.RESULT_CANCELED, permission = false)
        assertEquals(ContactPickResult.Cancelled, cancelled)
        assertTrue(provider.queries.isEmpty())
        assertSame(draft, applyContactPick(draft, cancelled))
        val choices = Provider(phoneRows = listOf(phone(first), phone(second))).pick()
        assertSame(draft, applyContactPick(draft, choices))
        assertSame(draft, applyContactPick(draft, ContactPickResult.Cancelled))
    }

    @Test fun deniedExistingPermissionNeverQueries() {
        val provider = Provider()
        assertEquals(ContactPickResult.PermissionDenied, provider.pick(permission = false))
        assertTrue(provider.queries.isEmpty())
        assertSame(draft, applyContactPick(draft, ContactPickResult.PermissionDenied))
    }

    @Test fun unsafeOrNonIndividualPickerUrisNeverQuery() {
        val invalid = listOf(null, "", "content://com.android.contacts/contacts",
            "content://com.android.contacts/data/phones/42", "content://com.android.contacts/contacts/0",
            "content://com.android.contacts/contacts/-1", "content://com.android.contacts/contacts/9223372036854775808",
            "content://com.android.contacts/contacts/42/data", "file:///contacts/42",
            "content://unexpected.provider/contacts/42", "content://10@com.android.contacts/contacts/42",
            "$selectedUri?directory=2", "$selectedUri#other", "content://com.android.contacts/contacts/lookup/",
            "content://com.android.contacts/contacts/1000000000",
            "content://com.android.contacts/contacts/lookup/c-managed/42",
            "content://com.android.contacts/contacts/lookup/%63-managed/42",
            "content://com.android.contacts/contacts/lookup/local/1000000000",
            "content://com.android.contacts/contacts/lookup/${"x".repeat(2048)}/42")
        for (uri in invalid) {
            val provider = Provider()
            assertEquals(uri, ContactPickResult.Failed, provider.pick(uri = uri))
            assertTrue(uri, provider.queries.isEmpty())
        }
    }

    @Test fun localLookupUrisResolveOnlyChosenContactIncludingUpdatedIdHint() {
        for (uri in listOf("content://com.android.contacts/contacts/lookup/local-key/41",
            "content://com.android.contacts/contacts/lookup/local-key",
            "content://com.android.contacts/contacts/lookup/local%2Fkey/41")) {
            val provider = Provider()
            assertTrue(isContactUri(uri, isEnterprise))
            assertEquals(ContactPickResult.Selected(first, "Test contact"), provider.pick(uri = uri))
            assertEquals("$uri?limit=2", provider.queries[0].uri)
            assertEquals(listOf("42"), provider.queries[1].args)
        }
    }

    @Test fun missingOrInvalidAggregateRowsDoNotQueryPhones() {
        val invalid = listOf<TestCursor?>(null, metadata(emptyList()), metadata(listOf(listOf(null, "Name"))),
            metadata(listOf(listOf("-1", "Name"))), metadata(listOf(listOf("0", "Name"))),
            metadata(listOf(listOf("1000000000", "Name"))), metadata(listOf(listOf("invalid", "Name"))),
            metadata(listOf(listOf("43", "Different aggregate"))),
            metadata(listOf(listOf("42", "Name"), listOf("43", "Other"))),
            TestCursor(listOf(Contacts.DISPLAY_NAME), listOf(listOf("Name"))))
        for (row in invalid) {
            val provider = Provider(contact = row)
            assertEquals(ContactPickResult.Failed, provider.pick())
            assertEquals(1, provider.queries.size)
            if (row != null) assertTrue(row.closed)
        }
    }

    @Test fun rowsFromOtherContactAndMissingPhoneColumnsFailWithoutPartialPrefill() {
        for (phones in listOf<TestCursor?>(null,
            phoneCursor(listOf(phone(first), phone(second, id = "43"))),
            TestCursor(listOf(Phone.NUMBER), listOf(listOf(first))),
            TestCursor(listOf(Phone.CONTACT_ID), listOf(listOf("42"))))) {
            val provider = Provider(phones = phones)
            val result = provider.pick()
            assertEquals(ContactPickResult.Failed, result)
            assertSame(draft, applyContactPick(draft, result))
            if (phones != null) assertTrue(phones.closed)
        }
    }

    @Test fun readLimitRejectsOverflowEvenWhenProviderIgnoresRequestedLimit() {
        val provider = Provider(phoneRows = List(MAX_CONTACT_PHONE_ROWS + 20) { phone(first) })
        assertEquals(ContactPickResult.TooMany, provider.pick())
        assertEquals(MAX_CONTACT_PHONE_ROWS + 1, provider.phones!!.moves)
        assertTrue(provider.phones.closed)
        assertSame(draft, applyContactPick(draft, ContactPickResult.TooMany))
        assertEquals(ContactPickResult.Selected(first, "Test contact"),
            Provider(phoneRows = List(MAX_CONTACT_PHONE_ROWS) { phone(first) }).pick())
    }

    @Test fun providerAndPermissionExceptionsAtEitherQueryAreRecoverable() {
        for (stage in 1..2) for (problem in listOf(SecurityException("denied"),
            IllegalArgumentException("unavailable"), IllegalStateException("closed"))) {
            val provider = Provider(failureOnQuery = stage, failure = problem)
            val result = provider.pick()
            assertEquals(if (problem is SecurityException) ContactPickResult.PermissionDenied else ContactPickResult.Failed, result)
            assertSame(draft, applyContactPick(draft, result))
            assertEquals(stage, provider.queries.size)
            if (stage == 2) assertTrue(provider.contact!!.closed)
        }
    }

    @Test fun cursorFailureClosesCursorAndCannotReturnPartialDraft() {
        for (stage in 1..2) {
            val contact = metadata(failRead = stage == 1)
            val phones = phoneCursor(listOf(phone(first)), failRead = stage == 2)
            val provider = Provider(contact = contact, phones = phones)
            assertEquals(ContactPickResult.Failed, provider.pick())
            assertTrue(contact.closed)
            if (stage == 2) assertTrue(phones.closed)
        }
    }

    @Test fun asyncCancellationPropagatesAtBothQueryStages() {
        for (stage in 1..2) {
            val provider = Provider(failureOnQuery = stage, failure = CancellationException())
            try {
                provider.pick()
                fail("Cancelled work must not update draft")
            } catch (_: CancellationException) { /* expected */ }
            if (stage == 2) assertTrue(provider.contact!!.closed)
        }
    }

    @Test fun absentNameRetainsManualLabelAndNamesAndCustomLabelsStayBounded() {
        for (contact in listOf(metadata(listOf(listOf("42", null))), metadata(listOf(listOf("42", " "))),
            TestCursor(listOf(Contacts._ID), listOf(listOf("42"))))) {
            assertEquals(NumberDraft(draft.label, first), applyContactPick(draft, Provider(contact = contact).pick()))
        }
        val choices = Provider(contact = metadata(listOf(listOf("42", "x".repeat(90)))),
            phoneRows = listOf(phone(first, Phone.TYPE_CUSTOM, "y".repeat(90)), phone(second))).pick() as ContactPickResult.Choices
        assertEquals(80, choices.name!!.length)
        assertEquals(80, choices.numbers[0].labels[0].custom!!.length)
    }

    @Test fun missingOptionalPhoneLabelsDoNotLoseValidNumbers() {
        val phones = TestCursor(listOf(Phone.CONTACT_ID, Phone.NUMBER), listOf(listOf("42", first), listOf("42", second)))
        val choices = Provider(phones = phones).pick() as ContactPickResult.Choices
        assertEquals(listOf(first, second), choices.numbers.map { it.e164 })
        assertTrue(choices.numbers.all { it.labels == listOf(ContactNumberLabel(Phone.TYPE_OTHER, null)) })
    }

    private fun phone(number: String?, type: Int = Phone.TYPE_MOBILE, label: String? = null, id: String = "42") =
        listOf(id, number, type.toString(), label)

    private fun metadata(rows: List<List<String?>> = listOf(listOf("42", "Test contact")), failRead: Boolean = false) =
        TestCursor(listOf(Contacts._ID, Contacts.DISPLAY_NAME), rows, failRead)

    private fun phoneCursor(rows: List<List<String?>>, failRead: Boolean = false) =
        TestCursor(listOf(Phone.CONTACT_ID, Phone.NUMBER, Phone.TYPE, Phone.LABEL), rows, failRead)

    private data class Query(val uri: String, val projection: List<String>, val selection: String?, val args: List<String>?)

    private inner class Provider(
        phoneRows: List<List<String?>> = listOf(phone(first)),
        val contact: TestCursor? = metadata(),
        val phones: TestCursor? = phoneCursor(phoneRows),
        private val failureOnQuery: Int? = null,
        private val failure: Exception = IllegalStateException("Provider failed"),
    ) {
        val queries = mutableListOf<Query>()
        fun pick(uri: String? = selectedUri, region: String = "US", permission: Boolean = true,
                 resultCode: Int = Activity.RESULT_OK): ContactPickResult =
            readContactPick(resultCode, uri, region, permission, isEnterprise) { queriedUri, projection, selection, args ->
                queries += Query(queriedUri, projection.toList(), selection, args?.toList())
                if (queries.size == failureOnQuery) throw failure
                when (queries.size) {
                    1 -> contact?.cursor
                    2 -> phones?.cursor
                    else -> error("Unexpected additional query")
                }
            }
    }

    /** Real Cursor interface contract, with no device data or Android runtime dependency. */
    private class TestCursor(
        private val columns: List<String>, private val rows: List<List<String?>>,
        private val failRead: Boolean = false,
    ) {
        var closed = false
        var moves = 0
        private var position = -1
        val cursor: Cursor = Proxy.newProxyInstance(Cursor::class.java.classLoader, arrayOf(Cursor::class.java)) { _, method, args ->
            when (method.name) {
                "moveToFirst" -> { position = 0; rows.isNotEmpty() }
                "moveToNext" -> { moves++; position++; position < rows.size }
                "getColumnIndex" -> columns.indexOf(args!![0] as String)
                "getString" -> if (failRead) throw IllegalStateException("Provider failed") else rows[position][args!![0] as Int]
                "close" -> { closed = true; null }
                else -> error("Unexpected cursor operation: ${method.name}")
            }
        } as Cursor
    }
}

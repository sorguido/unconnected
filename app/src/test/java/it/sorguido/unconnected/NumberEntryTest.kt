package it.sorguido.unconnected

import android.provider.ContactsContract.CommonDataKinds.Phone
import it.sorguido.unconnected.domain.NumberRule
import org.junit.Assert.*
import org.junit.Test

class NumberEntryTest {
    private val originalDraft = NumberDraft("Manual", "+12025550110")
    private val choices = ContactPickResult.Choices("Test contact", listOf(
        ContactNumber("+12025550111", listOf(ContactNumberLabel(Phone.TYPE_MOBILE, null))),
        ContactNumber("+12025550112", listOf(ContactNumberLabel(Phone.TYPE_WORK, null)))), false)

    @Test fun pickingChoosingCancellingAndEditingCannotPersistBeforeExplicitSave() {
        var persisted = emptyList<NumberRule>()
        var saves = 0
        val save: (NumberRule, String) -> Unit = { number, region ->
            assertEquals("US", region)
            persisted = persisted + number
            saves++
        }
        var draft = applyContactPick(originalDraft, choices)
        assertEquals(originalDraft, draft)
        for (result in listOf(ContactPickResult.Cancelled, ContactPickResult.Failed, ContactPickResult.PermissionDenied,
            ContactPickResult.Empty, ContactPickResult.InvalidNumbers, ContactPickResult.TooMany)) {
            draft = applyContactPick(draft, result)
            assertEquals(originalDraft, draft)
        }
        draft = applyContactPick(draft, chooseContactNumber(choices, "+12025550112"))
        assertEquals(NumberDraft("Test contact", "+12025550112"), draft)
        assertTrue(persisted.isEmpty())
        assertEquals(0, saves)
        // The same explicit Save action used by the UI commits the edited draft, not the original pick.
        draft = draft.copy(label = "  Edited label  ", number = "202-555-0113")
        assertNull(saveNumberEntry(draft, "US", persisted, null, onSave = save))
        assertEquals(1, saves)
        assertEquals(1, persisted.size)
        assertEquals("+12025550113", persisted.single().e164)
        assertEquals("Edited label", persisted.single().label)
        assertTrue(persisted.single().enabled)
    }

    // ---- Range masks in the shared editor ----

    @Test fun maskSavesWithoutConfirmationAtThreeStarsAndBelow() {
        var saved: NumberRule? = null
        for (raw in listOf("02 12345***", "0212345***", "+39 02 12345***")) {
            assertNull(saveNumberEntry(NumberDraft("Range", raw), "IT", emptyList(), null) { rule, _ -> saved = rule })
            assertEquals("+390212345***", saved!!.target.canonical)
            assertEquals("+390212345***", saved!!.displayValue())
        }
    }

    @Test fun wideMaskRequiresExplicitConfirmationBeforeSaving() {
        val draft = NumberDraft("Wide", "021234****")
        var saves = 0
        val error = saveNumberEntry(draft, "IT", emptyList(), null) { _, _ -> saves++ }
        assertEquals(R.string.range_confirm, error)
        assertEquals(0, saves)
        // The confirmed path (the UI dialog's Save button) persists exactly once.
        assertNull(saveNumberEntry(draft, "IT", emptyList(), null, confirmed = true) { _, _ -> saves++ })
        assertEquals(1, saves)
        // Cancelling the dialog leaves settings untouched: no save call at all above.
    }

    @Test fun malformedOrTooWideMasksAreRefusedInEditor() {
        for (raw in listOf("02*12345", "*0212345", "0212345*****", "0212345**x", "02***")) {
            assertEquals(R.string.number_invalid,
                saveNumberEntry(NumberDraft("Bad", raw), "IT", emptyList(), null) { _, _ -> fail("Invalid mask must not save: $raw") })
        }
    }

    @Test fun canonicalMaskDuplicateIsRefusedButSingleInsideRangeStaysSavable() {
        val range = NumberRule("range", "", it.sorguido.unconnected.domain.NumberMasks.parse("02 12345***", "IT")!!)
        assertEquals(R.string.number_duplicate,
            validateNumberEntry("+39 02 12345***", "IT", listOf(range), null).error)
        val inside = validateNumberEntry("02 12345777", "IT", listOf(range), null)
        assertNull(inside.error) // OR semantics on one shared calendar: legitimate overlap allowed
    }

    @Test fun editingAMaskPreservesIdentityAndDisabledState() {
        val original = NumberRule("existing", "Old label",
            it.sorguido.unconnected.domain.NumberMasks.parse("02 12345***", "IT")!!, enabled = false)
        var saved: NumberRule? = null
        assertNull(saveNumberEntry(NumberDraft("Renamed", "02 12345***"), "IT", listOf(original), original) { rule, _ -> saved = rule })
        assertEquals("existing", saved!!.id)
        assertFalse(saved!!.enabled)
        assertEquals("+390212345***", saved!!.target.canonical)
    }

    @Test fun rangeFeedbackOnlyForValidMasks() {
        assertEquals(1_000L, rangeFeedback("02 12345***", "IT"))
        assertEquals(100L, rangeFeedback("+3902123456**", "IT"))
        assertNull(rangeFeedback("02 123", "IT"))       // plain number: no range feedback
        assertNull(rangeFeedback("02 12*", "IT"))       // too short a stem
        assertNull(rangeFeedback("02*12***", "IT"))     // internal wildcard
        assertNull(rangeFeedback("02 12345", "IT"))     // incomplete non-mask
    }

    @Test fun manualAndPickedDuplicatesIncludingDisabledRulesCannotInvokeSave() {
        val existing = listOf(NumberRule("existing", "Test", "+12025550111", false))
        for (raw in listOf("2025550111", "+1 (202) 555-0111", "+12025550111")) {
            assertEquals(R.string.number_duplicate, validateNumberEntry(raw, "US", existing, null).error)
            assertEquals(R.string.number_duplicate,
                saveNumberEntry(NumberDraft("Duplicate", raw), "US", existing, null) { _, _ -> fail("Duplicate must not save") })
        }
        val picked = applyContactPick(originalDraft, chooseContactNumber(choices, "+12025550111"))
        assertEquals(R.string.number_duplicate,
            saveNumberEntry(picked, "US", existing, null) { _, _ -> fail("Picked duplicate must not save") })
    }

    @Test fun invalidNumbersOrRegionCannotInvokeSaveAndRemainManuallyCorrectable() {
        for (raw in listOf("invalid", "112", "+12025550111 ext 4", "")) {
            assertEquals(R.string.number_invalid,
                saveNumberEntry(NumberDraft("Manual", raw), "US", emptyList(), null) { _, _ -> fail("Invalid number must not save") })
        }
        assertEquals(R.string.number_invalid,
            saveNumberEntry(originalDraft, "ZZ", emptyList(), null) { _, _ -> fail("Invalid region must not save") })
        var saved: NumberRule? = null
        assertNull(saveNumberEntry(originalDraft.copy(number = "2025550114"), "US", emptyList(), null) { rule, _ -> saved = rule })
        assertEquals("+12025550114", saved!!.e164)
    }

    @Test fun editingOwnNumberPreservesIdentityAndDisabledStateButRejectsOtherDuplicate() {
        val original = NumberRule("existing", "Original", "+12025550111", false)
        val other = NumberRule("other", "Other", "+12025550112", true)
        var saved: NumberRule? = null
        assertNull(saveNumberEntry(NumberDraft("Updated", "+1 (202) 555-0111"), "US", listOf(original, other), original) { rule, _ -> saved = rule })
        assertEquals(NumberRule("existing", "Updated", "+12025550111", false), saved)
        assertEquals(R.string.number_duplicate,
            saveNumberEntry(NumberDraft("Duplicate", other.e164), "US", listOf(original, other), original) { _, _ -> fail("Other rule must not be overwritten") })
    }
}

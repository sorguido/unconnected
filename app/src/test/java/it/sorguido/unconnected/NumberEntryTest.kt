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
        assertNull(saveNumberEntry(draft, "US", persisted, null, save))
        assertEquals(1, saves)
        assertEquals(1, persisted.size)
        assertEquals("+12025550113", persisted.single().e164)
        assertEquals("Edited label", persisted.single().label)
        assertTrue(persisted.single().enabled)
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

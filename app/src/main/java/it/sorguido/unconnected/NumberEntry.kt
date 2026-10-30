package it.sorguido.unconnected

import it.sorguido.unconnected.domain.NumberRule
import it.sorguido.unconnected.domain.Numbers
import java.util.Locale
import java.util.UUID

internal data class NumberEntryValidation(val e164: String? = null, val error: Int? = null)

/** Shared validation for the editable draft, regardless of whether it was typed or picked. */
internal fun validateNumberEntry(
    raw: String,
    region: String,
    existing: List<NumberRule>,
    editingId: String?,
): NumberEntryValidation {
    val e164 = if (region in Locale.getISOCountries()) Numbers.normalize(raw, region) else null
    return when {
        e164 == null -> NumberEntryValidation(error = R.string.number_invalid)
        existing.any { it.e164 == e164 && it.id != editingId } -> NumberEntryValidation(error = R.string.number_duplicate)
        else -> NumberEntryValidation(e164 = e164)
    }
}

/** The editor's explicit Save action is the only path from a draft to its persistence callback. */
internal fun saveNumberEntry(
    draft: NumberDraft,
    region: String,
    existing: List<NumberRule>,
    original: NumberRule?,
    onSave: (NumberRule, String) -> Unit,
): Int? {
    val validation = validateNumberEntry(draft.number, region, existing, original?.id)
    if (validation.error != null) return validation.error
    onSave(NumberRule(original?.id ?: UUID.randomUUID().toString(), draft.label.trim(), validation.e164!!, original?.enabled ?: true), region)
    return null
}

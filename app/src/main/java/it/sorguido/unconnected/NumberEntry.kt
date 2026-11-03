package it.sorguido.unconnected

import it.sorguido.unconnected.domain.ExactTarget
import it.sorguido.unconnected.domain.MaskTarget
import it.sorguido.unconnected.domain.NumberMasks
import it.sorguido.unconnected.domain.NumberRule
import it.sorguido.unconnected.domain.NumberTarget
import it.sorguido.unconnected.domain.Numbers
import java.util.Locale
import java.util.UUID

internal data class NumberEntryValidation(
    val target: NumberTarget? = null,
    val error: Int? = null,
    /** Valid mask wider than the ordinary three-star case: the UI must confirm before saving. */
    val needsConfirmation: Boolean = false,
)

/** Shared validation for the editable draft, regardless of whether it was typed or picked. */
internal fun validateNumberEntry(
    raw: String,
    region: String,
    existing: List<NumberRule>,
    editingId: String?,
): NumberEntryValidation {
    if (region !in Locale.getISOCountries()) return NumberEntryValidation(error = R.string.number_invalid)
    val target: NumberTarget? = if (raw.contains('*')) NumberMasks.parse(raw, region)
    else Numbers.normalize(raw, region)?.let(::ExactTarget)
    if (target == null) return NumberEntryValidation(error = R.string.number_invalid)
    // Only true canonical duplicates of the same kind are refused; single-in-range and
    // overlapping ranges stay savable because all rules share one calendar (OR semantics).
    if (existing.any { it.id != editingId && it.target.canonical == target.canonical }) {
        return NumberEntryValidation(error = R.string.number_duplicate)
    }
    val wide = target is MaskTarget && target.possibleNumbers > NumberMasks.CONFIRMATION_THRESHOLD
    return NumberEntryValidation(target = target, needsConfirmation = wide)
}

/** The editor's explicit Save action is the only path from a draft to its persistence callback. */
internal fun saveNumberEntry(
    draft: NumberDraft,
    region: String,
    existing: List<NumberRule>,
    original: NumberRule?,
    confirmed: Boolean = false,
    onSave: (NumberRule, String) -> Unit,
): Int? {
    val validation = validateNumberEntry(draft.number, region, existing, original?.id)
    if (validation.error != null) return validation.error
    if (validation.needsConfirmation && !confirmed) return R.string.range_confirm
    onSave(NumberRule(original?.id ?: UUID.randomUUID().toString(), draft.label.trim(), validation.target!!, original?.enabled ?: true), region)
    return null
}

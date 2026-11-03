package it.sorguido.unconnected.domain

import com.google.i18n.phonenumbers.PhoneNumberUtil

/**
 * A saved rule target: either an exact E.164 number (the complete v1.0.0 behaviour) or a bounded
 * digit mask whose only wildcards are one contiguous run of trailing ASCII asterisks.
 */
sealed interface NumberTarget {
    /** Lowercase canonical form used for storage, duplicate rejection and display. */
    val canonical: String
}

/** Full international number; semantics identical to the released v1.0.0 rules. */
data class ExactTarget(override val canonical: String) : NumberTarget {
    init { require(canonical matches Numbers.canonicalForm) { "Exact target must be canonical E.164" } }
}

/**
 * Fixed-length range: [prefix] followed by exactly [wildcardCount] digits. The prefix is a
 * canonical E.164 stem ('+' plus country code plus literal digits), so a national spelling and
 * its international equivalent of the same mask collapse to one canonical value. Numbers of any
 * other length never match; there is no free-length "startsWith" rule.
 */
data class MaskTarget(val prefix: String, val wildcardCount: Int) : NumberTarget {
    init {
        require(wildcardCount in 1..NumberMasks.MAX_WILDCARDS) { "Wildcard count out of bounds" }
        require(prefix matches Numbers.canonicalForm) { "Mask prefix must be a canonical E.164 stem" }
    }
    override val canonical: String get() = prefix + "*".repeat(wildcardCount)
    /** Exactly the combinations on the wildcard positions: 10^wildcardCount, never enumerated. */
    val possibleNumbers: Long get() = POW10[wildcardCount]

    private companion object {
        val POW10 = listOf(1L, 10L, 100L, 1_000L, 10_000L)
    }
}

/**
 * Strict mask parsing and matching. A partial mask is never handed to
 * PhoneNumberUtil.parse() hoping it is a complete number: both bounds are rebuilt from plain
 * digits and validated through the existing full-number normalization for the selected region.
 */
object NumberMasks {
    private val phoneUtil by lazy { PhoneNumberUtil.getInstance() }
    const val MAX_WILDCARDS = 4
    /**
     * Minimum deterministic national stem digits (after '+CC'): four local digits are the most
     * plausible office core, keeping whole-country, trunk-only and very short prefix masks out.
     */
    const val MIN_STEM_DIGITS = 4
    /** Masks wider than this many numbers need an explicit proportional confirmation in the UI. */
    const val CONFIRMATION_THRESHOLD = 1_000L

    /**
     * Canonical [MaskTarget] for [raw] under [region], or null when the input is not a
     * recognizable, safely bounded mask: leading/internal wildcards, other wildcard characters,
     * misplaced '+', extensions, Unicode digits, letter/vanity input, too-short prefixes,
     * too-long totals or more than [MAX_WILDCARDS] trailing asterisks are all rejected.
     */
    fun parse(raw: String, region: String): MaskTarget? {
        if (raw.length > 80 || !Numbers.isSupportedRegion(region)) return null
        val trimmed = raw.trim()
        val star = trimmed.indexOf('*')
        if (star < 0) return null
        // Only ASCII '*', ending the string in one contiguous run.
        val wildcardCount = trimmed.length - star
        if (trimmed.substring(star).toSet() != setOf('*')) return null
        if (wildcardCount > MAX_WILDCARDS) return null
        val head = trimmed.substring(0, star)
        // Ordinary telephone formatting for the deterministic part, same rules as single numbers.
        if (!Numbers.validGrouping(head) || !Numbers.lexicalInput.matches(head)) return null
        if (head.none { it in '0'..'9' }) return null
        // Rebuild both bounds as complete numbers and normalize them strictly for the region.
        val low = Numbers.normalize(head + "0".repeat(wildcardCount), region) ?: return null
        val high = Numbers.normalize(head + "9".repeat(wildcardCount), region) ?: return null
        // The asterisks must replace exactly the final positions of two equal-length bounds,
        // leaving the international prefix untouched.
        if (low.length != high.length) return null
        val shared = low.length - wildcardCount
        if (low.take(shared) != high.take(shared)) return null
        if (low.drop(shared).any { it != '0' } || high.drop(shared).any { it != '9' }) return null
        val prefix = low.take(shared)
        // Prudent amplitude: deterministic stem (after '+CC') within bounded, plausible sizes.
        val stemDigits = prefix.length - 1 - countryCodeDigits(region)
        if (stemDigits < MIN_STEM_DIGITS) return null
        if (prefix.length - 1 + wildcardCount > Numbers.MAX_CANONICAL_DIGITS) return null
        // The '+' may only introduce a real international prefix; stray or misplaced '+' fails here
        // because normalize() rejects lexical input whose '+' is not the first character.
        return MaskTarget(prefix, wildcardCount)
    }

    private fun countryCodeDigits(region: String): Int =
        phoneUtil.getCountryCodeForRegion(region).let { if (it <= 0) 0 else it.toString().length }

    /** Deterministic fixed-length matching on canonical characters; enumerates nothing. */
    fun matches(mask: MaskTarget, e164: String): Boolean {
        if (e164.length != mask.prefix.length + mask.wildcardCount) return false
        if (!e164.startsWith(mask.prefix)) return false
        return e164.substring(mask.prefix.length).all { it in '0'..'9' }
    }

    /** True when some caller number would satisfy both targets (exact inside a range or nested ranges). */
    fun overlaps(first: NumberTarget, second: NumberTarget): Boolean = when {
        first is ExactTarget && second is ExactTarget -> first.canonical == second.canonical
        first is MaskTarget && second is MaskTarget ->
            first.prefix.startsWith(second.prefix) || second.prefix.startsWith(first.prefix)
        first is MaskTarget && second is ExactTarget -> matches(first, second.canonical)
        else -> second is MaskTarget && first is ExactTarget && matches(second, first.canonical)
    }
}

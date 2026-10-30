package it.sorguido.unconnected

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.provider.ContactsContract
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.google.i18n.phonenumbers.PhoneNumberUtil
import it.sorguido.unconnected.domain.Numbers
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.CancellationException

internal fun contactPickerIntent(): Intent = Intent(Intent.ACTION_PICK).apply {
    type = Contacts.CONTENT_TYPE
}

internal data class ContactNumberLabel(val type: Int, val custom: String?)
internal data class ContactNumber(val e164: String, val labels: List<ContactNumberLabel>)
internal sealed interface ContactPickResult {
    data object Cancelled : ContactPickResult
    data object Failed : ContactPickResult
    data object PermissionDenied : ContactPickResult
    data object Empty : ContactPickResult
    data object InvalidNumbers : ContactPickResult
    data object TooMany : ContactPickResult
    data class Selected(val number: String, val name: String?) : ContactPickResult
    data class Choices(val name: String?, val numbers: List<ContactNumber>, val hasUnusableNumbers: Boolean) : ContactPickResult
}

internal const val MAX_CONTACT_PHONE_ROWS = 100

/** Accept only an individual local aggregate, never a collection, directory or cross-user URI. */
internal fun isContactUri(value: String, isEnterpriseId: (Long) -> Boolean): Boolean = try {
    val uri = URI(value)
    val parts = uri.rawPath?.removePrefix("/")?.split("/") ?: emptyList()
    fun personalId(value: String): Boolean = value.toLongOrNull()?.let { it > 0 && !isEnterpriseId(it) } == true
    val pathAllowed = when {
        parts.size == 2 && parts[0] == "contacts" -> personalId(parts[1])
        parts.size in 3..4 && parts[0] == "contacts" && parts[1] == "lookup" -> {
            val key = URLDecoder.decode(parts[2], "UTF-8")
            // Android enterprise lookup keys have the documented AOSP "c-" prefix.
            key.isNotBlank() && !key.startsWith("c-") && (parts.size == 3 || personalId(parts[3]))
        }
        else -> false
    }
    value.length <= 2048 && uri.scheme == "content" && uri.rawAuthority == ContactsContract.AUTHORITY &&
        uri.rawQuery == null && uri.rawFragment == null && pathAllowed
} catch (_: Exception) { false }

/** Reuse entry normalization, but reject digit repairs before grouping potentially different rows. */
internal fun normalizeContactNumber(raw: String, region: String): String? {
    val e164 = Numbers.normalize(raw, region) ?: return null
    val digits = raw.filter { it in '0'..'9' }
    if (raw.trim().startsWith("+")) return e164.takeIf { it == "+$digits" }
    // The same-country formatter omits the IDD prefix; accept 00 only after regional parsing agrees.
    if (digits.startsWith("00") && "+${digits.drop(2)}" == e164) return e164
    val util = PhoneNumberUtil.getInstance()
    val number = util.parse(e164, region)
    val national = util.format(number, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)
    val international = util.formatOutOfCountryCallingNumber(number, region)
    return e164.takeIf {
        digits == util.getNationalSignificantNumber(number) ||
            digits == national.filter(Char::isDigit) ||
            (!international.startsWith("+") && digits == international.filter(Char::isDigit))
    }
}

/** Only the chosen aggregate and its phone rows are queried; both cursors have hard read bounds. */
internal fun readContactPick(
    resultCode: Int,
    uri: String?,
    region: String,
    hasContactsPermission: Boolean,
    isEnterpriseId: (Long) -> Boolean = Contacts::isEnterpriseContactId,
    query: (String, Array<String>, String?, Array<String>?) -> Cursor?,
): ContactPickResult {
    if (resultCode != Activity.RESULT_OK) return ContactPickResult.Cancelled
    if (!hasContactsPermission) return ContactPickResult.PermissionDenied
    if (uri == null || !isContactUri(uri, isEnterpriseId)) return ContactPickResult.Failed
    return try {
        val (id, name) = query("$uri?${ContactsContract.LIMIT_PARAM_KEY}=2", arrayOf(Contacts._ID, Contacts.DISPLAY_NAME), null, null).use { cursor ->
            if (cursor == null || !cursor.moveToFirst()) return ContactPickResult.Failed
            val idIndex = cursor.getColumnIndex(Contacts._ID)
            val id = if (idIndex < 0) null else cursor.getString(idIndex)?.toLongOrNull()
            if (id == null || id <= 0 || isEnterpriseId(id)) return ContactPickResult.Failed
            val path = URI(uri).rawPath.split("/")
            if (path.size == 3 && path[2].toLongOrNull() != id) return ContactPickResult.Failed
            val nameIndex = cursor.getColumnIndex(Contacts.DISPLAY_NAME)
            val name = if (nameIndex < 0) null else cursor.getString(nameIndex)?.trim()?.take(80)?.ifBlank { null }
            if (cursor.moveToNext()) return ContactPickResult.Failed
            id to name
        }
        val phoneUri = "content://${ContactsContract.AUTHORITY}/data/phones?${ContactsContract.LIMIT_PARAM_KEY}=${MAX_CONTACT_PHONE_ROWS + 1}"
        query(phoneUri, arrayOf(Phone.CONTACT_ID, Phone.NUMBER, Phone.TYPE, Phone.LABEL), "${Phone.CONTACT_ID} = ?", arrayOf(id.toString())).use { cursor ->
            if (cursor == null) return ContactPickResult.Failed
            val idIndex = cursor.getColumnIndex(Phone.CONTACT_ID)
            val numberIndex = cursor.getColumnIndex(Phone.NUMBER)
            if (idIndex < 0 || numberIndex < 0) return ContactPickResult.Failed
            val typeIndex = cursor.getColumnIndex(Phone.TYPE)
            val labelIndex = cursor.getColumnIndex(Phone.LABEL)
            val numbers = linkedMapOf<String, MutableSet<ContactNumberLabel>>()
            var rows = 0
            var unusable = false
            while (cursor.moveToNext()) {
                if (++rows > MAX_CONTACT_PHONE_ROWS) return ContactPickResult.TooMany
                if (cursor.getString(idIndex)?.toLongOrNull() != id) return ContactPickResult.Failed
                val e164 = cursor.getString(numberIndex)?.let { normalizeContactNumber(it, region) }
                if (e164 == null) { unusable = true; continue }
                val type = if (typeIndex < 0) Phone.TYPE_OTHER else cursor.getString(typeIndex)?.toIntOrNull() ?: Phone.TYPE_OTHER
                val custom = if (type != Phone.TYPE_CUSTOM || labelIndex < 0) null else cursor.getString(labelIndex)?.trim()?.take(80)?.ifBlank { null }
                numbers.getOrPut(e164) { linkedSetOf() }.add(ContactNumberLabel(type, custom))
            }
            when {
                rows == 0 -> ContactPickResult.Empty
                numbers.isEmpty() -> ContactPickResult.InvalidNumbers
                numbers.size == 1 && !unusable -> ContactPickResult.Selected(numbers.keys.single(), name)
                else -> ContactPickResult.Choices(name, numbers.map { ContactNumber(it.key, it.value.toList()) }, unusable)
            }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: SecurityException) {
        ContactPickResult.PermissionDenied
    } catch (_: Exception) {
        ContactPickResult.Failed
    }
}

internal data class NumberDraft(val label: String, val number: String)

internal fun chooseContactNumber(choices: ContactPickResult.Choices, e164: String): ContactPickResult =
    if (choices.numbers.any { it.e164 == e164 }) ContactPickResult.Selected(e164, choices.name)
    else ContactPickResult.Failed

internal fun applyContactPick(draft: NumberDraft, result: ContactPickResult): NumberDraft = when (result) {
    is ContactPickResult.Selected -> NumberDraft(result.name ?: draft.label, result.number)
    else -> draft
}

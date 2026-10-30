package it.sorguido.unconnected

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.provider.ContactsContract.CommonDataKinds.Phone
import java.net.URI
import java.util.concurrent.CancellationException

internal fun contactPickerIntent(): Intent = Intent(Intent.ACTION_PICK).apply {
    type = Phone.CONTENT_TYPE // Select a phone DATA row, not an arbitrary contact's first number.
}

internal sealed interface ContactPickResult {
    data object Cancelled : ContactPickResult
    data object Failed : ContactPickResult
    data class Selected(val number: String, val name: String?) : ContactPickResult
}

/** Do not let an unexpected picker response expand a row query into a directory scan. */
internal fun isPhoneRowUri(value: String): Boolean = try {
    val uri = URI(value)
    value.length <= 2048 && uri.scheme == "content" && uri.rawAuthority == "com.android.contacts" &&
        uri.rawQuery == null && uri.rawFragment == null &&
        Regex("/data/(?:phones/)?[1-9][0-9]*").matches(uri.rawPath ?: "")
} catch (_: Exception) { false }

/** The query callback is the platform resolver in production; tests supply a bounded cursor. */
internal fun readContactPick(
    resultCode: Int,
    uri: String?,
    query: (String, Array<String>) -> Cursor?,
): ContactPickResult {
    if (resultCode != Activity.RESULT_OK) return ContactPickResult.Cancelled
    if (uri == null || !isPhoneRowUri(uri)) return ContactPickResult.Failed
    return try {
        query(uri, arrayOf(Phone.NUMBER, Phone.DISPLAY_NAME)).use { cursor ->
            if (cursor == null || !cursor.moveToFirst()) return ContactPickResult.Failed
            val numberIndex = cursor.getColumnIndex(Phone.NUMBER)
            if (numberIndex < 0) return ContactPickResult.Failed
            val number = cursor.getString(numberIndex)?.trim()
            if (number.isNullOrBlank() || number.length > 80) return ContactPickResult.Failed
            val nameIndex = cursor.getColumnIndex(Phone.DISPLAY_NAME)
            val name = if (nameIndex < 0) null else cursor.getString(nameIndex)?.trim()?.take(80)?.ifBlank { null }
            if (cursor.moveToNext()) return ContactPickResult.Failed
            ContactPickResult.Selected(number, name)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        // Revoked URI permission, unavailable provider, or malformed provider data: no draft changes.
        ContactPickResult.Failed
    }
}

internal data class NumberDraft(val label: String, val number: String)

internal fun applyContactPick(draft: NumberDraft, result: ContactPickResult): NumberDraft = when (result) {
    is ContactPickResult.Selected -> NumberDraft(result.name ?: draft.label, result.number)
    else -> draft
}

package com.kotlin.mvvm.contact.data

import com.kotlin.mvvm.contact.model.Contact
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** The file was not a Contact Us backup (wrong format, or not JSON at all). */
class InvalidBackupException : Exception()

/**
 * The backup file the user saves with "Export backup" and opens with "Restore from backup". Plain
 * JSON, so it stays readable and can move between phones.
 */
object BackupFormat {

    private const val FORMAT = "contact-us-backup"
    private const val VERSION = 1

    fun encode(contacts: List<Contact>, exportedAt: Long): String {
        val items = JSONArray()
        contacts.forEach { contact ->
            items.put(
                JSONObject()
                    .put("id", contact.id)
                    .put("firstName", contact.firstName)
                    .put("lastName", contact.lastName)
                    .putOpt("email", contact.email)
                    .putOpt("phone", contact.phone)
            )
        }
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", exportedAt)
            .put("contacts", items)
            .toString(2)
    }

    /** Throws [InvalidBackupException] for anything that isn't one of our backups. */
    fun decode(text: String): List<Contact> {
        val root = try {
            JSONObject(text)
        } catch (e: JSONException) {
            throw InvalidBackupException()
        }
        if (root.optString("format") != FORMAT) throw InvalidBackupException()
        val items = root.optJSONArray("contacts") ?: throw InvalidBackupException()
        return (0 until items.length()).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            val id = item.text("id")
            val firstName = item.text("firstName")
            val lastName = item.text("lastName")
            if (id.isEmpty() || (firstName.isEmpty() && lastName.isEmpty())) return@mapNotNull null
            Contact(
                id = id,
                firstName = firstName,
                lastName = lastName,
                email = item.text("email").ifEmpty { null },
                phone = item.text("phone").ifEmpty { null }
            )
        }
    }

    // optString turns an explicit JSON null into the text "null"; treat it as missing instead.
    private fun JSONObject.text(key: String): String =
        if (isNull(key)) "" else optString(key).trim()
}

package com.kotlin.mvvm.contact.data

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Data
import androidx.activity.result.contract.ActivityResultContract
import com.kotlin.mvvm.contact.model.Contact

/** One row of the phone's ContactsContract.Data table, reduced to what the import needs. */
data class PhoneContactRow(
    val lookupKey: String,
    val mimeType: String,
    val displayName: String?,
    val data1: String?,
    val data2: String?,
    val data3: String?,
    val isPrimary: Boolean
)

/** Turns phone-contact rows into this app's contacts. Pure, so it is unit-tested without a phone. */
object PhoneContactsMapper {

    /** Imported contacts keep this prefix plus the phone's lookup key, so a re-import finds them. */
    const val ID_PREFIX = "phone:"

    fun toContacts(rows: List<PhoneContactRow>): List<Contact> =
        rows.groupBy { it.lookupKey }.mapNotNull { (lookupKey, group) ->
            val nameRow = group.firstOrNull { it.mimeType == StructuredName.CONTENT_ITEM_TYPE }
            var firstName = nameRow?.data2?.trim().orEmpty()
            var lastName = nameRow?.data3?.trim().orEmpty()
            if (firstName.isEmpty() && lastName.isEmpty()) {
                val displayName = nameRow?.data1 ?: group.firstNotNullOfOrNull { it.displayName }
                splitDisplayName(displayName.orEmpty()).let { (first, last) ->
                    firstName = first
                    lastName = last
                }
            }
            if (firstName.isEmpty() && lastName.isEmpty()) return@mapNotNull null
            Contact(
                id = ID_PREFIX + lookupKey,
                firstName = firstName,
                lastName = lastName,
                email = preferred(group, Email.CONTENT_ITEM_TYPE),
                phone = preferred(group, Phone.CONTENT_ITEM_TYPE)
            )
        }

    /**
     * Splits a display name at its last space: "Grace Brewster Hopper" → ("Grace Brewster",
     * "Hopper"). A name without spaces, such as most Chinese names, stays whole as the first name.
     */
    fun splitDisplayName(displayName: String): Pair<String, String> {
        val name = displayName.trim().replace(Regex("\\s+"), " ")
        val cut = name.lastIndexOf(' ')
        return if (cut < 0) name to "" else name.substring(0, cut) to name.substring(cut + 1)
    }

    /** Same person if the names match ignoring case and the phone numbers have the same digits. */
    fun duplicateKey(contact: Contact): String =
        listOf(
            contact.firstName.trim().lowercase(),
            contact.lastName.trim().lowercase(),
            contact.phone.orEmpty().filter { it.isDigit() }
        ).joinToString("|")

    private fun preferred(group: List<PhoneContactRow>, mimeType: String): String? =
        group.filter { it.mimeType == mimeType && !it.data1.isNullOrBlank() }
            .sortedByDescending { it.isPrimary }
            .firstOrNull()
            ?.data1
            ?.trim()
}

/** The phone's own address book. Every method needs to run off the main thread. */
interface PhoneContactsSource {

    /** Every contact on the phone. Needs READ_CONTACTS; throws SecurityException without it. */
    fun readAll(): List<Contact>

    /** The contact behind a number picked with [PickPhoneContact], or null if it is gone. */
    fun readPicked(phoneUri: Uri): Contact?
}

/** [PhoneContactsSource] backed by the ContactsContract provider. */
class PhoneContactsReader(private val resolver: ContentResolver) : PhoneContactsSource {

    override fun readAll(): List<Contact> = PhoneContactsMapper.toContacts(queryRows(null, null))

    /**
     * The contact behind a phone number the user picked with [PickPhoneContact]. The picker's
     * one-off grant covers only that number's row (name and number). With READ_CONTACTS also held,
     * the proper first/last name and the email address are filled in too.
     */
    override fun readPicked(phoneUri: Uri): Contact? {
        val picked = resolver.query(
            phoneUri,
            arrayOf(Phone.LOOKUP_KEY, Phone.DISPLAY_NAME, Phone.NUMBER),
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) null
            else Triple(cursor.getString(0), cursor.getString(1), cursor.getString(2))
        } ?: return null
        val (lookupKey, displayName, number) = picked

        val fullDetails = lookupKey?.let { key ->
            try {
                PhoneContactsMapper.toContacts(queryRows("${Data.LOOKUP_KEY} = ?", arrayOf(key)))
                    .firstOrNull()
            } catch (e: SecurityException) {
                null // No READ_CONTACTS: fall back to what the picker granted.
            }
        }
        val (first, last) = PhoneContactsMapper.splitDisplayName(displayName.orEmpty())
        return Contact(
            id = "",
            firstName = fullDetails?.firstName ?: first,
            lastName = fullDetails?.lastName ?: last,
            email = fullDetails?.email,
            phone = number?.trim()?.ifBlank { null } ?: fullDetails?.phone
        )
    }

    private fun queryRows(extraSelection: String?, extraArgs: Array<String>?): List<PhoneContactRow> {
        val mimeTypes = arrayOf(
            StructuredName.CONTENT_ITEM_TYPE,
            Phone.CONTENT_ITEM_TYPE,
            Email.CONTENT_ITEM_TYPE
        )
        val selection = buildString {
            append("${Data.MIMETYPE} IN (?, ?, ?)")
            if (extraSelection != null) append(" AND ($extraSelection)")
        }
        val rows = mutableListOf<PhoneContactRow>()
        resolver.query(
            Data.CONTENT_URI,
            arrayOf(
                Data.LOOKUP_KEY,
                Data.MIMETYPE,
                Data.DISPLAY_NAME_PRIMARY,
                Data.DATA1,
                Data.DATA2,
                Data.DATA3,
                Data.IS_PRIMARY
            ),
            selection,
            mimeTypes + extraArgs.orEmpty(),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val lookupKey = cursor.getString(0) ?: continue
                rows += PhoneContactRow(
                    lookupKey = lookupKey,
                    mimeType = cursor.getString(1).orEmpty(),
                    displayName = cursor.getString(2),
                    data1 = cursor.getString(3),
                    data2 = cursor.getString(4),
                    data3 = cursor.getString(5),
                    isPrimary = cursor.getInt(6) != 0
                )
            }
        }
        return rows
    }
}

/**
 * Opens the system contact picker filtered to phone numbers. No permission is needed: the picker
 * grants read access to the one number the user chooses.
 */
class PickPhoneContact : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_PICK).setType(Phone.CONTENT_TYPE)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent?.data?.takeIf { resultCode == Activity.RESULT_OK }
}

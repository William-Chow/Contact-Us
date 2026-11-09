package com.kotlin.mvvm.contact.data

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import com.kotlin.mvvm.contact.R
import com.kotlin.mvvm.contact.model.Contact
import com.kotlin.mvvm.contact.network.ResponseState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/**
 * The user's contacts, stored on the device. Room is the only source of truth; contacts only
 * arrive from the user: typed in, picked or imported from the phone's address book, or restored
 * from a backup file they chose. Nothing is fetched from the network.
 *
 * Failures (disk full, permission revoked, unreadable file) come back as [ResponseState.Error]
 * with a message for the user, rather than throwing out of the caller's viewModelScope.
 */
class ContactRepository(
    context: Context,
    private val dao: ContactDao = ContactDatabase.get(context).contactDao(),
    private val phoneContacts: PhoneContactsSource = PhoneContactsReader(context.contentResolver)
) {

    private val appContext = context.applicationContext

    fun observeContacts(): Flow<List<Contact>> =
        dao.observeAll().map { entities -> entities.map { it.toContact() } }

    suspend fun getContact(id: String): Contact? = dao.getById(id)?.toContact()

    /** Inserts a contact with a blank id, or updates the existing row. Returns the stored id. */
    suspend fun save(contact: Contact): ResponseState<String> = stored {
        val id = contact.id.ifBlank { UUID.randomUUID().toString() }
        dao.upsert(ContactEntity.from(contact.copy(id = id)))
        id
    }

    suspend fun delete(id: String): ResponseState<Unit> = stored { dao.deleteAndRemember(id) }

    /** The details of a phone number picked in the system contact picker, or null if it's gone. */
    suspend fun readPickedPhoneContact(phoneUri: Uri): ResponseState<Contact?> = stored {
        withContext(Dispatchers.IO) { phoneContacts.readPicked(phoneUri) }
    }

    /**
     * Copies the phone's contacts in. Needs READ_CONTACTS. Skips contacts already imported, ones
     * the user deleted here, and ones they already typed in by hand (same name and number).
     * Returns how many were added.
     */
    suspend fun importFromPhone(): ResponseState<Int> = stored {
        val fromPhone = withContext(Dispatchers.IO) {
            try {
                phoneContacts.readAll()
            } catch (e: SecurityException) {
                throw UserFacingException(R.string.import_permission_denied)
            }
        }
        val alreadyHere = dao.getAll().map { PhoneContactsMapper.duplicateKey(it.toContact()) }.toSet()
        dao.insertMissing(
            fromPhone
                .filterNot { PhoneContactsMapper.duplicateKey(it) in alreadyHere }
                .map { ContactEntity.from(it) }
        )
    }

    /** Writes every contact to the file the user chose. Returns how many were written. */
    suspend fun exportBackup(target: Uri): ResponseState<Int> = stored {
        val contacts = dao.getAll().map { it.toContact() }
        val json = BackupFormat.encode(contacts, System.currentTimeMillis())
        withContext(Dispatchers.IO) {
            try {
                appContext.contentResolver.openOutputStream(target, "wt")
                    ?.bufferedWriter()
                    ?.use { it.write(json) }
                    ?: throw IOException("No output stream for $target")
            } catch (e: IOException) {
                throw UserFacingException(R.string.export_failed)
            }
        }
        contacts.size
    }

    /**
     * Adds the contacts from a backup file the user chose, including ones they deleted here since.
     * Contacts already on the device are left as they are. Returns how many were added.
     */
    suspend fun restoreBackup(source: Uri): ResponseState<Int> = stored {
        val text = withContext(Dispatchers.IO) {
            try {
                appContext.contentResolver.openInputStream(source)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: throw IOException("No input stream for $source")
            } catch (e: IOException) {
                throw UserFacingException(R.string.restore_read_failed)
            }
        }
        val contacts = try {
            BackupFormat.decode(text)
        } catch (e: InvalidBackupException) {
            throw UserFacingException(R.string.restore_invalid)
        }
        dao.restore(contacts.map { ContactEntity.from(it) })
    }

    private suspend fun <T> stored(block: suspend () -> T): ResponseState<T> =
        try {
            ResponseState.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: UserFacingException) {
            ResponseState.Error(null, appContext.getString(e.messageRes))
        } catch (e: Exception) {
            ResponseState.Error(null, appContext.getString(R.string.storage_error))
        }

    /** A failure with a specific message for the user, instead of the generic storage error. */
    private class UserFacingException(@param:StringRes val messageRes: Int) : Exception()
}

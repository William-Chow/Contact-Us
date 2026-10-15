package com.kotlin.mvvm.contact.data

import android.content.Context
import com.kotlin.mvvm.contact.R
import com.kotlin.mvvm.contact.Utils
import com.kotlin.mvvm.contact.model.Contact
import com.kotlin.mvvm.contact.network.ResponseState
import com.kotlin.mvvm.contact.network.repository.Repository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Local-first store. Room is the source of truth, so anything the user saves survives process
 * death and works offline; the network is only an extra source of contacts to import.
 *
 * Writes report failure (disk full, unreadable sample file) as [ResponseState.Error], the same way
 * a failed request does, rather than throwing out of the caller's viewModelScope and crashing.
 */
class ContactRepository(
    context: Context,
    private val dao: ContactDao = ContactDatabase.get(context).contactDao(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val appContext = context.applicationContext

    fun observeContacts(): Flow<List<Contact>> =
        dao.observeAll().map { entities -> entities.map { it.toContact() } }

    suspend fun getContact(id: String): Contact? = dao.getById(id)?.toContact()

    suspend fun isEmpty(): Boolean = dao.count() == 0

    /** Inserts a contact with a blank id, or updates the existing row. Returns the stored id. */
    suspend fun save(contact: Contact): ResponseState<String> = stored {
        val id = contact.id.ifBlank { UUID.randomUUID().toString() }
        dao.upsert(ContactEntity.from(contact.copy(id = id)))
        id
    }

    suspend fun delete(id: String): ResponseState<Unit> = stored { dao.deleteAndRemember(id) }

    /** Pulls the remote directory, adding only contacts the device has neither stored nor deleted. */
    suspend fun syncFromRemote(): ResponseState<Int> =
        when (val response = Repository.getContacts()) {
            is ResponseState.Success -> stored { insertMissing(response.data) }
            is ResponseState.Error -> response
        }

    /** Imports the bundled sample directory, again without touching existing or deleted rows. */
    suspend fun importSampleData(): ResponseState<Int> = stored {
        val sample = withContext(ioDispatcher) { Utils.retrieveBackDataFromJson(appContext) }
        insertMissing(sample)
    }

    private suspend fun insertMissing(contacts: List<Contact>): Int =
        dao.insertMissing(contacts.map { ContactEntity.from(it) })

    private suspend fun <T> stored(block: suspend () -> T): ResponseState<T> =
        try {
            ResponseState.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ResponseState.Error(null, appContext.getString(R.string.storage_error))
        }
}

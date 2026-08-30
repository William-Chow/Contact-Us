package com.kotlin.mvvm.contact.data

import android.content.Context
import com.kotlin.mvvm.contact.Utils
import com.kotlin.mvvm.contact.model.Contact
import com.kotlin.mvvm.contact.network.ResponseState
import com.kotlin.mvvm.contact.network.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Local-first store. Room is the source of truth, so anything the user saves survives process
 * death and works offline; the network is only an extra source of contacts to import.
 */
class ContactRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dao = ContactDatabase.get(appContext).contactDao()

    fun observeContacts(): Flow<List<Contact>> =
        dao.observeAll().map { entities -> entities.map { it.toContact() } }

    suspend fun getContact(id: String): Contact? = dao.getById(id)?.toContact()

    suspend fun isEmpty(): Boolean = dao.count() == 0

    /** Inserts a contact with a blank id, or updates the existing row. Returns the stored id. */
    suspend fun save(contact: Contact): String {
        val id = contact.id.ifBlank { UUID.randomUUID().toString() }
        dao.upsert(ContactEntity.from(contact.copy(id = id)))
        return id
    }

    suspend fun delete(id: String) = dao.deleteById(id)

    /** Pulls the remote directory, adding only contacts the device does not already have. */
    suspend fun syncFromRemote(): ResponseState<Int> =
        when (val response = Repository.getContacts()) {
            is ResponseState.Success -> ResponseState.Success(insertMissing(response.data))
            is ResponseState.Error -> response
        }

    /** Imports the bundled sample directory, again without touching existing rows. */
    suspend fun importSampleData(): Int {
        val sample = withContext(Dispatchers.IO) { Utils.retrieveBackDataFromJson(appContext) }
        return insertMissing(sample)
    }

    private suspend fun insertMissing(contacts: List<Contact>): Int =
        dao.insertMissing(contacts.map { ContactEntity.from(it) }).count { it != -1L }
}

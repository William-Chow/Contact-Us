package com.kotlin.mvvm.contact.data

import android.content.Context
import com.kotlin.mvvm.contact.R
import com.kotlin.mvvm.contact.model.Contact
import com.kotlin.mvvm.contact.network.ResponseState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * The user's contacts, stored on the device. Room is the only source: nothing is fetched or
 * imported, so the list holds exactly what the user added.
 *
 * Writes report failure (e.g. disk full) as [ResponseState.Error] rather than throwing out of the
 * caller's viewModelScope and crashing.
 */
class ContactRepository(
    context: Context,
    private val dao: ContactDao = ContactDatabase.get(context).contactDao()
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

    private suspend fun <T> stored(block: suspend () -> T): ResponseState<T> =
        try {
            ResponseState.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ResponseState.Error(null, appContext.getString(R.string.storage_error))
        }
}

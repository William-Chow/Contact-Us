package com.kotlin.mvvm.contact.network.repository

import com.kotlin.mvvm.contact.model.Contact
import com.kotlin.mvvm.contact.network.ResponseState
import com.kotlin.mvvm.contact.network.Retrofit
import retrofit2.Response

object Repository {

    // Get list of contacts
    suspend fun getContacts(): ResponseState<List<Contact>> = safeCall {
        handle(Retrofit.api.getContacts()) { users -> users.map { it.toContact() } }
    }

    // Get a single contact by its real id
    suspend fun getContact(id: Int): ResponseState<Contact> = safeCall {
        handle(Retrofit.api.getContact(id)) { it.toContact() }
    }

    private inline fun <T, R> handle(response: Response<T>, transform: (T) -> R): ResponseState<R> {
        if (!response.isSuccessful) {
            return ResponseState.Error(response.code(), response.message())
        }
        val body = response.body()
            ?: return ResponseState.Error(response.code(), response.message())
        return ResponseState.Success(transform(body))
    }

    private inline fun <R> safeCall(block: () -> ResponseState<R>): ResponseState<R> =
        try {
            block()
        } catch (e: Exception) {
            ResponseState.Error(null, e.message ?: "Network error")
        }
}

package com.kotlin.mvvm.contact.network.repository

import androidx.lifecycle.MutableLiveData
import com.kotlin.mvvm.contact.model.Contact
import com.kotlin.mvvm.contact.network.ResponseState
import com.kotlin.mvvm.contact.network.Retrofit
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

object Repository {

    val contact = MutableLiveData<Contact>()

    // Get List of Contact
    suspend fun getContacts(): ResponseState<List<Contact>> {
        val response = Retrofit.api.getContacts()
        return if (response.isSuccessful) {
            val responseBody = response.body()
            if (responseBody != null) {
                ResponseState.Success(responseBody.map { it.toContact() })
            } else {
                errorFrom(response)
            }
        } else {
            errorFrom(response)
        }
    }

    // Get Particular Item
    suspend fun getContactPersonal(_selectedItem: Int): ResponseState<Contact> {
        val response = Retrofit.api.getContact(_selectedItem + 1)
        return if (response.isSuccessful) {
            val responseBody = response.body()
            if (responseBody != null) {
                ResponseState.Success(responseBody.toContact())
            } else {
                errorFrom(response)
            }
        } else {
            errorFrom(response)
        }
    }

    suspend fun updateContact(_selectedItem: Int, _contact: Contact): ResponseState<Contact> {
        val response = Retrofit.api.updateContact(_selectedItem + 1, _contact)
        return if (response.isSuccessful) {
            val responseBody = response.body()
            if (responseBody != null) {
                ResponseState.Success(responseBody)
            } else {
                errorFrom(response)
            }
        } else {
            errorFrom(response)
        }
    }

    suspend fun addContact(_contact: Contact): ResponseState<List<Contact>> {
        val response = Retrofit.api.addContact(_contact)
        return if (response.isSuccessful) {
            val responseBody = response.body()
            if (responseBody != null) {
                ResponseState.Success(listOf(responseBody))
            } else {
                errorFrom(response)
            }
        } else {
            errorFrom(response)
        }
    }

    private fun <T> errorFrom(response: Response<*>): ResponseState.Error<T> {
        val errorBody = response.errorBody() ?: response.message().toResponseBody(null)
        return ResponseState.Error(Response.error(response.code(), errorBody))
    }
}

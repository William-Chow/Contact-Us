package com.kotlin.mvvm.contact.network.repository

import androidx.lifecycle.MutableLiveData
import com.kotlin.mvvm.contact.model.Contact
import com.kotlin.mvvm.contact.network.ResponseState
import com.kotlin.mvvm.contact.network.Retrofit
import com.kotlin.mvvm.contact.network.Retrofit.SECRET_KEY

object Repository {

    val contact = MutableLiveData<Contact>()

    // Get List of Contact
    suspend fun getContacts(): ResponseState<List<Contact>> {
        val response = Retrofit.api.getContacts(SECRET_KEY)
        return if (response.isSuccessful) {
            val responseBody = response.body()
            if (responseBody != null) {
                ResponseState.Success(responseBody)
            } else {
                ResponseState.Error(response)
            }
        } else {
            ResponseState.Error(response)
        }
    }

    // Get Particular Item
    suspend fun getContactPersonal(_selectedItem: Int): ResponseState<Contact> {
        val response = Retrofit.api.getContact(SECRET_KEY, _selectedItem)
        return if (response.isSuccessful) {
            val responseBody = response.body()
            if (responseBody != null) {
                ResponseState.Success(responseBody)
            } else {
                ResponseState.Error(response)
            }
        } else {
            ResponseState.Error(response)
        }
    }

    suspend fun updateContact(_selectedItem: Int, _contact: Contact): ResponseState<Contact> {
        val response = Retrofit.api.updateContact(SECRET_KEY, _selectedItem, _contact)
        return if (response.isSuccessful) {
            val responseBody = response.body()
            if (responseBody != null) {
                ResponseState.Success(responseBody)
            } else {
                ResponseState.Error(response)
            }
        } else {
            ResponseState.Error(response)
        }
    }

    suspend fun addContact(_contact: Contact): ResponseState<List<Contact>> {
        val response = Retrofit.api.addContact(SECRET_KEY, _contact)
        return if (response.isSuccessful) {
            val responseBody = response.body()
            if (responseBody != null) {
                ResponseState.Success(responseBody)
            } else {
                ResponseState.Error(response)
            }
        } else {
            ResponseState.Error(response)
        }
    }
}
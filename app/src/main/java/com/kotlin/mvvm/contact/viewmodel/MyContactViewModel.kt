package com.kotlin.mvvm.contact.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.mvvm.contact.R
import com.kotlin.mvvm.contact.Utils
import com.kotlin.mvvm.contact.model.Contact
import com.kotlin.mvvm.contact.network.ResponseState
import com.kotlin.mvvm.contact.network.repository.Repository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MyContactViewModel : ViewModel() {

    val errorMessage = MutableLiveData<String>()

    // Coroutines
    private var job: Job? = null

    val contactLiveData = MutableLiveData<Contact>()
    val pbLoading = MutableLiveData<Boolean>()
    val isUpdateContactSuccess = MutableLiveData<Boolean>()
    val isAddedContactSuccess = MutableLiveData<Boolean>()

    fun getContact(selectedItem: Int) {
        job = viewModelScope.launch {
            when (val response = Repository.getContactPersonal(selectedItem)) {
                is ResponseState.Success -> {
                    pbLoading.value = false
                    contactLiveData.postValue(response.data)
                }

                is ResponseState.Error -> {
                    pbLoading.value = false
                    // Handling the Error State in Future
                    onError("Unable to get Contact " + response.response.code() + " " + response.response.message())
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
    }

    fun checkInternetConnection(context: Context): Boolean {
        return Utils.checkInternetConnection(context)
    }

    fun checkValidator(context: Context, selectedContactItem: Int, id: String, firstName: String, lastName: String, email: String, phone: String) {
        if (firstName.isEmpty()) {
            onError(context.getString(R.string.contact_first_name_empty))
        } else if (lastName.isEmpty()) {
            onError(context.getString(R.string.contact_last_name_empty))
        } else if (email.isNotEmpty()) {
            if (Utils.isEmail(email) != true) onError(context.getString(R.string.contact_email_invalid))
            else addOrUpdate(context, selectedContactItem, id, firstName, lastName, email, phone)
        } else {
            addOrUpdate(context, selectedContactItem, id, firstName, lastName, email, phone)
        }
    }

    private fun addOrUpdate(context: Context, selectedContactItem: Int, id: String, firstName: String, lastName: String, email: String, phone: String) {
        // Update/Add the Contact
        if (selectedContactItem != -1) {
            pbLoading.value = true
            // Update User
            updateContact(context, selectedContactItem, Contact(id, firstName, lastName, email, phone))
        } else {
            pbLoading.value = true
            // Add User
            addContact(context, Contact(firstName + lastName, firstName, lastName, email, phone))
        }
    }

    private fun updateContact(context: Context, selectedContactItem: Int, contact: Contact) {
        job = viewModelScope.launch {
            when (val response = Repository.updateContact(selectedContactItem, contact)) {
                is ResponseState.Success -> {
                    pbLoading.value = false
                    isUpdateContactSuccess.postValue(true)
                }

                is ResponseState.Error -> {
                    pbLoading.value = false
                    isUpdateContactSuccess.postValue(false)
                    // Handling the Error State in Future
                    onError(context.getString(R.string.contact_update_failed) + response.response.code() + " " + response.response.message())
                }
            }
        }
    }

    private fun addContact(context: Context, contact: Contact) {
        job = viewModelScope.launch {
            when (val response = Repository.addContact(contact)) {
                is ResponseState.Success -> {
                    pbLoading.value = false
                    isAddedContactSuccess.postValue(true)
                }

                is ResponseState.Error -> {
                    pbLoading.value = false
                    isAddedContactSuccess.postValue(false)
                    // Handling the Error State in Future
                    onError(context.getString(R.string.contact_added_failed) + response.response.code() + " " + response.response.message())
                }
            }
        }
    }

    private fun onError(message: String) {
        errorMessage.value = message
    }
}
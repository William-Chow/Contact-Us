package com.kotlin.mvvm.contact.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.mvvm.contact.R
import com.kotlin.mvvm.contact.Utils
import com.kotlin.mvvm.contact.model.Contact
import com.kotlin.mvvm.contact.network.ResponseState
import com.kotlin.mvvm.contact.network.repository.Repository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class MyContactViewModel(application: Application) : AndroidViewModel(application) {

    data class ContactForm(
        val firstName: String = "",
        val lastName: String = "",
        val email: String = "",
        val phone: String = ""
    )

    private val _form = MutableStateFlow(ContactForm())
    val form: StateFlow<ContactForm> = _form.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // -1 means a brand new contact; otherwise the real contact id being edited.
    private var contactId: String = ""
    private var selectedItemId: Int = -1
    private var started = false

    /** Loads the selected contact once; safe to call from a LaunchedEffect across recompositions. */
    fun start(selectedItem: Int) {
        if (started) return
        started = true
        selectedItemId = selectedItem
        if (selectedItem == -1) return

        if (!Utils.checkInternetConnection(getApplication())) {
            emit(getString(R.string.internet_connection_issues))
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            when (val response = Repository.getContact(selectedItem)) {
                is ResponseState.Success -> {
                    contactId = response.data.id
                    _form.value = ContactForm(
                        firstName = response.data.firstName,
                        lastName = response.data.lastName,
                        email = response.data.email.orEmpty(),
                        phone = response.data.phone.orEmpty()
                    )
                }
                is ResponseState.Error ->
                    emit("Unable to get Contact ${response.code ?: ""} ${response.message}".trim())
            }
            _isLoading.value = false
        }
    }

    fun onFirstNameChange(value: String) { _form.value = _form.value.copy(firstName = value) }
    fun onLastNameChange(value: String) { _form.value = _form.value.copy(lastName = value) }
    fun onEmailChange(value: String) { _form.value = _form.value.copy(email = value) }
    fun onPhoneChange(value: String) { _form.value = _form.value.copy(phone = value) }

    fun save() {
        val form = _form.value
        when {
            form.firstName.isEmpty() -> emit(getString(R.string.contact_first_name_empty))
            form.lastName.isEmpty() -> emit(getString(R.string.contact_last_name_empty))
            form.email.isNotEmpty() && !Utils.isEmail(form.email) ->
                emit(getString(R.string.contact_email_invalid))
            else -> submit(form)
        }
    }

    private fun submit(form: ContactForm) {
        val isUpdate = selectedItemId != -1
        _isLoading.value = true
        viewModelScope.launch {
            val response = if (isUpdate) {
                Repository.updateContact(
                    selectedItemId,
                    Contact(contactId, form.firstName, form.lastName, form.email, form.phone)
                )
            } else {
                // The mock API assigns the id; send an empty one rather than a synthetic value.
                Repository.addContact(
                    Contact("", form.firstName, form.lastName, form.email, form.phone)
                )
            }
            when (response) {
                is ResponseState.Success -> {
                    _form.value = ContactForm()
                    emit(
                        getString(
                            if (isUpdate) R.string.contact_update_success
                            else R.string.contact_added_success
                        )
                    )
                }
                is ResponseState.Error -> emit(
                    getString(
                        if (isUpdate) R.string.contact_update_failed
                        else R.string.contact_added_failed
                    )
                )
            }
            _isLoading.value = false
        }
    }

    private fun emit(message: String) {
        _events.trySend(message)
    }

    private fun getString(resId: Int): String = getApplication<Application>().getString(resId)
}

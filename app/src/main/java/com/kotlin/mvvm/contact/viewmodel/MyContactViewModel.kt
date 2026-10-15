package com.kotlin.mvvm.contact.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.mvvm.contact.R
import com.kotlin.mvvm.contact.Utils
import com.kotlin.mvvm.contact.data.ContactRepository
import com.kotlin.mvvm.contact.model.Contact
import com.kotlin.mvvm.contact.network.ResponseState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

// The single-argument constructor is what ViewModelProvider calls; tests pass a repository
// backed by an in-memory database.
class MyContactViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: ContactRepository = ContactRepository(application)
) : AndroidViewModel(application) {

    sealed interface UiEvent {
        data class Message(val text: String) : UiEvent

        /** Emitted after a save or delete lands, so the screen closes itself instead of stranding the user. */
        data object Close : UiEvent
    }

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

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // null means a brand new contact; otherwise the id of the contact being edited.
    private var contactId: String? = null
    private var started = false

    // A save or delete closes the screen, so only one may run, and none after one has landed (a
    // failed one re-allows it). Without this, taps that land before finish() write again: a
    // double-tapped Save inserts the new contact twice, and a Save right after Delete restores it.
    private var writeStarted = false

    /** Loads the selected contact once; safe to call from a LaunchedEffect across recompositions. */
    fun start(id: String?) {
        if (started) return
        started = true
        contactId = id
        if (id == null) return

        _isLoading.value = true
        viewModelScope.launch {
            val contact = repository.getContact(id)
            if (contact == null) {
                emit(getString(R.string.contact_not_found))
                _events.trySend(UiEvent.Close)
            } else {
                _form.value = ContactForm(
                    firstName = contact.firstName,
                    lastName = contact.lastName,
                    email = contact.email.orEmpty(),
                    phone = contact.phone.orEmpty()
                )
            }
            _isLoading.value = false
        }
    }

    fun onFirstNameChange(value: String) { _form.value = _form.value.copy(firstName = value) }
    fun onLastNameChange(value: String) { _form.value = _form.value.copy(lastName = value) }
    fun onEmailChange(value: String) { _form.value = _form.value.copy(email = value) }
    fun onPhoneChange(value: String) { _form.value = _form.value.copy(phone = value) }

    fun save() {
        if (writeStarted) return
        val form = _form.value
        when {
            form.firstName.isBlank() -> emit(getString(R.string.contact_first_name_empty))
            form.lastName.isBlank() -> emit(getString(R.string.contact_last_name_empty))
            form.email.isNotBlank() && !Utils.isEmail(form.email.trim()) ->
                emit(getString(R.string.contact_email_invalid))
            else -> persist(form)
        }
    }

    private fun persist(form: ContactForm) {
        val isUpdate = contactId != null
        writeStarted = true
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.save(
                Contact(
                    id = contactId.orEmpty(),
                    firstName = form.firstName.trim(),
                    lastName = form.lastName.trim(),
                    email = form.email.trim().ifBlank { null },
                    phone = form.phone.trim().ifBlank { null }
                )
            )
            _isLoading.value = false
            when (result) {
                is ResponseState.Success -> {
                    emit(
                        getString(
                            if (isUpdate) R.string.contact_update_success
                            else R.string.contact_added_success
                        )
                    )
                    _events.trySend(UiEvent.Close)
                }
                is ResponseState.Error -> onWriteFailed(result)
            }
        }
    }

    fun delete() {
        if (writeStarted) return
        val id = contactId ?: return
        writeStarted = true
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.delete(id)
            _isLoading.value = false
            when (result) {
                is ResponseState.Success -> {
                    emit(getString(R.string.contact_deleted))
                    _events.trySend(UiEvent.Close)
                }
                is ResponseState.Error -> onWriteFailed(result)
            }
        }
    }

    /** Nothing was written and the screen stays open, so the user may try again. */
    private fun onWriteFailed(error: ResponseState.Error) {
        writeStarted = false
        emit(error.message)
    }

    private fun emit(message: String) {
        _events.trySend(UiEvent.Message(message))
    }

    private fun getString(resId: Int): String = getApplication<Application>().getString(resId)
}

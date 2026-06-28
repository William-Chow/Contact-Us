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

class ContactViewModel(application: Application) : AndroidViewModel(application) {

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun onQueryChange(value: String) {
        _query.value = value
    }

    // One-shot messages (errors / info). Channel avoids the sticky-event replay of LiveData.
    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var loaded = false

    /** Loads the list only on the first composition; ignored on later recompositions. */
    fun loadInitial() {
        if (loaded) return
        loaded = true
        loadContacts()
    }

    fun loadContacts() {
        if (!Utils.checkInternetConnection(getApplication())) {
            emit(getString(R.string.internet_connection_issues))
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            when (val response = Repository.getContacts()) {
                is ResponseState.Success -> _contacts.value = response.data
                is ResponseState.Error -> emit("${response.code ?: ""} ${response.message}".trim())
            }
            _isLoading.value = false
        }
    }

    fun loadBackup() {
        _contacts.value = Utils.retrieveBackDataFromJson(getApplication())
    }

    private fun emit(message: String) {
        _events.trySend(message)
    }

    private fun getString(resId: Int): String = getApplication<Application>().getString(resId)
}

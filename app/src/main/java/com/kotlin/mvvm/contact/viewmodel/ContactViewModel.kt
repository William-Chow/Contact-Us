package com.kotlin.mvvm.contact.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.mvvm.contact.R
import com.kotlin.mvvm.contact.Utils
import com.kotlin.mvvm.contact.data.ContactRepository
import com.kotlin.mvvm.contact.model.Contact
import com.kotlin.mvvm.contact.network.ResponseState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// The single-argument constructor is what ViewModelProvider calls; tests pass a repository
// backed by an in-memory database.
class ContactViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: ContactRepository = ContactRepository(application)
) : AndroidViewModel(application) {

    // Backed by Room, so a save or delete on the edit screen shows up here without a manual refresh.
    // Null until Room's first emission, so "not loaded yet" isn't mistaken for "no contacts".
    val contacts: StateFlow<List<Contact>?> = repository.observeContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

    // Sync and sample import both write to Room and drive the one spinner, so only one runs at a
    // time; taps while one is running are ignored rather than queued.
    private var work: Job? = null

    /** First launch on an empty database pulls the directory once; later launches read from Room. */
    fun loadInitial() {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            if (repository.isEmpty()) sync(announce = false)
        }
    }

    fun onSyncClick() = sync(announce = true)

    private fun sync(announce: Boolean) {
        if (work?.isActive == true) return
        if (!Utils.checkInternetConnection(getApplication())) {
            if (announce) emit(getString(R.string.internet_connection_issues))
            return
        }
        _isLoading.value = true
        work = viewModelScope.launch {
            when (val response = repository.syncFromRemote()) {
                is ResponseState.Success -> if (announce) emit(importMessage(response.data))
                is ResponseState.Error ->
                    if (announce) emit("${response.code ?: ""} ${response.message}".trim())
            }
            _isLoading.value = false
        }
    }

    fun importSampleData() {
        if (work?.isActive == true) return
        _isLoading.value = true
        work = viewModelScope.launch {
            when (val result = repository.importSampleData()) {
                is ResponseState.Success -> emit(importMessage(result.data))
                is ResponseState.Error -> emit(result.message)
            }
            _isLoading.value = false
        }
    }

    private fun importMessage(added: Int): String =
        if (added == 0) {
            getString(R.string.import_up_to_date)
        } else {
            getApplication<Application>().resources
                .getQuantityString(R.plurals.import_added, added, added)
        }

    /** Surfaces an error raised outside the ViewModel (e.g. the UMP consent form). */
    fun reportError(message: String) = emit(message)

    private fun emit(message: String) {
        _events.trySend(message)
    }

    private fun getString(resId: Int): String = getApplication<Application>().getString(resId)
}

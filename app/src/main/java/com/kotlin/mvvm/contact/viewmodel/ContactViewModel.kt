package com.kotlin.mvvm.contact.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.mvvm.contact.data.ContactRepository
import com.kotlin.mvvm.contact.model.Contact
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

// The single-argument constructor is what ViewModelProvider calls; tests pass a repository
// backed by an in-memory database.
class ContactViewModel @JvmOverloads constructor(
    application: Application,
    repository: ContactRepository = ContactRepository(application)
) : AndroidViewModel(application) {

    // Backed by Room, so a save or delete on the edit screen shows up here without a manual refresh.
    // Null until Room's first emission, so "not loaded yet" isn't mistaken for "no contacts".
    // Only the user's own contacts live here: nothing is imported behind their back.
    val contacts: StateFlow<List<Contact>?> = repository.observeContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun onQueryChange(value: String) {
        _query.value = value
    }

    // One-shot messages (errors / info). Channel avoids the sticky-event replay of LiveData.
    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Surfaces an error raised outside the ViewModel (e.g. the UMP consent form). */
    fun reportError(message: String) {
        _events.trySend(message)
    }
}

package com.kotlin.mvvm.contact.viewmodel

import android.app.Application
import android.net.Uri
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.mvvm.contact.R
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
    // Only the user's own contacts live here: nothing is imported unless they ask for it.
    val contacts: StateFlow<List<Contact>?> = repository.observeContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    fun onQueryChange(value: String) {
        _query.value = value
    }

    // One-shot messages (errors / info). Channel avoids the sticky-event replay of LiveData.
    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // Import, export and restore all hit the database and drive the one spinner, so only one runs
    // at a time; taps while one is running are ignored rather than queued.
    private var work: Job? = null

    /** Copies the phone's contacts in. Call only once READ_CONTACTS is granted. */
    fun importFromPhone() = runExclusive(
        task = { repository.importFromPhone() },
        added = R.plurals.import_added
    )

    fun exportBackup(target: Uri) = runExclusive(
        task = { repository.exportBackup(target) },
        added = R.plurals.backup_exported,
        noneMessage = R.string.backup_exported_empty
    )

    fun restoreBackup(source: Uri) = runExclusive(
        task = { repository.restoreBackup(source) },
        added = R.plurals.restore_added
    )

    fun onImportPermissionDenied() = emit(getString(R.string.import_permission_denied))

    /** Surfaces an error raised outside the ViewModel (e.g. the UMP consent form). */
    fun reportError(message: String) = emit(message)

    private fun runExclusive(
        task: suspend () -> ResponseState<Int>,
        @PluralsRes added: Int,
        @StringRes noneMessage: Int = R.string.import_up_to_date
    ) {
        if (work?.isActive == true) return
        _isBusy.value = true
        work = viewModelScope.launch {
            when (val result = task()) {
                is ResponseState.Success -> emit(
                    if (result.data == 0) getString(noneMessage)
                    else getApplication<Application>().resources
                        .getQuantityString(added, result.data, result.data)
                )
                is ResponseState.Error -> emit(result.message)
            }
            _isBusy.value = false
        }
    }

    private fun emit(message: String) {
        _events.trySend(message)
    }

    private fun getString(resId: Int): String = getApplication<Application>().getString(resId)
}

package com.kotlin.mvvm.contact.view.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import com.kotlin.mvvm.contact.Utils
import com.kotlin.mvvm.contact.ads.ConsentManager
import com.kotlin.mvvm.contact.view.compose.ContactListScreen
import com.kotlin.mvvm.contact.view.compose.ContactUsTheme
import com.kotlin.mvvm.contact.viewmodel.ContactViewModel

class ContactActivity : AppCompatActivity() {

    private lateinit var viewModel: ContactViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[ContactViewModel::class.java]

        setContent {
            ContactUsTheme {
                val contacts by viewModel.contacts.collectAsState()
                val query by viewModel.query.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }
                var showPrivacyOptions by remember {
                    mutableStateOf(ConsentManager.isPrivacyOptionsRequired(this@ContactActivity))
                }

                val filtered by remember {
                    derivedStateOf {
                        val q = query.trim()
                        if (q.isEmpty()) contacts
                        else contacts.filter {
                            "${it.firstName} ${it.lastName}".contains(q, ignoreCase = true)
                        }
                    }
                }

                LaunchedEffect(Unit) { viewModel.loadInitial() }
                LaunchedEffect(Unit) {
                    viewModel.events.collect { snackbarHostState.showSnackbar(it) }
                }

                ContactListScreen(
                    contacts = filtered,
                    totalCount = contacts.size,
                    searchQuery = query,
                    isLoading = isLoading,
                    snackbarHostState = snackbarHostState,
                    onSearchChange = viewModel::onQueryChange,
                    onSyncClick = { viewModel.onSyncClick() },
                    onBackupClick = { viewModel.importSampleData() },
                    onAddClick = { openEdit(null) },
                    onContactClick = { contact -> openEdit(contact.id) },
                    showPrivacyOptions = showPrivacyOptions,
                    onPrivacyOptionsClick = {
                        ConsentManager.showPrivacyOptions(this@ContactActivity) { errorMessage ->
                            if (errorMessage != null) viewModel.reportError(errorMessage)
                            // Only meaningful once the user has closed the form: their choice can
                            // clear the requirement to offer it at all.
                            showPrivacyOptions =
                                ConsentManager.isPrivacyOptionsRequired(this@ContactActivity)
                        }
                    }
                )
            }
        }
    }

    /** Opens the edit screen. A null id starts a new contact. */
    private fun openEdit(contactId: String?) {
        val intent = Intent(this, MyContactActivity::class.java)
        if (contactId != null) intent.putExtra(Utils.EXTRA_CONTACT_ID, contactId)
        startActivity(intent)
    }
}

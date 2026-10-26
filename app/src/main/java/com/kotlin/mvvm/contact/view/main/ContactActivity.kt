package com.kotlin.mvvm.contact.view.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
                val snackbarHostState = remember { SnackbarHostState() }
                val consent by ConsentManager.state.collectAsState()

                val filtered by remember {
                    derivedStateOf {
                        val all = contacts.orEmpty()
                        val q = query.trim()
                        if (q.isEmpty()) all
                        else all.filter {
                            "${it.firstName} ${it.lastName}".contains(q, ignoreCase = true)
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.events.collect { snackbarHostState.showSnackbar(it) }
                }

                ContactListScreen(
                    contacts = filtered,
                    totalCount = contacts?.size ?: 0,
                    hasLoaded = contacts != null,
                    searchQuery = query,
                    snackbarHostState = snackbarHostState,
                    onSearchChange = viewModel::onQueryChange,
                    onAddClick = { openEdit(null) },
                    onContactClick = { contact -> openEdit(contact.id) },
                    showPrivacyOptions = consent.privacyOptionsRequired,
                    onPrivacyOptionsClick = {
                        ConsentManager.showPrivacyOptions(this@ContactActivity) { errorMessage ->
                            if (errorMessage != null) viewModel.reportError(errorMessage)
                        }
                    }
                )
            }
        }

        // The splash normally settles consent before handing over. If the user skipped it or it
        // timed out first, gather here so the form still appears; ads stay off until it settles.
        if (!ConsentManager.isSettled) ConsentManager.gather(this)
    }

    /** Opens the edit screen. A null id starts a new contact. */
    private fun openEdit(contactId: String?) {
        val intent = Intent(this, MyContactActivity::class.java)
        if (contactId != null) intent.putExtra(Utils.EXTRA_CONTACT_ID, contactId)
        startActivity(intent)
    }
}

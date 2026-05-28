package com.kotlin.mvvm.contact.view.main

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.ads.MobileAds
import com.kotlin.mvvm.contact.R
import com.kotlin.mvvm.contact.Utils
import com.kotlin.mvvm.contact.model.Contact
import com.kotlin.mvvm.contact.view.compose.ContactListScreen
import com.kotlin.mvvm.contact.view.compose.ContactUsTheme
import com.kotlin.mvvm.contact.viewmodel.ContactViewModel
import kotlinx.coroutines.launch

class ContactActivity : AppCompatActivity() {

    private lateinit var contactViewModel: ContactViewModel
    private var contacts by mutableStateOf<List<Contact>>(emptyList())
    private var isLoading by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        contactViewModel = ViewModelProvider(this)[ContactViewModel::class.java]
        MobileAds.initialize(this) { }

        contactViewModel.contactLiveData.observe(this) { contacts = it }
        contactViewModel.pbLoading.observe(this) { isLoading = it }
        contactViewModel.errorMessage.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }

        setContent {
            ContactUsTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    loadContacts(snackbarHostState)
                }

                ContactListScreen(
                    contacts = contacts,
                    isLoading = isLoading,
                    snackbarHostState = snackbarHostState,
                    onSyncClick = {
                        coroutineScope.launch { loadContacts(snackbarHostState) }
                    },
                    onBackupClick = {
                        contacts = Utils.retrieveBackDataFromJson(this@ContactActivity)
                    },
                    onAddClick = {
                        contactViewModel.intentAddContact(this@ContactActivity, -1)
                    },
                    onContactClick = { selectedItem ->
                        contactViewModel.intentAddContact(this@ContactActivity, selectedItem)
                    }
                )
            }
        }
    }

    private suspend fun loadContacts(snackbarHostState: SnackbarHostState) {
        if (contactViewModel.checkInternetConnection(this)) {
            isLoading = true
            contactViewModel.getContacts()
        } else {
            snackbarHostState.showSnackbar(getString(R.string.internet_connection_issues))
        }
    }
}

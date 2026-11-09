package com.kotlin.mvvm.contact.view.main

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import com.kotlin.mvvm.contact.R
import com.kotlin.mvvm.contact.Utils
import com.kotlin.mvvm.contact.ads.ConsentManager
import com.kotlin.mvvm.contact.model.ContactNames
import com.kotlin.mvvm.contact.view.compose.ContactListScreen
import com.kotlin.mvvm.contact.view.compose.ContactUsTheme
import com.kotlin.mvvm.contact.view.compose.ImportContactsDisclosure
import com.kotlin.mvvm.contact.viewmodel.ContactViewModel
import java.time.LocalDate

class ContactActivity : AppCompatActivity() {

    private lateinit var viewModel: ContactViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        // Transparent system bars on every Android version, not only 15+ where it is enforced;
        // the Compose screens already pad for the status bar, navigation bar and keyboard.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[ContactViewModel::class.java]

        setContent {
            ContactUsTheme {
                val contacts by viewModel.contacts.collectAsState()
                val query by viewModel.query.collectAsState()
                val isBusy by viewModel.isBusy.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }
                val consent by ConsentManager.state.collectAsState()
                var showImportDisclosure by rememberSaveable { mutableStateOf(false) }

                val filtered by remember {
                    derivedStateOf {
                        val all = contacts.orEmpty()
                        val q = query.trim()
                        if (q.isEmpty()) all
                        else all.filter {
                            ContactNames.displayName(it.firstName, it.lastName).contains(q, ignoreCase = true) ||
                                "${it.firstName} ${it.lastName}".contains(q, ignoreCase = true) ||
                                it.phone?.contains(q) == true ||
                                it.email?.contains(q, ignoreCase = true) == true
                        }
                    }
                }

                val contactsPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) viewModel.importFromPhone() else viewModel.onImportPermissionDenied()
                }
                val exportFile = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE)
                ) { uri -> if (uri != null) viewModel.exportBackup(uri) }
                val restoreFile = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri -> if (uri != null) viewModel.restoreBackup(uri) }

                LaunchedEffect(Unit) {
                    viewModel.events.collect { snackbarHostState.showSnackbar(it) }
                }

                ContactListScreen(
                    contacts = filtered,
                    totalCount = contacts?.size ?: 0,
                    hasLoaded = contacts != null,
                    searchQuery = query,
                    isBusy = isBusy,
                    snackbarHostState = snackbarHostState,
                    onSearchChange = viewModel::onQueryChange,
                    onAddClick = { openEdit(null) },
                    onContactClick = { contact -> openEdit(contact.id) },
                    onImportClick = {
                        // The disclosure only comes before the first permission request; once
                        // granted, importing again just runs.
                        if (hasContactsPermission()) viewModel.importFromPhone()
                        else showImportDisclosure = true
                    },
                    onExportClick = {
                        startOrExplain { exportFile.launch("contact-us-backup-${LocalDate.now()}.json") }
                    },
                    onRestoreClick = {
                        startOrExplain { restoreFile.launch(RESTORE_MIME_TYPES) }
                    },
                    onPrivacyPolicyClick = {
                        startOrExplain {
                            startActivity(
                                Intent(Intent.ACTION_VIEW, getString(R.string.privacy_policy_url).toUri())
                            )
                        }
                    },
                    showPrivacyOptions = consent.privacyOptionsRequired,
                    onPrivacyOptionsClick = {
                        ConsentManager.showPrivacyOptions(this@ContactActivity) { errorMessage ->
                            if (errorMessage != null) viewModel.reportError(errorMessage)
                        }
                    }
                )

                if (showImportDisclosure) {
                    ImportContactsDisclosure(
                        onContinue = {
                            showImportDisclosure = false
                            contactsPermission.launch(Manifest.permission.READ_CONTACTS)
                        },
                        onDismiss = { showImportDisclosure = false }
                    )
                }
            }
        }

        // The splash normally settles consent before handing over. If the user skipped it or it
        // timed out first, gather here so the form still appears; ads stay off until it settles.
        if (!ConsentManager.isSettled) ConsentManager.gather(this)
    }

    private fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    /** Some stripped-down phones ship without a document picker or a browser. */
    private fun startOrExplain(launch: () -> Unit) {
        try {
            launch()
        } catch (e: ActivityNotFoundException) {
            viewModel.reportError(getString(R.string.no_app_for_action))
        }
    }

    /** Opens the edit screen. A null id starts a new contact. */
    private fun openEdit(contactId: String?) {
        val intent = Intent(this, MyContactActivity::class.java)
        if (contactId != null) intent.putExtra(Utils.EXTRA_CONTACT_ID, contactId)
        startActivity(intent)
    }

    private companion object {
        const val BACKUP_MIME_TYPE = "application/json"

        // File managers label .json files inconsistently, so accept the usual variants.
        val RESTORE_MIME_TYPES = arrayOf("application/json", "text/*", "application/octet-stream")
    }
}

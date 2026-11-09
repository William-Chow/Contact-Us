package com.kotlin.mvvm.contact.view.main

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import com.kotlin.mvvm.contact.R
import com.kotlin.mvvm.contact.Utils
import com.kotlin.mvvm.contact.data.PickPhoneContact
import com.kotlin.mvvm.contact.view.compose.ContactUsTheme
import com.kotlin.mvvm.contact.view.compose.EditContactScreen
import com.kotlin.mvvm.contact.viewmodel.MyContactViewModel

class MyContactActivity : AppCompatActivity() {

    private lateinit var viewModel: MyContactViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        // Transparent system bars on every Android version, not only 15+ where it is enforced;
        // the Compose screens already pad for the status bar, navigation bar and keyboard.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[MyContactViewModel::class.java]
        val selectedId = intent.getStringExtra(Utils.EXTRA_CONTACT_ID)

        setContent {
            ContactUsTheme {
                val form by viewModel.form.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val context = LocalContext.current
                val pickPhoneContact = rememberLauncherForActivityResult(PickPhoneContact()) { uri ->
                    if (uri != null) viewModel.fillFromPhoneContact(uri)
                }

                LaunchedEffect(Unit) { viewModel.start(selectedId) }
                LaunchedEffect(Unit) {
                    viewModel.events.collect { event ->
                        when (event) {
                            is MyContactViewModel.UiEvent.Message ->
                                Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()

                            MyContactViewModel.UiEvent.Close -> finish()
                        }
                    }
                }

                EditContactScreen(
                    title = stringResource(
                        if (selectedId == null) R.string.new_contact else R.string.edit_contact
                    ),
                    firstName = form.firstName,
                    lastName = form.lastName,
                    email = form.email,
                    phone = form.phone,
                    isLoading = isLoading,
                    isExisting = selectedId != null,
                    onFirstNameChange = viewModel::onFirstNameChange,
                    onLastNameChange = viewModel::onLastNameChange,
                    onEmailChange = viewModel::onEmailChange,
                    onPhoneChange = viewModel::onPhoneChange,
                    onCancelClick = { finish() },
                    onSaveClick = { viewModel.save() },
                    onDeleteClick = { viewModel.delete() },
                    onFillFromContactsClick = { startOrExplain { pickPhoneContact.launch(Unit) } },
                    onCallClick = {
                        startOrExplain { startActivity(handOff(Intent.ACTION_DIAL, "tel", form.phone)) }
                    },
                    onMessageClick = {
                        startOrExplain { startActivity(handOff(Intent.ACTION_SENDTO, "smsto", form.phone)) }
                    },
                    onEmailClick = {
                        startOrExplain { startActivity(handOff(Intent.ACTION_SENDTO, "mailto", form.email)) }
                    }
                )
            }
        }
    }

    // These hand over to the phone's dialler, messaging and mail apps, so no permission is needed.
    private fun handOff(action: String, scheme: String, target: String): Intent =
        Intent(action, Uri.fromParts(scheme, target.trim(), null))

    /** A phone without a dialler, messaging or contacts app throws instead of opening one. */
    private fun startOrExplain(start: () -> Unit) {
        try {
            start()
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_app_for_action, Toast.LENGTH_SHORT).show()
        }
    }
}

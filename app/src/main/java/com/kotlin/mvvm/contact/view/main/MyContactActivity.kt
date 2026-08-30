package com.kotlin.mvvm.contact.view.main

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import com.kotlin.mvvm.contact.R
import com.kotlin.mvvm.contact.Utils
import com.kotlin.mvvm.contact.view.compose.ContactUsTheme
import com.kotlin.mvvm.contact.view.compose.EditContactScreen
import com.kotlin.mvvm.contact.viewmodel.MyContactViewModel

class MyContactActivity : AppCompatActivity() {

    private lateinit var viewModel: MyContactViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[MyContactViewModel::class.java]
        val selectedId = intent.getStringExtra(Utils.EXTRA_CONTACT_ID)

        setContent {
            ContactUsTheme {
                val form by viewModel.form.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val context = LocalContext.current

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
                    canDelete = selectedId != null,
                    onFirstNameChange = viewModel::onFirstNameChange,
                    onLastNameChange = viewModel::onLastNameChange,
                    onEmailChange = viewModel::onEmailChange,
                    onPhoneChange = viewModel::onPhoneChange,
                    onCancelClick = { finish() },
                    onSaveClick = { viewModel.save() },
                    onDeleteClick = { viewModel.delete() }
                )
            }
        }
    }
}

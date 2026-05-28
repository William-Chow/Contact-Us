package com.kotlin.mvvm.contact.view.main

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.ads.MobileAds
import com.kotlin.mvvm.contact.R
import com.kotlin.mvvm.contact.Utils
import com.kotlin.mvvm.contact.view.compose.ContactUsTheme
import com.kotlin.mvvm.contact.view.compose.EditContactScreen
import com.kotlin.mvvm.contact.viewmodel.MyContactViewModel

class MyContactActivity : AppCompatActivity() {

    private lateinit var myContactViewModel: MyContactViewModel

    private var contactID = ""
    private var selectedContactItem = -1
    private var firstName by mutableStateOf("")
    private var lastName by mutableStateOf("")
    private var email by mutableStateOf("")
    private var phone by mutableStateOf("")
    private var isLoading by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        myContactViewModel = ViewModelProvider(this)[MyContactViewModel::class.java]
        MobileAds.initialize(this) { }

        selectedContactItem = intent.getIntExtra(Utils.object_num, -1)

        myContactViewModel.contactLiveData.observe(this) { contact ->
            contactID = contact.id
            firstName = contact.firstName
            lastName = contact.lastName
            email = contact.email.orEmpty()
            phone = contact.phone.orEmpty()
        }
        myContactViewModel.pbLoading.observe(this) { isLoading = it }
        myContactViewModel.errorMessage.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
        myContactViewModel.isUpdateContactSuccess.observe(this) {
            clearUI()
            if (it) Toast.makeText(this, getString(R.string.contact_update_success), Toast.LENGTH_SHORT).show()
        }
        myContactViewModel.isAddedContactSuccess.observe(this) {
            clearUI()
            if (it) Toast.makeText(this, getString(R.string.contact_added_success), Toast.LENGTH_SHORT).show()
        }

        if (myContactViewModel.checkInternetConnection(this)) {
            if (selectedContactItem != -1) {
                isLoading = true
                myContactViewModel.getContact(selectedContactItem)
            }
        } else {
            Toast.makeText(this, getString(R.string.internet_connection_issues), Toast.LENGTH_LONG).show()
        }

        setContent {
            ContactUsTheme {
                EditContactScreen(
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    phone = phone,
                    isLoading = isLoading,
                    onFirstNameChange = { firstName = it },
                    onLastNameChange = { lastName = it },
                    onEmailChange = { email = it },
                    onPhoneChange = { phone = it },
                    onCancelClick = { finish() },
                    onSaveClick = {
                        myContactViewModel.checkValidator(
                            context = this@MyContactActivity,
                            selectedContactItem = selectedContactItem,
                            id = contactID,
                            firstName = firstName,
                            lastName = lastName,
                            email = email,
                            phone = phone
                        )
                    }
                )
            }
        }
    }

    private fun clearUI() {
        firstName = ""
        lastName = ""
        email = ""
        phone = ""
    }
}

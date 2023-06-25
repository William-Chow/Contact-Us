package com.kotlin.mvvm.contact.view.main

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar
import com.kotlin.mvvm.contact.R
import com.kotlin.mvvm.contact.Utils
import com.kotlin.mvvm.contact.databinding.ActivityMyContactBinding
import com.kotlin.mvvm.contact.model.Contact
import com.kotlin.mvvm.contact.viewmodel.MyContactViewModel


class MyContactActivity : AppCompatActivity() {

    private lateinit var context: Context
    private lateinit var myContactBinding: ActivityMyContactBinding
    private lateinit var myContactViewModel: MyContactViewModel // View Model

    private var contactID = ""
    private var selectedContactItem: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        myContactBinding = ActivityMyContactBinding.inflate(layoutInflater)
        setContentView(myContactBinding.root)

        context = this@MyContactActivity // Context
        myContactViewModel = ViewModelProvider(this@MyContactActivity)[MyContactViewModel::class.java] // init ViewModel

        MobileAds.initialize(this) { }
        myContactBinding.adView.loadAd(AdRequest.Builder().build())
        selectedContactItem = intent.getIntExtra(Utils.object_num, -1)
        if (myContactViewModel.checkInternetConnection(this@MyContactActivity)) {
            if (selectedContactItem != -1) myContactViewModel.getContact(selectedContactItem) else myContactBinding.pbLoading.visibility = View.GONE
            myContactViewModel.contactLiveData.observe(this@MyContactActivity) { contact ->
                initUI(contact)
            }
            myContactViewModel.pbLoading.observe(this@MyContactActivity) {
                if (it) myContactBinding.pbLoading.visibility = View.VISIBLE else myContactBinding.pbLoading.visibility = View.GONE
            }
        } else {
            Snackbar.make(myContactBinding.root, this@MyContactActivity.getString(R.string.internet_connection_issues), Snackbar.LENGTH_LONG).show()
        }

        myContactBinding.ivCancel.setOnClickListener { finish() }
        myContactBinding.ivSave.setOnClickListener {
            // Check the Validator
            myContactViewModel.checkValidator(
                this@MyContactActivity,
                selectedContactItem,
                contactID,
                myContactBinding.etFirstName.text.toString(),
                myContactBinding.etLastName.text.toString(),
                myContactBinding.etEmail.text.toString(),
                myContactBinding.etPhone.text.toString()
            )
        }

        // Check Error exist or not
        myContactViewModel.errorMessage.observe(this@MyContactActivity) {
            Toast.makeText(this@MyContactActivity, it, Toast.LENGTH_SHORT).show()
        }
        //Update Contact Success or Not
        myContactViewModel.isUpdateContactSuccess.observe(this@MyContactActivity) {
            clearUI()
            if (it) Toast.makeText(this@MyContactActivity, this@MyContactActivity.getString(R.string.contact_update_success), Toast.LENGTH_SHORT).show()
        }
        myContactViewModel.isAddedContactSuccess.observe(this@MyContactActivity) {
            clearUI()
            if (it) Toast.makeText(this@MyContactActivity, this@MyContactActivity.getString(R.string.contact_added_success), Toast.LENGTH_SHORT).show()
        }
    }

    private fun initUI(contact: Contact) {
        contactID = contact.id
        myContactBinding.etFirstName.setText(contact.firstName)
        myContactBinding.etLastName.setText(contact.lastName)
        myContactBinding.etEmail.setText(contact.email)
        myContactBinding.etPhone.setText(contact.phone)
    }

    private fun clearUI() {
        myContactBinding.etFirstName.text?.clear()
        myContactBinding.etLastName.text?.clear()
        myContactBinding.etEmail.text?.clear()
        myContactBinding.etPhone.text?.clear()
    }
}
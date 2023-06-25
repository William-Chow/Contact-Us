package com.kotlin.mvvm.contact.view.main

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar
import com.kotlin.mvvm.contact.R
import com.kotlin.mvvm.contact.Utils
import com.kotlin.mvvm.contact.adapter.ContactAdapter
import com.kotlin.mvvm.contact.databinding.ActivityContactBinding
import com.kotlin.mvvm.contact.model.Contact
import com.kotlin.mvvm.contact.viewmodel.ContactViewModel

class ContactActivity : AppCompatActivity() {

    private lateinit var context: Context
    private lateinit var contactBinding: ActivityContactBinding
    private lateinit var contactViewModel: ContactViewModel // View Model

    private var contactList = ArrayList<Contact>() // ArrayList
    private var originalContactList = ArrayList<Contact>() // ArrayList
    private lateinit var contactAdapter: ContactAdapter

    private var backPressedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactBinding = ActivityContactBinding.inflate(layoutInflater)
        setContentView(contactBinding.root)

        context = this@ContactActivity // Context
        contactViewModel = ViewModelProvider(this@ContactActivity)[ContactViewModel::class.java] // init ViewModel

        MobileAds.initialize(this) { }
        contactBinding.adView.loadAd(AdRequest.Builder().build())
        prepareRecyclerView(this@ContactActivity)
        if (contactViewModel.checkInternetConnection(this@ContactActivity)) {
            contactViewModel.contactLiveData.observe(this@ContactActivity) { contact ->
                contactList = contact as ArrayList<Contact>
                originalContactList = contactList
                contactAdapter.updateList(contactList)
            }
            contactViewModel.pbLoading.observe(this@ContactActivity) {
                if (it) contactBinding.pbLoading.visibility = View.VISIBLE else contactBinding.pbLoading.visibility = View.GONE
            }
            contactViewModel.getContacts()
        } else {
            Snackbar.make(contactBinding.root, this@ContactActivity.getString(R.string.internet_connection_issues), Snackbar.LENGTH_LONG).show()
        }
        contactBinding.ivSyncApi.setOnClickListener {
            contactBinding.pbLoading.visibility = View.VISIBLE
            contactViewModel.getContacts()
        }
        contactBinding.ivSorting.setOnClickListener {
            // contactAdapter.sortList()
        }
        contactBinding.ivAdd.setOnClickListener {
            // Add Contact
            contactViewModel.intentAddContact(this@ContactActivity, -1)
        }
        contactBinding.swipeRefreshLayout.setOnRefreshListener {
            contactBinding.swipeRefreshLayout.isRefreshing = false
            contactAdapter.updateList(Utils.retrieveBackDataFromJson(this@ContactActivity))
        }
        contactViewModel.errorMessage.observe(this@ContactActivity) {
            Toast.makeText(this@ContactActivity, it, Toast.LENGTH_SHORT).show()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun prepareRecyclerView(context: Context) {
        contactAdapter = ContactAdapter(context)
        contactBinding.rvContact.apply {
            layoutManager = LinearLayoutManager(context)
            contactBinding.rvContact.itemAnimator = DefaultItemAnimator()
            adapter = contactAdapter
            contactAdapter.onItemClick = { _, selectedItem ->
                // Update Contact
                contactViewModel.intentAddContact(this@ContactActivity, selectedItem)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (backPressedTime + 3000 > System.currentTimeMillis()) {
            onBackPressedDispatcher.onBackPressed()
        } else {
            Toast.makeText(this@ContactActivity, this@ContactActivity.getString(R.string.back_pressed), Toast.LENGTH_LONG).show()
        }
        backPressedTime = System.currentTimeMillis()
    }
}
package com.kotlin.mvvm.contact.viewmodel

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.mvvm.contact.Utils
import com.kotlin.mvvm.contact.model.Contact
import com.kotlin.mvvm.contact.network.ResponseState
import com.kotlin.mvvm.contact.network.repository.Repository
import com.kotlin.mvvm.contact.view.main.MyContactActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ContactViewModel : ViewModel() {

    val errorMessage = MutableLiveData<String>()

    // Coroutines
    private var job: Job? = null

    val contactLiveData = MutableLiveData<List<Contact>>()
    val pbLoading = MutableLiveData<Boolean>()

    fun getContacts() {
        job = viewModelScope.launch {
            when (val response = Repository.getContacts()) {
                is ResponseState.Success -> {
                    pbLoading.value = false
                    contactLiveData.postValue(response.data)
                }

                is ResponseState.Error -> {
                    pbLoading.value = false
                    // Handling the Error State in Future
                    onError("" + response.response.code() + " " + response.response.message())
                    Log.i("William", "" + response.response.code() + " " + response.response.message())
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
    }

    fun checkInternetConnection(context: Context): Boolean {
        return Utils.checkInternetConnection(context)
    }

    fun intentAddContact(activity: Activity, selectedItem: Int) {
        if (selectedItem == -1) {
            // -1 means new Contact Record
            Utils.intent(activity, MyContactActivity::class.java)
        } else {
            Utils.intent(activity, selectedItem, MyContactActivity::class.java)
        }
    }

    private fun onError(message: String) {
        errorMessage.value = message
    }
}
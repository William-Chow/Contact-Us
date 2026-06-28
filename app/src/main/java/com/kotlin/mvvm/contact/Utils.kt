package com.kotlin.mvvm.contact

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Patterns
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kotlin.mvvm.contact.model.Contact
import java.io.IOException

object Utils {

    const val OBJECT_NUM = "OBJECT"

    fun isEmail(email: String?): Boolean =
        !email.isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    fun checkInternetConnection(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    fun retrieveBackDataFromJson(context: Context): List<Contact> {
        val jsonFileString = getJsonDataFromAsset(context, "data.json") ?: return emptyList()
        val listContactType = object : TypeToken<List<Contact>>() {}.type
        return Gson().fromJson(jsonFileString, listContactType)
    }

    private fun getJsonDataFromAsset(context: Context, fileName: String): String? =
        try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            null
        }
}

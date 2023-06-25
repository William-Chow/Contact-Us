package com.kotlin.mvvm.contact

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kotlin.mvvm.contact.model.Contact
import java.io.IOException
import java.util.regex.Pattern

class Utils {

    companion object {
        const val object_num = "OBJECT"
        private const val REGEX_EMAIL_ = "^[A-Za-z0-9\\u4e00-\\u9fa5]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$"

        fun isEmail(email: String?): Boolean? {
            return email?.let { Pattern.matches(REGEX_EMAIL_, it) }
        }

        fun checkInternetConnection(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        }

        fun <T> intent(activity: Activity, selectedItem: Int, myClass: Class<T>) {
            val intent = Intent(activity, myClass)
            intent.putExtra(object_num, selectedItem)
            activity.startActivity(intent)
        }

        fun <T> intent(activity: Activity, myClass: Class<T>) {
            val intent = Intent(activity, myClass)
            activity.startActivity(intent)
        }

        fun <T> intentWithFinish(activity: Activity, myClass: Class<T>) {
            val intent = Intent(activity, myClass)
            activity.startActivity(intent)
            activity.finish()
        }

        /**
         * Start GSON
         */
        private fun getJsonDataFromAsset(context: Context, fileName: String): String? {
            val jsonString: String
            try {
                jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            } catch (ioException: IOException) {
                ioException.printStackTrace()
                return null
            }
            return jsonString
        }

        fun retrieveBackDataFromJson(context: Context): List<Contact> {
            val jsonFileString = getJsonDataFromAsset(context, "data.json")
            val gson = Gson()
            val listContactType = object : TypeToken<List<Contact>>() {}.type

            return gson.fromJson(jsonFileString, listContactType)
        }
        /**
         * End GSON
         */
    }
}
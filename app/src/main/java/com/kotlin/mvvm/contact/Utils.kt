package com.kotlin.mvvm.contact

import android.util.Patterns

object Utils {

    const val EXTRA_CONTACT_ID = "contact_id"

    fun isEmail(email: String?): Boolean =
        !email.isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

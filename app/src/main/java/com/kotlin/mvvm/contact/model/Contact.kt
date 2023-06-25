package com.kotlin.mvvm.contact.model

import com.google.gson.annotations.SerializedName

data class Contact(
    @SerializedName("id")
    val id: String,
    @SerializedName("firstName")
    val firstName: String,
    @SerializedName("lastName")
    val lastName: String,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("phone")
    val phone: String? = null
)
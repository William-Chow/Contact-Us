package com.kotlin.mvvm.contact.model

data class Contact(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String? = null,
    val phone: String? = null
)

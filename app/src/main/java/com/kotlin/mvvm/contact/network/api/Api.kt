package com.kotlin.mvvm.contact.network.api

import com.kotlin.mvvm.contact.model.Contact
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface Api {

    // Get Contact List
    @GET("me/contactDB/contact")
    suspend fun getContacts(@Header("Authorization") _auth: String): Response<List<Contact>>

    // Get Contact List Item
    @GET("me/contactDB/contact/{num}")
    suspend fun getContact(@Header("Authorization") _auth: String, @Path("num") _num: Int): Response<Contact>

    // Update Contact List Item
    @POST("me/contactDB/contact/{num}")
    suspend fun updateContact(@Header("Authorization") _auth: String, @Path("num") _num: Int, @Body _contact: Contact): Response<Contact>

    @PATCH("me/contactDB/contact")
    suspend fun addContact(@Header("Authorization") _auth: String, @Body _contact: Contact): Response<List<Contact>>
}
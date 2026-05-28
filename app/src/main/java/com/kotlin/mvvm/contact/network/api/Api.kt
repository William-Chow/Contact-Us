package com.kotlin.mvvm.contact.network.api

import com.google.gson.annotations.SerializedName
import com.kotlin.mvvm.contact.model.Contact
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface Api {

    // JSONPlaceholder is a mock REST API. POST/PATCH return fake success responses but do not persist data.
    @GET("users")
    suspend fun getContacts(): Response<List<JsonPlaceholderUser>>

    @GET("users/{id}")
    suspend fun getContact(@Path("id") id: Int): Response<JsonPlaceholderUser>

    @PATCH("users/{id}")
    suspend fun updateContact(@Path("id") id: Int, @Body contact: Contact): Response<Contact>

    @POST("users")
    suspend fun addContact(@Body contact: Contact): Response<Contact>
}

data class JsonPlaceholderUser(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("phone")
    val phone: String? = null
) {
    fun toContact(): Contact {
        val parts = name.trim().split(" ", limit = 2)
        return Contact(
            id = id.toString(),
            firstName = parts.firstOrNull().orEmpty(),
            lastName = parts.getOrNull(1).orEmpty(),
            email = email,
            phone = phone
        )
    }
}

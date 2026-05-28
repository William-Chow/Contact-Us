package com.kotlin.mvvm.contact.network

import com.kotlin.mvvm.contact.network.api.Api
import okhttp3.OkHttpClient
import retrofit2.Retrofit.Builder
import retrofit2.converter.gson.GsonConverterFactory

object Retrofit {

    private const val BASE_URL = "https://jsonplaceholder.typicode.com/"

    private val okHttpClient = OkHttpClient()
        .newBuilder()
        .build()

    val api: Api by lazy {
        Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(Api::class.java)
    }
}

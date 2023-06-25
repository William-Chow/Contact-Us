package com.kotlin.mvvm.contact.network

import com.kotlin.mvvm.contact.network.Retrofit.SECRET_KEY
import com.kotlin.mvvm.contact.network.api.Api
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit.Builder
import retrofit2.converter.gson.GsonConverterFactory

object Retrofit {

    // Base URL
    private const val BASE_URL = "https://jsonbin.org/"
    const val SECRET_KEY = "token bee0e2f3-5f35-4829-8c79-4f1ebbc5a1e8"

    private val okHttpClient = OkHttpClient()
        .newBuilder()
        .addInterceptor(AuthorizationInterceptor)
        .addInterceptor(RequestInterceptor)
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

object RequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return chain.proceed(request)
    }
}

object AuthorizationInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestWithHeader = chain.request()
            .newBuilder()
            .header("Authorization", SECRET_KEY).build()
        return chain.proceed(requestWithHeader)
    }
}
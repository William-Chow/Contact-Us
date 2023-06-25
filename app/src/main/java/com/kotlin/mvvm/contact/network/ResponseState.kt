package com.kotlin.mvvm.contact.network

import retrofit2.Response


sealed class ResponseState<out T> {
    data class Success<out T>(val data: T) : ResponseState<T>()
    data class Error<T>(val response: Response<T>) : ResponseState<T>()
}
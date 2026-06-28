package com.kotlin.mvvm.contact.network

sealed class ResponseState<out T> {
    data class Success<out T>(val data: T) : ResponseState<T>()
    data class Error(val code: Int?, val message: String) : ResponseState<Nothing>()
}

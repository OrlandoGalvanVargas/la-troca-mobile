package com.troca.latroca.domain.models

sealed class AuthResult<out T> {
    object Idle : AuthResult<Nothing>()
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
    object Loading : AuthResult<Nothing>()
}
package com.example.latroca.domain.models

data class AuthData(
    val token: String,
    val rol: String,
    val userId: String
)
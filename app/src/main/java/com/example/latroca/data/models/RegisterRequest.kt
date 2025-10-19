package com.example.latroca.data.models

data class RegisterRequest(
    val nombre: String,
    val email: String,
    val password: String,
    val rol: String = "USER",
    val bio: String
)
package com.example.latroca.data.models

import com.example.latroca.domain.models.AuthData

data class AuthResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val token: String? = null,
    val data: AuthData? = null
)
{
    val effectiveToken: String?
        get() = data?.token ?: token
}



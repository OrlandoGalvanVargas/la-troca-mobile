package com.troca.latroca.data.models

import com.google.gson.annotations.SerializedName

data class GoogleLoginRequest(
    @SerializedName("IdToken")
    val idToken: String
)
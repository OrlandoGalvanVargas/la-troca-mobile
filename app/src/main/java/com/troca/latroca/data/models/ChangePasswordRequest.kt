package com.troca.latroca.data.models

import com.google.gson.annotations.SerializedName

data class ChangePasswordRequest(
    @SerializedName("NewPassword")
    val newPassword: String
)

data class ChangePasswordResponse(
    @SerializedName("Message")
    val message: String? = null
)
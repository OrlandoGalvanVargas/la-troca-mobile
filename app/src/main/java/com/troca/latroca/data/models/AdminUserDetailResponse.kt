package com.troca.latroca.data.models

import com.google.gson.annotations.SerializedName

data class AdminUserDetailResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: AdminUserResponse
)

data class DeleteUserResponse(
    @SerializedName("message")
    val message: String
)
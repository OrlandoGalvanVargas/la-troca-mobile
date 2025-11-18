package com.troca.latroca.data.models

import com.google.gson.annotations.SerializedName

data class AdminUserResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("role")
    val role: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("profilePicUrl")
    val profilePicUrl: String? = null,

    @SerializedName("bio")
    val bio: String? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null,

    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

data class AdminUsersResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: List<AdminUserResponse>
)
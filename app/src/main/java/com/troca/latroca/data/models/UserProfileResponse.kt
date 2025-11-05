package com.troca.latroca.data.models

data class UserProfileResponse(
    val id: String,
    val name: String,
    val email: String,
    val profilePicUrl: String?,
    val bio: String?,
    val role: String,
    val location: LocationDto?,
    val reputation: Int,
    val createdAt: String
)

data class LocationDto(
    val latitude: Double,
    val longitude: Double,
    val manual: String
)
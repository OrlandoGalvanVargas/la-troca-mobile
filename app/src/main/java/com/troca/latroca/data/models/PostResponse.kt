package com.troca.latroca.data.models

import com.google.gson.annotations.SerializedName

data class PostResponse(
    val message: String,
    val data: List<PostItem>
)

data class PostItem(
    val id: String,
    val userId: String,
    val titulo: String,
    val descripcion: String,
    val categoria: String,
    val fotosUrl: List<String>,
    val ubicacion: Ubicacion,
    val necesidad: String,
    val creadoEn: String,
    val actualizadoEn: String,
    val estado: String,
    @SerializedName("userInfo")
    val userInfo: UserBasicInfo?
)

data class Ubicacion(
    val latitude: Double,
    val longitude: Double,
    val manual: String
)

data class UserBasicInfo(
    val userId: String,
    val name: String,
    val profileImageUrl: String?
)
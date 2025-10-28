package com.example.latroca.data.models

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
    val estado: String
)

data class Ubicacion(
    val latitude: Double,
    val longitude: Double,
    val manual: String
)

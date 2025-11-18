package com.troca.latroca.data.api

import com.troca.latroca.data.models.PostResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface PostApi {

    @GET("api/Post")
    suspend fun getAllPosts(
        @Header("Authorization") token: String
    ): PostResponse

    @Multipart
    @POST("api/Post/CrearPublicacion")
    suspend fun createPostValidated(
        @Header("Authorization") token: String,
        @Part("Titulo") titulo: RequestBody,
        @Part("Descripcion") descripcion: RequestBody,
        @Part("Categoria") categoria: RequestBody,
        @Part("Necesidad") necesidad: RequestBody,
        @Part("Ubicacion.Manual") ubicacionManual: RequestBody,
        @Part("Ubicacion.Latitude") latitude: RequestBody,
        @Part("Ubicacion.Longitude") longitude: RequestBody,
        @Part fotos: List<MultipartBody.Part>
    ): Response<ResponseBody>

    @Multipart
    @PUT("api/Post/{id}")
    suspend fun updatePost(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Part("Titulo") titulo: RequestBody,
        @Part("Descripcion") descripcion: RequestBody,
        @Part("Categoria") categoria: RequestBody,
        @Part("Necesidad") necesidad: RequestBody,
        @Part("Ubicacion.Manual") ubicacionManual: RequestBody,
        @Part("Ubicacion.Latitude") latitude: RequestBody,
        @Part("Ubicacion.Longitude") longitude: RequestBody,
        @Part Fotos: List<MultipartBody.Part>?
    ): Response<ResponseBody>

    @DELETE("api/Post/{id}")
    suspend fun deletePost(
        @Header("Authorization") token: String,
        @Path("id") postId: String
    ): Response<ResponseBody>

    @Multipart
    @POST("api/ImagenModeration/AnalizarImagen")
    suspend fun analyzeImage(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>
}

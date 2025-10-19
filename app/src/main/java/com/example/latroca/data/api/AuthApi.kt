package com.example.latroca.data.api

import com.example.latroca.data.models.AuthResponse
import com.example.latroca.data.models.LoginRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface AuthApi {

    @POST("api/Auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<AuthResponse>

    @Multipart
    @POST("api/Auth/register")
    suspend fun register(
        @Part("Nombre") nombre: RequestBody,
        @Part("Email") email: RequestBody,
        @Part("Password") password: RequestBody,
        @Part("Rol") rol: RequestBody,
        @Part("Bio") bio: RequestBody,
        @Part imagenPerfil: MultipartBody.Part?
    ): Response<AuthResponse>

    @POST("api/Auth/logout")
    suspend fun logout(): Response<AuthResponse>
}
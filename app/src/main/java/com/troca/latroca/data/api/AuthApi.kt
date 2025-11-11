package com.troca.latroca.data.api

import com.troca.latroca.data.models.AuthResponse
import com.troca.latroca.data.models.DeactivateAccountRequest
import com.troca.latroca.data.models.LoginRequest
import com.troca.latroca.data.models.UserProfileResponse
import com.troca.latroca.data.models.UserIdProfileResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

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
        @Part("Location.Manual") ubicacionManual: RequestBody,
        @Part("Location.Latitude") latitude: RequestBody,
        @Part("Location.Longitude") longitude: RequestBody,
        @Part imagenPerfil: MultipartBody.Part?
    ): Response<AuthResponse>

    @POST("api/Auth/deactivate-account")
    suspend fun deactivateAccount(
        @Header("Authorization") token: String,
        @Body request: DeactivateAccountRequest
    ): Response<AuthResponse>

    // Perfil del usuario autenticado
    @GET("api/Auth/profile")
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): Response<UserProfileResponse>

    // Perfil de otro usuario (por su ID)
    @GET("api/User/profile/{userId}")
    suspend fun getUserProfileById(
        @Path("userId") userId: String
    ): Response<UserIdProfileResponse>

    @POST("api/Auth/logout")
    suspend fun logout(): Response<AuthResponse>

    @POST("api/Auth/login-google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): Response<AuthResponse>

    // DTO
    data class GoogleLoginRequest(
        val idToken: String
    )
}

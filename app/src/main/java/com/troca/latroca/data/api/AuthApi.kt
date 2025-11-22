package com.troca.latroca.data.api

import com.troca.latroca.data.models.AdminUserDetailResponse
import com.troca.latroca.data.models.AdminUsersResponse
import com.troca.latroca.data.models.AuthResponse
import com.troca.latroca.data.models.ChangePasswordRequest
import com.troca.latroca.data.models.ChangePasswordResponse
import com.troca.latroca.data.models.DeactivateAccountRequest
import com.troca.latroca.data.models.DeleteUserResponse
import com.troca.latroca.data.models.GoogleLoginRequest
import com.troca.latroca.data.models.LoginRequest
import com.troca.latroca.data.models.UpdateProfileResponse
import com.troca.latroca.data.models.UserIdProfileResponse
import com.troca.latroca.data.models.UserProfileResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
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

    @POST("api/Auth/login-google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): Response<AuthResponse>

    @Multipart
    @PUT("api/User/me")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Part("Nombre") nombre: RequestBody?,
        @Part("Bio") bio: RequestBody?,
        @Part("LocationManual") ubicacionManual: RequestBody?,
        @Part("LocationLatitude") latitude: RequestBody?,
        @Part("LocationLongitude") longitude: RequestBody?,
        @Part imagenPerfil: MultipartBody.Part?
    ): Response<UpdateProfileResponse>

    @GET("api/Auth/profile")
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): Response<UserProfileResponse>

    @GET("api/User/profile/{userId}")
    suspend fun getUserProfileById(
        @Path("userId") userId: String
    ): Response<UserIdProfileResponse>

    @PUT("api/User/me/password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<ChangePasswordResponse>

    @GET("api/Admin/users")
    suspend fun getAllUsers(
        @Header("Authorization") token: String
    ): Response<AdminUsersResponse>

    @GET("api/Admin/users/{id}")
    suspend fun getUserById(
        @Header("Authorization") token: String,
        @Path("id") userId: String
    ): Response<AdminUserDetailResponse>

    @DELETE("api/Admin/users/{id}")
    suspend fun deleteUser(
        @Header("Authorization") token: String,
        @Path("id") userId: String
    ): Response<DeleteUserResponse>

    @DELETE("api/Admin/me")
    suspend fun deleteMyAccount(
        @Header("Authorization") token: String
    ): Response<AuthResponse>

}
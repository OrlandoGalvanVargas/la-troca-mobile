package com.example.latroca.data.repository

import com.example.latroca.data.api.ApiClient
import com.example.latroca.data.models.AuthResponse
import com.example.latroca.data.models.LoginRequest
import com.example.latroca.domain.models.AuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File

class AuthRepository {

    private val authApi = ApiClient.authApi

    suspend fun login(email: String, password: String): AuthResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = authApi.login(LoginRequest(email, password))
                handleLoginResponse(response)
            } catch (e: Exception) {
                AuthResult.Error("Error de conexión: ${e.message}")
            }
        }

    suspend fun register(
        nombre: String,
        email: String,
        password: String,
        bio: String,
        imageFile: File? = null
    ): AuthResult<String> = withContext(Dispatchers.IO) {
        try {
            val imagenPerfilPart = imageFile?.toFormDataPart("ImagenPerfil")

            val response = authApi.register(
                nombre = nombre.toRequestBody(),
                email = email.toRequestBody(),
                password = password.toRequestBody(),
                rol = "USER".toRequestBody(),
                bio = bio.toRequestBody(),
                imagenPerfil = imagenPerfilPart
            )

            handleRegisterResponse(response)
        } catch (e: Exception) {
            AuthResult.Error("Error de conexión: ${e.message}")
        }
    }

    suspend fun logout(): AuthResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = authApi.logout()
            AuthResult.Success(response.isSuccessful && response.body()?.success == true)
        } catch (e: Exception) {
            AuthResult.Error("Error al cerrar sesión: ${e.message}")
        }
    }

    private fun handleRegisterResponse(response: Response<AuthResponse>): AuthResult<String> =
        if (response.isSuccessful) {
            val successMessage = response.body()?.message ?: "Usuario creado correctamente"
            AuthResult.Success(successMessage)
        } else {
            val errorMessage = response.body()?.message
                ?: response.errorBody()?.string()
                ?: "Error desconocido en el registro"
            AuthResult.Error(errorMessage)
        }

    private fun handleLoginResponse(response: Response<AuthResponse>): AuthResult<String> =
        if (response.isSuccessful) {
            val token = response.body()?.token
            if (!token.isNullOrBlank()) {
                AuthResult.Success(token)
            } else {
                AuthResult.Error("Credenciales inválidas")
            }
        } else {
            val errorMessage = when (response.code()) {
                401 -> "Credenciales incorrectas"
                400 -> "Solicitud inválida"
                500 -> "Error del servidor"
                else -> "Error de conexión: ${response.code()}"
            }
            AuthResult.Error(errorMessage)
        }

    private fun File?.toFormDataPart(fieldName: String): MultipartBody.Part? =
        this?.let { file ->
            MultipartBody.Part.createFormData(
                fieldName,
                file.name,
                file.asRequestBody()
            )
        }
}
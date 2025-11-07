package com.troca.latroca.data.repository

import com.troca.latroca.data.api.ApiClient
import com.troca.latroca.data.api.AuthApi
import com.troca.latroca.data.models.AuthResponse
import com.troca.latroca.data.models.DeactivateAccountRequest
import com.troca.latroca.data.models.GoogleLoginRequest
import com.troca.latroca.data.models.LoginRequest
import com.troca.latroca.data.models.UserProfileResponse
import com.troca.latroca.domain.models.AuthResult
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

    // 👇 AGREGAR ESTA FUNCIÓN
    suspend fun loginWithGoogle(googleIdToken: String): AuthResult<String> =
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("AuthRepository", "Token a enviar (primeros 30): ${googleIdToken.take(30)}...")

                // 👇 CAMBIAR ESTO
                val request = GoogleLoginRequest(idToken = googleIdToken)
                val response = authApi.loginWithGoogle(request)

                android.util.Log.d("AuthRepository", "Request JSON: ${com.google.gson.Gson().toJson(request)}")
                android.util.Log.d("AuthRepository", "Response code: ${response.code()}")

                handleLoginResponse(response)
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Exception en loginWithGoogle", e)
                AuthResult.Error("Error de conexión: ${e.message}")
            }
        }

    suspend fun register(
        nombre: String,
        email: String,
        password: String,
        bio: String,
        ubicacion: String,
        latitude: Double,
        longitude: Double,
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
                ubicacionManual = ubicacion.toRequestBody(),
                latitude = latitude.toString().toRequestBody(),
                longitude = longitude.toString().toRequestBody(),
                imagenPerfil = imagenPerfilPart
            )

            handleRegisterResponse(response)
        } catch (e: Exception) {
            AuthResult.Error("Error de conexión: ${e.message}")
        }
    }

    suspend fun deactivateAccount(token: String, reason: String): AuthResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = authApi.deactivateAccount(
                    token = "Bearer $token",
                    request = DeactivateAccountRequest(reason)
                )

                if (response.isSuccessful) {
                    AuthResult.Success(response.body()?.message ?: "Cuenta desactivada")
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Error desconocido"
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                AuthResult.Error("Error de conexión: ${e.message}")
            }
        }

    suspend fun getUserProfile(token: String): AuthResult<UserProfileResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = authApi.getUserProfile("Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    AuthResult.Success(response.body()!!)
                } else {
                    AuthResult.Error("No se pudo obtener el perfil")
                }
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

    private fun handleLoginResponse(response: Response<AuthResponse>): AuthResult<String> {
        android.util.Log.d("AuthRepository", "Response code: ${response.code()}")
        android.util.Log.d("AuthRepository", "Response successful: ${response.isSuccessful}")

        return if (response.isSuccessful) {
            val authResponse = response.body()
            val token = authResponse?.effectiveToken

            if (!token.isNullOrBlank()) {
                AuthResult.Success(token)
            } else {
                AuthResult.Error("Credenciales inválidas - token no recibido")
            }
        } else {
            val errorBody = response.errorBody()?.string()
            android.util.Log.d("AuthRepository", "Error body: $errorBody")  // 👈 VER QUÉ DEVUELVE

            val errorMessage = try {
                if (!errorBody.isNullOrBlank()) {
                    val json = org.json.JSONObject(errorBody)
                    // Probar ambas variantes (minúscula y mayúscula)
                    val msg = json.optString("message", json.optString("Message", ""))
                    android.util.Log.d("AuthRepository", "Extracted message: $msg")
                    msg.ifBlank {
                        when (response.code()) {
                            401 -> "Credenciales incorrectas"
                            else -> "Error ${response.code()}"
                        }
                    }
                } else {
                    when (response.code()) {
                        401 -> "Credenciales incorrectas"
                        400 -> "Solicitud inválida"
                        500 -> "Error del servidor"
                        else -> "Error de conexión: ${response.code()}"
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Error parsing: ${e.message}")
                when (response.code()) {
                    401 -> "Credenciales incorrectas"
                    else -> "Error ${response.code()}"
                }
            }

            android.util.Log.d("AuthRepository", "Final error message: $errorMessage")
            AuthResult.Error(errorMessage)
        }
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
package com.troca.latroca.data.repository

import com.troca.latroca.data.api.ApiClient
import com.troca.latroca.data.models.AdminUserResponse
import com.troca.latroca.data.models.AuthResponse
import com.troca.latroca.data.models.ChangePasswordRequest
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

    suspend fun login(email: String, password: String): AuthResult<Pair<String, String>> =
        withContext(Dispatchers.IO) {
            try {
                val response = authApi.login(LoginRequest(email, password))
                handleLoginResponse(response)
            } catch (e: Exception) {
                AuthResult.Error("Error de conexión: ${e.message}")
            }
        }

    suspend fun loginWithGoogle(googleIdToken: String): AuthResult<Pair<String, String>> =
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("AuthRepository", "Token a enviar (primeros 30): ${googleIdToken.take(30)}...")

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

    suspend fun getAllUsers(token: String): AuthResult<List<AdminUserResponse>> =
        withContext(Dispatchers.IO) {
            try {
                val response = authApi.getAllUsers("Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    val users = response.body()?.data ?: emptyList()
                    AuthResult.Success(users)
                } else {
                    AuthResult.Error("No se pudieron obtener los usuarios")
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Error en getAllUsers", e)
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

    private fun handleRegisterResponse(response: Response<AuthResponse>): AuthResult<String> =
        if (response.isSuccessful) {
            val successMessage = response.body()?.message ?: "Usuario creado correctamente"
            AuthResult.Success(successMessage)
        } else {
            val errorBody = response.errorBody()?.string()
            val errorMessage = when {
                response.code() == 400 -> {
                    when {
                        errorBody?.contains("El nombre contiene lenguaje inapropiado", ignoreCase = true) == true ->
                            "Nombre inapropiado. Por favor, usa un nombre adecuado"
                        errorBody?.contains("La biografía contiene lenguaje inapropiado", ignoreCase = true) == true ->
                            "La biografía contiene lenguaje inapropiado. Por favor, corrígelo"
                        errorBody?.contains("La imagen de perfil no es apropiada", ignoreCase = true) == true ->
                            "La imagen de perfil no es apropiada. Por favor, selecciona otra imagen."
                        errorBody?.contains("lenguaje inapropiado", ignoreCase = true) == true ->
                            "El contenido contiene lenguaje inapropiado. Por favor, revisa tu información."
                        else -> errorBody ?: "Datos incorrectos. Verifica la información ingresada."
                    }
                }
                response.code() == 500 -> {
                    "Error interno del servidor. Por favor, intenta más tarde."
                }
                else -> {
                    errorBody ?: "Error desconocido en el registro"
                }
            }
            AuthResult.Error(errorMessage)
        }

    private fun handleLoginResponse(response: Response<AuthResponse>): AuthResult<Pair<String, String>> {
        android.util.Log.d("AuthRepository", "Response code: ${response.code()}")
        android.util.Log.d("AuthRepository", "Response successful: ${response.isSuccessful}")

        return if (response.isSuccessful) {
            val authResponse = response.body()
            val token = authResponse?.effectiveToken
            val role = authResponse?.effectiveRole ?: "USER"

            if (!token.isNullOrBlank()) {
                AuthResult.Success(Pair(token, role))
            } else {
                AuthResult.Error("Credenciales inválidas - token no recibido")
            }
        } else {
            val errorBody = response.errorBody()?.string()
            android.util.Log.d("AuthRepository", "Error body: $errorBody")

            val errorMessage = try {
                if (!errorBody.isNullOrBlank()) {
                    val json = org.json.JSONObject(errorBody)
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

    suspend fun updateProfile(
        token: String,
        nombre: String?,
        bio: String?,
        ubicacion: String?,
        latitude: Double?,
        longitude: Double?,
        imageFile: File?
    ): AuthResult<String> = withContext(Dispatchers.IO) {
        try {
            val imagenPerfilPart = imageFile?.toFormDataPart("ImagenPerfil")

            val nombrePart = nombre?.toRequestBody()
            val bioPart = bio?.toRequestBody()
            val ubicacionPart = ubicacion?.toRequestBody()
            val latitudePart = latitude?.toString()?.toRequestBody()
            val longitudePart = longitude?.toString()?.toRequestBody()

            val response = authApi.updateProfile(
                token = "Bearer $token",
                nombre = nombrePart,
                bio = bioPart,
                ubicacionManual = ubicacionPart,
                latitude = latitudePart,
                longitude = longitudePart,
                imagenPerfil = imagenPerfilPart
            )

            if (response.isSuccessful) {
                AuthResult.Success(response.body()?.message ?: "Perfil actualizado correctamente")
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = when {
                    response.code() == 400 -> {
                        when {
                            errorBody?.contains("El nombre contiene lenguaje inapropiado", ignoreCase = true) == true ->
                                "Lenguaje inapropiado en el nombre. Por favor, corrígelo"
                            errorBody?.contains("La biografía contiene lenguaje inapropiado", ignoreCase = true) == true ->
                                "La biografía contiene lenguaje inapropiado. Por favor, corrígelo."
                            errorBody?.contains("lenguaje inapropiado", ignoreCase = true) == true ->
                                "El contenido contiene lenguaje inapropiado. Por favor, revisa tu información."
                            else -> errorBody ?: "Solicitud incorrecta. Verifica los datos ingresados."
                        }
                    }
                    response.code() == 401 -> {
                        "Sesión expirada. Por favor, inicia sesión nuevamente."
                    }
                    response.code() == 500 -> {
                        "Error interno del servidor. Por favor, intenta más tarde."
                    }
                    else -> {
                        errorBody ?: "Error desconocido al actualizar el perfil"
                    }
                }
                AuthResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            AuthResult.Error("Error de conexión: ${e.message}")
        }
    }

    suspend fun changePassword(token: String, newPassword: String): AuthResult<String> =
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("AuthRepository", "Cambiando contraseña...")

                val response = authApi.changePassword(
                    token = "Bearer $token",
                    request = ChangePasswordRequest(newPassword = newPassword)
                )

                android.util.Log.d("AuthRepository", "Response code: ${response.code()}")

                if (response.isSuccessful) {
                    val message = response.body()?.message ?: "Contraseña actualizada correctamente"
                    android.util.Log.d("AuthRepository", "✅ Contraseña cambiada: $message")
                    AuthResult.Success(message)
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("AuthRepository", "Error body: $errorBody")

                    val errorMessage = try {
                        if (!errorBody.isNullOrBlank()) {
                            val json = org.json.JSONObject(errorBody)
                            json.optString("Message", json.optString("message", ""))
                                .ifBlank {
                                    when (response.code()) {
                                        400 -> "La contraseña debe tener al menos 8 caracteres"
                                        401 -> "Sesión inválida"
                                        else -> "Error ${response.code()}"
                                    }
                                }
                        } else {
                            when (response.code()) {
                                400 -> "Solicitud inválida"
                                401 -> "No autorizado"
                                500 -> "Error del servidor"
                                else -> "Error ${response.code()}"
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AuthRepository", "Error parsing: ${e.message}")
                        "Error al cambiar contraseña"
                    }

                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Exception en changePassword", e)
                AuthResult.Error("Error de conexión: ${e.message}")
            }
        }

    suspend fun getUserById(token: String, userId: String): AuthResult<AdminUserResponse> =
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("AuthRepository", "Obteniendo usuario: $userId")

                val response = authApi.getUserById("Bearer $token", userId)

                android.util.Log.d("AuthRepository", "Response code: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!.data
                    android.util.Log.d("AuthRepository", "✅ Usuario obtenido: ${user.name}")
                    AuthResult.Success(user)
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("AuthRepository", "Error body: $errorBody")

                    val errorMessage = when (response.code()) {
                        404 -> "Usuario no encontrado"
                        401 -> "No autorizado"
                        403 -> "No tienes permisos"
                        else -> "Error al obtener usuario (${response.code()})"
                    }

                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Exception en getUserById", e)
                AuthResult.Error("Error de conexión: ${e.message}")
            }
        }

    suspend fun deleteUser(token: String, userId: String): AuthResult<String> =
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("AuthRepository", "Eliminando usuario: $userId")

                val response = authApi.deleteUser("Bearer $token", userId)

                android.util.Log.d("AuthRepository", "Response code: ${response.code()}")

                if (response.isSuccessful) {
                    val message = response.body()?.message ?: "Usuario eliminado correctamente"
                    android.util.Log.d("AuthRepository", "✅ $message")
                    AuthResult.Success(message)
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("AuthRepository", "Error body: $errorBody")

                    val errorMessage = when (response.code()) {
                        404 -> "Usuario no encontrado"
                        401 -> "No autorizado"
                        403 -> "No tienes permisos para eliminar usuarios"
                        else -> "Error al eliminar usuario (${response.code()})"
                    }

                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthRepository", "Exception en deleteUser", e)
                AuthResult.Error("Error de conexión: ${e.message}")
            }
        }
}
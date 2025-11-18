package com.troca.latroca.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.troca.latroca.data.api.ApiClient
import com.troca.latroca.data.models.PostItem
import com.troca.latroca.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

class PostRepository {

    private val postApi = ApiClient.postApi
    private val TAG = "PostRepository"

    suspend fun getAllPosts(token: String): List<PostItem> = withContext(Dispatchers.IO) {
        try {
            postApi.getAllPosts("Bearer $token").data
        } catch (e: Exception) {
            Log.e(TAG, "Error getting posts: ${e.message}")
            emptyList()
        }
    }

    suspend fun createPostWithImage(
        context: Context,
        token: String,
        titulo: String,
        descripcion: String,
        categoria: String,
        necesidad: String,
        ubicacion: String,
        latitude: Double,
        longitude: Double,
        imageUri: Uri,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting post creation...")

            val file = FileUtils.getFileFromUri(context, imageUri)
            if (file == null) {
                Log.e(TAG, "File is null")
                onError("No se pudo procesar la imagen seleccionada")
                return@withContext
            }

            if (!file.exists()) {
                Log.e(TAG, "File doesn't exist")
                onError("El archivo de imagen no existe")
                return@withContext
            }

            Log.d(TAG, "File prepared: ${file.name}, size: ${file.length()} bytes")

            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val fotoPart = MultipartBody.Part.createFormData("Fotos", file.name, requestFile)

            val tituloPart = titulo.trim().toRequestBodyUtf8()
            val descripcionPart = descripcion.trim().toRequestBodyUtf8()
            val categoriaPart = categoria.trim().toRequestBodyUtf8()
            val necesidadPart = necesidad.trim().toRequestBodyUtf8()
            val ubicacionPart = ubicacion.trim().toRequestBodyUtf8()
            val latitudePart = latitude.toString().toRequestBodyUtf8()
            val longitudePart = longitude.toString().toRequestBodyUtf8()

            Log.d(TAG, "Making API request...")

            val response = postApi.createPostValidated(
                "Bearer $token",
                tituloPart,
                descripcionPart,
                categoriaPart,
                necesidadPart,
                ubicacionPart,
                latitudePart,
                longitudePart,
                listOf(fotoPart)
            )

            Log.d(TAG, "Response code: ${response.code()}")

            if (response.isSuccessful) {
                Log.d(TAG, "Post created successfully")
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Error response: $errorBody")

                val errorMessage = when (response.code()) {
                    400 -> {
                        when {
                            errorBody?.contains("Título", ignoreCase = true) == true ->
                                "El título contiene lenguaje inapropiado"
                            errorBody?.contains("Descripción", ignoreCase = true) == true ->
                                "La descripción contiene lenguaje inapropiado"
                            errorBody?.contains("Categoría", ignoreCase = true) == true ->
                                "La categoría contiene lenguaje inapropiado"
                            errorBody?.contains("Necesidad", ignoreCase = true) == true ->
                                "La necesidad contiene lenguaje inapropiado"
                            errorBody?.contains("Imagen inapropiada", ignoreCase = true) == true ->
                                "La imagen contiene contenido inapropiado"
                            else -> "Datos incorrectos. Verifica la información"
                        }
                    }
                    401 -> "Sesión expirada. Inicia sesión nuevamente"
                    500 -> "Error del servidor. Intenta más tarde"
                    else -> errorBody ?: "Error al crear la publicación"
                }
                withContext(Dispatchers.Main) {
                    onError(errorMessage)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in createPostWithImage", e)
            onError("Error: ${e.message ?: "Problema de conexión"}")
        }
    }

    suspend fun updatePost(
        context: Context,
        token: String,
        postId: String,
        titulo: String,
        descripcion: String,
        categoria: String,
        necesidad: String,
        ubicacionManual: String,
        latitude: Double,
        longitude: Double,
        existingImageUrl: String?,
        newImageUri: Uri?
    ): Result<String> = withContext(Dispatchers.IO) { // 🔥 Cambiamos el retorno a Result<String>
        try {
            val tituloPart = titulo.toRequestBody("text/plain".toMediaTypeOrNull())
            val descripcionPart = descripcion.toRequestBody("text/plain".toMediaTypeOrNull())
            val categoriaPart = categoria.toRequestBody("text/plain".toMediaTypeOrNull())
            val necesidadPart = necesidad.toRequestBody("text/plain".toMediaTypeOrNull())
            val ubicacionManualPart = ubicacionManual.toRequestBody("text/plain".toMediaTypeOrNull())
            val latitudePart = latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val longitudePart = longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            val fotosParts = mutableListOf<MultipartBody.Part>()

            when {
                newImageUri != null -> {
                    FileUtils.getFileFromUri(context, newImageUri)?.let { file ->
                        val body = file.asRequestBody("image/*".toMediaTypeOrNull())
                        fotosParts.add(MultipartBody.Part.createFormData("Fotos", file.name, body))
                    }
                }
                !existingImageUrl.isNullOrBlank() -> {
                    downloadToCache(context, existingImageUrl)?.let { tmp ->
                        val body = tmp.asRequestBody("image/*".toMediaTypeOrNull())
                        fotosParts.add(MultipartBody.Part.createFormData("Fotos", tmp.name, body))
                    }
                }
            }

            val response = postApi.updatePost(
                "Bearer $token",
                postId,
                tituloPart,
                descripcionPart,
                categoriaPart,
                necesidadPart,
                ubicacionManualPart,
                latitudePart,
                longitudePart,
                fotosParts.ifEmpty { null }
            )

            if (response.isSuccessful) {
                Result.success("Publicación actualizada correctamente")
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = when {
                    response.code() == 400 -> {
                        when {
                            errorBody?.contains("Texto inapropiado detectado", ignoreCase = true) == true -> {
                                when {
                                    errorBody.contains("Título", ignoreCase = true) ->
                                        "Lenguaje inapropiado en el título. Por favor, corrígelo"
                                    errorBody.contains("Descripción", ignoreCase = true) ->
                                        "La descripción contiene lenguaje inapropiado. Por favor, corrígelo"
                                    errorBody.contains("Categoría", ignoreCase = true) ->
                                        "La categoría contiene lenguaje inapropiado. Por favor, selecciona una categoría válida"
                                    errorBody.contains("Necesidad", ignoreCase = true) ->
                                        "la necesidad contiene lenguaje inapropiado. Por favor, corrígelo."
                                    else -> "El contenido contiene lenguaje inapropiado. Por favor, revisa toda la información."
                                }
                            }
                            errorBody?.contains("Imagen inapropiada detectada", ignoreCase = true) == true ->
                                "La imagen contiene contenido inapropiado. Por favor, selecciona otra imagen."
                            else -> errorBody ?: "Datos incorrectos. Verifica la información ingresada."
                        }
                    }
                    response.code() == 401 -> {
                        "Sesión expirada. Por favor, inicia sesión nuevamente."
                    }
                    response.code() == 500 -> {
                        "Error interno del servidor. Por favor, intenta más tarde."
                    }
                    else -> {
                        errorBody ?: "Error desconocido al actualizar la publicación"
                    }
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating post", e)
            Result.failure(Exception("Error de conexión: ${e.message ?: "Intenta nuevamente"}"))
        }
    }

    suspend fun deletePost(token: String, postId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            postApi.deletePost("Bearer $token", postId).isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting post", e)
            false
        }
    }

    suspend fun analyzeImage(
        context: Context,
        token: String,
        imageUri: Uri
    ): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                val file = FileUtils.getFileFromUri(context, imageUri)
                    ?: return@withContext false to "Archivo no encontrado"

                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val response = postApi.analyzeImage("Bearer $token", imagePart)
                if (response.isSuccessful) {
                    val body = response.body()?.string() ?: return@withContext false to "Respuesta vacía"
                    val json = JSONObject(body)
                    val isSafe = json.optBoolean("isSafe", false)
                    val message = if (isSafe) "Imagen segura" else "Imagen con contenido no permitido"
                    isSafe to message
                } else {
                    false to "Error del servidor (${response.code()})"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing image", e)
                false to (e.localizedMessage ?: "Error desconocido")
            }
        }
    }

    private fun downloadToCache(context: Context, url: String): File? {
        return try {
            val client = okhttp3.OkHttpClient()
            val req = okhttp3.Request.Builder().url(url).build()
            val res = client.newCall(req).execute()
            if (!res.isSuccessful) return null
            val bytes = res.body?.bytes() ?: return null
            val file = File(context.cacheDir, "cache_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { it.write(bytes) }
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading to cache", e)
            null
        }
    }

    private fun String.toRequestBodyUtf8() =
        toRequestBody("text/plain; charset=utf-8".toMediaTypeOrNull())
}
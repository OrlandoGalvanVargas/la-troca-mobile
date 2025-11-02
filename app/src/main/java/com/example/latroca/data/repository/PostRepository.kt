package com.example.latroca.data.repository

import android.content.Context
import android.net.Uri
import com.example.latroca.data.api.ApiClient
import com.example.latroca.data.models.PostItem
import com.example.latroca.utils.FileUtils
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

    suspend fun getAllPosts(token: String): List<PostItem> = withContext(Dispatchers.IO) {
        try {
            postApi.getAllPosts("Bearer $token").data
        } catch (_: Exception) {
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
            val file = FileUtils.getFileFromUri(context, imageUri) ?: throw Exception("Archivo inválido")
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val fotoPart = MultipartBody.Part.createFormData("Fotos", file.name, requestFile)

            val tituloPart = titulo.clean().toRequestBodyUtf8()
            val descripcionPart = descripcion.clean().toRequestBodyUtf8()
            val categoriaPart = categoria.clean().toRequestBodyUtf8()
            val necesidadPart = necesidad.clean().toRequestBodyUtf8()
            val ubicacionPart = ubicacion.clean().toRequestBodyUtf8()
            val latitudePart = latitude.toString().toRequestBodyUtf8()
            val longitudePart = longitude.toString().toRequestBodyUtf8()

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

            if (response.isSuccessful) onSuccess()
            else onError("Error ${response.code()}: ${response.message()}")
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Error desconocido")
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
    ): Boolean = withContext(Dispatchers.IO) {
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
                if (fotosParts.isNotEmpty()) fotosParts else null
            )

            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deletePost(token: String, postId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            postApi.deletePost("Bearer $token", postId).isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    // Analizar imagen
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
                false to (e.localizedMessage ?: "Error desconocido")
            }
        }
    }

    // Analizar texto
    suspend fun analyzeText(token: String, text: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                val escapedText = text
                    .replace("\\", "\\\\")
                    .replace("\r", "\\r")
                    .replace("\n", "\\n")
                    .replace("\"", "\\\"")

                val jsonBody = """{"descripcion": "$escapedText"}"""
                val requestBody = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())

                val response = postApi.analyzeText("Bearer $token", requestBody)
                if (response.isSuccessful) {
                    val body = response.body()?.string() ?: return@withContext false to "Respuesta vacía"
                    val json = JSONObject(body)
                    val isSafe = json.optBoolean("isSafe", false)
                    val message = json.optString("message", "Sin mensaje")
                    isSafe to message
                } else {
                    false to "Error del servidor (${response.code()})"
                }
            } catch (e: Exception) {
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
        } catch (_: Exception) {
            null
        }
    }

    // Helpers
    private fun String.clean(): String = replace("\r", " ").replace("\n", " ").trim()

    private fun String.toRequestBodyUtf8() =
        toRequestBody("text/plain; charset=utf-8".toMediaTypeOrNull())
}

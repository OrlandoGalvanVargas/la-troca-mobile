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
import java.io.File

class PostRepository {

    private val postApi = ApiClient.postApi

    suspend fun getAllPosts(token: String): List<PostItem> = withContext(Dispatchers.IO) {
        try {
            postApi.getAllPosts("Bearer $token").data
        } catch (e: Exception) {
            emptyList<PostItem>()
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
        imageUri: Uri
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = FileUtils.getFileFromUri(context, imageUri) ?: return@withContext false
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val fotoPart = MultipartBody.Part.createFormData("Fotos", file.name, requestFile)

            val tituloPart = titulo.toRequestBody("text/plain".toMediaTypeOrNull())
            val descripcionPart = descripcion.toRequestBody("text/plain".toMediaTypeOrNull())
            val categoriaPart = categoria.toRequestBody("text/plain".toMediaTypeOrNull())
            val necesidadPart = necesidad.toRequestBody("text/plain".toMediaTypeOrNull())
            val ubicacionManualPart = ubicacion.toRequestBody("text/plain".toMediaTypeOrNull())
            val latitudePart = latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val longitudePart = longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            val response = postApi.createPostWithImage(
                "Bearer $token",
                tituloPart,
                descripcionPart,
                categoriaPart,
                necesidadPart,
                ubicacionManualPart,
                latitudePart,
                longitudePart,
                listOf(fotoPart)
            )
            response.isSuccessful
        } catch (_: Exception) {
            false
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
            if (newImageUri != null) {
                FileUtils.getFileFromUri(context, newImageUri)?.let { file ->
                    val body = file.asRequestBody("image/*".toMediaTypeOrNull())
                    fotosParts.add(MultipartBody.Part.createFormData("Fotos", file.name, body))
                }
            } else if (!existingImageUrl.isNullOrBlank()) {
                downloadToCache(context, existingImageUrl)?.let { tmp ->
                    val body = tmp.asRequestBody("image/*".toMediaTypeOrNull())
                    fotosParts.add(MultipartBody.Part.createFormData("Fotos", tmp.name, body))
                }
            }

            val resp = postApi.updatePost(
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
            resp.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    private fun downloadToCache(context: Context, url: String): File? {
        return try {
            val client = okhttp3.OkHttpClient()
            val req = okhttp3.Request.Builder().url(url).build()
            val res = client.newCall(req).execute()
            if (!res.isSuccessful) return null
            val bytes = res.body?.bytes() ?: return null
            val f = File(context.cacheDir, "orig_${System.currentTimeMillis()}.jpg")
            f.outputStream().use { it.write(bytes) }
            f
        } catch (_: Exception) {
            null
        }
    }

    suspend fun deletePost(token: String, postId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = postApi.deletePost("Bearer $token", postId)
            response.isSuccessful
        } catch (_: Exception) {
            false
        }
    }
}

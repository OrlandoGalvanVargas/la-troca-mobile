package com.troca.latroca.ui.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.troca.latroca.data.models.PostItem
import com.troca.latroca.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class PostViewModel(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _posts = MutableStateFlow<List<PostItem>>(emptyList())
    val posts: StateFlow<List<PostItem>> get() = _posts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> get() = _error

    fun loadPosts(token: String) {
        if (token.isBlank()) {
            _error.value = "Token inválido o vacío"
            Log.w("PostViewModel", "⚠️ Intento de cargar posts sin token")
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                Log.d("PostViewModel", "🔄 Cargando publicaciones...")
                val result = postRepository.getAllPosts(token)

                _posts.value = result
                Log.d("PostViewModel", "✅ ${result.size} publicaciones cargadas correctamente")

            } catch (e: UnknownHostException) {
                _error.value = "Sin conexión a internet. Verifica tu conexión."
                Log.e("PostViewModel", "❌ Error de conexión: No hay internet", e)

            } catch (e: SocketTimeoutException) {
                _error.value = "Tiempo de espera agotado. Intenta de nuevo."
                Log.e("PostViewModel", "❌ Error: Timeout al cargar posts", e)

            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("401") == true ->
                        "Sesión expirada. Inicia sesión nuevamente."
                    e.message?.contains("403") == true ->
                        "No tienes permisos para ver publicaciones."
                    e.message?.contains("404") == true ->
                        "Servicio no encontrado. Contacta soporte."
                    e.message?.contains("500") == true ->
                        "Error en el servidor. Intenta más tarde."
                    e.message?.contains("Unable to resolve host") == true ->
                        "No se puede conectar al servidor."
                    else ->
                        "Error al cargar publicaciones: ${e.message ?: "Error desconocido"}"
                }
                _error.value = errorMessage
                Log.e("PostViewModel", "❌ Error cargando publicaciones: ${e.message}", e)

            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearPosts() {
        _posts.value = emptyList()
        _error.value = null
        _isLoading.value = false
        Log.d("PostViewModel", "🧹 Posts limpiados")
    }

    fun createPostWithImage(
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
    ) {
        if (token.isBlank()) {
            onError("Token inválido o vacío")
            return
        }
        viewModelScope.launch {
            postRepository.createPostWithImage(
                context = context,
                token = token,
                titulo = titulo,
                descripcion = descripcion,
                categoria = categoria,
                necesidad = necesidad,
                ubicacion = ubicacion,
                latitude = latitude,
                longitude = longitude,
                imageUri = imageUri,
                onSuccess = onSuccess,
                onError = onError
            )
        }
    }

    fun updatePost(
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
        newImageUri: Uri?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (token.isBlank()) {
            onError("Token inválido o vacío")
            return
        }
        viewModelScope.launch {
            try {
                val result = postRepository.updatePost(
                    context = context,
                    token = token,
                    postId = postId,
                    titulo = titulo,
                    descripcion = descripcion,
                    categoria = categoria,
                    necesidad = necesidad,
                    ubicacionManual = ubicacionManual,
                    latitude = latitude,
                    longitude = longitude,
                    existingImageUrl = existingImageUrl,
                    newImageUri = newImageUri
                )

                if (result.isSuccess) {
                    _posts.value = _posts.value.map {
                        if (it.id == postId) it.copy(
                            titulo = titulo,
                            descripcion = descripcion,
                            categoria = categoria,
                            necesidad = necesidad,
                            ubicacion = it.ubicacion.copy(
                                manual = ubicacionManual,
                                latitude = latitude,
                                longitude = longitude
                            )
                        ) else it
                    }
                    onSuccess()
                } else {
                    onError(result.exceptionOrNull()?.message ?: "Error al actualizar publicación")
                }
            } catch (e: Exception) {
                onError("Error: ${e.message ?: "Error desconocido"}")
            }
        }
    }

    fun deletePost(token: String, postId: String, chatViewModel: ChatViewModel? = null) {
        if (token.isBlank()) {
            _error.value = "Token inválido o vacío"
            return
        }
        viewModelScope.launch {
            try {
                val result = postRepository.deletePost(token, postId)
                if (result) {
                    _posts.value = _posts.value.filterNot { it.id == postId }

                    chatViewModel?.deleteChatsForPost(postId) {
                        Log.d("PostViewModel", "Chats del post eliminados: $postId")
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error desconocido al eliminar publicación"
            }
        }
    }



    suspend fun analyzeImage(context: Context, token: String, imageUri: Uri): Pair<Boolean, String> {
        return postRepository.analyzeImage(context, token, imageUri)
    }
}

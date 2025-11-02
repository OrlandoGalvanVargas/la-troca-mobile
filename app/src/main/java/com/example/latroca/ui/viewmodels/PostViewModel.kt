package com.example.latroca.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.latroca.data.models.PostItem
import com.example.latroca.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
            return
        }
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = postRepository.getAllPosts(token)
                _posts.value = result
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Error desconocido al cargar publicaciones"
            } finally {
                _isLoading.value = false
            }
        }
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

                if (result) {
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
                    onError("Error al actualizar publicación")
                }
            } catch (e: Exception) {
                onError("Error: ${e.message ?: "Error desconocido"}")
            }
        }
    }

    fun deletePost(token: String, id: String) {
        if (token.isBlank()) {
            _error.value = "Token inválido o vacío"
            return
        }
        viewModelScope.launch {
            try {
                val result = postRepository.deletePost(token, id)
                if (result) {
                    _posts.value = _posts.value.filterNot { it.id == id }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error desconocido al eliminar publicación"
            }
        }
    }

    // 🆕 Función para limpiar posts
    fun clearPosts() {
        _posts.value = emptyList()
    }

    suspend fun analyzeText(token: String, text: String): Pair<Boolean, String> {
        return postRepository.analyzeText(token, text)
    }

    suspend fun analyzeImage(context: Context, token: String, imageUri: Uri): Pair<Boolean, String> {
        return postRepository.analyzeImage(context, token, imageUri)
    }
}

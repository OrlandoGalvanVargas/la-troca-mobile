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

    // 🆕 Nueva variable para controlar si ya se cargó al menos una vez
    private var hasLoadedInitially = false

    // 🆕 Variable para indicar si hay una actualización en segundo plano
    private val _isRefreshingInBackground = MutableStateFlow(false)
    val isRefreshingInBackground: StateFlow<Boolean> get() = _isRefreshingInBackground

    /**
     * 🆕 Carga posts con estrategia de caché:
     * - Primera vez: Muestra loading y carga
     * - Siguientes veces: Muestra caché inmediatamente y actualiza en segundo plano
     */
    fun loadPosts(token: String, forceRefresh: Boolean = false) {
        if (token.isBlank()) {
            _error.value = "Token inválido o vacío"
            Log.w("PostViewModel", "⚠️ Intento de cargar posts sin token")
            return
        }

        // Si es la primera carga o se fuerza refresh, mostrar loading
        if (!hasLoadedInitially || forceRefresh) {
            loadPostsWithLoading(token)
        } else {
            // Ya tenemos caché, actualizar en segundo plano
            loadPostsInBackground(token)
        }
    }

    /**
     * 🆕 Carga inicial con indicador de loading visible
     */
    private fun loadPostsWithLoading(token: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                Log.d("PostViewModel", "🔄 Cargando publicaciones (primera vez o refresh forzado)...")
                val result = postRepository.getAllPosts(token)

                _posts.value = result
                hasLoadedInitially = true
                Log.d("PostViewModel", "✅ ${result.size} publicaciones cargadas correctamente")

            } catch (e: UnknownHostException) {
                _error.value = "Sin conexión a internet. Verifica tu conexión."
                Log.e("PostViewModel", "❌ Error de conexión: No hay internet", e)

            } catch (e: SocketTimeoutException) {
                _error.value = "Tiempo de espera agotado. Intenta de nuevo."
                Log.e("PostViewModel", "❌ Error: Timeout al cargar posts", e)

            } catch (e: Exception) {
                val errorMessage = getErrorMessage(e)
                _error.value = errorMessage
                Log.e("PostViewModel", "❌ Error cargando publicaciones: ${e.message}", e)

            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 🆕 Actualización en segundo plano sin mostrar loading principal
     */
    private fun loadPostsInBackground(token: String) {
        viewModelScope.launch {
            try {
                _isRefreshingInBackground.value = true
                Log.d("PostViewModel", "🔄 Actualizando publicaciones en segundo plano...")

                val currentPosts = _posts.value
                val newPosts = postRepository.getAllPosts(token)

                // Solo actualizar si hay cambios
                if (hasPostsChanged(currentPosts, newPosts)) {
                    _posts.value = newPosts
                    Log.d("PostViewModel", "✅ Posts actualizados: ${currentPosts.size} → ${newPosts.size}")
                } else {
                    Log.d("PostViewModel", "ℹ️ No hay cambios en las publicaciones")
                }

            } catch (e: Exception) {
                // En segundo plano, solo logueamos errores sin mostrarlos al usuario
                Log.w("PostViewModel", "⚠️ Error en actualización de fondo (no crítico): ${e.message}")
            } finally {
                _isRefreshingInBackground.value = false
            }
        }
    }

    /**
     * 🆕 Compara si los posts han cambiado
     */
    private fun hasPostsChanged(oldPosts: List<PostItem>, newPosts: List<PostItem>): Boolean {
        // Primero comparar tamaño
        if (oldPosts.size != newPosts.size) return true

        // Comparar IDs (más rápido que comparar todo el objeto)
        val oldIds = oldPosts.map { it.id }.toSet()
        val newIds = newPosts.map { it.id }.toSet()

        return oldIds != newIds
    }

    /**
     * 🆕 Forzar recarga completa (útil para pull-to-refresh)
     */
    fun forceRefresh(token: String) {
        loadPostsWithLoading(token)
    }

    /**
     * 🆕 Obtener mensaje de error apropiado
     */
    private fun getErrorMessage(e: Exception): String {
        return when {
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
    }

    fun clearPosts() {
        _posts.value = emptyList()
        _error.value = null
        _isLoading.value = false
        hasLoadedInitially = false // 🆕 Resetear flag
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
                onSuccess = {
                    // 🆕 Después de crear, refrescar en segundo plano
                    loadPostsInBackground(token)
                    onSuccess()
                },
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
                    // 🆕 Actualizar localmente primero (optimistic update)
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

                    // Luego refrescar en segundo plano para sincronizar
                    loadPostsInBackground(token)
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
                    // 🆕 Eliminar localmente primero (optimistic update)
                    _posts.value = _posts.value.filterNot { it.id == postId }

                    chatViewModel?.deleteChatsForPost(postId) {
                        Log.d("PostViewModel", "Chats del post eliminados: $postId")
                    }

                    // Luego refrescar en segundo plano para sincronizar
                    loadPostsInBackground(token)
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
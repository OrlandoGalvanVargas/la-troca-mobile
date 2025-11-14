package com.troca.latroca.ui.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.troca.latroca.data.repository.AuthRepository
import com.troca.latroca.domain.models.AuthResult
import com.troca.latroca.domain.models.RegistrationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class RegistrationViewModel(private val authRepository: AuthRepository, private val authViewModel: AuthViewModel) : ViewModel() {

    private val _registrationData = MutableStateFlow(RegistrationData())
    val registrationData: StateFlow<RegistrationData> = _registrationData.asStateFlow()

    private val _uiState = MutableStateFlow<AuthResult<String>>(AuthResult.Idle)
    val uiState: StateFlow<AuthResult<String>> = _uiState.asStateFlow()

    fun updateStep1Data(nombre: String, email: String, password: String) {
        _registrationData.value = _registrationData.value.copy(
            nombre = nombre,
            email = email,
            password = password
        )
    }

    fun updateStep2Data(bio: String, ubicacionManual: String, lat: Double, lon: Double, imageUri: Uri?) {
        _registrationData.value = _registrationData.value.copy(
            bio = bio,
            ubicacion = ubicacionManual,
            latitude = lat,
            longitude = lon,
            imageUri = imageUri
        )
    }

    fun clearErrors() {
        if (_uiState.value is AuthResult.Error) {
            _uiState.value = AuthResult.Idle
        }
    }

    fun completeRegistration(context: android.content.Context) {
        val data = _registrationData.value

        if (data.nombre.isBlank() || data.email.isBlank() || data.password.isBlank() || data.bio.isBlank() || data.ubicacion.isBlank()) {
            _uiState.value = AuthResult.Error("Todos los campos son requeridos")
            return
        }

        _uiState.value = AuthResult.Loading

        viewModelScope.launch {
            try {
                val imageFile = data.imageUri?.let { uri ->
                    if (uri.toString().startsWith("http://") || uri.toString().startsWith("https://")) {
                        downloadImageFromUrl(uri.toString(), context)
                    } else {
                        convertUriToFile(uri, context)
                    }
                }

                val registerResult = authRepository.register(
                    nombre = data.nombre,
                    email = data.email,
                    password = data.password,
                    bio = data.bio,
                    ubicacion = data.ubicacion,
                    latitude = data.latitude,
                    longitude = data.longitude,
                    imageFile = imageFile
                )

                if (registerResult is AuthResult.Success) {
                    Log.d("RegistrationVM", "Registro exitoso, iniciando login automático...")

                    authViewModel.login(data.email, data.password)

                    delay(1500)

                    val token = authViewModel.getToken()
                    if (token != null) {
                        Log.d("RegistrationVM", "Token obtenido exitosamente")
                        _uiState.value = AuthResult.Success("Registro completado y sesión iniciada")
                    } else {
                        Log.e("RegistrationVM", "No se pudo obtener el token")
                        _uiState.value = AuthResult.Success("Registro completado")
                    }
                } else {
                    _uiState.value = registerResult
                }

            } catch (e: Exception) {
                _uiState.value = AuthResult.Error(e.message ?: "Error en el registro")
            }
        }
    }

    private suspend fun downloadImageFromUrl(imageUrl: String, context: android.content.Context): File? = withContext(Dispatchers.IO) {
        try {
            Log.d("RegistrationVM", "Descargando imagen de Google: $imageUrl")

            val url = URL(imageUrl)
            val connection = url.openConnection()
            connection.connect()

            val file = File.createTempFile("google_profile_image", ".jpg", context.cacheDir)

            connection.getInputStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            Log.d("RegistrationVM", "Imagen descargada exitosamente: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("RegistrationVM", "Error descargando imagen de Google: ${e.message}")
            null
        }
    }

    private suspend fun convertUriToFile(uri: Uri, context: android.content.Context): File? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val file = File.createTempFile("profile_image", ".jpg", context.cacheDir)

            contentResolver.openInputStream(uri)?.use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            file
        } catch (e: Exception) {
            Log.e("RegistrationVM", "Error convirtiendo URI a File: ${e.message}")
            null
        }
    }

    fun resetState() {
        _registrationData.value = RegistrationData()
        _uiState.value = AuthResult.Idle
    }

    val isLoading: Boolean
        get() = uiState.value is AuthResult.Loading
}
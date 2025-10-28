package com.example.latroca.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.latroca.data.repository.AuthRepository
import com.example.latroca.domain.models.AuthResult
import com.example.latroca.domain.models.RegistrationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


class RegistrationViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _registrationData = MutableStateFlow(RegistrationData())
    val registrationData: StateFlow<RegistrationData> = _registrationData.asStateFlow()

    private val _uiState = MutableStateFlow<AuthResult<String>>(AuthResult.Idle)
    val uiState: StateFlow<AuthResult<String>> = _uiState.asStateFlow()

    // Guardar datos del paso 1 (registro)
    fun updateStep1Data(nombre: String, email: String, password: String) {
        _registrationData.value = _registrationData.value.copy(
            nombre = nombre,
            email = email,
            password = password

        )
    }

    // Guardar datos del paso 2 (perfil)
    fun updateStep2Data(bio: String,ubicacionManual: String, lat: Double, lon: Double,imageUri: Uri?) {
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
                    convertUriToFile(uri, context)
                }

                val result = authRepository.register(
                    nombre = data.nombre,
                    email = data.email,
                    password = data.password,
                    bio = data.bio,
                    ubicacion = data.ubicacion,
                    latitude = data.latitude,
                    longitude = data.longitude,
                    imageFile = imageFile
                )

                _uiState.value = result

            } catch (e: Exception) {
                _uiState.value = AuthResult.Error(e.message ?: "Error en el registro")
            }
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
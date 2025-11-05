package com.troca.latroca.ui.viewmodels

import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.troca.latroca.data.models.UserProfileResponse
import com.troca.latroca.data.repository.AuthRepository
import com.troca.latroca.domain.models.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<AuthResult<String>>(AuthResult.Idle)
    val loginState: StateFlow<AuthResult<String>> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<AuthResult<String>>(AuthResult.Idle)
    val registerState: StateFlow<AuthResult<String>> = _registerState.asStateFlow()

    // 🆕 Hacer el token observable
    private val _currentToken = MutableStateFlow<String?>(null)
    val currentToken: StateFlow<String?> = _currentToken.asStateFlow()

    fun getToken(): String? = _currentToken.value

    fun getUserId(): String {
        val token = getToken() ?: return ""
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return ""
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val json = JSONObject(payload)
            json.optString("userId", "")
        } catch (e: Exception) {
            ""
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = AuthResult.Error("Email y contraseña son requeridos")
            return
        }

        _loginState.value = AuthResult.Loading
        viewModelScope.launch {
            try {
                val result = authRepository.login(email, password)

                if (result is AuthResult.Success) {
                    _currentToken.value = result.data  // 👈 Actualizar StateFlow
                }

                _loginState.value = result
            } catch (e: Exception) {
                _loginState.value = AuthResult.Error("Error inesperado: ${e.message}")
            }
        }
    }
    // En AuthViewModel.kt
    fun loginWithGoogle(googleIdToken: String) {
        _loginState.value = AuthResult.Loading
        viewModelScope.launch {
            try {
                val result = authRepository.loginWithGoogle(googleIdToken)

                if (result is AuthResult.Success) {
                    _currentToken.value = result.data  // 👈 Actualizar StateFlow
                }

                _loginState.value = result
            } catch (e: Exception) {
                _loginState.value = AuthResult.Error("Error inesperado: ${e.message}")
            }
        }
    }

    private val _userProfile = MutableStateFlow<UserProfileResponse?>(null)
    val userProfile: StateFlow<UserProfileResponse?> = _userProfile.asStateFlow()

    fun loadUserProfile() {
        viewModelScope.launch {
            val token = getToken()
            if (token.isNullOrBlank()) return@launch

            when (val result = authRepository.getUserProfile(token)) {
                is AuthResult.Success -> {
                    _userProfile.value = result.data
                }
                is AuthResult.Error -> {
                    Log.e("AuthViewModel", "Error loading profile: ${result.message}")
                }
                else -> {}
            }
        }
    }

    fun register(
        nombre: String,
        email: String,
        password: String,
        bio: String,
        ubicacion: String,
        latitude: Double,
        longitude: Double,
        imageFile: java.io.File? = null
    ) {
        if (nombre.isBlank() || email.isBlank() || password.isBlank() || bio.isBlank() || ubicacion.isBlank()) {
            _registerState.value = AuthResult.Error("Todos los campos son requeridos")
            return
        }

        _registerState.value = AuthResult.Loading
        viewModelScope.launch {
            try {
                _registerState.value = authRepository.register(
                    nombre,
                    email,
                    password,
                    bio,
                    ubicacion,
                    latitude,
                    longitude,
                    imageFile
                )
            } catch (e: Exception) {
                _registerState.value = AuthResult.Error("Error en el registro: ${e.message}")
            }
        }
    }

    fun deactivateAccount(reason: String) {
        viewModelScope.launch {
            try {
                val token = getToken()
                if (token.isNullOrBlank()) {
                    _loginState.value = AuthResult.Error("No hay token disponible")
                    return@launch
                }

                val result = authRepository.deactivateAccount(token, reason)
                _loginState.value = result
            } catch (e: Exception) {
                _loginState.value = AuthResult.Error("Error: ${e.message}")
            }
        }
    }

    // 🆕 Función para logout completo
    fun logout() {
        _loginState.value = AuthResult.Idle
        _registerState.value = AuthResult.Idle
        _currentToken.value = null  // 👈 Limpiar token
    }

    fun resetLoginState() {
        _loginState.value = AuthResult.Idle
    }

    fun resetRegisterState() {
        _registerState.value = AuthResult.Idle
    }

    fun clearAllStates() {
        _loginState.value = AuthResult.Idle
        _registerState.value = AuthResult.Idle
        _currentToken.value = null  // 👈 Limpiar token
    }

    val isLoading: Boolean
        get() = loginState.value is AuthResult.Loading ||
                registerState.value is AuthResult.Loading
}

package com.example.latroca.ui.viewmodels

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.latroca.data.repository.AuthRepository
import com.example.latroca.domain.models.AuthResult
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

    private var currentToken: String? = null
    fun getToken(): String? = currentToken

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
                    currentToken = result.data
                }

                _loginState.value = result
            } catch (e: Exception) {
                _loginState.value = AuthResult.Error("Error inesperado: ${e.message}")
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

    fun resetLoginState() {
        _loginState.value = AuthResult.Idle
    }

    fun resetRegisterState() {
        _registerState.value = AuthResult.Idle
    }

    fun clearAllStates() {
        _loginState.value = AuthResult.Idle
        _registerState.value = AuthResult.Idle
    }

    val isLoading: Boolean
        get() = loginState.value is AuthResult.Loading ||
                registerState.value is AuthResult.Loading
}

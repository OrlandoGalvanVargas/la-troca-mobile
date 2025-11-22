package com.troca.latroca.ui.viewmodels

import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.troca.latroca.data.local.TokenManager
import com.google.firebase.auth.FirebaseAuth
import com.troca.latroca.data.models.AdminUserResponse
import com.troca.latroca.data.models.UserProfileResponse
import com.troca.latroca.data.repository.AuthRepository
import com.troca.latroca.data.repository.ChatRepository
import com.troca.latroca.domain.models.AuthResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _loginState = MutableStateFlow<AuthResult<String>>(AuthResult.Idle)
    val loginState: StateFlow<AuthResult<String>> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<AuthResult<String>>(AuthResult.Idle)

    private val _currentToken = MutableStateFlow<String?>(null)
    val currentToken: StateFlow<String?> = _currentToken.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfileResponse?>(null)
    val userProfile: StateFlow<UserProfileResponse?> = _userProfile.asStateFlow()

    private val _updateProfileState = MutableStateFlow<AuthResult<String>>(AuthResult.Idle)
    val updateProfileState: StateFlow<AuthResult<String>> = _updateProfileState.asStateFlow()

    private val _changePasswordState = MutableStateFlow<AuthResult<String>>(AuthResult.Idle)
    val changePasswordState: StateFlow<AuthResult<String>> = _changePasswordState.asStateFlow()

    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole.asStateFlow()

    private val _allUsers = MutableStateFlow<List<AdminUserResponse>>(emptyList())
    val allUsers: StateFlow<List<AdminUserResponse>> = _allUsers.asStateFlow()

    private val _loadingUsers = MutableStateFlow(false)
    val loadingUsers: StateFlow<Boolean> = _loadingUsers.asStateFlow()
    private val _selectedUser = MutableStateFlow<AdminUserResponse?>(null)
    val selectedUser: StateFlow<AdminUserResponse?> = _selectedUser.asStateFlow()

    private val _loadingUser = MutableStateFlow(false)
    val loadingUser: StateFlow<Boolean> = _loadingUser.asStateFlow()

    private val _deleteUserState = MutableStateFlow<AuthResult<String>>(AuthResult.Idle)
    val deleteUserState: StateFlow<AuthResult<String>> = _deleteUserState.asStateFlow()

    private val chatRepository = ChatRepository()

    init {
        loadSavedToken()
        loadSavedRole()
    }

    private fun loadSavedToken() {
        val savedToken = tokenManager.getToken()
        if (!savedToken.isNullOrBlank()) {
            _currentToken.value = savedToken
            loadUserProfile()
        }
    }

    private fun loadSavedRole() {
        _userRole.value = tokenManager.getRole()
    }

    fun getToken(): String? = _currentToken.value

    fun getUserId(): String {
        val token = getToken() ?: return ""
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return ""
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val json = JSONObject(payload)
            json.optString("userId", "")
        } catch (_: Exception) {
            ""
        }
    }

    fun isAdmin(): Boolean {
        return _userRole.value?.uppercase() == "ADMIN"
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

                when (result) {
                    is AuthResult.Success -> {
                        val (token, role) = result.data
                        _currentToken.value = token
                        _userRole.value = role
                        tokenManager.saveToken(token)
                        tokenManager.saveRole(role)
                        loadUserProfile()

                        Log.d("AuthViewModel", "✅ Login exitoso - Rol: $role")

                        _loginState.value = AuthResult.Success(token)
                    }
                    is AuthResult.Error -> {
                        _loginState.value = AuthResult.Error(result.message)
                    }
                    else -> {
                        _loginState.value = AuthResult.Idle
                    }
                }
            } catch (e: Exception) {
                _loginState.value = AuthResult.Error("Error inesperado: ${e.message}")
            }
        }
    }

    fun loginWithGoogle(googleIdToken: String) {
        _loginState.value = AuthResult.Loading
        viewModelScope.launch {
            try {
                val result = authRepository.loginWithGoogle(googleIdToken)

                when (result) {
                    is AuthResult.Success -> {
                        val (token, role) = result.data
                        _currentToken.value = token
                        _userRole.value = role
                        tokenManager.saveToken(token)
                        tokenManager.saveRole(role)
                        loadUserProfile()

                        Log.d("AuthViewModel", "✅ Login con Google exitoso - Rol: $role")
                        Log.d("AuthViewModel", "✅ isAdmin(): ${isAdmin()}")
                        Log.d("AuthViewModel", "✅ _userRole.value: ${_userRole.value}")
                        _loginState.value = AuthResult.Success(token)
                    }
                    is AuthResult.Error -> {
                        _loginState.value = AuthResult.Error(result.message)
                    }
                    else -> {
                        _loginState.value = AuthResult.Idle
                    }
                }
            } catch (e: Exception) {
                _loginState.value = AuthResult.Error("Error inesperado: ${e.message}")
            }
        }
    }

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

    fun loadAllUsers() {
        viewModelScope.launch {
            try {
                val token = getToken()
                if (token.isNullOrBlank()) {
                    Log.w("AuthViewModel", "No hay token para cargar usuarios")
                    return@launch
                }

                if (!isAdmin()) {
                    Log.w("AuthViewModel", "Usuario no es admin")
                    return@launch
                }

                _loadingUsers.value = true

                when (val result = authRepository.getAllUsers(token)) {
                    is AuthResult.Success -> {
                        _allUsers.value = result.data
                        Log.d("AuthViewModel", "✅ ${result.data.size} usuarios cargados")
                    }
                    is AuthResult.Error -> {
                        Log.e("AuthViewModel", "Error cargando usuarios: ${result.message}")
                    }
                    else -> {}
                }

                _loadingUsers.value = false
            } catch (e: Exception) {
                _loadingUsers.value = false
                Log.e("AuthViewModel", "Error inesperado cargando usuarios", e)
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
    fun deleteMyAccount() {
        viewModelScope.launch {
            try {
                val token = getToken()
                if (token.isNullOrBlank()) {
                    _loginState.value = AuthResult.Error("No hay token disponible")
                    return@launch
                }

                val result = authRepository.deleteMyAccount(token)
                _loginState.value = result
            } catch (e: Exception) {
                _loginState.value = AuthResult.Error("Error: ${e.message}")
            }
        }
    }

    fun updateProfile(
        nombre: String?,
        bio: String?,
        ubicacion: String?,
        latitude: Double?,
        longitude: Double?,
        imageFile: File?
    ) {
        viewModelScope.launch {
            try {
                val token = getToken()
                if (token.isNullOrBlank()) {
                    _updateProfileState.value = AuthResult.Error("No hay token disponible")
                    return@launch
                }

                _updateProfileState.value = AuthResult.Loading

                val result = authRepository.updateProfile(
                    token = token,
                    nombre = nombre,
                    bio = bio,
                    ubicacion = ubicacion,
                    latitude = latitude,
                    longitude = longitude,
                    imageFile = imageFile
                )

                _updateProfileState.value = result

                if (result is AuthResult.Success) {
                    loadUserProfile()
                }
            } catch (e: Exception) {
                _updateProfileState.value = AuthResult.Error("Error inesperado: ${e.message}")
            }
        }
    }

    fun changePassword(newPassword: String) {
        viewModelScope.launch {
            try {
                val token = getToken()
                if (token.isNullOrBlank()) {
                    _changePasswordState.value = AuthResult.Error("No hay token disponible")
                    return@launch
                }

                if (newPassword.isBlank()) {
                    _changePasswordState.value = AuthResult.Error("La contraseña es requerida")
                    return@launch
                }

                if (newPassword.length < 8) {
                    _changePasswordState.value = AuthResult.Error("La contraseña debe tener al menos 8 caracteres")
                    return@launch
                }

                _changePasswordState.value = AuthResult.Loading

                val result = authRepository.changePassword(token, newPassword)
                _changePasswordState.value = result

                if (result is AuthResult.Success) {
                    Log.d("AuthViewModel", "✅ Contraseña cambiada, iniciando logout...")
                    delay(1000)
                    logout()
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error en changePassword", e)
                _changePasswordState.value = AuthResult.Error("Error inesperado: ${e.message}")
            }
        }
    }

    fun updateFcmToken(fcmToken: String) {
        viewModelScope.launch {
            try {
                val userId = getUserId()
                val jwtToken = getToken()

                if (userId.isEmpty() || jwtToken.isNullOrEmpty()) {
                    Log.w("AuthViewModel", "No hay usuario autenticado o token JWT para actualizar FCM token")
                    return@launch
                }

                Log.d("AuthViewModel", "Actualizando FCM token para usuario: $userId")

                val result = chatRepository.updateFcmToken(fcmToken, jwtToken)

                result.onSuccess {
                    Log.d("AuthViewModel", "✅ FCM token actualizado correctamente")
                }.onFailure { exception ->
                    Log.e("AuthViewModel", "❌ Error actualizando FCM token: ${exception.message}")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error inesperado actualizando FCM token", e)
            }
        }
    }

    private suspend fun removeFcmTokenInternal(): Result<Unit> {
        return try {
            val jwtToken = getToken()

            if (jwtToken.isNullOrEmpty()) {
                Log.w("AuthViewModel", "No hay token JWT para eliminar FCM token")
                return Result.failure(Exception("No JWT token available"))
            }

            Log.d("AuthViewModel", "Eliminando FCM token del usuario")

            chatRepository.removeFcmToken(jwtToken)
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error inesperado eliminando FCM token", e)
            Result.failure(e)
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "🔐 Iniciando proceso de logout...")

                val removalResult = removeFcmTokenInternal()

                removalResult.onSuccess {
                    Log.d("AuthViewModel", "✅ FCM token eliminado del backend")
                }.onFailure { exception ->
                    Log.w("AuthViewModel", "⚠️ No se pudo eliminar FCM token del backend: ${exception.message}")
                }

                _loginState.value = AuthResult.Idle
                _registerState.value = AuthResult.Idle

                FirebaseAuth.getInstance().signOut()

                tokenManager.clearAll()

                _currentToken.value = null
                _userProfile.value = null
                _userRole.value = null
                _allUsers.value = emptyList()

                Log.d("AuthViewModel", "✅ Sesión cerrada exitosamente")

            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Error durante el logout", e)

                _currentToken.value = null
                _userProfile.value = null
                _userRole.value = null
                tokenManager.clearAll()
                FirebaseAuth.getInstance().signOut()
            }
        }
    }

    fun loadUserById(userId: String) {
        viewModelScope.launch {
            try {
                val token = getToken()
                if (token.isNullOrBlank()) {
                    Log.w("AuthViewModel", "No hay token para cargar usuario")
                    return@launch
                }

                if (!isAdmin()) {
                    Log.w("AuthViewModel", "Usuario no es admin")
                    return@launch
                }

                _loadingUser.value = true

                when (val result = authRepository.getUserById(token, userId)) {
                    is AuthResult.Success -> {
                        _selectedUser.value = result.data
                        Log.d("AuthViewModel", "✅ Usuario ${result.data.name} cargado")
                    }
                    is AuthResult.Error -> {
                        Log.e("AuthViewModel", "Error cargando usuario: ${result.message}")
                    }
                    else -> {}
                }

                _loadingUser.value = false
            } catch (e: Exception) {
                _loadingUser.value = false
                Log.e("AuthViewModel", "Error inesperado cargando usuario", e)
            }
        }
    }

    fun deleteUser(userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val token = getToken()
                if (token.isNullOrBlank()) {
                    _deleteUserState.value = AuthResult.Error("No hay token disponible")
                    return@launch
                }

                if (!isAdmin()) {
                    _deleteUserState.value = AuthResult.Error("No tienes permisos")
                    return@launch
                }

                _deleteUserState.value = AuthResult.Loading

                when (val result = authRepository.deleteUser(token, userId)) {
                    is AuthResult.Success -> {
                        Log.d("AuthViewModel", "✅ Usuario eliminado")
                        _deleteUserState.value = result

                        loadAllUsers()

                        onSuccess()
                    }
                    is AuthResult.Error -> {
                        Log.e("AuthViewModel", "Error eliminando usuario: ${result.message}")
                        _deleteUserState.value = result
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error inesperado eliminando usuario", e)
                _deleteUserState.value = AuthResult.Error("Error inesperado: ${e.message}")
            }
        }
    }

    fun resetDeleteUserState() {
        _deleteUserState.value = AuthResult.Idle
    }

    fun resetChangePasswordState() {
        _changePasswordState.value = AuthResult.Idle
    }

    fun resetUpdateProfileState() {
        _updateProfileState.value = AuthResult.Idle
    }

    fun resetLoginState() {
        _loginState.value = AuthResult.Idle
    }
}
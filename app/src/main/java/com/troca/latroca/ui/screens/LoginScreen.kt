package com.troca.latroca.ui.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.navigation.NavController
import com.datadog.android.rum.GlobalRumMonitor
import com.datadog.android.rum.RumActionType
import com.troca.latroca.R
import com.troca.latroca.domain.models.AuthResult
import com.troca.latroca.ui.components.LoadingModal
import com.troca.latroca.ui.viewmodels.AuthViewModel
import com.troca.latroca.ui.viewmodels.RegistrationViewModel
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.troca.latroca.BuildConfig
import com.troca.latroca.data.local.FirstTimeManager
import com.troca.latroca.ui.components.WelcomeModal
import com.troca.latroca.utils.validateEmailInput
import com.troca.latroca.utils.validatePasswordInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.UnknownHostException
import java.io.IOException

private fun isValidGeneralDomain(domain: String): Boolean {
    val domainRegex = Regex("^[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z]{2,})+$")
    return domainRegex.matches(domain)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    registrationViewModel: RegistrationViewModel
) {
    val loginState by authViewModel.loginState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val firstTimeManager = remember { FirstTimeManager(context) }
    var showWelcomeModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showWelcomeModal = firstTimeManager.isFirstTime()
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf("") }
    var emailTouched by remember { mutableStateOf(false) }
    var credentialsError by remember { mutableStateOf(false) }

    val isLoading = loginState is AuthResult.Loading
    var isGoogleLoading by remember { mutableStateOf(false) }
    var isGoogleLogin by remember { mutableStateOf(false) }

    val validateEmailRealTime = remember {
        { email: String ->
            if (email.isBlank()) return@remember ""

            when {
                email.startsWith(".") -> "No puede empezar con punto"
                email.endsWith(".") -> "No puede terminar con punto"
                email.contains("..") -> "No se permiten puntos consecutivos"
                !email.contains("@") -> "El correo debe contener @"
                email.split("@").size != 2 -> "Formato de correo inválido"
                email.split("@")[0].isEmpty() -> "Falta la parte antes del @"
                email.split("@")[1].isEmpty() -> "Falta el dominio después del @"
                email.split("@")[0].length > 30 -> "Máximo 30 caracteres antes del @"
                email.length > 64 -> "Máximo 64 caracteres en total"
                !isValidGeneralDomain(email.split("@")[1]) -> "Dominio de correo no válido"
                else -> ""
            }
        }
    }

    val isFormValid by remember(email, password, emailError) {
        derivedStateOf {
            email.isNotBlank() &&
                    password.isNotBlank() &&
                    emailError.isBlank()
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.resetLoginState()
        credentialsError = false
    }

    LaunchedEffect(loginState) {
        when (loginState) {
            is AuthResult.Success -> {
                credentialsError = false
                // 🔥 DATADOG: Identificar usuario al iniciar sesión
                val userId = authViewModel.getUserId()
                val userEmail = email.takeIf { it.isNotBlank() } ?: "google_user"
                // ✅ Método correcto para versión 3.2.0
                GlobalRumMonitor.get().addAttribute("user_id", userId)
                GlobalRumMonitor.get().addAttribute("user_email", userEmail)
                GlobalRumMonitor.get().addAttribute("login_method", if (isGoogleLogin) "google" else "email")
                GlobalRumMonitor.get().addAttribute("app_version", BuildConfig.VERSION_NAME)

                // Track acción de login exitoso
                GlobalRumMonitor.get().addAction(
                    type = RumActionType.CUSTOM,
                    name = "login_success",
                    attributes = mapOf(
                        "login_method" to if (isGoogleLogin) "google" else "email",
                        "user_id" to userId,
                        "user_email" to userEmail
                    )
                )

                Log.d("DatadogRUM", "👤 Usuario identificado: $userId")
                authViewModel.resetLoginState()
                isGoogleLogin = false
                isGoogleLoading = false

                navController.navigate("home") {
                    popUpTo(0) { inclusive = true }
                }
            }
            is AuthResult.Error -> {
                val errorMessage = (loginState as AuthResult.Error).message
                // 🔥 DATADOG: Track errores de login
                GlobalRumMonitor.get().addAction(
                    type = RumActionType.CUSTOM,
                    name = "login_error",
                    attributes = mapOf(
                        "error_message" to errorMessage,
                        "login_method" to if (isGoogleLogin) "google" else "email",
                        "error_type" to when {
                            errorMessage.contains("inactiva", ignoreCase = true) -> "account_inactive"
                            errorMessage.contains("network", ignoreCase = true) -> "network_error"
                            errorMessage.contains("500", ignoreCase = true) -> "server_error"
                            else -> "credentials_error"
                        }
                    )
                )

                Log.e("LoginScreen", "Error completo: $errorMessage")

                if (errorMessage.contains("inactiva", ignoreCase = true) ||
                    errorMessage.contains("suspendida", ignoreCase = true) ||
                    errorMessage.contains("desactivada", ignoreCase = true)) {

                    isGoogleLogin = false
                    isGoogleLoading = false
                    credentialsError = false

                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    authViewModel.resetLoginState()
                }
                else if (isGoogleLogin &&
                    (errorMessage.contains("no registrado", ignoreCase = true) ||
                            errorMessage.contains("not found", ignoreCase = true) ||
                            errorMessage.contains("404", ignoreCase = true) ||
                            errorMessage.contains("Usuario no registrado", ignoreCase = true) ||
                            errorMessage.contains("Credenciales incorrectas", ignoreCase = true))) {

                    Log.d("LoginScreen", "Usuario Google no registrado → completeProfile")

                    isGoogleLogin = false
                    isGoogleLoading = false
                    authViewModel.resetLoginState()

                    navController.navigate("completeProfile") {
                        popUpTo(0) { inclusive = true }
                    }
                }
                else if (errorMessage.contains("network", ignoreCase = true) ||
                    errorMessage.contains("timeout", ignoreCase = true) ||
                    errorMessage.contains("connection", ignoreCase = true)) {

                    isGoogleLogin = false
                    isGoogleLoading = false

                    Toast.makeText(
                        context,
                        "Sin conexión a internet. Verifica tu red",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("LoginScreen", "Error de red: $errorMessage")
                }
                else if (errorMessage.contains("500", ignoreCase = true) ||
                    errorMessage.contains("502", ignoreCase = true) ||
                    errorMessage.contains("503", ignoreCase = true)) {

                    isGoogleLogin = false
                    isGoogleLoading = false

                    Toast.makeText(
                        context,
                        "Servidor no disponible. Inténtalo más tarde",
                        Toast.LENGTH_LONG
                    ).show()
                }
                else {
                    isGoogleLogin = false
                    isGoogleLoading = false

                    credentialsError = errorMessage.containsAny(
                        "contraseña", "password", "usuario", "user", "email", "credenciales", "not found"
                    )

                    if (!credentialsError) {
                        Toast.makeText(
                            context,
                            "Error al iniciar sesión. Inténtalo nuevamente",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            else -> {}
        }
    }

    val credentialManager = CredentialManager.create(context)
    val auth = Firebase.auth

    LoadingModal(
        isVisible = isLoading || isGoogleLoading,
        message = if (isGoogleLogin) "Iniciando sesión con Google..." else "Iniciando sesión...",
        timeoutSeconds = 5
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.la_troca_logo),
                contentDescription = "Logo La Troca",
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Iniciar Sesión",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color(0xFF2D3748)
            )

            Text(
                text = "Bienvenido de nuevo",
                fontSize = 14.sp,
                color = Color(0xFF718096)
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { newEmail ->
                    val filteredEmail = validateEmailInput(email, newEmail)
                    email = filteredEmail
                    emailTouched = true
                        if (emailTouched) {
                            emailError = validateEmailRealTime(newEmail)
                        }
                        if (credentialsError) {
                            credentialsError = false
                        }

                },
                label = { Text("Correo", fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                enabled = !isLoading && !isGoogleLoading,
                isError = emailError.isNotBlank() || credentialsError,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (emailError.isNotBlank() || credentialsError) Color.Red else Color(0xFFE53E3E),
                    unfocusedBorderColor = if (emailError.isNotBlank() || credentialsError) Color.Red else Color(0xFFE2E8F0),
                    disabledBorderColor = Color(0xFFE2E8F0),
                    focusedTextColor = Color(0xFF2D3748),
                    unfocusedTextColor = Color(0xFF2D3748),
                    disabledTextColor = Color(0xFF718096),
                    cursorColor = Color(0xFFE53935),
                    errorBorderColor = Color.Red
                ),
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.email_input),
                        contentDescription = "Correo electrónico",
                        tint = if (emailError.isNotBlank() || credentialsError) Color.Red else
                            if (email.isNotEmpty()) Color(0xFFE53935) else Color(0xFF718096),
                        modifier = Modifier.size(20.dp)
                    )
                },
                placeholder = {
                    Text(
                        text = "ejemplo@correo.com",
                        color = Color(0xFFA0AEC0),
                        fontSize = 14.sp
                    )
                }
            )

            if (emailError.isNotBlank()) {
                Text(
                    text = emailError,
                    color = Color.Red,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { newPassword ->
                    val filteredPassword = validatePasswordInput(password, newPassword)
                    password = filteredPassword
                        if (credentialsError) {
                            credentialsError = false
                        }

                },
                label = { Text("Contraseña", fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = !isLoading && !isGoogleLoading,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = credentialsError,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (credentialsError) Color.Red else Color(0xFFE53E3E),
                    unfocusedBorderColor = if (credentialsError) Color.Red else Color(0xFFE2E8F0),
                    disabledBorderColor = Color(0xFFE2E8F0),
                    focusedTextColor = Color(0xFF2D3748),
                    unfocusedTextColor = Color(0xFF2D3748),
                    disabledTextColor = Color(0xFF718096),
                    cursorColor = Color(0xFFE53935),
                    errorBorderColor = Color.Red
                ),
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.password_input),
                        contentDescription = "Contraseña",
                        tint = if (credentialsError) Color.Red else
                            if (password.isNotEmpty()) Color(0xFFE53935) else Color(0xFF718096),
                        modifier = Modifier.size(24.dp)
                    )
                },
                placeholder = {
                    Text(
                        text = "Ingresa tu contraseña",
                        color = Color(0xFFA0AEC0),
                        fontSize = 14.sp
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        enabled = !isLoading && !isGoogleLoading
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = if (isLoading || isGoogleLoading) Color(0xFFCBD5E0) else
                                if (credentialsError) Color.Red else Color(0xFF718096)
                        )
                    }
                }
            )

            if (credentialsError) {
                Text(
                    text = "Credenciales incorrectas",
                    color = Color.Red,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isFormValid) {
                        credentialsError = false
                        isGoogleLogin = false
                        authViewModel.login(email, password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935),
                    disabledContainerColor = Color(0xFFE2E8F0)
                ),
                enabled = isFormValid && !isLoading && !isGoogleLoading && !credentialsError
            ) {
                Text(
                    text = "Iniciar sesión",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Divider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                Text(
                    text = "O continúa con",
                    fontSize = 12.sp,
                    color = Color(0xFF718096),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Divider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = {
                    if (!isLoading && !isGoogleLoading) {
                        isGoogleLoading = true
                        coroutineScope.launch(Dispatchers.Main) {
                            try {
                                isGoogleLogin = true

                                val googleIdOption = GetSignInWithGoogleOption.Builder(
                                    context.getString(R.string.default_web_client_id)
                                ).build()

                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()

                                val result = credentialManager.getCredential(
                                    context = context,
                                    request = request
                                )

                                val credential = result.credential
                                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    val googleIdTokenCredential =
                                        GoogleIdTokenCredential.createFrom(credential.data)
                                    val googleIdToken = googleIdTokenCredential.idToken

                                    val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                                    auth.signInWithCredential(firebaseCredential)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                val user = auth.currentUser
                                                val nombre = user?.displayName ?: ""
                                                val email = user?.email ?: ""
                                                val photoUrl = user?.photoUrl?.toString()?.let { url ->
                                                    // Cambiar s96-c a s400-c para obtener imagen de 400x400px
                                                    Uri.parse(url.replace("=s96-c", "=s400-c"))
                                                }
                                                authViewModel.loginWithGoogle(googleIdToken)

                                                val randomPassword = (1..12)
                                                    .map { ('a'..'z') + ('A'..'Z') + ('0'..'9') }
                                                    .flatten()
                                                    .shuffled()
                                                    .take(10)
                                                    .joinToString("")

                                                registrationViewModel.updateStep1Data(
                                                    nombre = nombre,
                                                    email = email,
                                                    password = randomPassword
                                                )

                                                photoUrl?.let { uri ->
                                                    registrationViewModel.updateStep2Data(
                                                        bio = "",
                                                        ubicacionManual = "",
                                                        lat = 0.0,
                                                        lon = 0.0,
                                                        imageUri = Uri.parse(uri.toString())
                                                    )
                                                }
                                            } else {
                                                isGoogleLogin = false
                                                isGoogleLoading = false
                                                Toast.makeText(
                                                    context,
                                                    "Error con Google. Inténtalo de nuevo",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                }
                            } catch (_: GetCredentialCancellationException) {
                                isGoogleLogin = false
                                isGoogleLoading = false
                            } catch (_: GoogleIdTokenParsingException) {
                                isGoogleLogin = false
                                isGoogleLoading = false
                                Toast.makeText(
                                    context,
                                    "Error al procesar Google",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (_: UnknownHostException) {
                                isGoogleLogin = false
                                isGoogleLoading = false
                                Toast.makeText(
                                    context,
                                    "Sin conexión a internet",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (_: IOException) {
                                isGoogleLogin = false
                                isGoogleLoading = false
                                Toast.makeText(
                                    context,
                                    "Error de conexión. Inténtalo nuevamente",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                isGoogleLogin = false
                                isGoogleLoading = false
                                Toast.makeText(
                                    context,
                                    "Error inesperado: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF2D3748)
                ),
                enabled = !isLoading && !isGoogleLoading
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google",
                        modifier = Modifier.size(24.dp),
                        alpha = if (isLoading || isGoogleLoading) 0.5f else 1f
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continuar con Google",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿No tienes cuenta?",
                    fontSize = 14.sp,
                    color = if (isLoading || isGoogleLoading) Color(0xFFCBD5E0) else Color(0xFF718096)
                )
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(
                    onClick = {
                        authViewModel.resetLoginState()
                        credentialsError = false
                        navController.navigate("register")
                    },
                    enabled = !isLoading && !isGoogleLoading,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Regístrate",
                        fontSize = 14.sp,
                        color = if (isLoading || isGoogleLoading) Color(0xFFCBD5E0) else Color(0xFFE53935),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        WelcomeModal(
            isVisible = showWelcomeModal,
            onDismiss = {
                firstTimeManager.markWelcomeShown()
                showWelcomeModal = false
            }
        )
    }
}

private fun String.containsAny(vararg terms: String, ignoreCase: Boolean = true): Boolean {
    return terms.any { this.contains(it, ignoreCase) }
}
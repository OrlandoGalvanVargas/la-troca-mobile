package com.example.latroca.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.navigation.NavController
import com.example.latroca.R
import com.example.latroca.ui.viewmodels.AuthViewModel
import com.example.latroca.domain.models.AuthResult
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.firebase.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.latroca.ui.viewmodels.RegistrationViewModel



import android.util.Log
import android.widget.Toast
import androidx.credentials.*
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


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
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var emailError by remember { mutableStateOf("") }
    var emailTouched by remember { mutableStateOf(false) }
    var credentialsError by remember { mutableStateOf(false) }

    val isLoading = loginState is AuthResult.Loading

    val validateEmailRealTime = remember {
        { email: String ->
            if (email.isBlank()) return@remember ""

            when {
                email.contains(" ") -> "No se permiten espacios en el correo"
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

    var isGoogleLogin by remember { mutableStateOf(false) }

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
                authViewModel.resetLoginState()
                isGoogleLogin = false

                navController.navigate("home") {
                    popUpTo(0) { inclusive = true }
                }
            }
            is AuthResult.Error -> {
                val errorMessage = (loginState as AuthResult.Error).message

                Log.d("LoginScreen", "Error recibido: $errorMessage")
                Log.d("LoginScreen", "isGoogleLogin: $isGoogleLogin")

                // 🔴 IMPORTANTE: Verificar PRIMERO si está desactivada/inactiva/suspendida
                if (errorMessage.contains("inactiva", ignoreCase = true) ||
                    errorMessage.contains("suspendida", ignoreCase = true) ||
                    errorMessage.contains("desactivada", ignoreCase = true)) {

                    isGoogleLogin = false
                    credentialsError = false

                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = errorMessage,
                            duration = SnackbarDuration.Long
                        )
                    }

                    // NO navegar a ningún lado, solo mostrar el mensaje
                    authViewModel.resetLoginState()
                }
                // 🟡 Si NO está desactivada, entonces verificar si es Google y no está registrado
                else if (isGoogleLogin &&
                    (errorMessage.contains("no registrado", ignoreCase = true) ||
                            errorMessage.contains("not found", ignoreCase = true) ||
                            errorMessage.contains("404", ignoreCase = true) ||
                            errorMessage.contains("Usuario no registrado", ignoreCase = true) ||
                            errorMessage.contains("Credenciales incorrectas", ignoreCase = true))) {

                    Log.d("LoginScreen", "Usuario Google no registrado → Navegando a completeProfile")

                    // Usuario de Google no registrado → Ir a completar perfil
                    isGoogleLogin = false
                    authViewModel.resetLoginState()

                    navController.navigate("completeProfile") {
                        popUpTo(0) { inclusive = true }
                    }
                }
                // 🔵 Otros errores (login tradicional)
                else {
                    Log.d("LoginScreen", "Error de login tradicional")

                    isGoogleLogin = false

                    credentialsError = errorMessage.containsAny(
                        "contraseña", "password", "usuario", "user", "email", "credenciales", "not found"
                    )

                    if (errorMessage.contains("network", ignoreCase = true)) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Problema de conexión. Verifica tu internet",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }

    val context = LocalContext.current
    val credentialManager = CredentialManager.create(context)
    val auth = Firebase.auth


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Image(
            painter = painterResource(id = R.drawable.la_troca_logo),
            contentDescription = "Logo La Troca",
            modifier = Modifier
                .size(150.dp) // antes 90.dp, agrandado
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Iniciar Sesión",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color(0xFF2D3748)
        )

        Text(
            text = "Bienvenido de nuevo a La Troca",
            fontSize = 14.sp,
            color = Color(0xFF718096)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Correo",
                color = Color(0xFFE53E3E),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            OutlinedTextField(
                value = email,
                onValueChange = { newEmail ->

                    if (newEmail.length <= 64) {
                        email = newEmail
                        emailTouched = true

                        if (emailTouched) {
                            emailError = validateEmailRealTime(newEmail)
                        }

                        if (credentialsError) {
                            credentialsError = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                isError = emailError.isNotBlank() || credentialsError,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = when {
                        emailError.isNotBlank() -> Color.Red
                        credentialsError -> Color.Red
                        else -> Color(0xFFE53E3E)
                    },
                    unfocusedBorderColor = when {
                        emailError.isNotBlank() -> Color.Red
                        credentialsError -> Color.Red
                        else -> Color(0xFFE2E8F0)
                    },
                    focusedTextColor = Color(0xFF2D3748),
                    unfocusedTextColor = Color(0xFF2D3748),
                    cursorColor = Color(0xFFE53E3E),
                    errorBorderColor = Color.Red,
                    errorTextColor = Color.Red
                ),
                placeholder = {
                    Text("ejemplo@uttt.edu.mx", color = Color(0xFFB0BEC5))
                }
            )

            if (emailError.isNotBlank()) {
                Text(
                    text = emailError,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, start = 4.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "Contraseña",
                color = Color(0xFFE53E3E),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            OutlinedTextField(
                value = password,
                onValueChange = { newPassword ->
                    if (newPassword.length <= 30) {
                        password = newPassword

                        if (credentialsError) {
                            credentialsError = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = credentialsError,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = when {
                        credentialsError -> Color.Red
                        else -> Color(0xFFE53E3E)
                    },
                    unfocusedBorderColor = when {
                        credentialsError -> Color.Red
                        else -> Color(0xFFE2E8F0)
                    },
                    focusedTextColor = Color(0xFF2D3748),
                    unfocusedTextColor = Color(0xFF2D3748),
                    cursorColor = Color(0xFFE53E3E),
                    errorBorderColor = Color.Red,
                    errorTextColor = Color.Red
                ),
                placeholder = {
                    Text("**********", color = Color(0xFFB0BEC5))
                },
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Filled.Visibility
                    else
                        Icons.Filled.VisibilityOff

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = image,
                            contentDescription = null,
                            tint = Color(0xFF718096)
                        )
                    }
                }
            )

            if (credentialsError) {
                Text(
                    text = "Credenciales incorrectas",
                    color = Color.Red,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp)
                        .align(Alignment.Start)
                )
            }

            TextButton(
                onClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            "Funcionalidad en desarrollo",
                            duration = SnackbarDuration.Short
                        )
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    color = Color(0xFF718096),
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (isFormValid) {
                    credentialsError = false
                    isGoogleLogin = false // 👈 Login tradicional
                    authViewModel.login(email, password)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF6B6B)
            ),
            enabled = isFormValid && !isLoading && !credentialsError
        ) {
            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Iniciando sesión...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "Iniciar sesión",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Inicio con Fecebook y Google (implementar)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Divider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
            Text(
                text = "O continúa con",
                fontSize = 13.sp,
                color = Color(0xFF718096)
            )
            Divider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = {
                    coroutineScope.launch(Dispatchers.Main) {
                        try {
                            isGoogleLogin = true // 👈 Marcar como login de Google

                            // 1️⃣ Crear opción de inicio de sesión con Google
                            val googleIdOption = GetSignInWithGoogleOption.Builder(
                                context.getString(R.string.default_web_client_id)
                            ).build()

                            // 2️⃣ Crear solicitud de credencial
                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()

                            // 3️⃣ Ejecutar sign-in
                            val result = credentialManager.getCredential(
                                context = context,
                                request = request
                            )

                            val credential = result.credential
                            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                val googleIdTokenCredential =
                                    GoogleIdTokenCredential.createFrom(credential.data)
                                val googleIdToken = googleIdTokenCredential.idToken

                                // 4️⃣ Pasar token a Firebase
                                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                                auth.signInWithCredential(firebaseCredential)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val user = auth.currentUser
                                            val nombre = user?.displayName ?: ""
                                            val email = user?.email ?: ""
                                            val photoUrl = user?.photoUrl

                                            // 🔍 INTENTAR LOGIN CON GOOGLE DIRECTAMENTE
                                            authViewModel.loginWithGoogle(googleIdToken)

                                            // ⚠️ Guardar datos temporales por si necesita registrarse
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

                                            // El LaunchedEffect manejará la navegación
                                        } else {
                                            isGoogleLogin = false // Resetear si falla
                                            Toast.makeText(
                                                context,
                                                "Error al iniciar sesión con Google",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            Log.e("FirebaseAuth", "signInWithCredential:failure", task.exception)
                                        }
                                    }
                            }
                        } catch (e: GoogleIdTokenParsingException) {
                            isGoogleLogin = false // Resetear en caso de error
                            Toast.makeText(context, "Error al procesar token", Toast.LENGTH_SHORT).show()
                            Log.e("SignIn", "Token error: ${e.message}")
                        } catch (e: GetCredentialException) {
                            isGoogleLogin = false // Resetear en caso de cancelación
                            Toast.makeText(context, "Cancelado o sin credenciales", Toast.LENGTH_SHORT).show()
                            Log.e("SignIn", "Credential error: ${e.message}")
                        } catch (e: Exception) {
                            isGoogleLogin = false // Resetear en caso de error
                            Toast.makeText(context, "Error general: ${e.message}", Toast.LENGTH_SHORT).show()
                            Log.e("SignIn", "Error general: ${e.message}")
                        }
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .padding(4.dp)
                    .background(Color.White, CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_google_logo),
                    contentDescription = "Login con Google",
                    modifier = Modifier.size(24.dp)
                )
            }



        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "¿No tienes cuenta?",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF718096)
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            TextButton(
                onClick = {
                    authViewModel.resetLoginState()
                    credentialsError = false
                    navController.navigate("register")
                },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "Regístrate",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(16.dp)
        )
    }
}

private fun String.containsAny(vararg terms: String, ignoreCase: Boolean = true): Boolean {
    return terms.any { this.contains(it, ignoreCase) }
}
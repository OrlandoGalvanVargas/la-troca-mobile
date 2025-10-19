package com.example.latroca.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.latroca.R
import com.example.latroca.ui.viewmodels.AuthViewModel
import com.example.latroca.domain.models.AuthResult
import kotlinx.coroutines.launch

// dominios de correo permitiodos
private fun isValidEmailDomain(domain: String): Boolean {
    val allowedDomains = listOf(
        "gmail.com", "google.com", "hotmail.com", "outlook.com", "yahoo.com",
        "uttt.edu.mx"
    )
    return allowedDomains.any { domain.equals(it, ignoreCase = true) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel
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
                email.contains("@") && !isValidEmailDomain(email.split("@")[1]) -> "Dominio de correo no válido"
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
                authViewModel.resetLoginState()

                navController.navigate("home") {
                    popUpTo(0) { inclusive = true }
                }
            }
            is AuthResult.Error -> {
                val errorMessage = (loginState as AuthResult.Error).message

                credentialsError = errorMessage.containsAny(
                    "contraseña", "password", "usuario", "user", "email", "credenciales", "not found"
                )

                if (errorMessage.contains("network", ignoreCase = true)) {
                    snackbarHostState.showSnackbar(
                        message = "Problema de conexión. Verifica tu internet",
                        duration = SnackbarDuration.Short
                    )
                }
            }
            else -> {
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Image(
            painter = painterResource(id = R.drawable.cloud_icon),
            contentDescription = "Logo La Troca",
            modifier = Modifier.size(90.dp)
        )

        Text(
            text = "La Troca",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Color(0xFF90A4AE)
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

            Spacer(modifier = Modifier.width(12.dp))
            IconButton(
                onClick = {

                },
                modifier = Modifier
                    .size(48.dp)
                    .padding(4.dp)
                    .background(Color.White, CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_facebook_logo),
                    contentDescription = "Login con Facebook",
                    modifier = Modifier.size(34.dp)
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
package com.example.latroca.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.latroca.R
import com.example.latroca.ui.viewmodels.RegistrationViewModel
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

private fun validateNombreRealTime(nombre: String): String {
    if (nombre.isBlank()) return ""

    return when {
        nombre.length < 2 -> "Mínimo 2 caracteres"
        nombre.length > 50 -> "Máximo 50 caracteres"
        nombre.any { it.isDigit() } -> "No puede contener números"
        nombre.contains(Regex(".*\\d.*")) -> "No puede contener números"
        !nombre.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+\$")) -> "Solo letras y espacios"
        nombre.trim().split("\\s+".toRegex()).size < 3 -> "Ingresa nombre y apellidos"
        else -> ""
    }
}

private fun validateEmailRealTime(email: String): String {
    if (email.isBlank()) return ""

    return when {
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

private fun validatePasswordRealTime(password: String): String {
    if (password.isBlank()) return ""

    return when {
        password.length < 8 -> "Mínimo 8 caracteres"
        password.length > 30 -> "Máximo 30 caracteres"
        !password.any { it.isUpperCase() } -> "Al menos una mayúscula"
        !password.any { it.isLowerCase() } -> "Al menos una minúscula"
        !password.any { it.isDigit() } -> "Al menos un número"
        else -> ""
    }
}

private fun validateConfirmPasswordRealTime(confirmPassword: String, password: String): String {
    if (confirmPassword.isBlank()) return ""

    return when {
        confirmPassword != password -> "Las contraseñas no coinciden"
        else -> ""
    }
}

private fun isValidEmailDomain(domain: String): Boolean {
    val allowedDomains = listOf(
        "gmail.com", "google.com", "hotmail.com", "outlook.com", "yahoo.com", "uttt.edu.mx"
    )
    return allowedDomains.any { domain.equals(it, ignoreCase = true) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    registrationViewModel: RegistrationViewModel
) {
    val context = LocalContext.current
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var acceptTerms by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var nombreError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var confirmPasswordError by remember { mutableStateOf("") }

    var nombreTouched by remember { mutableStateOf(false) }
    var emailTouched by remember { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }
    var confirmPasswordTouched by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val isFormValid by remember(
        nombre, email, password, confirmPassword,
        nombreError, emailError, passwordError, confirmPasswordError, acceptTerms
    ) {
        derivedStateOf {
            nombre.isNotBlank() &&
                    email.isNotBlank() &&
                    password.isNotBlank() &&
                    confirmPassword.isNotBlank() &&
                    nombreError.isBlank() &&
                    emailError.isBlank() &&
                    passwordError.isBlank() &&
                    confirmPasswordError.isBlank() &&
                    acceptTerms
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
            painter = painterResource(id = R.drawable.la_troca_logo),
            contentDescription = "Logo La Troca",
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Crear cuenta",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color(0xFF2D3748)
        )

        Text(
            text = "Completa tus datos para registrarte",
            fontSize = 14.sp,
            color = Color(0xFF718096)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Nombre completo",
                color = Color(0xFFE53E3E),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            OutlinedTextField(
                value = nombre,
                onValueChange = { newNombre ->
                    if (newNombre.length <= 50) {
                        nombre = newNombre
                        nombreTouched = true
                        if (nombreTouched) {
                            nombreError = validateNombreRealTime(newNombre)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                isError = nombreError.isNotBlank(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (nombreError.isNotBlank()) Color.Red else Color(0xFFE53E3E),
                    unfocusedBorderColor = if (nombreError.isNotBlank()) Color.Red else Color(0xFFE2E8F0),
                    focusedTextColor = Color(0xFF2D3748),
                    unfocusedTextColor = Color(0xFF2D3748),
                    cursorColor = Color(0xFFE53E3E),
                    errorBorderColor = Color.Red,
                    errorTextColor = Color.Red
                ),
                placeholder = {
                    Text("Ingresa tu nombre completo", color = Color(0xFFB0BEC5))
                }
            )
            if (nombreError.isNotBlank() && nombreTouched) {
                Text(
                    text = nombreError,
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
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                isError = emailError.isNotBlank(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (emailError.isNotBlank()) Color.Red else Color(0xFFE53E3E),
                    unfocusedBorderColor = if (emailError.isNotBlank()) Color.Red else Color(0xFFE2E8F0),
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
            if (emailError.isNotBlank() && emailTouched) {
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
                        passwordTouched = true
                        if (passwordTouched) {
                            passwordError = validatePasswordRealTime(newPassword)
                        }
                        if (confirmPasswordTouched && confirmPassword.isNotBlank()) {
                            confirmPasswordError = validateConfirmPasswordRealTime(confirmPassword, newPassword)
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
                isError = passwordError.isNotBlank(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (passwordError.isNotBlank()) Color.Red else Color(0xFFE53E3E),
                    unfocusedBorderColor = if (passwordError.isNotBlank()) Color.Red else Color(0xFFE2E8F0),
                    focusedTextColor = Color(0xFF2D3748),
                    unfocusedTextColor = Color(0xFF2D3748),
                    cursorColor = Color(0xFFE53E3E),
                    errorBorderColor = Color.Red,
                    errorTextColor = Color.Red
                ),
                placeholder = {
                    Text("Mínimo 8 caracteres", color = Color(0xFFB0BEC5))
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
            if (passwordError.isNotBlank() && passwordTouched) {
                Text(
                    text = passwordError,
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
                text = "Confirmar Contraseña",
                color = Color(0xFFE53E3E),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { newConfirmPassword ->
                    if (newConfirmPassword.length <= 30) {
                        confirmPassword = newConfirmPassword
                        confirmPasswordTouched = true
                        if (confirmPasswordTouched) {
                            confirmPasswordError = validateConfirmPasswordRealTime(newConfirmPassword, password)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = confirmPasswordError.isNotBlank(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (confirmPasswordError.isNotBlank()) Color.Red else Color(0xFFE53E3E),
                    unfocusedBorderColor = if (confirmPasswordError.isNotBlank()) Color.Red else Color(0xFFE2E8F0),
                    focusedTextColor = Color(0xFF2D3748),
                    unfocusedTextColor = Color(0xFF2D3748),
                    cursorColor = Color(0xFFE53E3E),
                    errorBorderColor = Color.Red,
                    errorTextColor = Color.Red
                ),
                placeholder = {
                    Text("Confirma tu contraseña", color = Color(0xFFB0BEC5))
                },
                trailingIcon = {
                    val image = if (confirmPasswordVisible)
                        Icons.Filled.Visibility
                    else
                        Icons.Filled.VisibilityOff

                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = image,
                            contentDescription = null,
                            tint = Color(0xFF718096)
                        )
                    }
                }
            )
            if (confirmPasswordError.isNotBlank() && confirmPasswordTouched) {
                Text(
                    text = confirmPasswordError,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, start = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = acceptTerms,
                onCheckedChange = { acceptTerms = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFFEF4444)
                )
            )
            Text(
                text = "Acepto los ",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF4A5568)
                )
            )
            Text(
                text = "Términos de Servicio",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://orlandogalvanvargas.github.io/la-troca-mobile-terminos-de-servicio/"))
                    context.startActivity(intent)                }
            )
            Text(
                text = " y ",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF4A5568)
                )
            )
            Text(
                text = "Política de Privacidad",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://la-troca-app.web.app/privacy-policy.html"))
                    context.startActivity(intent)
                }
            )
        }
        Button(
            onClick = {
                nombreTouched = true
                emailTouched = true
                passwordTouched = true
                confirmPasswordTouched = true

                nombreError = validateNombreRealTime(nombre)
                emailError = validateEmailRealTime(email)
                passwordError = validatePasswordRealTime(password)
                confirmPasswordError = validateConfirmPasswordRealTime(confirmPassword, password)

                if (isFormValid) {
                    registrationViewModel.updateStep1Data(nombre, email, password)

                    navController.navigate("completeProfile") {
                        popUpTo("register") { inclusive = false }
                    }
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            "Por favor corrige los errores en el formulario",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF6B6B)
            ),
            enabled = isFormValid
        ) {
            Text(
                text = "Registrarse",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(15.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "¿Ya tienes cuenta?",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF718096)
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            TextButton(
                onClick = { navController.navigate("login") },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "Inicia sesión",
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
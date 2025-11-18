package com.troca.latroca.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.troca.latroca.ui.utils.clickableOnce
import com.troca.latroca.R
import com.troca.latroca.ui.components.LoadingModal
import com.troca.latroca.ui.viewmodels.RegistrationViewModel
import com.troca.latroca.utils.validatePasswordInput
import kotlinx.coroutines.launch

private fun formatNombreText(currentText: String, newText: String): String {
    if (newText.isEmpty()) return ""

    if (newText.length < currentText.length) {
        return newText
    }

    if (newText.length > 30) {
        return currentText
    }

    val hasContent = currentText.any { it != ' ' }

    if (!hasContent) {
        if (newText.first().isWhitespace()) {
            return ""
        }
    }

    val result = StringBuilder()
    var spaceCount = 0

    for (char in newText) {
        when {
            result.isEmpty() && char.isWhitespace() -> {
                continue
            }
            char == ' ' -> {
                spaceCount++
                if (spaceCount <= 1) {
                    result.append(char)
                }
            }
            else -> {
                spaceCount = 0
                result.append(char)
            }
        }
    }

    return result.toString()
}

private fun formatEmailText(currentText: String, newText: String): String {
    if (newText.isEmpty()) return ""

    if (newText.length < currentText.length) {
        return newText
    }

    if (newText.length > 35) {
        return currentText
    }

    if (newText.any { it.isWhitespace() }) {
        return currentText
    }

    return newText
}

private fun validateNombreRealTime(nombre: String): String {
    if (nombre.isBlank()) return ""

    return when {
        nombre.length < 3 -> "Mínimo 3 caracteres"
        nombre.length > 30 -> "Máximo 30 caracteres"
        nombre.any { it.isDigit() } -> "No puede contener números"
        nombre.contains(Regex(".*\\d.*")) -> "No puede contener números"
        !nombre.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+\$")) -> "Solo letras y espacios"
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
        email.length > 35 -> "Máximo 35 caracteres en total"
        email.contains("@") && !isValidEmailDomain(email.split("@")[1]) -> "Dominio de correo no válido"
        else -> ""
    }
}

private fun validatePasswordRealTime(password: String): String {
    if (password.isBlank()) return ""

    return when {
        password.length < 8 -> "Mínimo 8 caracteres"
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

private object RegisterScreenDimens {
    val horizontalPadding: Dp
        @Composable get() = with(LocalDensity.current) {
            if (LocalDensity.current.density > 2.5f) 28.dp else 24.dp
        }

    val verticalSpacingSmall: Dp
        @Composable get() = 8.dp

    val verticalSpacingMedium: Dp
        @Composable get() = 16.dp

    val verticalSpacingLarge: Dp
        @Composable get() = 24.dp

    val verticalSpacingXLarge: Dp
        @Composable get() = 32.dp

    val logoSize: Dp
        @Composable get() = with(LocalDensity.current) {
            if (LocalDensity.current.density > 2.5f) 100.dp else 90.dp
        }

    val buttonHeight: Dp
        @Composable get() = 52.dp

    val textFieldHeight: Dp
        @Composable get() = 56.dp
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

    var isLoading by remember { mutableStateOf(false) }

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

    fun handleNombreChange(newNombre: String) {
        val formattedNombre = formatNombreText(nombre, newNombre)
        if (formattedNombre != nombre) {
            nombre = formattedNombre
            nombreTouched = true
            if (nombreTouched) {
                nombreError = validateNombreRealTime(formattedNombre)
            }
        }
    }

    fun handleEmailChange(newEmail: String) {
        val formattedEmail = formatEmailText(email, newEmail)
        if (formattedEmail != email) {
            email = formattedEmail
            emailTouched = true
            if (emailTouched) {
                emailError = validateEmailRealTime(formattedEmail)
            }
        }
    }
    fun handlePasswordChange(newPassword: String) {
        val filteredPassword = validatePasswordInput(password, newPassword)
        if (filteredPassword != password) {
            password = filteredPassword
            passwordTouched = true
            if (passwordTouched) {
                passwordError = validatePasswordRealTime(filteredPassword)
            }
            if (confirmPasswordTouched && confirmPassword.isNotBlank()) {
                confirmPasswordError = validateConfirmPasswordRealTime(confirmPassword, filteredPassword)
            }
        }
    }

    fun handleConfirmPasswordChange(newConfirmPassword: String) {
        val filteredConfirmPassword = validatePasswordInput(confirmPassword, newConfirmPassword)
        if (filteredConfirmPassword != confirmPassword) {
            confirmPassword = filteredConfirmPassword
            confirmPasswordTouched = true
            if (confirmPasswordTouched) {
                confirmPasswordError = validateConfirmPasswordRealTime(filteredConfirmPassword, password)
            }
        }
    }

    LoadingModal(
        isVisible = isLoading,
        message = "Creando tu cuenta...",
        timeoutSeconds = 5
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RegisterScreenDimens.horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(RegisterScreenDimens.verticalSpacingXLarge))

        Image(
            painter = painterResource(id = R.drawable.la_troca_logo),
            contentDescription = "Logo La Troca",
            modifier = Modifier.size(RegisterScreenDimens.logoSize)
        )

        Spacer(modifier = Modifier.height(RegisterScreenDimens.verticalSpacingLarge))

        Text(
            text = "Crear cuenta",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color(0xFF2D3748)
        )

        Spacer(modifier = Modifier.height(RegisterScreenDimens.verticalSpacingSmall))

        Text(
            text = "Completa tus datos para registrarte",
            fontSize = 14.sp,
            color = Color(0xFF718096)
        )

        Spacer(modifier = Modifier.height(RegisterScreenDimens.verticalSpacingXLarge))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(RegisterScreenDimens.verticalSpacingMedium)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nombre completo",
                        color = Color(0xFFE53E3E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                OutlinedTextField(
                    value = nombre,
                    onValueChange = ::handleNombreChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(RegisterScreenDimens.textFieldHeight),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    enabled = !isLoading,
                    isError = nombreError.isNotBlank(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (nombreError.isNotBlank()) Color.Red else Color(0xFFE53E3E),
                        unfocusedBorderColor = if (nombreError.isNotBlank()) Color.Red else Color(0xFFE2E8F0),
                        disabledBorderColor = Color(0xFFE2E8F0),
                        focusedTextColor = Color(0xFF2D3748),
                        unfocusedTextColor = Color(0xFF2D3748),
                        disabledTextColor = Color(0xFF718096),
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
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Correo",
                        color = Color(0xFFE53E3E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = ::handleEmailChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(RegisterScreenDimens.textFieldHeight),
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    enabled = !isLoading,
                    isError = emailError.isNotBlank(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (emailError.isNotBlank()) Color.Red else Color(0xFFE53E3E),
                        unfocusedBorderColor = if (emailError.isNotBlank()) Color.Red else Color(0xFFE2E8F0),
                        disabledBorderColor = Color(0xFFE2E8F0),
                        focusedTextColor = Color(0xFF2D3748),
                        unfocusedTextColor = Color(0xFF2D3748),
                        disabledTextColor = Color(0xFF718096),
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
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            Column {
                Text(
                    text = "Contraseña",
                    color = Color(0xFFE53E3E),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = ::handlePasswordChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(RegisterScreenDimens.textFieldHeight),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = passwordError.isNotBlank(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (passwordError.isNotBlank()) Color.Red else Color(0xFFE53E3E),
                        unfocusedBorderColor = if (passwordError.isNotBlank()) Color.Red else Color(0xFFE2E8F0),
                        disabledBorderColor = Color(0xFFE2E8F0),
                        focusedTextColor = Color(0xFF2D3748),
                        unfocusedTextColor = Color(0xFF2D3748),
                        disabledTextColor = Color(0xFF718096),
                        cursorColor = Color(0xFFE53E3E),
                        errorBorderColor = Color.Red,
                        errorTextColor = Color.Red
                    ),
                    placeholder = {
                        Text("Mínimo 8 caracteres", color = Color(0xFFB0BEC5))
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                                tint = if (isLoading) Color(0xFFCBD5E0) else Color(0xFF718096)
                            )
                        }
                    }
                )
                if (passwordError.isNotBlank() && passwordTouched) {
                    Text(
                        text = passwordError,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            Column {
                Text(
                    text = "Confirmar Contraseña",
                    color = Color(0xFFE53E3E),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = ::handleConfirmPasswordChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(RegisterScreenDimens.textFieldHeight),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = confirmPasswordError.isNotBlank(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (confirmPasswordError.isNotBlank()) Color.Red else Color(0xFFE53E3E),
                        unfocusedBorderColor = if (confirmPasswordError.isNotBlank()) Color.Red else Color(0xFFE2E8F0),
                        disabledBorderColor = Color(0xFFE2E8F0),
                        focusedTextColor = Color(0xFF2D3748),
                        unfocusedTextColor = Color(0xFF2D3748),
                        disabledTextColor = Color(0xFF718096),
                        cursorColor = Color(0xFFE53E3E),
                        errorBorderColor = Color.Red,
                        errorTextColor = Color.Red
                    ),
                    placeholder = {
                        Text("Confirma tu contraseña", color = Color(0xFFB0BEC5))
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (confirmPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                                tint = if (isLoading) Color(0xFFCBD5E0) else Color(0xFF718096)
                            )
                        }
                    }
                )
                if (confirmPasswordError.isNotBlank() && confirmPasswordTouched) {
                    Text(
                        text = confirmPasswordError,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(RegisterScreenDimens.verticalSpacingLarge))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = RegisterScreenDimens.verticalSpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = acceptTerms,
                onCheckedChange = { if (!isLoading) acceptTerms = it },
                enabled = !isLoading,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFFEF4444),
                    disabledCheckedColor = Color(0xFFCBD5E0)
                )
            )

            TermsAndPrivacyText(isLoading = isLoading)
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
                    isLoading = true
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(800)
                        registrationViewModel.updateStep1Data(nombre, email, password)
                        isLoading = false
                        navController.navigate("completeProfile") {
                            popUpTo("register") { inclusive = false }
                        }
                    }
                } else {
                    coroutineScope.launch {
                        Toast.makeText(
                            context,
                            "Por favor corrige los errores en el formulario",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(RegisterScreenDimens.buttonHeight),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF6B6B),
                disabledContainerColor = Color(0xFFE2E8F0)
            ),
            enabled = isFormValid && !isLoading
        ) {
            Text(
                text = "Registrarse",
                color = if (isFormValid && !isLoading) Color.White else Color(0xFF718096),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(RegisterScreenDimens.verticalSpacingMedium))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "¿Ya tienes cuenta?",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isLoading) Color(0xFFCBD5E0) else Color(0xFF718096)
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            TextButton(
                onClick = { navController.popBackStack() },
                enabled = !isLoading,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "Inicia sesión",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isLoading) Color(0xFFCBD5E0) else Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(RegisterScreenDimens.verticalSpacingXLarge))
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

@Composable
private fun TermsAndPrivacyText(isLoading: Boolean) {
    val context = LocalContext.current

    Text(
        text = "Acepto los ",
        style = MaterialTheme.typography.bodySmall.copy(
            color = if (isLoading) Color(0xFFCBD5E0) else Color(0xFF4A5568)
        )
    )
    Text(
        text = "Términos",
        style = MaterialTheme.typography.bodySmall.copy(
            color = if (isLoading) Color(0xFFCBD5E0) else Color(0xFFEF4444),
            fontWeight = FontWeight.Bold
        ),
        modifier = Modifier.clickableOnce(enabled = !isLoading) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://orlandogalvanvargas.github.io/la-troca-mobile-terminos-de-servicio/")
            )
            context.startActivity(intent)
        }
    )
    Text(
        text = " y ",
        style = MaterialTheme.typography.bodySmall.copy(
            color = if (isLoading) Color(0xFFCBD5E0) else Color(0xFF4A5568)
        )
    )
    Text(
        text = "Política de Privacidad",
        style = MaterialTheme.typography.bodySmall.copy(
            color = if (isLoading) Color(0xFFCBD5E0) else Color(0xFFEF4444),
            fontWeight = FontWeight.Bold
        ),
        modifier = Modifier.clickableOnce(enabled = !isLoading) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://la-troca-app.web.app/privacy-policy.html")
            )
            context.startActivity(intent)
        }
    )
}
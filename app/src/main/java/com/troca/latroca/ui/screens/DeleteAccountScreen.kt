package com.troca.latroca.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.troca.latroca.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var selectedReason by remember { mutableStateOf("") }
    var deleteImmediately by remember { mutableStateOf(false) }

    // 🔔 Estados del modal
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    val reasons = listOf(
        "No uso la app",
        "Privacidad",
        "Otro"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Eliminar cuenta",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = Color(0xFF2D3748)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF7FAFC))
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // ⚠️ Advertencia principal
            Text(
                text = buildAnnotatedString {
                    append("Si confirmas esta acción, tu cuenta será ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("desactivada inmediatamente")
                    }
                    append(" y programada para su eliminación definitiva en un plazo de ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("30 días")
                    }
                    append(". Durante este periodo no tendrás acceso a tu cuenta.")
                },
                fontSize = 15.sp,
                color = Color(0xFF2D3748),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Si decides regresar antes de que finalicen los 30 días, podrás recuperarla iniciando sesión.",
                fontSize = 15.sp,
                color = Color(0xFF2D3748),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 📋 Motivo (opcional)
            Text(
                text = "Motivo (opcional):",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2D3748)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Botones de motivo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                reasons.forEach { reason ->
                    val isSelected = selectedReason == reason
                    Button(
                        onClick = { selectedReason = reason },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFF2196F3) else Color.White,
                            contentColor = if (isSelected) Color.White else Color(0xFF2D3748)
                        ),
                        border = if (!isSelected) {
                            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        } else null,
                        shape = RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = if (isSelected) 2.dp else 0.dp
                        )
                    ) {
                        Text(
                            text = reason,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ⚡ Checkbox de eliminación inmediata
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Checkbox(
                    checked = deleteImmediately,
                    onCheckedChange = { deleteImmediately = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFE53E3E),
                        uncheckedColor = Color(0xFF718096)
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = buildAnnotatedString {
                        append("He leído, y entiendo que mi cuenta será desactivada de inmediato ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("y eliminada definitivamente después de 30 días")
                        }
                    },
                    fontSize = 14.sp,
                    color = Color(0xFF2D3748),
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            // 🚫 Botón Eliminar
            Button(
                onClick = {
                    if (deleteImmediately) {
                        showDeleteDialog = true // 👈 Mostrar modal de confirmación
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                "Debes confirmar que entiendes las consecuencias",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (deleteImmediately) Color(0xFFE53E3E) else Color(0xFFE2E8F0),
                    contentColor = if (deleteImmediately) Color.White else Color(0xFF718096)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = deleteImmediately
            ) {
                Text(
                    text = "Eliminar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // 🔔 MODAL DE ELIMINACIÓN (3 ESTADOS)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                // 👈 NO hacer nada - modal no se puede cerrar haciendo clic fuera
            },
            confirmButton = {
                when {
                    // ✅ Estado 3: ÉXITO
                    isSuccess -> {
                        Button(
                            onClick = {
                                showDeleteDialog = false
                                authViewModel.logout() // 👈 Limpiar token
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text("Ok")
                        }
                    }
                    // ⏳ Estado 2: PROCESANDO
                    isProcessing -> {
                        // No mostrar botones mientras procesa
                    }
                    // ⚠️ Estado 1: CONFIRMACIÓN
                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    showDeleteDialog = false
                                    isProcessing = false
                                    isSuccess = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "Cancelar",
                                    color = Color(0xFF718096)
                                )
                            }

                            Button(
                                onClick = {
                                    isProcessing = true
                                    coroutineScope.launch {
                                        // 🔄 Llamar a la API
                                        authViewModel.deactivateAccount(selectedReason)

                                        // Esperar 3 segundos
                                        delay(3000)

                                        // Verificar el estado
                                        val loginState = authViewModel.loginState.value

                                        when (loginState) {
                                            is com.troca.latroca.domain.models.AuthResult.Success -> {
                                                isProcessing = false
                                                isSuccess = true
                                                authViewModel.resetLoginState()
                                            }
                                            is com.troca.latroca.domain.models.AuthResult.Error -> {
                                                isProcessing = false
                                                showDeleteDialog = false

                                                snackbarHostState.showSnackbar(
                                                    message = loginState.message,
                                                    duration = SnackbarDuration.Long
                                                )

                                                authViewModel.resetLoginState()
                                            }
                                            else -> {
                                                isProcessing = false
                                                isSuccess = true
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE53E3E)
                                )
                            ) {
                                Text("Aceptar")
                            }
                        }
                    }
                }
            },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when {
                        isSuccess -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Éxito",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "¡Cuenta Desactivada!",
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                        isProcessing -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(60.dp),
                                color = Color(0xFFE53E3E),
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Procesando...",
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                        else -> {
                            Text(
                                text = "¿Estás seguro?",
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                    }
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when {
                            isSuccess -> "Tu cuenta ha sido desactivada con éxito. Será eliminada de forma permanente después de 30 días."
                            isProcessing -> "Por favor espera mientras completamos el proceso"
                            else -> "Al continuar, su cuenta se desactivará inmediatamente y se eliminará de forma permanente tras 30 días.\n\n¿Deseas continuar?"
                        },
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        color = Color(0xFF2D3748),
                        lineHeight = 22.sp
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
package com.troca.latroca.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
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
    authViewModel: AuthViewModel,
    onAccountDeleted: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedReason by remember { mutableStateOf("") }
    var deleteImmediately by remember { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var isFinalizingDeletion by remember { mutableStateOf(false) }

    val reasons = listOf(
        "No uso la app",
        "Privacidad",
        "Otro"
    )

    // 🔥 Box principal que envuelve TODO (incluyendo Scaffold)
    Box(modifier = Modifier.fillMaxSize()) {
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
                        IconButton(
                            onClick = { navController.navigateUp() },
                            enabled = !isProcessing && !isFinalizingDeletion // 🆕 Deshabilitar si está procesando
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Volver",
                                tint = if (!isProcessing && !isFinalizingDeletion)
                                    Color(0xFF2D3748)
                                else
                                    Color(0xFFE2E8F0) // Gris claro cuando está deshabilitado
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { },
                            enabled = false // Siempre deshabilitado (es decorativo)
                        ) {
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
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF7FAFC))
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("Esta acción es ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFE53E3E))) {
                            append("irreversible")
                        }
                        append(". Si confirmas, tu cuenta será ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("eliminada permanentemente")
                        }
                        append(" junto con toda tu información.")
                    },
                    fontSize = 15.sp,
                    color = Color(0xFF2D3748),
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Se eliminarán:",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2D3748)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    listOf(
                        "• Tu perfil y toda tu información personal",
                        "• Todas las publicaciones que hayas creado",
                        "• Todos tus chats y conversaciones",
                        "• Cualquier dato asociado a tu cuenta"
                    ).forEach { item ->
                        Text(
                            text = item,
                            fontSize = 14.sp,
                            color = Color(0xFF2D3748),
                            lineHeight = 20.sp
                        )
                        if (item != "• Cualquier dato asociado a tu cuenta") {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Motivo (opcional):",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2D3748)
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                            append("Entiendo que mi cuenta y toda mi información serán ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("eliminadas permanentemente")
                            }
                            append(" y que esta acción ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFE53E3E))) {
                                append("no se puede deshacer")
                            }
                        },
                        fontSize = 14.sp,
                        color = Color(0xFF2D3748),
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (deleteImmediately) {
                            showDeleteDialog = true
                        } else {
                            Toast.makeText(
                                context,
                                "Debes confirmar que entiendes las consecuencias",
                                Toast.LENGTH_SHORT
                            ).show()
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
                    enabled = deleteImmediately && !isProcessing && !isFinalizingDeletion
                ) {
                    Text(
                        text = "Eliminar cuenta permanentemente",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 🔥 OVERLAY DE LOADING - Ahora cubre TODA la pantalla incluyendo TopAppBar
        if (isProcessing || isFinalizingDeletion) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)), // 🆕 Más oscuro para mejor contraste
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(60.dp),
                        color = Color.White, // 🆕 Blanco para mejor visibilidad
                        strokeWidth = 4.dp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isFinalizingDeletion) "Cerrando sesión..." else "Eliminando cuenta...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // Diálogo de confirmación/estado
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isProcessing && !isFinalizingDeletion) {
                    showDeleteDialog = false
                    isSuccess = false
                }
            },
            confirmButton = {
                when {
                    isSuccess && !isFinalizingDeletion -> {
                        Button(
                            onClick = {
                                showDeleteDialog = false
                                isFinalizingDeletion = true

                                coroutineScope.launch {
                                    onAccountDeleted()
                                    delay(500)
                                    authViewModel.logout()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Ok",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    isProcessing -> {
                        // No mostrar botones mientras procesa
                    }
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
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "Cancelar",
                                    color = Color(0xFF718096),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Button(
                                onClick = {
                                    isProcessing = true
                                    coroutineScope.launch {
                                        authViewModel.deleteMyAccount()
                                        delay(2000)
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

                                                Toast.makeText(
                                                    context,
                                                    loginState.message,
                                                    Toast.LENGTH_LONG
                                                ).show()

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
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "Eliminar",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
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
                                text = "Cuenta Eliminada",
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFF2D3748)
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
                                text = "Eliminando...",
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFF2D3748)
                            )
                        }
                        else -> {
                            Text(
                                text = "¿Estás completamente seguro?",
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFF2D3748)
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
                            isSuccess -> "Tu cuenta ha sido eliminada permanentemente junto con todas tus publicaciones, chats y datos.\n\nSerás redirigido al inicio de sesión."
                            isProcessing -> "Por favor espera mientras eliminamos tu cuenta y toda tu información..."
                            else -> "Esta acción eliminará permanentemente tu cuenta y TODOS tus datos incluyendo publicaciones y chats.\n\nEsta acción NO se puede deshacer.\n\n¿Deseas continuar?"
                        },
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        color = Color(0xFF4A5568),
                        lineHeight = 22.sp
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
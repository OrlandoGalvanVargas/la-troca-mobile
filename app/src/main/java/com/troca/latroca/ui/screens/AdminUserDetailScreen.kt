package com.troca.latroca.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.troca.latroca.domain.models.AuthResult
import com.troca.latroca.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserDetailScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    userId: String
) {
    val context = LocalContext.current
    val selectedUser by authViewModel.selectedUser.collectAsState()
    val isLoading by authViewModel.loadingUser.collectAsState()
    val deleteState by authViewModel.deleteUserState.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var isBackButtonEnabled by remember { mutableStateOf(true) }

    fun handleBackNavigation() {
        if (isBackButtonEnabled) {
            isBackButtonEnabled = false
            navController.popBackStack()

            coroutineScope.launch {
                delay(500)
                isBackButtonEnabled = true
            }
        }
    }

    LaunchedEffect(userId) {
        if (!authViewModel.isAdmin()) {
            Toast.makeText(context, "No tienes permisos", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        } else {
            authViewModel.loadUserById(userId)
        }
    }

    LaunchedEffect(deleteState) {
        when (deleteState) {
            is AuthResult.Success -> {
                Toast.makeText(
                    context,
                    "Usuario eliminado correctamente",
                    Toast.LENGTH_SHORT
                ).show()
                authViewModel.resetDeleteUserState()
                navController.popBackStack()
            }
            is AuthResult.Error -> {
                Toast.makeText(
                    context,
                    (deleteState as AuthResult.Error).message,
                    Toast.LENGTH_LONG
                ).show()
                authViewModel.resetDeleteUserState()
            }
            else -> {}
        }
    }

    if (showDeleteDialog && selectedUser != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "¿Eliminar Usuario?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "¿Estás seguro que deseas eliminar a:",
                        fontSize = 14.sp,
                        color = Color(0xFF718096),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        selectedUser!!.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D3748)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        selectedUser!!.email,
                        fontSize = 13.sp,
                        color = Color(0xFF718096)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Esta acción no se puede deshacer",
                        fontSize = 12.sp,
                        color = Color(0xFFE53935),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        authViewModel.deleteUser(userId) {
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF718096)
                    )
                ) {
                    Text("Cancelar", fontWeight = FontWeight.Medium)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Detalle del Usuario",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { handleBackNavigation() },
                        enabled = isBackButtonEnabled
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = if (isBackButtonEnabled) Color(0xFFE53935) else Color(0xFFCBD5E0)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF7FAFC))
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFE53935))
                    }
                }

                selectedUser != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ProfileCard(user = selectedUser!!)

                        InfoCard(user = selectedUser!!)

                        Button(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE53935)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            enabled = deleteState !is AuthResult.Loading
                        ) {
                            if (deleteState is AuthResult.Loading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Eliminar Usuario",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No se pudo cargar el usuario",
                            color = Color(0xFF718096)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(user: com.troca.latroca.data.models.AdminUserResponse) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                if (!user.profilePicUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = user.profilePicUrl,
                        contentDescription = "Foto de ${user.name}",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(4.dp, Color(0xFFE2E8F0), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(Color(0xFFF7FAFC), CircleShape)
                            .border(4.dp, Color(0xFFE2E8F0), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Sin foto",
                            tint = Color(0xFF718096),
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }

                if (user.status == "active") {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.BottomEnd)
                            .background(Color(0xFF4CAF50), CircleShape)
                            .border(4.dp, Color.White, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = user.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D3748),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = user.email,
                fontSize = 14.sp,
                color = Color(0xFF718096)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = if (user.role == "ADMIN") Color(0xFFFFD700).copy(alpha = 0.3f)
                else Color(0xFF2196F3).copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (user.role == "ADMIN") "👑 ADMINISTRADOR" else "👤 USUARIO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (user.role == "ADMIN") Color(0xFFFFB300)
                    else Color(0xFF2196F3),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            if (!user.bio.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = user.bio,
                    fontSize = 14.sp,
                    color = Color(0xFF718096),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun InfoCard(user: com.troca.latroca.data.models.AdminUserResponse) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Información",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D3748)
            )

            InfoItem(
                icon = Icons.Default.Shield,
                label = "Rol",
                value = user.role
            )

            InfoItem(
                icon = Icons.Default.CheckCircle,
                label = "Estado",
                value = when (user.status) {
                    "active" -> "Activo"
                    "inactive" -> "Inactivo"
                    else -> user.status
                }
            )

            if (!user.createdAt.isNullOrBlank()) {
                InfoItem(
                    icon = Icons.Default.CalendarToday,
                    label = "Fecha de Registro",
                    value = formatDate(user.createdAt)
                )
            }

            if (!user.updatedAt.isNullOrBlank()) {
                InfoItem(
                    icon = Icons.Default.Update,
                    label = "Última Actualización",
                    value = formatDate(user.updatedAt)
                )
            }
        }
    }
}

@Composable
private fun InfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFE53935),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF718096),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                color = Color(0xFF2D3748),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(dateString)

        val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (_: Exception) {
        dateString
    }
}
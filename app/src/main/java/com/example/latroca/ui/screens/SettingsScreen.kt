package com.example.latroca.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.latroca.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    authViewModel: AuthViewModel,  // 👈 AGREGAR ESTO
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    // 🆕 Observar perfil del usuario
    val userProfile by authViewModel.userProfile.collectAsState()
    // 🔔 Estado de permisos de notificaciones
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // En versiones anteriores a Android 13, siempre están permitidas
            }
        )
    }

    // Launcher para pedir permisos
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        coroutineScope.launch {
            if (isGranted) {
                snackbarHostState.showSnackbar(
                    "Notificaciones activadas ✅",
                    duration = SnackbarDuration.Short
                )
            } else {
                snackbarHostState.showSnackbar(
                    "Notificaciones desactivadas",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
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
        ) {
            // 👤 Sección de perfil
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 🆕 Mostrar foto de perfil real o placeholder
                    if (!userProfile?.profilePicUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = userProfile?.profilePicUrl,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .background(Color(0xFFE53E3E), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Perfil",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = userProfile?.name ?: "Cargando...",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D3748)
                        )
                        Text(
                            text = userProfile?.email ?: "",
                            fontSize = 14.sp,
                            color = Color(0xFF718096)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 📋 Opciones de configuración
            SettingsSection(title = "Cuenta") {
                SettingsOption(
                    icon = Icons.Default.Person,
                    title = "Editar datos de cuenta",
                    onClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                "Funcionalidad próximamente disponible",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )

                SettingsOption(
                    icon = Icons.Default.LocationOn,
                    title = "Editar ubicación",
                    onClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                "Funcionalidad próximamente disponible",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🔔 Notificaciones
            SettingsSection(title = "Preferencias") {
                SettingsOptionWithSwitch(
                    icon = Icons.Default.Notifications,
                    title = "Notificaciones",
                    subtitle = if (hasNotificationPermission) "Activadas" else "Desactivadas",
                    isChecked = hasNotificationPermission,
                    onCheckedChange = { isEnabled ->
                        if (isEnabled) {
                            // Activar notificaciones
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            // Desactivar notificaciones (ir a configuración del sistema)
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    "Desactiva las notificaciones en la configuración del sistema",
                                    duration = SnackbarDuration.Long
                                )
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 📜 Legal
            SettingsSection(title = "Legal") {
                SettingsOption(
                    icon = Icons.Default.Info,
                    title = "Ver términos y políticas",
                    onClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                "Funcionalidad próximamente disponible",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                )
                // 🆕 AGREGAR AYUDA
                SettingsOption(
                    icon = Icons.Default.Help,  // 👈 Icono de ayuda
                    title = "Ayuda",
                    onClick = {
                        navController.navigate("help")
                    }
                )
            }



            Spacer(modifier = Modifier.height(16.dp))

            // ⚠️ Zona peligrosa
            SettingsSection(title = "Zona de peligro") {
                SettingsOption(
                    icon = Icons.Default.Delete,
                    title = "Eliminar cuenta",
                    subtitle = "Esta acción es irreversible",
                    iconTint = Color(0xFFE53E3E),
                    textColor = Color(0xFFE53E3E),
                    onClick = {
                        // 👇 Navegar a pantalla de confirmación
                        navController.navigate("deleteAccount")
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 🚪 Cerrar sesión
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53E3E)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Cerrar sesión",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cerrar sesión",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 🔔 Diálogo de confirmación de logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "¿Cerrar sesión?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("¿Estás seguro de que quieres cerrar sesión?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53E3E)
                    )
                ) {
                    Text("Cerrar sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar", color = Color(0xFF718096))
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF718096),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        content()
    }
}

@Composable
fun SettingsOption(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = Color(0xFF2D3748),
    textColor: Color = Color(0xFF2D3748),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF718096)
                )
            }
        }

        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "Ir",
            tint = Color(0xFF718096)
        )
    }
}

@Composable
fun SettingsOptionWithSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFF2D3748),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = Color(0xFF2D3748),
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF718096)
                )
            }
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4CAF50),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE2E8F0)
            )
        )
    }
}
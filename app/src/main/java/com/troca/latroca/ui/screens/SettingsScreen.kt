package com.troca.latroca.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.troca.latroca.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val userProfile by authViewModel.userProfile.collectAsState()

    var isBackButtonEnabled by remember { mutableStateOf(true) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var showPermissionRationale by remember { mutableStateOf(false) }
    var showManualSettingsDialog by remember { mutableStateOf(false) }
    var permissionRequestCount by remember { mutableIntStateOf(0) }

    fun shouldShowRequestPermissionRationale(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (context as? Activity)?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) ?: false
        } else {
            false
        }
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val currentlyGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (currentlyGranted != hasNotificationPermission) {
                hasNotificationPermission = currentlyGranted
            }
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted

        coroutineScope.launch {
            if (isGranted) {
                Toast.makeText(
                    context,
                    "Notificaciones activadas",
                    Toast.LENGTH_SHORT
                ).show()
                permissionRequestCount = 0
                showPermissionRationale = false
                showManualSettingsDialog = false
            } else {
                permissionRequestCount++

                val shouldShowRationale = shouldShowRequestPermissionRationale()

                if (!shouldShowRationale && permissionRequestCount >= 1) {
                    showManualSettingsDialog = true
                    Toast.makeText(
                        context,
                        "Permiso denegado permanentemente. Activa en configuración",
                        Toast.LENGTH_LONG
                    ).show()
                } else if (permissionRequestCount == 1) {
                    showPermissionRationale = true
                    Toast.makeText(
                        context,
                        "Las notificaciones te ayudan a no perderte mensajes importantes",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Permiso denegado. Puedes intentar nuevamente",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun handleBackNavigation() {
        if (isBackButtonEnabled) {
            isBackButtonEnabled = false
            navController.navigateUp()

            coroutineScope.launch {
                delay(500)
                isBackButtonEnabled = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { handleBackNavigation() },
                        enabled = isBackButtonEnabled
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = when {
                                !isBackButtonEnabled -> Color(0xFFCBD5E0)
                                else -> Color(0xFFE53935)
                            }
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!userProfile?.profilePicUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = userProfile?.profilePicUrl,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
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

            SettingsSection(title = "Cuenta") {
                SettingsOption(
                    icon = Icons.Default.Person,
                    title = "Editar datos de cuenta",
                    enabled = true,
                    onClick = {
                        navController.navigate("editProfile")
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "Preferencias") {
                SettingsOptionWithSwitch(
                    icon = Icons.Default.Notifications,
                    title = "Notificaciones",
                    subtitle = when {
                        hasNotificationPermission -> "Activadas"
                        showManualSettingsDialog -> "Activar en configuración"
                        else -> "Desactivadas"
                    },
                    isChecked = hasNotificationPermission,
                    enabled = true,
                    onCheckedChange = { isEnabled ->
                        if (isEnabled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                when {
                                    hasNotificationPermission -> {
                                    }
                                    showManualSettingsDialog -> {
                                        showManualSettingsDialog = true
                                    }
                                    showPermissionRationale -> {
                                        showPermissionRationale = true
                                    }
                                    else -> {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            }
                        } else {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            try {
                                context.startActivity(intent)
                                coroutineScope.launch {
                                    Toast.makeText(
                                        context,
                                        "Desactiva las notificaciones en la configuración del sistema",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } catch (e: Exception) {
                                coroutineScope.launch {
                                    Toast.makeText(
                                        context,
                                        "Abre Configuración > Aplicaciones > La Troca > Notificaciones",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "Legal") {
                SettingsOption(
                    icon = Icons.Default.Info,
                    title = "Ver términos y políticas",
                    enabled = true,
                    onClick = {
                        navController.navigate("termsAndPolicies")
                    }
                )
                SettingsOption(
                    icon = Icons.Default.Help,
                    title = "Ayuda",
                    enabled = true,
                    onClick = {
                        navController.navigate("help")
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection(title = "Zona de peligro") {
                SettingsOption(
                    icon = Icons.Default.Delete,
                    title = "Eliminar cuenta",
                    subtitle = "Esta acción es irreversible",
                    iconTint = Color(0xFFE53E3E),
                    textColor = Color(0xFFE53E3E),
                    enabled = true,
                    onClick = {
                        navController.navigate("deleteAccount")
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = {
                Text("¿Por qué necesitamos notificaciones?", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Las notificaciones te ayudan a:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Recibir mensajes de chat al instante")
                    Text("• Saber cuando alguien quiere hacer trueque")
                    Text("• Mantenerte informado de nuevas ofertas")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No enviamos spam ni publicidad no deseada.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationale = false
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                ) {
                    Text("Entendido, permitir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Ahora no")
                }
            }
        )
    }

    if (showManualSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showManualSettingsDialog = false },
            title = {
                Text("Activar notificaciones manualmente", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Has denegado los permisos permanentemente. Para activarlos:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("1. Ve a Configuración del dispositivo")
                    Text("2. Busca 'Aplicaciones' o 'Apps'")
                    Text("3. Encuentra 'La Troca'")
                    Text("4. Toca 'Notificaciones'")
                    Text("5. Activa 'Permitir notificaciones'")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showManualSettingsDialog = false
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Abre Configuración > Aplicaciones > La Troca > Notificaciones",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                ) {
                    Text("Abrir Configuración")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualSettingsDialog = false }) {
                    Text("Cancelar")
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
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .then(
                if (!enabled) Modifier.alpha(0.5f) else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (enabled) iconTint else Color(0xFFCBD5E0),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = if (enabled) textColor else Color(0xFF718096),
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
            tint = if (enabled) Color(0xFF718096) else Color(0xFFCBD5E0)
        )
    }
}

@Composable
fun SettingsOptionWithSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    isChecked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .then(
                if (!enabled) Modifier.alpha(0.5f) else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (enabled) Color(0xFF2D3748) else Color(0xFFCBD5E0),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = if (enabled) Color(0xFF2D3748) else Color(0xFF718096),
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
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4CAF50),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE2E8F0),
                disabledCheckedThumbColor = Color.White,
                disabledCheckedTrackColor = Color(0xFFE2E8F0),
                disabledUncheckedThumbColor = Color.White,
                disabledUncheckedTrackColor = Color(0xFFE2E8F0)
            )
        )
    }
}

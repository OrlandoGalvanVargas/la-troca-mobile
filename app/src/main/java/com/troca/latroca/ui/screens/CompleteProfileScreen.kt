package com.troca.latroca.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AddAlert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.location.*
import com.troca.latroca.R
import com.troca.latroca.domain.models.AuthResult
import com.troca.latroca.ui.components.ImagePickerDialog
import com.troca.latroca.ui.components.LoadingModal
import com.troca.latroca.ui.viewmodels.RegistrationViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

private fun isLocationEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isLocationEnabled
}

private fun getRealLocation(
    context: Context,
    onLocationResult: (String?, Double, Double) -> Unit,
    onError: (String) -> Unit
) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L).build()

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                    val locationName = if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        buildString {
                            address.locality?.let { append(it) }
                            address.adminArea?.let {
                                if (isNotEmpty()) append(", ")
                                append(it)
                            }
                            address.countryName?.let {
                                if (isNotEmpty()) append(", ")
                                append(it)
                            }
                            if (isEmpty()) append("${location.latitude}, ${location.longitude}")
                        }
                    } else {
                        "${location.latitude}, ${location.longitude}"
                    }

                    onLocationResult(locationName, location.latitude, location.longitude)
                } catch (_: IOException) {
                    onError("Error de red al obtener ubicación")
                } catch (_: Exception) {
                    onError("Error al procesar ubicación")
                }
            } ?: run {
                onError("No se pudo obtener tu ubicación")
            }
            fusedLocationClient.removeLocationUpdates(this)
        }
    }

    try {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                android.os.Looper.getMainLooper()
            )
        } else {
            onError("Permisos de ubicación no concedidos")
        }
    } catch (_: SecurityException) {
        onError("Error de permisos de ubicación")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
    navController: NavController,
    registrationViewModel: RegistrationViewModel
) {
    val context = LocalContext.current
    val uiState by registrationViewModel.uiState.collectAsState()
    val registrationData by registrationViewModel.registrationData.collectAsState()

    var bio by remember { mutableStateOf("") }
    var ubicacion by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var bioError by remember { mutableStateOf("") }
    var ubicacionError by remember { mutableStateOf("") }

    var isGettingLocation by remember { mutableStateOf(false) }
    var showImagePicker by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showImageErrorDialog by remember { mutableStateOf(false) }
    var showLocationSettingsDialog by remember { mutableStateOf(false) }
    // 🔥 NUEVO: Estado para el diálogo de confirmación de salida
    var showExitConfirmation by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var permissionRequestCount by remember { mutableIntStateOf(0) }
    var showManualPermissionDialog by remember { mutableStateOf(false) }
    var locationPermissionRequestCount by remember { mutableIntStateOf(0) }
    var showLocationPermissionRationale by remember { mutableStateOf(false) }
    var showLocationManualSettingsDialog by remember { mutableStateOf(false) }

    val validateBio = remember {
        { text: String ->
            when {
                text.isBlank() -> ""
                text.length < 10 -> "Mínimo 10 caracteres"
                text.length > 130 -> "Máximo 130 caracteres"
                text.trim().split("\\s+".toRegex()).size < 3 -> "Escribe al menos 3 palabras"
                else -> ""
            }
        }
    }

    val validateUbicacion = remember {
        { text: String ->
            when {
                text.isBlank() -> ""
                text.length < 3 -> "Mínimo 3 caracteres"
                text.length > 100 -> "Máximo 100 caracteres"
                else -> ""
            }
        }
    }

    val isFormValid by remember(bio, ubicacion, bioError, ubicacionError, latitude, longitude) {
        derivedStateOf {
            bio.isNotBlank() &&
                    ubicacion.isNotBlank() &&
                    bioError.isBlank() &&
                    ubicacionError.isBlank() &&
                    latitude != 0.0 &&
                    longitude != 0.0
        }
    }

    // 🔥 NUEVO: Manejar el botón físico de regreso
    BackHandler(enabled = true) {
        showExitConfirmation = true
    }

    LaunchedEffect(registrationData.imageUri) {
        selectedImageUri = registrationData.imageUri
    }

    LaunchedEffect(bio, ubicacion, latitude, longitude, selectedImageUri) {
        registrationViewModel.updateStep2Data(bio, ubicacion, latitude, longitude, selectedImageUri)
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthResult.Success -> {
                showSuccessDialog = true
            }
            is AuthResult.Error -> {
                val errorMessage = state.message

                when {
                    errorMessage.contains("imagen", ignoreCase = true) &&
                            errorMessage.contains("apropiada", ignoreCase = true) -> {
                        showImageErrorDialog = true
                    }
                    errorMessage.contains("network", ignoreCase = true) ||
                            errorMessage.contains("timeout", ignoreCase = true) -> {
                        Toast.makeText(
                            context,
                            "Sin conexión a internet. Inténtalo nuevamente",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    errorMessage.contains("500", ignoreCase = true) ||
                            errorMessage.contains("502", ignoreCase = true) -> {
                        Toast.makeText(
                            context,
                            "Servidor no disponible. Inténtalo más tarde",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else -> {
                        Toast.makeText(
                            context,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                registrationViewModel.clearErrors()
            }
            else -> {}
        }
    }

    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val photoFile = remember {
        File(context.cacheDir, "JPEG_${timeStamp}_${UUID.randomUUID()}.jpg")
    }
    val photoUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
    }


     fun proceedWithLocationRequest() {
        if (!isLocationEnabled(context)) {
            showLocationSettingsDialog = true
            return
        }

        isGettingLocation = true
        getRealLocation(
            context = context,
            onLocationResult = { locationName, lat, lon ->
                isGettingLocation = false
                if (locationName != null && lat != 0.0 && lon != 0.0) {
                    ubicacion = locationName
                    latitude = lat
                    longitude = lon
                    ubicacionError = validateUbicacion(locationName)
                    Toast.makeText(context, "Ubicación detectada correctamente", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error ->
                isGettingLocation = false
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (context as? Activity)?.shouldShowRequestPermissionRationale(permission) ?: false
        } else {
            false
        }
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        val isGranted = fineLocationGranted || coarseLocationGranted

        if (isGranted) {
            locationPermissionRequestCount = 0
            showLocationPermissionRationale = false
            proceedWithLocationRequest()
        } else {
            locationPermissionRequestCount++

            val shouldShowRationale = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                    shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)

            if (!shouldShowRationale && locationPermissionRequestCount >= 1) {
                showLocationManualSettingsDialog = true
            } else if (locationPermissionRequestCount == 1) {
                showLocationPermissionRationale = true
            } else {
                Toast.makeText(
                    context,
                    "Se necesitan permisos de ubicación para detectar tu ubicación automáticamente",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri = photoUri
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri = it }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(photoUri)
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted

        if (granted) {
            Toast.makeText(
                context,
                "¡Notificaciones activadas! Te mantendremos informado",
                Toast.LENGTH_LONG
            ).show()
            showPermissionRationale = false
        } else {
            permissionRequestCount++

            if (permissionRequestCount == 1) {
                showPermissionRationale = true
                Toast.makeText(
                    context,
                    "Las notificaciones te ayudan a no perderte mensajes importantes",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                showManualPermissionDialog = true
            }
        }
    }

    // 🔥 NUEVO: Diálogo de confirmación para salir
    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "¿Salir del registro?",
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
                        "Si sales ahora, perderás todo el progreso",
                        fontSize = 14.sp,
                        color = Color(0xFF718096),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "¿Estás seguro de que quieres salir?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2D3748),
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmation = false
                        // 🔥 Regresar al login
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sí, salir", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showExitConfirmation = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continuar registro")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showManualPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showManualPermissionDialog = false },
            title = {
                Text(
                    "Activar notificaciones manualmente",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("Para activar las notificaciones:")
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
                        showManualPermissionDialog = false
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
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
                TextButton(onClick = { showManualPermissionDialog = false }) {
                    Text("Más tarde")
                }
            }
        )
    }

    if (showPermissionRationale) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddAlert,
                    contentDescription = "Información",
                    tint = Color(0xFFD97706),
                    modifier = Modifier
                        .size(32.dp)
                        .padding(end = 12.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "¿Por qué son importantes?",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF92400E),
                        fontSize = 14.sp
                    )
                    Text(
                        "Recibirás alertas instantáneas cuando alguien quiera hacer trueque contigo o te envíe mensajes",
                        fontSize = 12.sp,
                        color = Color(0xFF92400E),
                        lineHeight = 14.sp
                    )
                }

                Button(
                    onClick = {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        showPermissionRationale = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD97706),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Intentar de nuevo", fontSize = 12.sp)
                }
            }
        }
    }
    fun requestLocation() {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        when {
            hasFineLocation || hasCoarseLocation -> {
                if (!isLocationEnabled(context)) {
                    showLocationSettingsDialog = true
                } else {
                    proceedWithLocationRequest()
                }
            }

            showLocationManualSettingsDialog -> {
                showLocationManualSettingsDialog = true
            }

            showLocationPermissionRationale -> {
                showLocationPermissionRationale = true
            }

            else -> {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
    if (showLocationPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showLocationPermissionRationale = false },
            title = {
                Text("Ubicación necesaria", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("La ubicación nos ayuda a:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Mostrar trueques cerca de ti")
                    Text("• Conectar con usuarios de tu zona")
                    Text("• Mejorar tu experiencia de trueques")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tu privacidad es importante - solo usamos tu ubicación para estos fines.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationPermissionRationale = false
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                ) {
                    Text("Entendido, permitir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationPermissionRationale = false }) {
                    Text("Ahora no")
                }
            }
        )
    }

    if (showLocationManualSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showLocationManualSettingsDialog = false },
            title = {
                Text("Activar ubicación manualmente", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Has denegado los permisos de ubicación. Para activarlos:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("1. Ve a Configuración del dispositivo")
                    Text("2. Busca 'Aplicaciones' o 'Apps'")
                    Text("3. Encuentra 'La Troca'")
                    Text("4. Toca 'Permisos'")
                    Text("5. Activa 'Ubicación'")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationManualSettingsDialog = false
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(
                                context,
                                "Abre Configuración > Aplicaciones > La Troca > Permisos > Ubicación",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                ) {
                    Text("Abrir Configuración")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLocationManualSettingsDialog = false
                        Toast.makeText(
                            context,
                            "Puedes escribir tu ubicación manualmente",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                ) {
                    Text("Escribir manualmente")
                }
            }
        )
    }

    LoadingModal(
        isVisible = uiState is AuthResult.Loading,
        message = "Configurando tu cuenta...",
        timeoutSeconds = 5
    )

    if (showLocationSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showLocationSettingsDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.LocationOff,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Ubicación desactivada",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    "Para obtener tu ubicación automáticamente, activa el GPS en la configuración de tu dispositivo.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationSettingsDialog = false
                        val intent = android.content.Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Abrir configuración")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationSettingsDialog = false }) {
                    Text("Cancelar", color = Color(0xFF718096))
                }
            }
        )
    }

    if (showImageErrorDialog) {
        AlertDialog(
            onDismissRequest = {
                showImageErrorDialog = false
                registrationViewModel.clearErrors()
            },
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
                    "Imagen no apropiada",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    "Por favor, selecciona una imagen diferente para tu perfil.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImageErrorDialog = false
                        selectedImageUri = null
                        registrationViewModel.clearErrors()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Seleccionar otra imagen")
                }
            }
        )
    }

    if (showSuccessDialog) {
        LaunchedEffect(Unit) {
            delay(1500)
            showSuccessDialog = false
            navController.navigate("home") {
                popUpTo(0) { inclusive = true }
            }
        }

        AlertDialog(
            onDismissRequest = {},
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(60.dp)
                )
            },
            title = {
                Text(
                    "¡Registro exitoso!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    "Tu cuenta ha sido creada correctamente",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    Scaffold(
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Configura tu perfil",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color(0xFF2D3748)
            )

            Text(
                text = "Completa tu información para empezar",
                fontSize = 14.sp,
                color = Color(0xFF718096),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.notificacion_icon),
                            contentDescription = "Notificaciones",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier
                                .size(40.dp)
                                .padding(end = 16.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "No te pierdas nada importante",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534),
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Activa las notificaciones para recibir alertas de Mensajes de chat",
                                fontSize = 13.sp,
                                color = Color(0xFF4B5563),
                                lineHeight = 16.sp
                            )
                        }

                        Button(
                            onClick = {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF16A34A),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            Text("Activar", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Surface(
                onClick = { showImagePicker = true },
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                color = Color(0xFFF7FAFC),
                enabled = uiState !is AuthResult.Loading
            ) {
                if (selectedImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImageUri),
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                tint = Color(0xFF718096),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Agregar foto",
                                fontSize = 12.sp,
                                color = Color(0xFF718096)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { newText ->
                    val formattedText = formatBioText(bio, newText)
                    if (formattedText.length <= 130) {
                        bio = formattedText
                        bioError = validateBio(formattedText)
                    }
                },
                label = { Text("Biografía", fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4,
                enabled = uiState !is AuthResult.Loading,
                isError = bioError.isNotBlank(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (bioError.isNotBlank()) Color.Red else Color(0xFFE53935),
                    unfocusedBorderColor = if (bioError.isNotBlank()) Color.Red else Color(0xFFE2E8F0),
                    disabledBorderColor = Color(0xFFE2E8F0),
                    focusedTextColor = Color(0xFF2D3748),
                    unfocusedTextColor = Color(0xFF2D3748),
                    disabledTextColor = Color(0xFF718096),
                    cursorColor = Color(0xFFE53935),
                    errorBorderColor = Color.Red
                ),
                placeholder = {
                    Text("Cuéntanos sobre ti (min. 10 caracteres)", fontSize = 13.sp)
                },
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (bioError.isNotBlank()) {
                            Text(bioError, color = Color.Red, fontSize = 11.sp)
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        Text("${bio.length}/130", fontSize = 11.sp, color = Color(0xFF718096))
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = ubicacion,
                    onValueChange = {
                        ubicacion = it
                        ubicacionError = validateUbicacion(it)
                    },
                    label = { Text("Ubicación", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = false,
                    readOnly = true,
                    singleLine = true,
                    isError = ubicacionError.isNotBlank(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (ubicacionError.isNotBlank()) Color.Red else Color(0xFFE53935),
                        unfocusedBorderColor = if (ubicacionError.isNotBlank()) Color.Red else Color(0xFFE2E8F0),
                        disabledBorderColor = Color(0xFFE2E8F0),
                        focusedTextColor = Color(0xFF2D3748),
                        unfocusedTextColor = Color(0xFF2D3748),
                        disabledTextColor = Color(0xFF2D3748),
                        disabledPlaceholderColor = Color(0xFFA0AEC0),
                        disabledLeadingIconColor = Color(0xFFA0AEC0),
                        disabledTrailingIconColor = Color(0xFFA0AEC0),
                        cursorColor = Color(0xFFE53935),
                        errorBorderColor = Color.Red
                    ),
                    supportingText = if (ubicacionError.isNotBlank()) {
                        { Text(ubicacionError, color = Color.Red, fontSize = 11.sp) }
                    } else null
                )
                IconButton(
                    onClick = { requestLocation() },
                    modifier = Modifier
                        .size(56.dp)
                        .padding(top = 4.dp),
                    enabled = uiState !is AuthResult.Loading && !isGettingLocation
                ) {
                    if (isGettingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFE53935)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "GPS",
                            tint = if (uiState is AuthResult.Loading) Color(0xFFCBD5E0) else Color(0xFFE53935),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    bioError = validateBio(bio)
                    ubicacionError = validateUbicacion(ubicacion)

                    if (isFormValid) {
                        registrationViewModel.completeRegistration(context)
                    } else {
                        Toast.makeText(
                            context,
                            if (latitude == 0.0) "Usa el botón GPS para obtener tu ubicación"
                            else "Completa correctamente todos los campos",
                            Toast.LENGTH_SHORT
                        ).show()
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
                enabled = isFormValid && uiState !is AuthResult.Loading
            ) {
                Text(
                    "Guardar y continuar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showImagePicker && uiState !is AuthResult.Loading) {
        ImagePickerDialog(
            onTakePhoto = {
                showImagePicker = false
                val hasCamera = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED

                if (hasCamera) cameraLauncher.launch(photoUri)
                else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onSelectFromGallery = {
                showImagePicker = false
                galleryLauncher.launch("image/*")
            },
            onDismiss = { showImagePicker = false }
        )
    }
}
private fun formatBioText(currentText: String, newText: String): String {
    if (newText.isEmpty()) return ""

    if (newText.length < currentText.length) {
        return newText
    }

    val hasContent = currentText.any { it != ' ' && it != '\n' }

    if (!hasContent) {
        if (newText.first().isWhitespace()) {
            return currentText
        }
    }

    val result = StringBuilder()
    var lastChar: Char? = null

    for (char in newText) {
        when {
            result.isEmpty() && char.isWhitespace() -> {
                continue
            }
            char == '\n' -> {
                continue
            }
            char == ' ' && lastChar == ' ' -> {
                continue
            }
            else -> {
                result.append(char)
                lastChar = char
            }
        }
    }

    return result.toString()
}
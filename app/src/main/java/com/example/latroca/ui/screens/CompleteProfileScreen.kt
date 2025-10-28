package com.example.latroca.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.location.*
import com.example.latroca.ui.components.ImagePickerDialog
import com.example.latroca.ui.viewmodels.RegistrationViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private fun getRealLocation(context: android.content.Context, onLocationResult: (String?, Double, Double) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        10000L
    ).build()

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                val geocoder = Geocoder(context, java.util.Locale.getDefault())
                try {
                    val addresses = geocoder.getFromLocation(
                        location.latitude,
                        location.longitude,
                        1
                    )
                    if (addresses?.isNotEmpty() == true) {
                        val address = addresses[0]
                        val locationName = buildString {
                            address.locality?.let { append(it) }
                            address.adminArea?.let {
                                if (isNotEmpty()) append(", ")
                                append(it)
                            }
                            address.countryName?.let {
                                if (isNotEmpty()) append(", ")
                                append(it)
                            }
                            if (isEmpty()) {
                                append("${location.latitude}, ${location.longitude}")
                            }
                        }
                        onLocationResult(locationName, location.latitude, location.longitude)
                    } else {
                        onLocationResult("${location.latitude}, ${location.longitude}", location.latitude, location.longitude)
                    }
                } catch (e: Exception) {
                    onLocationResult("${location.latitude}, ${location.longitude}", location.latitude, location.longitude)
                }
            } ?: run {
                onLocationResult("No se pudo obtener ubicación", 0.0, 0.0)
            }
            fusedLocationClient.removeLocationUpdates(this)
        }
    }

    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            android.os.Looper.getMainLooper()
        )
    } else {
        onLocationResult("Permisos de ubicación no concedidos", 0.0, 0.0)
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

    var bio by remember { mutableStateOf("") }
    var ubicacion by remember { mutableStateOf("") }
    var showImagePicker by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var isGettingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }

    var bioError by remember { mutableStateOf("") }
    var ubicacionError by remember { mutableStateOf("") }
    var bioTouched by remember { mutableStateOf(false) }
    var ubicacionTouched by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showSuccessAlert by remember { mutableStateOf(false) }

    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    "Notificaciones activadas ",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    fun validateBioRealTime(bio: String): String {
        if (bio.isBlank()) return ""
        return when {
            bio.length < 10 -> "Mínimo 10 caracteres"
            bio.length > 200 -> "Máximo 200 caracteres"
            bio.contains("..") -> "No se permiten puntos consecutivos"
            bio.trim().split("\\s+".toRegex()).size < 3 -> "Escribe al menos 3 palabras"
            else -> ""
        }
    }

    fun validateUbicacionRealTime(ubicacion: String): String {
        if (ubicacion.isBlank()) return ""
        return when {
            ubicacion.length < 3 -> "Mínimo 3 caracteres"
            ubicacion.length > 100 -> "Máximo 100 caracteres"
            ubicacion.contains("..") -> "No se permiten puntos consecutivos"
            else -> ""
        }
    }

    fun isFormValid(): Boolean {
        return bio.isNotBlank() &&
                ubicacion.isNotBlank() &&
                bioError.isBlank() &&
                ubicacionError.isBlank()
    }

    LaunchedEffect(Unit) {
        registrationViewModel.clearErrors()
        registrationViewModel.updateStep2Data("", "", 0.0, 0.0, null)

        delay(1000)

        // Solo pedir permiso si es Android 13+ y no lo tiene
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasNotificationPermission
        ) {
            showNotificationPermissionDialog = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!showSuccessAlert) {
                registrationViewModel.resetState()
            }
        }
    }

    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val photoFile = remember {
        File(context.cacheDir, "JPEG_${timeStamp}_${UUID.randomUUID()}.jpg")
    }
    val photoUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
    }

    // Launchers para permisos de ubicación
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            isGettingLocation = true
            locationError = null
            getRealLocation(context) { locationName, lat, lon ->
                isGettingLocation = false
                if (locationName != null && lat != 0.0 && lon != 0.0) {
                    ubicacion = locationName
                    latitude = lat
                    longitude = lon
                    ubicacionError = validateUbicacionRealTime(locationName)
                } else {
                    locationError = "No se pudo obtener la ubicación"
                    latitude = 0.0
                    longitude = 0.0
                }
            }
        } else {
            isGettingLocation = false
            locationError = "Permiso de ubicación denegado"
        }
    }

    // Launchers para cámara y galería
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri = photoUri
            registrationViewModel.updateStep2Data(bio, ubicacion, latitude, longitude, selectedImageUri)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            registrationViewModel.updateStep2Data(bio, ubicacion, latitude, longitude, selectedImageUri)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(photoUri)
        }
    }

    // Función para solicitar ubicación
    fun requestLocation() {
        ubicacionTouched = true
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocationPermission && hasCoarseLocationPermission) {
            isGettingLocation = true
            locationError = null
            getRealLocation(context) { locationName, lat, lon ->
                isGettingLocation = false
                if (locationName != null && lat != 0.0 && lon != 0.0) {
                    ubicacion = locationName
                    latitude = lat
                    longitude = lon
                    ubicacionError = validateUbicacionRealTime(locationName)

                } else {
                    locationError = "No se pudo obtener la ubicación"
                    latitude = 0.0
                    longitude = 0.0
                }
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(bio, ubicacion, latitude, longitude, selectedImageUri) {
        registrationViewModel.updateStep2Data(bio, ubicacion, latitude, longitude, selectedImageUri)
    }

    // Manejo de estados del ViewModel
    LaunchedEffect(uiState) {
        when (uiState) {
            is com.example.latroca.domain.models.AuthResult.Success -> {
                showSuccessAlert = true
            }
            is com.example.latroca.domain.models.AuthResult.Error -> {
                val errorMessage = (uiState as com.example.latroca.domain.models.AuthResult.Error).message

                val relevantErrors = listOf("biografía", "foto", "perfil", "registro", "completar", "ubicación")
                if (relevantErrors.any { errorMessage.contains(it, ignoreCase = true) }) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            "Error: $errorMessage",
                            duration = SnackbarDuration.Long
                        )
                    }
                }
            }
            else -> {}
        }
    }

    if (showNotificationPermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                showNotificationPermissionDialog = false
            },
            title = {
                Text(
                    "¡No te pierdas los mejores trueques! 🔔",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("Activa las notificaciones para:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• 📦 Nuevos productos cerca de ti")
                    Text("• 💬 Mensajes de otros usuarios")
                    Text("• ✅ Confirmaciones de intercambios")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Puedes activarlas después en Configuración",
                        color = Color(0xFF718096),
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNotificationPermissionDialog = false
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53E3E)
                    )
                ) {
                    Text("Activar notificaciones")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNotificationPermissionDialog = false
                    }
                ) {
                    Text("Ahora no", color = Color(0xFF718096))
                }
            }
        )
    }

    if (showSuccessAlert) {
        var countdown by remember { mutableStateOf(3) }

        LaunchedEffect(showSuccessAlert) {
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            if (showSuccessAlert) {
                showSuccessAlert = false
                navController.navigate("home") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }

        AlertDialog(
            onDismissRequest = {
                // No permitir cerrar haciendo clic fuera de
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessAlert = false
                        navController.navigate("home") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ir a Home")
                }
            },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Éxito",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "¡Cuenta Creada!",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Tu registro se completó exitosamente",
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Redirigiendo en $countdown segundos...",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Configura tu perfil",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color(0xFF2D3748),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Completa esta información para empezar a intercambiar",
                fontSize = 14.sp,
                color = Color(0xFF718096),
                modifier = Modifier.padding(bottom = 40.dp)
            )

            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Activar notificaciones",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF166534)
                            )
                            Text(
                                "Recibe alertas de nuevos trueques",
                                fontSize = 14.sp,
                                color = Color(0xFF718096)
                            )
                        }
                        Button(
                            onClick = {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF16A34A)
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Activar", fontSize = 14.sp)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Foto de perfil:",
                    color = Color(0xFFE53E3E),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .align(Alignment.Start)
                )

                Surface(
                    onClick = { showImagePicker = true },
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape),
                    color = Color(0xFFF7FAFC),
                    shape = CircleShape,
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
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "+",
                                    fontSize = 28.sp,
                                    color = Color(0xFF718096),
                                    fontWeight = FontWeight.Light
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Agregar foto",
                                    fontSize = 12.sp,
                                    color = Color(0xFF718096)
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Biografía:",
                    color = Color(0xFFE53E3E),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Cuéntanos un poco sobre ti",
                    fontSize = 14.sp,
                    color = Color(0xFF718096),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = {
                        bio = it
                        bioTouched = true
                        if (bioTouched) {
                            bioError = validateBioRealTime(it)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = false,
                    maxLines = 4,
                    isError = bioError.isNotBlank(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (bioError.isNotBlank()) Color.Red else Color(0xFFE53E3E),
                        unfocusedBorderColor = if (bioError.isNotBlank()) Color.Red else Color(0xFFE2E8F0),
                        focusedTextColor = Color(0xFF2D3748),
                        unfocusedTextColor = Color(0xFF2D3748),
                        cursorColor = Color(0xFFE53E3E),
                        errorBorderColor = Color.Red,
                        errorTextColor = Color.Red
                    ),
                    placeholder = {
                        Text(
                            "Escribe tu biografía aquí... (mínimo 10 caracteres, 3 palabras)",
                            color = Color(0xFFA0AEC0),
                            fontSize = 14.sp
                        )
                    }
                )
                if (bioError.isNotBlank() && bioTouched) {
                    Text(
                        text = bioError,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 4.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp)
            ) {
                Text(
                    text = "Ubicación:",
                    color = Color(0xFFE53E3E),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Escribe tu ubicación o usa el GPS",
                    fontSize = 14.sp,
                    color = Color(0xFF718096),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                locationError?.let { error ->
                    Text(
                        text = error,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = ubicacion,
                        onValueChange = {
                            ubicacion = it
                            ubicacionTouched = true
                            if (ubicacionTouched) {
                                ubicacionError = validateUbicacionRealTime(it)
                            }
                            locationError = null
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Ubicación",
                                tint = Color(0xFF718096)
                            )
                        },
                        singleLine = true,
                        isError = ubicacionError.isNotBlank() || locationError != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = when {
                                ubicacionError.isNotBlank() || locationError != null -> Color.Red
                                else -> Color(0xFFE53E3E)
                            },
                            unfocusedBorderColor = when {
                                ubicacionError.isNotBlank() || locationError != null -> Color.Red
                                else -> Color(0xFFE2E8F0)
                            },
                            focusedTextColor = Color(0xFF2D3748),
                            unfocusedTextColor = Color(0xFF2D3748),
                            cursorColor = Color(0xFFE53E3E),
                            errorBorderColor = Color.Red,
                            errorTextColor = Color.Red
                        ),
                        placeholder = {
                            Text(
                                "Ingresa tu ubicación..",
                                color = Color(0xFFA0AEC0),
                                fontSize = 14.sp
                            )
                        }
                    )

                    OutlinedButton(
                        onClick = {
                            ubicacionTouched = true
                            requestLocation()
                        },
                        modifier = Modifier
                            .height(56.dp)
                            .width(80.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFF7FAFC)
                        ),
                        enabled = !isGettingLocation
                    ) {
                        if (isGettingLocation) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Usar GPS",
                                modifier = Modifier.size(22.dp),
                                tint = Color(0xFFE53E3E)
                            )
                        }
                    }
                }
                if (ubicacionError.isNotBlank() && ubicacionTouched) {
                    Text(
                        text = ubicacionError,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, start = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    bioTouched = true
                    ubicacionTouched = true
                    bioError = validateBioRealTime(bio)
                    ubicacionError = validateUbicacionRealTime(ubicacion)

                    if (isFormValid() && latitude != 0.0 && longitude != 0.0) {
                        registrationViewModel.completeRegistration(context)
                    } else {
                        coroutineScope.launch {
                            val errorMessage = if (latitude == 0.0 && longitude == 0.0) {
                                "Error: No se pudieron obtener las coordenadas GPS. Usa el botón de GPS nuevamente."
                            } else {
                                "Por favor completa correctamente todos los campos"
                            }
                            snackbarHostState.showSnackbar(
                                errorMessage,
                                duration = SnackbarDuration.Long
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6B6B)
                ),
                enabled = isFormValid() && uiState !is com.example.latroca.domain.models.AuthResult.Loading
            ) {
                if (uiState is com.example.latroca.domain.models.AuthResult.Loading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Configurando cuenta....",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "Guardar y continuar",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (uiState is com.example.latroca.domain.models.AuthResult.Loading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Esto puede tomar unos segundos...",
                        color = Color(0xFF718096),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                }
            } else {
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }

    // seleccionar imagen
    if (showImagePicker) {
        ImagePickerDialog(
            onTakePhoto = {
                showImagePicker = false
                val hasCameraPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED

                if (hasCameraPermission) {
                    cameraLauncher.launch(photoUri)
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onSelectFromGallery = {
                showImagePicker = false
                val hasStoragePermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED

                if (hasStoragePermission) {
                    galleryLauncher.launch("image/*")
                } else {
                    galleryLauncher.launch("image/*")
                }
            },
            onDismiss = { showImagePicker = false }
        )
    }
}
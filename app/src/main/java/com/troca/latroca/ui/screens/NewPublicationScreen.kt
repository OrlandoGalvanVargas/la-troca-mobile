package com.troca.latroca.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.location.*
import com.troca.latroca.ui.components.ImagePickerDialog
import com.troca.latroca.ui.components.LoadingModal
import com.troca.latroca.ui.viewmodels.AuthViewModel
import com.troca.latroca.ui.viewmodels.PostViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

private val CATEGORIAS = listOf(
    "Electrónicos", "Muebles", "Ropa y Accesorios", "Deportes y Fitness",
    "Libros y Revistas", "Juguetes y Juegos", "Hogar y Jardín", "Herramientas",
    "Vehículos y Accesorios", "Arte y Manualidades", "Música e Instrumentos",
    "Mascotas y Accesorios", "Alimentos y Bebidas", "Salud y Belleza", "Otro"
)

private fun isLocationEnabled(context: android.content.Context): Boolean {
    val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

private fun getRealLocation(
    context: android.content.Context,
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
                    } else "${location.latitude}, ${location.longitude}"

                    onLocationResult(locationName, location.latitude, location.longitude)
                } catch (e: IOException) {
                    onError("Error de red al obtener ubicación")
                } catch (e: Exception) {
                    onError("Error al procesar ubicación")
                }
            } ?: onError("No se pudo obtener tu ubicación")
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
    } catch (e: SecurityException) {
        onError("Error de permisos de ubicación")
    }
}

// Función para formatear título (máx 30 chars, sin saltos de línea)
private fun formatTituloText(currentText: String, newText: String): String {
    if (newText.isEmpty()) return ""
    if (newText.length < currentText.length) return newText
    if (newText.length > 30) return currentText

    val hasContent = currentText.any { it != ' ' }
    if (!hasContent && newText.first().isWhitespace()) return currentText

    val result = StringBuilder()
    var lastChar: Char? = null

    for (char in newText) {
        when {
            result.isEmpty() && char.isWhitespace() -> continue
            char == ' ' && lastChar == ' ' -> continue
            char == '\n' -> continue
            else -> {
                result.append(char)
                lastChar = char
            }
        }
    }

    return result.toString()
}

// Función para formatear descripción (máx 120 chars, sin saltos manuales)
private fun formatDescripcionText(currentText: String, newText: String): String {
    if (newText.isEmpty()) return ""
    if (newText.length < currentText.length) return newText
    if (newText.length > 120) return currentText

    val isAddingNewLine = newText.endsWith('\n') && !currentText.endsWith('\n')
    if (isAddingNewLine) return currentText

    val hasContent = currentText.any { it != ' ' && it != '\n' }
    if (!hasContent && newText.first().isWhitespace()) return ""

    val result = StringBuilder()
    var lastChar: Char? = null

    for (char in newText) {
        when {
            result.isEmpty() && char.isWhitespace() -> continue
            char == ' ' && lastChar == ' ' -> continue
            char == '\n' -> continue
            else -> {
                result.append(char)
                lastChar = char
            }
        }
    }

    return result.toString()
}

// Función para formatear necesidad (máx 120 chars, sin saltos manuales)
private fun formatNecesidadText(currentText: String, newText: String): String {
    if (newText.isEmpty()) return ""
    if (newText.length < currentText.length) return newText
    if (newText.length > 120) return currentText

    val isAddingNewLine = newText.endsWith('\n') && !currentText.endsWith('\n')
    if (isAddingNewLine) return currentText

    val hasContent = currentText.any { it != ' ' && it != '\n' }
    if (!hasContent && newText.first().isWhitespace()) return ""

    val result = StringBuilder()
    var lastChar: Char? = null

    for (char in newText) {
        when {
            result.isEmpty() && char.isWhitespace() -> continue
            char == ' ' && lastChar == ' ' -> continue
            char == '\n' -> continue
            else -> {
                result.append(char)
                lastChar = char
            }
        }
    }

    return result.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPublicationScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    postViewModel: PostViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isBackButtonEnabled by remember { mutableStateOf(true) }

    // Estados del formulario
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("") }
    var necesidad by remember { mutableStateOf("") }
    var ubicacion by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Solo validación IA para imagen
    var imagenSegura by remember { mutableStateOf<Boolean?>(null) }
    var isValidatingImage by remember { mutableStateOf(false) }

    // Estados de UI
    var isGettingLocation by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }
    var showImagePicker by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showLocationSettingsDialog by remember { mutableStateOf(false) }

    fun handleBackNavigation() {
        if (isBackButtonEnabled && !isPublishing) {
            isBackButtonEnabled = false
            navController.popBackStack()

            coroutineScope.launch {
                delay(500)
                isBackButtonEnabled = true
            }
        }
    }

    // ✅ CÓDIGO NUEVO (USAR ESTE):
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    // Función para generar un nuevo URI de foto cada vez
    fun generateNewPhotoUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val photoFile = File(context.cacheDir, "JPEG_${timeStamp}_${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
    }

    // Launchers
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            if (!isLocationEnabled(context)) {
                showLocationSettingsDialog = true
            } else {
                isGettingLocation = true
                getRealLocation(
                    context = context,
                    onLocationResult = { locationName, lat, lon ->
                        isGettingLocation = false
                        if (locationName != null && lat != 0.0 && lon != 0.0) {
                            ubicacion = locationName
                            latitude = lat
                            longitude = lon
                        }
                    },
                    onError = { error ->
                        isGettingLocation = false
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        } else {
            Toast.makeText(context, "Permisos de ubicación necesarios", Toast.LENGTH_LONG).show()
        }
    }

    // ✅ CÓDIGO NUEVO (USAR ESTE):
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            selectedImageUri = photoUri
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selectedImageUri = it }
    }

// ✅ CÓDIGO NUEVO (USAR ESTE):
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            photoUri = generateNewPhotoUri()
            cameraLauncher.launch(photoUri!!)
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestLocation() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        when {
            hasFine || hasCoarse -> {
                if (!isLocationEnabled(context)) {
                    showLocationSettingsDialog = true
                } else {
                    isGettingLocation = true
                    getRealLocation(
                        context = context,
                        onLocationResult = { locationName, lat, lon ->
                            isGettingLocation = false
                            if (locationName != null && lat != 0.0 && lon != 0.0) {
                                ubicacion = locationName
                                latitude = lat
                                longitude = lon
                            }
                        },
                        onError = { error ->
                            isGettingLocation = false
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
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

    // Validación IA solo para imagen
    LaunchedEffect(selectedImageUri) {
        imagenSegura = null
        val uri = selectedImageUri ?: return@LaunchedEffect
        val token = authViewModel.getToken() ?: return@LaunchedEffect

        isValidatingImage = true
        try {
            val result = postViewModel.analyzeImage(context, token, uri)
            imagenSegura = result.first
        } catch (e: Exception) {
            Toast.makeText(context, "Error validando imagen", Toast.LENGTH_SHORT).show()
        } finally {
            isValidatingImage = false
        }
    }

    // Validar formulario completo
    val isFormComplete = titulo.isNotBlank() &&
            descripcion.isNotBlank() &&
            categoria.isNotBlank() &&
            necesidad.isNotBlank() &&
            ubicacion.isNotBlank() &&
            selectedImageUri != null

    val canPublish by remember(imagenSegura, isFormComplete, isPublishing, isValidatingImage) {
        derivedStateOf {
            imagenSegura == true && isFormComplete && !isPublishing && !isValidatingImage
        }
    }

    LoadingModal(
        isVisible = isPublishing,
        message = "Publicando tu producto...",
        timeoutSeconds = 7
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
                Text("Ubicación desactivada", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text(
                    "Activa el GPS para obtener tu ubicación automáticamente.",
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

    if (showSuccessDialog) {
        LaunchedEffect(Unit) {
            delay(1500)
            showSuccessDialog = false
            navController.navigate("home") {
                popUpTo("home") { inclusive = true }
            }
        }

        AlertDialog(
            onDismissRequest = {},
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(60.dp)
                )
            },
            title = {
                Text(
                    "¡Publicación creada!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text("Tu producto ha sido publicado correctamente", textAlign = TextAlign.Center)
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva publicación", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { handleBackNavigation() },
                        enabled = isBackButtonEnabled && !isPublishing
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Atrás",
                            tint = when {
                                !isBackButtonEnabled -> Color(0xFFCBD5E0)
                                isPublishing -> Color(0xFFCBD5E0)
                                else -> Color(0xFFE53935)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Foto del producto
            Text(
                "Foto del producto",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE53935),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF7FAFC))
                    .clickable(enabled = !isPublishing) { showImagePicker = true },
                contentAlignment = Alignment.Center
            ) {
                selectedImageUri?.let {
                    Image(
                        painter = rememberAsyncImagePainter(it),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } ?: Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = null,
                        tint = Color(0xFF718096),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Agregar foto", fontSize = 14.sp, color = Color(0xFF718096))
                }
            }
// 🔥 Mensaje de validación de imagen
            Box(
                modifier = Modifier.fillMaxWidth().height(32.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isValidatingImage -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF2196F3)
                            )
                            Text(
                                "Validando imagen...",
                                color = Color(0xFF2196F3),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    imagenSegura == false -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Contenido de imagen no permitido",
                                color = Color(0xFFD32F2F),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    imagenSegura == true && selectedImageUri != null -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Imagen validada correctamente",
                                color = Color(0xFF4CAF50),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Campo Título
            CampoTexto(
                label = "Título",
                valor = titulo,
                isPublishing = isPublishing,
                altura = 56.dp,
                singleLine = true,
                maxChars = 30
            ) { newText ->
                if (!isPublishing) {
                    titulo = formatTituloText(titulo, newText)
                }
            }

            // Campo Descripción
            CampoTexto(
                label = "Descripción",
                valor = descripcion,
                isPublishing = isPublishing,
                altura = 100.dp,
                singleLine = false,
                maxChars = 120
            ) { newText ->
                if (!isPublishing) {
                    descripcion = formatDescripcionText(descripcion, newText)
                }
            }

            // Categoría
            Text(
                "Categoría",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE53935),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ExposedDropdownMenuBox(
                expanded = showCategoryDropdown && !isPublishing,
                onExpandedChange = { if (!isPublishing) showCategoryDropdown = it }
            ) {
                OutlinedTextField(
                    value = categoria,
                    onValueChange = {},
                    readOnly = true,
                    enabled = !isPublishing,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    placeholder = { Text("Selecciona categoría", fontSize = 14.sp) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE53935),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        disabledBorderColor = Color(0xFFE2E8F0),
                        disabledTextColor = Color(0xFF718096)
                    )
                )

                ExposedDropdownMenu(
                    expanded = showCategoryDropdown,
                    onDismissRequest = { showCategoryDropdown = false }
                ) {
                    CATEGORIAS.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                categoria = cat
                                showCategoryDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo Necesidad
            CampoTexto(
                label = "Necesidad (qué buscas)",
                valor = necesidad,
                isPublishing = isPublishing,
                altura = 100.dp,
                singleLine = false,
                maxChars = 120
            ) { newText ->
                if (!isPublishing) {
                    necesidad = formatNecesidadText(necesidad, newText)
                }
            }

            // Ubicación
            Text(
                "Ubicación",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE53935),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = ubicacion,
                    onValueChange = {},
                    modifier = Modifier.weight(1f),
                    enabled = false,
                    readOnly = true,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("Obtén ubicación con GPS", fontSize = 14.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE53935),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        disabledBorderColor = Color(0xFFE2E8F0),
                        disabledTextColor = Color(0xFF2D3748),
                        disabledPlaceholderColor = Color(0xFFA0AEC0)
                    )
                )

                IconButton(
                    onClick = { requestLocation() },
                    modifier = Modifier.size(56.dp),
                    enabled = !isPublishing && !isGettingLocation
                ) {
                    if (isGettingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFE53935)
                        )
                    } else {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "GPS",
                            tint = if (isPublishing) Color(0xFFCBD5E0) else Color(0xFFE53935),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botón publicar
            Button(
                onClick = {
                    val emptyFields = mutableListOf<String>()
                    if (selectedImageUri == null) emptyFields.add("Foto")
                    if (titulo.isBlank()) emptyFields.add("Título")
                    if (descripcion.isBlank()) emptyFields.add("Descripción")
                    if (categoria.isBlank()) emptyFields.add("Categoría")
                    if (necesidad.isBlank()) emptyFields.add("Necesidad")
                    if (ubicacion.isBlank()) emptyFields.add("Ubicación")

                    when {
                        emptyFields.isNotEmpty() -> {
                            Toast.makeText(
                                context,
                                "Completa: ${emptyFields.joinToString(", ")}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        imagenSegura != true -> {
                            Toast.makeText(
                                context,
                                "La imagen debe ser validada como apropiada",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> {
                            coroutineScope.launch {
                                val token = authViewModel.getToken()
                                if (token == null) {
                                    Toast.makeText(context, "Sesión expirada", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }

                                isPublishing = true

                                postViewModel.createPostWithImage(
                                    context, token, titulo, descripcion, categoria,
                                    necesidad, ubicacion, latitude, longitude, selectedImageUri!!,
                                    onSuccess = {
                                        isPublishing = false
                                        showSuccessDialog = true
                                    },
                                    onError = { error ->
                                        isPublishing = false
                                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canPublish) Color(0xFFE53935) else Color(0xFFE2E8F0),
                    disabledContainerColor = Color(0xFFE2E8F0)
                ),
                enabled = canPublish
            ) {
                Text(
                    "Publicar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (canPublish) Color.White else Color(0xFF718096)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showImagePicker && !isPublishing) {
            ImagePickerDialog(
                onTakePhoto = {
                    showImagePicker = false
                    val hasCamera = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasCamera) {
                        photoUri = generateNewPhotoUri()
                        cameraLauncher.launch(photoUri!!)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onSelectFromGallery = {
                    showImagePicker = false
                    galleryLauncher.launch("image/*")
                },
                onDismiss = { showImagePicker = false }
            )
        }
    }
}

@Composable
fun CampoTexto(
    label: String,
    valor: String,
    isPublishing: Boolean,
    altura: Dp = 56.dp,
    singleLine: Boolean = false,
    maxChars: Int? = null,
    onChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE53935),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (maxChars != null) {
                Text(
                    "${valor.length}/$maxChars",
                    fontSize = 12.sp,
                    color = Color(0xFF718096),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        OutlinedTextField(
            value = valor,
            onValueChange = onChange,
            enabled = !isPublishing,
            modifier = Modifier.fillMaxWidth().heightIn(min = altura),
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text("Escribe aquí...", fontSize = 14.sp, color = Color(0xFFB0BEC5)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFE53935),
                unfocusedBorderColor = Color(0xFFE2E8F0),
                disabledBorderColor = Color(0xFFE2E8F0),
                disabledTextColor = Color(0xFF718096),
                focusedTextColor = Color(0xFF2D3748),
                unfocusedTextColor = Color(0xFF2D3748)
            )
        )
    }
}
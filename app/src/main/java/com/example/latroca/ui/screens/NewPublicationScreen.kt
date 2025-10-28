package com.example.latroca.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.latroca.ui.components.ImagePickerDialog
import com.example.latroca.ui.viewmodels.AuthViewModel
import com.example.latroca.ui.viewmodels.PostViewModel
import com.google.android.gms.location.*
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.ui.unit.Dp
import java.text.SimpleDateFormat
import java.util.*

private fun getRealLocation(context: android.content.Context, onLocationResult: (String?, Double, Double) -> Unit) {
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
                            address.adminArea?.let { append(", $it") }
                            address.countryName?.let { append(", $it") }
                        }
                    } else "${location.latitude}, ${location.longitude}"

                    onLocationResult(locationName, location.latitude, location.longitude)
                } catch (e: Exception) {
                    onLocationResult("Ubicación no disponible", 0.0, 0.0)
                }
            } ?: run {
                onLocationResult("No se pudo obtener ubicación", 0.0, 0.0)
            }
            fusedLocationClient.removeLocationUpdates(this)
        }
    }

    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    ) {
        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, android.os.Looper.getMainLooper()
        )
    } else {
        onLocationResult("Permisos de ubicación no concedidos", 0.0, 0.0)
    }
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

    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("") }
    var necesidad by remember { mutableStateOf("") }

    var ubicacion by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }
    var isGettingLocation by remember { mutableStateOf(false) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImagePicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val photoFile = remember { File(context.cacheDir, "JPEG_${timeStamp}_${UUID.randomUUID()}.jpg") }
    val photoUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) selectedImageUri = photoUri
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { selectedImageUri = it }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) cameraLauncher.launch(photoUri)
    }


    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            isGettingLocation = true
            getRealLocation(context) { name, lat, lon ->
                ubicacion = name ?: "Desconocida"
                latitude = lat
                longitude = lon
                isGettingLocation = false
            }
        } else {
            coroutineScope.launch { snackbarHostState.showSnackbar("Permiso de ubicación denegado") }
        }
    }

    fun requestLocation() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        if (hasFine && hasCoarse) {
            isGettingLocation = true
            getRealLocation(context) { name, lat, lon ->
                ubicacion = name ?: "Desconocida"
                latitude = lat
                longitude = lon
                isGettingLocation = false
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva publicación", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color(0xFFE53935))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ====== Imagen rectangular ======
            Text(
                text = "Foto del producto:",
                color = Color(0xFFE53935),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF7FAFC))
                    .clickable { showImagePicker = true },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImageUri),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("+", fontSize = 50.sp, color = Color(0xFF718096))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            CampoTexto("Título", titulo) { titulo = it }
            CampoTexto("Descripción", descripcion, 100.dp) { descripcion = it }
            CampoTexto("Categoría", categoria) { categoria = it }
            CampoTexto("Necesidad (qué buscas a cambio)", necesidad, 100.dp) { necesidad = it }

            Text(
                text = "Ubicación:",
                color = Color(0xFFE53935),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 16.dp, bottom = 8.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = ubicacion,
                    onValueChange = { ubicacion = it },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFE53935)) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE53935),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )
                IconButton(
                    onClick = { requestLocation() },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(50.dp)
                        .background(Color(0xFFFFCDD2), RoundedCornerShape(12.dp))
                ) {
                    if (isGettingLocation)
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else
                        Icon(Icons.Default.MyLocation, contentDescription = "Ubicación", tint = Color(0xFFE53935))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val token = authViewModel.getToken()
                    if (token.isNullOrEmpty()) {
                        coroutineScope.launch { snackbarHostState.showSnackbar("Error: usuario no autenticado") }
                        return@Button
                    }

                    if (titulo.isBlank() || descripcion.isBlank() || categoria.isBlank() || necesidad.isBlank() ||
                        selectedImageUri == null || ubicacion.isBlank() || latitude == 0.0
                    ) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Completa todos los campos e incluye una foto y ubicación.")
                        }
                        return@Button
                    }

                    postViewModel.createPostWithImage(
                        context = context,
                        token = token,
                        titulo = titulo,
                        descripcion = descripcion,
                        categoria = categoria,
                        necesidad = necesidad,
                        ubicacion = ubicacion,
                        latitude = latitude,
                        longitude = longitude,
                        imageUri = selectedImageUri!!,
                        onSuccess = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Publicación creada correctamente")
                                navController.navigate("home")
                            }
                        },
                        onError = { msg ->
                            coroutineScope.launch { snackbarHostState.showSnackbar("Error: $msg") }
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B))
            ) {
                Text("Publicar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        if (showImagePicker) {
            ImagePickerDialog(
                onTakePhoto = {
                    showImagePicker = false
                    val hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED
                    if (hasCameraPermission) cameraLauncher.launch(photoUri)
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
}

@Composable
fun CampoTexto(label: String, valor: String, altura: Dp = 56.dp, onChange: (String) -> Unit) {
    Text(
        text = label,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFFE53935),
        textAlign = androidx.compose.ui.text.style.TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    )
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .height(altura),
        placeholder = { Text("Escribe aquí...", color = Color(0xFFB0BEC5)) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFE53935),
            unfocusedBorderColor = Color(0xFFE2E8F0)
        )
    )
}

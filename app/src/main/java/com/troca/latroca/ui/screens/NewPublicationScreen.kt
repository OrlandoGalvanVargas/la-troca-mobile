package com.troca.latroca.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
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
import com.troca.latroca.ui.components.AlertTop
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
import com.troca.latroca.ui.components.ImagePickerDialog
import com.troca.latroca.ui.viewmodels.AuthViewModel
import com.troca.latroca.ui.viewmodels.PostViewModel
import com.google.android.gms.location.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
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
            } ?: onLocationResult("No se pudo obtener ubicación", 0.0, 0.0)
            fusedLocationClient.removeLocationUpdates(this)
        }
    }

    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    ) {
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, android.os.Looper.getMainLooper())
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

    var isPublishing by remember { mutableStateOf(false) }
    var isWaitingAfterPublish by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    // IA
    var tituloSeguro by remember { mutableStateOf<Boolean?>(null) }
    var descripcionSegura by remember { mutableStateOf<Boolean?>(null) }
    var necesidadSegura by remember { mutableStateOf<Boolean?>(null) }
    var categoriaSegura by remember { mutableStateOf<Boolean?>(null) }
    var imagenSegura by remember { mutableStateOf<Boolean?>(null) }

    // Foto
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val photoFile = remember { File(context.cacheDir, "JPEG_${timeStamp}_${UUID.randomUUID()}.jpg") }
    val photoUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
        if (it) selectedImageUri = photoUri
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selectedImageUri = it }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) cameraLauncher.launch(photoUri)
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
        }
    }

    // IA Validación
    LaunchedEffect(titulo) {
        val token = authViewModel.getToken() ?: return@LaunchedEffect
        delay(1000)
        if (titulo.isNotBlank()) tituloSeguro = postViewModel.analyzeText(token, titulo).first
    }
    LaunchedEffect(descripcion) {
        val token = authViewModel.getToken() ?: return@LaunchedEffect
        delay(1000)
        if (descripcion.isNotBlank()) descripcionSegura = postViewModel.analyzeText(token, descripcion).first
    }
    LaunchedEffect(necesidad) {
        val token = authViewModel.getToken() ?: return@LaunchedEffect
        delay(1000)
        if (necesidad.isNotBlank()) necesidadSegura = postViewModel.analyzeText(token, necesidad).first
    }
    LaunchedEffect(categoria) {
        val token = authViewModel.getToken() ?: return@LaunchedEffect
        delay(1000)
        if (categoria.isNotBlank()) categoriaSegura = postViewModel.analyzeText(token, categoria).first
    }
    LaunchedEffect(selectedImageUri) {
        val token = authViewModel.getToken() ?: return@LaunchedEffect
        imagenSegura = null
        selectedImageUri?.let {
            imagenSegura = postViewModel.analyzeImage(context, token, it).first
        }
    }


    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(2000)
            showSuccess = false
            navController.navigate("home")
        }
    }
    LaunchedEffect(showError) {
        if (showError) {
            delay(2000)
            showError = false
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
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Foto
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Foto del producto:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE53935),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF7FAFC))
                            .clickable { showImagePicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        selectedImageUri?.let {
                            Image(
                                painter = rememberAsyncImagePainter(it),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 200.dp, max = 300.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        } ?: Text("+", fontSize = 50.sp, color = Color(0xFF718096))
                    }
                }

                if (imagenSegura == false) {
                    Text(
                        "Contenido de imagen no permitido",
                        color = Color(0xFFD32F2F),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Campos a validar con IA
                CampoIA("Título", titulo, tituloSeguro) { titulo = it }
                CampoIA("Descripción", descripcion, descripcionSegura, 100.dp) { descripcion = it }
                CampoIA("Categoría", categoria, categoriaSegura) { categoria = it }
                CampoIA("Necesidad (qué buscas a cambio)", necesidad, necesidadSegura, 100.dp) { necesidad = it }

                // Ubicación
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Ubicación:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE53935),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = ubicacion,
                            onValueChange = { ubicacion = it },
                            leadingIcon = {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFE53935))
                            },
                            modifier = Modifier.weight(1f),
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
                                .background(Color(0xFFFFCDD2), RoundedCornerShape(8.dp))
                        ) {
                            if (isGettingLocation)
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            else
                                Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFFE53935))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val isReady = tituloSeguro == true &&
                        descripcionSegura == true &&
                        necesidadSegura == true &&
                        categoriaSegura == true &&
                        imagenSegura == true &&
                        titulo.isNotBlank() &&
                        descripcion.isNotBlank() &&
                        categoria.isNotBlank() &&
                        necesidad.isNotBlank() &&
                        ubicacion.isNotBlank() &&
                        selectedImageUri != null &&
                        !isPublishing &&
                        !isWaitingAfterPublish

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val token = authViewModel.getToken() ?: return@launch
                            isPublishing = true
                            postViewModel.createPostWithImage(
                                context,
                                token,
                                titulo,
                                descripcion,
                                categoria,
                                necesidad,
                                ubicacion,
                                latitude,
                                longitude,
                                selectedImageUri!!,
                                onSuccess = {
                                    coroutineScope.launch {
                                        isPublishing = false
                                        isWaitingAfterPublish = true
                                        showSuccess = true
                                        delay(2500)
                                        isWaitingAfterPublish = false
                                    }
                                },
                                onError = {
                                    coroutineScope.launch {
                                        isPublishing = false
                                        isWaitingAfterPublish = true
                                        showError = true
                                        delay(2500)
                                        isWaitingAfterPublish = false
                                    }
                                }
                            )
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isPublishing || isWaitingAfterPublish -> Color(0xFFB0BEC5)
                            isReady -> Color(0xFFFF6B6B)
                            else -> Color.LightGray
                        }
                    ),
                    enabled = isReady
                ) {
                    if (isPublishing)
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    else
                        Text("Publicar", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(30.dp))
            }

            if (showSuccess)
                AlertTop("Publicación creada correctamente", Color(0xFF4CAF50), Icons.Default.CheckCircle)
            if (showError)
                AlertTop("Error al crear la publicación", Color(0xFFD32F2F), Icons.Default.Error)
        }

        if (showImagePicker) {
            ImagePickerDialog(
                onTakePhoto = {
                    showImagePicker = false
                    val hasCameraPermission =
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
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
fun CampoIA(label: String, valor: String, seguro: Boolean?, altura: Dp = 56.dp, onChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFE53935),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        OutlinedTextField(
            value = valor,
            onValueChange = onChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(altura),
            placeholder = { Text("Escribe aquí...", color = Color(0xFFB0BEC5)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFE53935),
                unfocusedBorderColor = Color(0xFFE2E8F0)
            )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (seguro == false) {
                Text(
                    text = "Contenido inapropiado detectado",
                    color = Color(0xFFD32F2F),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}






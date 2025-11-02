package com.example.latroca.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.latroca.ui.components.AlertTop
import com.example.latroca.ui.components.ImagePickerDialog
import com.example.latroca.ui.viewmodels.AuthViewModel
import com.example.latroca.ui.viewmodels.PostViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicationDetailScreen(
    navController: NavController,
    postId: String,
    postViewModel: PostViewModel,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val post = postViewModel.posts.collectAsState().value.find { it.id == postId }
    val token = authViewModel.getToken() ?: ""
    val currentUserId = authViewModel.getUserId()
    val isOwner = post?.userId == currentUserId

    var isEditing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var lastAction by remember { mutableStateOf("") }

    var titulo by remember { mutableStateOf(post?.titulo ?: "") }
    var descripcion by remember { mutableStateOf(post?.descripcion ?: "") }
    var necesidad by remember { mutableStateOf(post?.necesidad ?: "") }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImagePicker by remember { mutableStateOf(false) }

    // IA
    var tituloSeguro by remember { mutableStateOf<Boolean?>(true) }
    var descripcionSegura by remember { mutableStateOf<Boolean?>(true) }
    var necesidadSegura by remember { mutableStateOf<Boolean?>(true) }
    var imagenSegura by remember { mutableStateOf<Boolean?>(true) }

    // Cámara / galería
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val photoFile = remember { File(context.cacheDir, "JPEG_${timeStamp}_${UUID.randomUUID()}.jpg") }
    val photoUri = androidx.core.content.FileProvider.getUriForFile(
        context, "${context.packageName}.fileprovider", photoFile
    )

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
        if (it) selectedImageUri = photoUri
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selectedImageUri = it }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) cameraLauncher.launch(photoUri)
    }

    // IA análisis
    LaunchedEffect(titulo) {
        if (isEditing && titulo.isNotBlank()) tituloSeguro = postViewModel.analyzeText(token, titulo).first
    }
    LaunchedEffect(descripcion) {
        if (isEditing && descripcion.isNotBlank()) descripcionSegura = postViewModel.analyzeText(token, descripcion).first
    }
    LaunchedEffect(necesidad) {
        if (isEditing && necesidad.isNotBlank()) necesidadSegura = postViewModel.analyzeText(token, necesidad).first
    }
    LaunchedEffect(selectedImageUri) {
        if (isEditing && selectedImageUri != null) {
            imagenSegura = null
            imagenSegura = postViewModel.analyzeImage(context, token, selectedImageUri!!).first
        }
    }

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(2000)
            showSuccess = false
            if (lastAction == "eliminar") navController.navigate("home")
        }
    }

    val allSafe = listOf(tituloSeguro, descripcionSegura, necesidadSegura, imagenSegura).all { it == true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de publicación", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color(0xFFE53935))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        post?.let {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(paddingValues)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF7FAFC))
                            .clickable(enabled = isEditing) { showImagePicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = selectedImageUri ?: post.fotosUrl.firstOrNull()
                                ?: "https://via.placeholder.com/600x400.png?text=Sin+imagen"
                            ),
                            contentDescription = post.titulo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 200.dp, max = 280.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth().height(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imagenSegura == false)
                            Text("Contenido de imagen no permitido", color = Color(0xFFD32F2F), fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFF8A8A), RoundedCornerShape(50))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = post.categoria,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isEditing) {
                        CampoIAEdit("Título", titulo, tituloSeguro) { titulo = it }
                        CampoIAEdit("Descripción", descripcion, descripcionSegura, 100.dp) { descripcion = it }
                        CampoIAEdit("Necesidad", necesidad, necesidadSegura, 100.dp) { necesidad = it }
                    } else {
                        CampoSoloLectura("Título", post.titulo)
                        CampoSoloLectura("Descripción", post.descripcion)
                        CampoSoloLectura("Ubicación", post.ubicacion.manual, icon = Icons.Default.Place)
                        CampoSoloLectura("Fecha de publicación", post.creadoEn.substring(0, 10), icon = Icons.Default.CalendarToday)
                        CampoSoloLectura("Necesidad", post.necesidad)
                    }

                    Spacer(modifier = Modifier.height(25.dp))

                    if (isOwner) {
                        var isUpdating by remember { mutableStateOf(false) }

                        if (isEditing) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            isUpdating = true
                                            postViewModel.updatePost(
                                                context = context,
                                                token = token,
                                                postId = post.id,
                                                titulo = titulo,
                                                descripcion = descripcion,
                                                categoria = post.categoria,
                                                necesidad = necesidad,
                                                ubicacionManual = post.ubicacion.manual,
                                                latitude = post.ubicacion.latitude,
                                                longitude = post.ubicacion.longitude,
                                                existingImageUrl = post.fotosUrl.firstOrNull(),
                                                newImageUri = selectedImageUri,
                                                onSuccess = {
                                                    isEditing = false
                                                    isUpdating = false
                                                    lastAction = "actualizar"
                                                    showSuccess = true

                                                    selectedImageUri?.let { nuevaUri ->
                                                        selectedImageUri = nuevaUri
                                                    } ?: run {
                                                        selectedImageUri = null
                                                    }
                                                },
                                                onError = { isUpdating = false }
                                            )
                                        }
                                    },
                                    enabled = allSafe && !isUpdating,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isUpdating) Color(0xFFB0BEC5) else Color(0xFF4CAF50)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isUpdating)
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    else {
                                        Icon(Icons.Default.Check, null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Guardar", color = Color.White)
                                    }
                                }

                                Button(
                                    onClick = {
                                        isEditing = false
                                        titulo = post.titulo
                                        descripcion = post.descripcion
                                        necesidad = post.necesidad
                                        selectedImageUri = null

                                        tituloSeguro = true
                                        descripcionSegura = true
                                        necesidadSegura = true
                                        imagenSegura = true
                                    },
                                    enabled = !isUpdating,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Close, null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cancelar", color = Color.White)
                                }
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        titulo = post.titulo
                                        descripcion = post.descripcion
                                        necesidad = post.necesidad
                                        selectedImageUri = null
                                        isEditing = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Edit, null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Editar")
                                }
                                Button(
                                    onClick = { showDeleteDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Delete, null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Eliminar")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }

                if (showSuccess)
                    AlertTop(
                        message = if (lastAction == "eliminar")
                            "Publicación eliminada correctamente"
                        else "Publicación actualizada correctamente",
                        color = Color(0xFF4CAF50),
                        icon = Icons.Default.CheckCircle
                    )

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(60.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Confirmación",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2D3748)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "¿Eliminar la publicación? Esta acción no se puede deshacer.",
                                    textAlign = TextAlign.Center,
                                    fontSize = 15.sp,
                                    color = Color(0xFF4A5568)
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDeleteDialog = false
                                    coroutineScope.launch {
                                        postViewModel.deletePost(token, post.id)
                                        lastAction = "eliminar"
                                        showSuccess = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Sí, eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            OutlinedButton(
                                onClick = { showDeleteDialog = false },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2D3748))
                            ) {
                                Text("Cancelar")
                            }
                        },
                        containerColor = Color.White,
                        tonalElevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
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
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Publicación no encontrada", color = Color.Gray)
        }
    }
}

@Composable
fun CampoIAEdit(label: String, valor: String, seguro: Boolean?, altura: Dp = 56.dp, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE53935))
        OutlinedTextField(
            value = valor,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().height(altura),
            placeholder = { Text("Escribe aquí...", color = Color(0xFFB0BEC5)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFE53935),
                unfocusedBorderColor = Color(0xFFE2E8F0)
            )
        )
        Box(Modifier.fillMaxWidth().height(20.dp), contentAlignment = Alignment.CenterStart) {
            if (seguro == false)
                Text("Contenido inapropiado detectado", color = Color(0xFFD32F2F), fontSize = 12.sp)
        }
    }
}

@Composable
fun CampoSoloLectura(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label, color = Color(0xFFE53935)) },
        leadingIcon = icon?.let { { Icon(it, null, tint = Color(0xFFE53935)) } },
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFE53935),
            unfocusedBorderColor = Color(0xFFE2E8F0)
        )
    )
}

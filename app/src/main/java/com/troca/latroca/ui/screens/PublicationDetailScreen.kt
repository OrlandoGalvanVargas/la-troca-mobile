package com.troca.latroca.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.troca.latroca.ui.components.AlertTop
import com.troca.latroca.ui.components.ImagePickerDialog
import com.troca.latroca.ui.components.LoadingModal
import com.troca.latroca.ui.viewmodels.AuthViewModel
import com.troca.latroca.ui.viewmodels.ChatViewModel
import com.troca.latroca.ui.viewmodels.PostViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.troca.latroca.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
fun PublicationDetailScreen(
    navController: NavController,
    postId: String,
    postViewModel: PostViewModel,
    authViewModel: AuthViewModel,
    chatViewModel: ChatViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val post = postViewModel.posts.collectAsState().value.find { it.id == postId }
    val token = authViewModel.getToken() ?: ""
    val currentUserId = authViewModel.getUserId()
    val isOwner = post?.userId == currentUserId

    var isBackButtonEnabled by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var lastAction by remember { mutableStateOf("") }
    var isUpdating by remember { mutableStateOf(false) }
    var showFullScreenImage by remember { mutableStateOf(false) }
    var titulo by remember { mutableStateOf(post?.titulo ?: "") }
    var descripcion by remember { mutableStateOf(post?.descripcion ?: "") }
    var necesidad by remember { mutableStateOf(post?.necesidad ?: "") }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImagePicker by remember { mutableStateOf(false) }

    var imagenSegura by remember { mutableStateOf<Boolean?>(true) }
    var isValidatingImage by remember { mutableStateOf(false) }

    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            selectedImageUri = photoUri
            photoUri = null
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selectedImageUri = it }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val photoFile = File(context.cacheDir, "JPEG_${timeStamp}_${UUID.randomUUID()}.jpg")
            photoUri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", photoFile
            )
            cameraLauncher.launch(photoUri!!)
        }
    }

    fun handleBackNavigation() {
        if (isBackButtonEnabled && !isUpdating) {
            isBackButtonEnabled = false
            navController.popBackStack()

            coroutineScope.launch {
                delay(500)
                isBackButtonEnabled = true
            }
        }
    }

    LaunchedEffect(selectedImageUri) {
        if (isEditing && selectedImageUri != null) {
            isValidatingImage = true
            imagenSegura = null
            imagenSegura = postViewModel.analyzeImage(context, token, selectedImageUri!!).first
            isValidatingImage = false
        }
    }

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(750)
            showSuccess = false
            if (lastAction == "eliminar") {
                navController.popBackStack()
            } else if (lastAction == "actualizar") {
                navController.popBackStack()
            }
        }
    }

    val isFormComplete = titulo.isNotBlank() &&
            descripcion.isNotBlank() &&
            necesidad.isNotBlank() &&
            (selectedImageUri != null || post?.fotosUrl?.isNotEmpty() == true)

    val canUpdate by remember(imagenSegura, isFormComplete, isUpdating, isValidatingImage) {
        derivedStateOf {
            imagenSegura == true && isFormComplete && !isUpdating && !isValidatingImage
        }
    }

    LoadingModal(
        isVisible = isUpdating,
        message = "Actualizando publicación...",
        timeoutSeconds = 5
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de publicación", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { handleBackNavigation() },
                        enabled = isBackButtonEnabled && !isUpdating
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = when {
                                !isBackButtonEnabled -> Color(0xFFCBD5E0)
                                isUpdating -> Color(0xFFCBD5E0)
                                else -> Color(0xFFE53935)
                            }
                        )
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
                            .clickable(
                                enabled = if (isEditing) {
                                    !isUpdating
                                } else {
                                    true
                                }
                            ) {
                                if (isEditing) {
                                    showImagePicker = true
                                } else {
                                    showFullScreenImage = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        key(selectedImageUri?.toString() ?: post.fotosUrl.firstOrNull()) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(selectedImageUri ?: post.fotosUrl.firstOrNull()
                                        ?: "https://via.placeholder.com/600x400.png?text=Sin+imagen")
                                        .crossfade(true)
                                        .diskCachePolicy(CachePolicy.DISABLED)
                                        .memoryCachePolicy(CachePolicy.DISABLED)
                                        .build()
                                ),
                                contentDescription = "Haz clic para ver la imagen completa",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 200.dp, max = 280.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }

                        if (!isEditing) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .size(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = "Ver imagen completa",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

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
                                Text(
                                    "Contenido de imagen no permitido",
                                    color = Color(0xFFD32F2F),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
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

                    if (isEditing) {
                        CampoEdit(
                            label = "Título",
                            valor = titulo,
                            altura = 56.dp,
                            singleLine = true,
                            maxChars = 30,
                            isUpdating = isUpdating
                        ) { newText ->
                            if (!isUpdating) {
                                titulo = formatTituloText(titulo, newText)
                            }
                        }

                        CampoEdit(
                            label = "Descripción",
                            valor = descripcion,
                            altura = 100.dp,
                            singleLine = false,
                            maxChars = 120,
                            isUpdating = isUpdating
                        ) { newText ->
                            if (!isUpdating) {
                                descripcion = formatDescripcionText(descripcion, newText)
                            }
                        }

                        CampoEdit(
                            label = "Necesidad",
                            valor = necesidad,
                            altura = 100.dp,
                            singleLine = false,
                            maxChars = 120,
                            isUpdating = isUpdating
                        ) { newText ->
                            if (!isUpdating) {
                                necesidad = formatNecesidadText(necesidad, newText)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFE8F5E8), RoundedCornerShape(50))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Fecha",
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Publicado: ${post.creadoEn.substring(0, 10)}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

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

                        CampoSoloLectura("Título", post.titulo)
                        CampoSoloLectura("Descripción", post.descripcion)
                        CampoSoloLectura("Ubicación", post.ubicacion.manual, icon = Icons.Default.Place)
                        CampoSoloLectura("Necesidad", post.necesidad)

                        if (!isOwner) {
                            Spacer(modifier = Modifier.height(8.dp))

                            val userName = post.userInfo?.name ?: "Usuario"
                            val userProfileImage = post.userInfo?.profileImageUrl

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFFF5F5), RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Publicado por",
                                        fontSize = 14.sp,
                                        color = Color(0xFFE53935),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = userName,
                                        fontSize = 16.sp,
                                        color = Color(0xFF3D5AFE),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(userProfileImage)
                                            .crossfade(true)
                                            .placeholder(R.drawable.avatar_placeholder)
                                            .error(R.drawable.avatar_placeholder)
                                            .build(),
                                        contentDescription = "Avatar de $userName",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color.LightGray),
                                        contentScale = ContentScale.Crop
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Button(
                                        onClick = {
                                            navController.navigate("userProfile/${post.userId}")
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFF8A8A)
                                        ),
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "Ver perfil",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val currentUserId = authViewModel.getUserId()
                                    val currentUserName = authViewModel.userProfile.value?.name ?: "Usuario"
                                    val otherUserId = post.userId
                                    val otherUserName = post.userInfo?.name ?: "Usuario"

                                    chatViewModel.getOrCreateChat(
                                        currentUserId = currentUserId,
                                        currentUserName = currentUserName,
                                        otherUserId = otherUserId,
                                        otherUserName = otherUserName,
                                        postId = post.id,
                                        postTitle = post.titulo,
                                        postImageUrl = post.fotosUrl.firstOrNull() ?: ""
                                    ) { chatId ->
                                        navController.navigate(
                                            "chat_conversation/$chatId/$otherUserName/$otherUserId"
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2196F3)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "Chat",
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Abrir chat",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(25.dp))

                    if (isOwner) {
                        if (isEditing) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        val emptyFields = mutableListOf<String>()
                                        if (titulo.isBlank()) emptyFields.add("Título")
                                        if (descripcion.isBlank()) emptyFields.add("Descripción")
                                        if (necesidad.isBlank()) emptyFields.add("Necesidad")
                                        if (selectedImageUri == null && post.fotosUrl.isEmpty()) emptyFields.add("Imagen")

                                        when {
                                            emptyFields.isNotEmpty() -> {
                                                Toast.makeText(
                                                    context,
                                                    "Completa: ${emptyFields.joinToString(", ")}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            imagenSegura == false -> {
                                                Toast.makeText(
                                                    context,
                                                    "La imagen contiene contenido inapropiado",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            else -> {
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
                                                        },
                                                        onError = { errorMessage ->
                                                            isUpdating = false
                                                            Toast.makeText(
                                                                context,
                                                                errorMessage,
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    enabled = canUpdate,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (canUpdate) Color(0xFF4CAF50) else Color(0xFFE2E8F0),
                                        disabledContainerColor = Color(0xFFE2E8F0)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        null,
                                        tint = if (canUpdate) Color.White else Color(0xFF718096)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Guardar",
                                        color = if (canUpdate) Color.White else Color(0xFF718096),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Button(
                                    onClick = {
                                        isEditing = false
                                        titulo = post.titulo
                                        descripcion = post.descripcion
                                        necesidad = post.necesidad
                                        selectedImageUri = null
                                        imagenSegura = true
                                        isValidatingImage = false
                                    },
                                    enabled = !isUpdating,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE53935),
                                        disabledContainerColor = Color(0xFFE2E8F0)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        null,
                                        tint = if (isUpdating) Color(0xFF718096) else Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Cancelar",
                                        color = if (isUpdating) Color(0xFF718096) else Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
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
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Icon(Icons.Default.Edit, null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Editar", fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = { showDeleteDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(48.dp)
                                ) {
                                    Icon(Icons.Default.Delete, null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Eliminar", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }

                if (showSuccess && lastAction == "actualizar") {
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
                                "¡Actualización exitosa!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF2D3748)
                            )
                        },
                        text = {
                            Text(
                                "Tu publicación ha sido actualizada correctamente",
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp,
                                color = Color(0xFF4A5568)
                            )
                        },
                        confirmButton = {},
                        dismissButton = {},
                        containerColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                if (showSuccess && lastAction == "eliminar") {
                    AlertTop(
                        message = "Publicación eliminada correctamente",
                        color = Color(0xFF4CAF50),
                        icon = Icons.Default.CheckCircle
                    )
                }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(60.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "¿Eliminar publicación?",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2D3748),
                                    textAlign = TextAlign.Center
                                )
                            }
                        },
                        text = {
                            Column(
                                horizontalAlignment = Alignment.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Esta acción eliminará permanentemente:",
                                    fontSize = 15.sp,
                                    color = Color(0xFF4A5568),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                )

                                val items = listOf(
                                    Icons.Default.Article to "La publicación \"${post.titulo}\"",
                                    Icons.Default.Chat to "Todas las conversaciones relacionadas con esta publicación",
                                    Icons.Default.Info to "Esta acción no se puede deshacer"
                                )

                                items.forEachIndexed { index, (icon, text) ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = if (index == items.lastIndex) 0.dp else 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (index == items.lastIndex) Color(0xFFE53935) else Color(0xFFE53935),
                                            modifier = Modifier
                                                .size(18.dp)
                                                .padding(top = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = text,
                                            fontSize = 14.sp,
                                            color = if (index == items.lastIndex) Color(0xFFD32F2F) else Color(0xFF2D3748),
                                            fontWeight = if (index == items.lastIndex) FontWeight.Medium else FontWeight.Normal,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDeleteDialog = false
                                    coroutineScope.launch {
                                        postViewModel.deletePost(
                                            token = token,
                                            postId = post.id,
                                            chatViewModel = chatViewModel
                                        )
                                        lastAction = "eliminar"
                                        showSuccess = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Sí, eliminar todo",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        dismissButton = {
                            OutlinedButton(
                                onClick = { showDeleteDialog = false },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2D3748)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Cancelar",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cancelar")
                            }
                        },
                        containerColor = Color.White,
                        tonalElevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                if (showImagePicker && !isUpdating) {
                    ImagePickerDialog(
                        onTakePhoto = {
                            showImagePicker = false
                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            val photoFile = File(context.cacheDir, "JPEG_${timeStamp}_${UUID.randomUUID()}.jpg")
                            photoUri = androidx.core.content.FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", photoFile
                            )

                            val hasCameraPermission =
                                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                        PackageManager.PERMISSION_GRANTED
                            if (hasCameraPermission) {
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
                if (showFullScreenImage) {
                    Dialog(
                        onDismissRequest = { showFullScreenImage = false },
                        properties = DialogProperties(
                            usePlatformDefaultWidth = false,
                            dismissOnClickOutside = true,
                            dismissOnBackPress = true
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.95f))
                                .clickable { showFullScreenImage = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(selectedImageUri ?: post.fotosUrl.firstOrNull()
                                        ?: "https://via.placeholder.com/600x400.png?text=Sin+imagen")
                                        .crossfade(true)
                                        .build()
                                ),
                                contentDescription = "Imagen completa de ${post.titulo}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.8f)
                                    .padding(16.dp),
                                contentScale = ContentScale.Fit
                            )

                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar visor de imagen",
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(24.dp)
                                    .size(32.dp)
                                    .clickable { showFullScreenImage = false }
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .padding(6.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 32.dp)
                            ) {
                                Text(
                                    text = "Toca en cualquier lugar para cerrar",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Publicación no encontrada", color = Color.Gray)
        }
    }
}

@Composable
fun CampoEdit(
    label: String,
    valor: String,
    altura: Dp = 56.dp,
    singleLine: Boolean = false,
    maxChars: Int? = null,
    isUpdating: Boolean = false,
    onChange: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
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
            enabled = !isUpdating,
            modifier = Modifier.fillMaxWidth().heightIn(min = altura),
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            placeholder = { Text("Escribe aquí...", color = Color(0xFFB0BEC5), fontSize = 14.sp) },
            shape = RoundedCornerShape(12.dp),
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

@Composable
fun CampoSoloLectura(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label, color = Color(0xFFE53935), fontWeight = FontWeight.SemiBold) },
        leadingIcon = icon?.let { { Icon(it, null, tint = Color(0xFFE53935)) } },
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFE53935),
            unfocusedBorderColor = Color(0xFFE2E8F0),
            disabledBorderColor = Color(0xFFE2E8F0),
            disabledTextColor = Color(0xFF2D3748)
        )
    )
}
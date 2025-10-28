package com.example.latroca.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import coil.compose.rememberAsyncImagePainter
import com.example.latroca.ui.viewmodels.AuthViewModel
import com.example.latroca.ui.viewmodels.PostViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.unit.Dp


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

    var titulo by remember { mutableStateOf(post?.titulo ?: "") }
    var descripcion by remember { mutableStateOf(post?.descripcion ?: "") }
    var necesidad by remember { mutableStateOf(post?.necesidad ?: "") }

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
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF7FAFC)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = post.fotosUrl.firstOrNull()
                                ?: "https://via.placeholder.com/600x400.png?text=Sin+imagen"
                        ),
                        contentDescription = post.titulo,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFFCDD2))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = post.categoria,
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isEditing) {
                    CampoEditable("Título", titulo) { titulo = it }
                    CampoEditable("Descripción", descripcion, 100.dp) { descripcion = it }
                    CampoEditable("Necesidad", necesidad, 100.dp) { necesidad = it }
                } else {
                    CampoSoloLectura("Título", post.titulo)
                    CampoSoloLectura("Descripción", post.descripcion)
                    CampoSoloLectura("Ubicación", post.ubicacion.manual, icon = Icons.Default.Place)
                    CampoSoloLectura("Fecha de publicación", post.creadoEn.substring(0, 10), icon = Icons.Default.CalendarToday)
                    CampoSoloLectura("Necesidad", post.necesidad)
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (!isOwner) {
                    Button(
                        onClick = { /* Abrir chat con propietario de la publicacon */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                    ) {
                        Text("Abrir chat", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isOwner) {
                    if (isEditing) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
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
                                            newImageUri = null,
                                            onSuccess = { isEditing = false },
                                            onError = { isEditing = false }
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Guardar", color = Color.White)
                            }

                            Button(
                                onClick = {
                                    isEditing = false
                                    titulo = post.titulo
                                    descripcion = post.descripcion
                                    necesidad = post.necesidad
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cancelar", color = Color.Black)
                            }
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { isEditing = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Editar")
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        postViewModel.deletePost(token, post.id)
                                        navController.popBackStack()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Eliminar")
                            }
                        }
                    }
                }
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Publicación no encontrada", color = Color.Gray)
        }
    }
}

@Composable
fun CampoEditable(label: String, value: String, altura: Dp = 56.dp, onChange: (String) -> Unit) {
    Text(
        text = label,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFFE53935),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    )
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .height(altura),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFE53935),
            unfocusedBorderColor = Color(0xFFE2E8F0)
        )
    )
}

@Composable
fun CampoSoloLectura(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label, color = Color(0xFFE53935)) },
        leadingIcon = icon?.let {
            { Icon(it, contentDescription = null, tint = Color(0xFFE53935)) }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFE53935),
            unfocusedBorderColor = Color(0xFFE2E8F0)
        )
    )
}

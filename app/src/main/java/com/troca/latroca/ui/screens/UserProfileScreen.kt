package com.troca.latroca.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.troca.latroca.data.api.ApiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    navController: NavController,
    userId: String
) {
    var userData by remember { mutableStateOf<com.troca.latroca.data.models.UserProfileResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // 🔥 Estado para debounce del botón de regresar
    var isBackButtonEnabled by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()

    // 🔥 Función para manejar el regreso con debounce
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

    LaunchedEffect(userId) {
        coroutineScope.launch {
            try {
                val response = ApiClient.authApi.getUserProfileById(userId)
                if (response.isSuccessful) {
                    userData = response.body()?.data
                } else {
                    error = "Error ${response.code()}: ${response.message()}"
                }
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil del usuario", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { handleBackNavigation() },
                        enabled = isBackButtonEnabled
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = if (isBackButtonEnabled) Color(0xFFE53935) else Color(0xFFCBD5E0)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->

        when {
            isLoading -> Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFE53935))
            }

            error != null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No se pudo cargar el perfil",
                        color = Color(0xFF2D3748),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        error ?: "Error desconocido",
                        color = Color(0xFF718096),
                        fontSize = 14.sp
                    )
                }
            }

            userData != null -> {
                val name = userData?.name ?: "Usuario"
                val bio = userData?.bio ?: ""
                val location = userData?.location?.manual ?: "Sin ubicación"
                val profilePicUrl = userData?.profilePicUrl ?: ""

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF7FAFC))
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(32.dp))

                    // 📸 Foto de perfil
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profilePicUrl.isNotEmpty()) {
                            AsyncImage(
                                model = profilePicUrl,
                                contentDescription = "Foto de perfil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(CircleShape)
                                    .border(4.dp, Color(0xFFE53935), CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE53935)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Sin foto",
                                    tint = Color.White,
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 👤 Nombre
                    Text(
                        text = name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A202C),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 📍 Ubicación
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Ubicación",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = location,
                            color = Color(0xFF4A5568),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // 📝 Biografía
                    Text(
                        text = "Biografía",
                        color = Color(0xFFE53935),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = if (bio.isNotEmpty()) bio else "Este usuario no ha agregado una biografía",
                            color = if (bio.isNotEmpty()) Color(0xFF2D3748) else Color(0xFF718096),
                            fontSize = 15.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }
}
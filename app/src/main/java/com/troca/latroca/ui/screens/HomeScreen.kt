package com.troca.latroca.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.troca.latroca.R
import com.troca.latroca.data.models.UserProfileResponse
import com.troca.latroca.ui.viewmodels.AuthViewModel
import com.troca.latroca.ui.viewmodels.PostViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    onLogout: () -> Unit,
    authViewModel: AuthViewModel,
    postViewModel: PostViewModel
) {
    val publicaciones by postViewModel.posts.collectAsState()
    val isLoading by postViewModel.isLoading.collectAsState()
    val token by authViewModel.currentToken.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val userProfile by authViewModel.userProfile.collectAsState()

    // 🔥 Optimización: Evitar recargas innecesarias
    LaunchedEffect(token) {
        if (!token.isNullOrBlank() && publicaciones.isEmpty()) {
            postViewModel.loadPosts(token!!)
            authViewModel.loadUserProfile()
        }
    }

    val currentUserId = authViewModel.getUserId()
    var searchQuery by remember { mutableStateOf("") }

// 🔥 Optimización: Computación memoizada CORREGIDA
    val filteredPublicaciones by remember(publicaciones, searchQuery) {
        derivedStateOf {
            if (searchQuery.isEmpty()) {
                publicaciones
            } else {
                val query = searchQuery.trim().lowercase()
                publicaciones.filter { publicacion ->
                    publicacion.titulo.lowercase().contains(query) ||
                            publicacion.descripcion.lowercase().contains(query)
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color.White) {
                DrawerContent(
                    onHomeClick = { scope.launch { drawerState.close() } },
                    onConfigClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("settings")
                    },
                    userProfile = userProfile
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menú",
                                    tint = Color(0xFF2D3748)
                                )
                            }

                            Image(
                                painter = painterResource(id = R.drawable.la_troca_logo_2),
                                contentDescription = "Logo La Troca",
                                modifier = Modifier.size(60.dp)
                            )

                            // Espacio para balancear el diseño
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navController.navigate("newPublication") },
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva publicación")
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF7FAFC))
            ) {
                // 🔥 Mejor diseño de búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = {
                        Text(
                            "Buscar publicaciones...",
                            color = Color(0xFFA0AEC0)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = Color(0xFF718096)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE53E3E),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedTextColor = Color(0xFF2D3748),
                        unfocusedTextColor = Color(0xFF2D3748)
                    ),
                    singleLine = true
                )

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(0xFFE53E3E))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Cargando publicaciones...",
                                    color = Color(0xFF718096),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    token.isNullOrBlank() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "⚠️ No se pudo obtener el token",
                                    fontSize = 16.sp,
                                    color = Color(0xFFE53E3E),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Por favor, inicia sesión nuevamente",
                                    fontSize = 14.sp,
                                    color = Color(0xFF718096)
                                )
                            }
                        }
                    }

                    filteredPublicaciones.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = painterResource(id = R.drawable.not_found),
                                    contentDescription = "Sin publicaciones",
                                    tint = Color(0xFFCBD5E0),
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "No se encontraron resultados" else "No hay publicaciones",
                                    fontSize = 16.sp,
                                    color = Color(0xFF718096),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            item(span = { GridItemSpan(2) }) {
                                Text(
                                    text = "Publicaciones (${filteredPublicaciones.size})",
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2D3748)
                                )
                            }

                            // 🔥 CLAVE: Agregar keys únicas para optimización
                            items(
                                items = filteredPublicaciones,
                                key = { it.id } // Importante para performance
                            ) { publicacion ->
                                val esPropia = publicacion.userId == currentUserId

                                // 🔥 Optimización: Usar remember para evitar recálculos
                                val publicationData = remember(publicacion.id) {
                                    Publicacion(
                                        id = publicacion.id,
                                        categoria = publicacion.categoria,
                                        titulo = publicacion.titulo,
                                        descripcion = publicacion.descripcion,
                                        ubicacion = publicacion.ubicacion.manual,
                                        imagenUrl = publicacion.fotosUrl.firstOrNull() ?: ""
                                    )
                                }

                                CardPublicationItem(
                                    publicacion = publicationData,
                                    esPropia = esPropia,
                                    onClick = {
                                        navController.navigate("publicationDetail/${publicacion.id}")
                                    }
                                )
                            }

                            // 🔥 Espacio final para mejor scroll
                            item(span = { GridItemSpan(2) }) {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// 🆕 Contenido del Drawer
@Composable
fun DrawerContent(
    onHomeClick: () -> Unit,
    onConfigClick: () -> Unit,
    userProfile: UserProfileResponse? // 👈 Recibir perfil
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 🎨 Header del drawer con info del usuario
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE53E3E))
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Foto de perfil
                if (!userProfile?.profilePicUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = userProfile?.profilePicUrl,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Perfil",
                            tint = Color(0xFFE53E3E),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = userProfile?.name ?: "Cargando...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = userProfile?.email ?: "",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        NavigationDrawerItem(
            label = { Text("Home") },
            icon = { Icon(Icons.Default.Home, "Home") },
            selected = false,
            onClick = onHomeClick,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        NavigationDrawerItem(
            label = { Text("Configuración") },
            icon = { Icon(Icons.Default.Settings, "Configuración") },
            selected = false,
            onClick = onConfigClick,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardPublicationItem(
    publicacion: Publicacion,
    esPropia: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp, max = 280.dp), // 🔥 Altura flexible
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // 🔥 Imagen optimizada con estados de carga
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF7FAFC))
            ) {
                AsyncImage(
                    model = publicacion.imagenUrl.ifEmpty {
                        "https://via.placeholder.com/600x400/FFFFFF/E53E3E?text=Sin+imagen"
                    },
                    contentDescription = "Imagen publicación",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    placeholder = painterResource(id = R.drawable.image_not_found),
                    error = painterResource(id = R.drawable.image_not_found)
                )

                // Badge de categoría
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(Color(0xFFE53E3E), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = publicacion.categoria,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }

                // Indicador de publicación propia
                if (esPropia) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(24.dp)
                            .background(Color(0xFF4CAF50), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Tu publicación",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🔥 Mejor jerarquía de texto
            Text(
                text = publicacion.titulo,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D3748),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = publicacion.descripcion,
                fontSize = 12.sp,
                color = Color(0xFF718096),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 🔥 Información de ubicación
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ubication_img),
                    contentDescription = "Ubicación",
                    modifier = Modifier.size(12.dp),
                    colorFilter = null // 🔥 Esto evita que se aplique tint
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = publicacion.ubicacion,
                    fontSize = 10.sp,
                    color = Color(0xFF718096),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 🔥 Botón mejorado
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53E3E)
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    "Ver detalles",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

}

data class Publicacion(
    val id: String = "",
    val categoria: String,
    val titulo: String,
    val descripcion: String,
    val ubicacion: String,
    val imagenUrl: String = ""
)

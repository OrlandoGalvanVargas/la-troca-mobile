package com.example.latroca.ui.screens

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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import com.example.latroca.R
import com.example.latroca.ui.viewmodels.AuthViewModel
import com.example.latroca.ui.viewmodels.PostViewModel

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

    LaunchedEffect(Unit) {
        val token = authViewModel.getToken()
        if (!token.isNullOrBlank()) {
            postViewModel.loadPosts(token)
        }
    }

    val currentUserId = authViewModel.getUserId()
    var searchQuery by remember { mutableStateOf("") }

    val filteredPublicaciones = publicaciones.filter { publicacion ->
        searchQuery.isEmpty() ||
                publicacion.titulo.contains(searchQuery, ignoreCase = true) ||
                publicacion.descripcion.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.cloud_icon),
                            contentDescription = "Logo La Troca",
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Cerrar sesión", color = Color(0xFFE53E3E))
                    }
                }
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Buscar publicaciones...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = Color(0xFF718096)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE53E3E),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                )
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFE53E3E))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = "Publicaciones",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D3748)
                        )
                    }

                    items(filteredPublicaciones) { publicacion ->
                        val esPropia = publicacion.userId == currentUserId
                        CardPublicationItem(
                            publicacion = Publicacion(
                                id = publicacion.id,
                                categoria = publicacion.categoria,
                                titulo = publicacion.titulo,
                                descripcion = publicacion.descripcion,
                                ubicacion = publicacion.ubicacion.manual,
                                imagenUrl = publicacion.fotosUrl.firstOrNull() ?: ""
                            ),
                            esPropia = esPropia,
                            onClick = { navController.navigate("publicationDetail/${publicacion.id}") }
                        )
                    }
                }
            }
        }
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
            .height(260.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                AsyncImage(
                    model = publicacion.imagenUrl.ifEmpty { "https://via.placeholder.com/600x400.png?text=Sin+imagen" },
                    contentDescription = "Imagen publicación",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = publicacion.categoria,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE53E3E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = publicacion.titulo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D3748),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = publicacion.descripcion,
                    fontSize = 12.sp,
                    color = Color(0xFF718096),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53E3E),
                        contentColor = Color.White
                    )
                ) {
                    Text("Ver más", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (esPropia) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(26.dp)
                        .background(Color(0xFF4CAF50), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Tu publicación",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
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

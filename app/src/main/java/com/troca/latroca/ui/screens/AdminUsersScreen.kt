package com.troca.latroca.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.troca.latroca.ui.viewmodels.AuthViewModel
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val allUsers by authViewModel.allUsers.collectAsState()
    val isLoading by authViewModel.loadingUsers.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var isBackButtonEnabled by remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }

    val filteredUsers by remember(allUsers, searchQuery) {
        derivedStateOf {
            if (searchQuery.isEmpty()) {
                allUsers
            } else {
                val query = searchQuery.trim().lowercase()
                allUsers.filter { user ->
                    user.name.lowercase().contains(query) ||
                            user.email.lowercase().contains(query)
                }
            }
        }
    }

    fun handleBackNavigation() {
        if (isBackButtonEnabled) {
            isBackButtonEnabled = false
            navController.popBackStack()

            coroutineScope.launch {
                delay(500)
                isBackButtonEnabled = true
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!authViewModel.isAdmin()) {
            Toast.makeText(
                context,
                "No tienes permisos para acceder",
                Toast.LENGTH_SHORT
            ).show()
            navController.popBackStack()
        } else {
            authViewModel.loadAllUsers()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = Color(0xFFE53935)
                        )
                        Text(
                            "Usuarios Registrados",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { handleBackNavigation() },
                        enabled = isBackButtonEnabled
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = if (isBackButtonEnabled) Color(0xFFE53935) else Color(0xFFCBD5E0)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { authViewModel.loadAllUsers() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recargar",
                            tint = Color(0xFF718096)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF7FAFC))
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFE53935),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Cargando usuarios...",
                                color = Color(0xFF718096),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                allUsers.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonOff,
                                contentDescription = "Sin usuarios",
                                tint = Color(0xFFCBD5E0),
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No hay usuarios registrados",
                                fontSize = 16.sp,
                                color = Color(0xFF2D3748),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            placeholder = {
                                Text(
                                    "Buscar por nombre o correo...",
                                    color = Color(0xFFA0AEC0),
                                    fontSize = 14.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Buscar",
                                    tint = Color(0xFF718096)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Limpiar búsqueda",
                                            tint = Color(0xFF718096)
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE53935),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedTextColor = Color(0xFF2D3748),
                                unfocusedTextColor = Color(0xFF2D3748),
                                cursorColor = Color(0xFFE53935)
                            ),
                            singleLine = true
                        )

                        if (filteredUsers.isEmpty() && searchQuery.isNotEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SearchOff,
                                        contentDescription = "Sin resultados",
                                        tint = Color(0xFFCBD5E0),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No se encontraron usuarios",
                                        fontSize = 16.sp,
                                        color = Color(0xFF2D3748),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Intenta con otro término de búsqueda",
                                        fontSize = 14.sp,
                                        color = Color(0xFF718096)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 16.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    Surface(
                                        color = Color(0xFFE53935).copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = Color(0xFFE53935),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Column {
                                                    Text(
                                                        if (searchQuery.isEmpty())
                                                            "Total de usuarios"
                                                        else
                                                            "Resultados encontrados",
                                                        fontSize = 13.sp,
                                                        color = Color(0xFF718096),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        "${filteredUsers.size} ${if (filteredUsers.size == 1) "usuario" else "usuarios"}",
                                                        fontSize = 16.sp,
                                                        color = Color(0xFF2D3748),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            val adminCount = filteredUsers.count { it.role == "ADMIN" }
                                            val userCount = filteredUsers.count { it.role == "USER" }

                                            Column(
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Shield,
                                                        contentDescription = "Admin",
                                                        tint = Color(0xFFFFB300),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        "$adminCount Admin",
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF2D3748),
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = "User",
                                                        tint = Color(0xFF718096),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        "$userCount User",
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF718096)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                items(
                                    items = filteredUsers,
                                    key = { it.id }
                                ) { user ->
                                    UserItem(
                                        user = user,
                                        onViewClick = {
                                            navController.navigate("admin_user_detail/${user.id}")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserItem(
    user: com.troca.latroca.data.models.AdminUserResponse,
    onViewClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box {
                    if (!user.profilePicUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = user.profilePicUrl,
                            contentDescription = "Foto de ${user.name}",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color(0xFFE2E8F0), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFFF7FAFC), CircleShape)
                                .border(2.dp, Color(0xFFE2E8F0), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Sin foto",
                                tint = Color(0xFF718096),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    if (user.status == "active") {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.BottomEnd)
                                .background(Color(0xFF4CAF50), CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = user.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D3748),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (user.role == "ADMIN") {
                            Surface(
                                color = Color(0xFFFFD700).copy(alpha = 0.3f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Admin",
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "ADMIN",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFB300)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = user.email,
                        fontSize = 13.sp,
                        color = Color(0xFF718096),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!user.bio.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = user.bio,
                            fontSize = 12.sp,
                            color = Color(0xFFA0AEC0),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Button(
                onClick = onViewClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(40.dp)
                    .width(70.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    "Ver",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
package com.troca.latroca.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.troca.latroca.R
import com.troca.latroca.data.models.UserProfileResponse
import com.troca.latroca.ui.components.LoadingModal
import com.troca.latroca.ui.viewmodels.AuthViewModel
import com.troca.latroca.ui.viewmodels.ChatViewModel
import com.troca.latroca.ui.viewmodels.PostViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.datadog.android.rum.GlobalRumMonitor
import com.datadog.android.rum.RumActionType
import com.datadog.android.rum.RumResourceKind
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    onLogout: () -> Unit,
    authViewModel: AuthViewModel,
    postViewModel: PostViewModel,
    chatViewModel: ChatViewModel
) {
    // 🔥 TRACKING: Registrar entrada a la pantalla Home
    DisposableEffect(Unit) {
        val startTime = System.currentTimeMillis()

        GlobalRumMonitor.get().startView(
            key = "home_screen",
            name = "Home",
            attributes = mapOf(
                "screen_name" to "Home",
                "user_id" to authViewModel.getUserId()
            )
        )

        Log.d("DatadogRUM", "📱 Vista Home iniciada")

        onDispose {
            val timeSpent = System.currentTimeMillis() - startTime
            GlobalRumMonitor.get().stopView(
                key = "home_screen",
                attributes = mapOf(
                    "time_spent_ms" to timeSpent,
                    "time_spent_seconds" to (timeSpent / 1000)
                )
            )
            Log.d("DatadogRUM", "📱 Vista Home cerrada - Tiempo: ${timeSpent/1000}s")
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val publicaciones by postViewModel.posts.collectAsState()
    val isLoading by postViewModel.isLoading.collectAsState()
    val error by postViewModel.error.collectAsState()
    val token by authViewModel.currentToken.collectAsState()
    val userProfile by authViewModel.userProfile.collectAsState()
    val userChats by chatViewModel.userChats.collectAsState()

    val userRole by authViewModel.userRole.collectAsState()

    val isAdmin by remember(userRole) {
        derivedStateOf {
            userRole?.uppercase() == "ADMIN"
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentUserId = authViewModel.getUserId()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todas") }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var isLoggingOut by remember { mutableStateOf(false) }
    val isRefreshingInBackground by postViewModel.isRefreshingInBackground.collectAsState()

    val gridState = rememberLazyGridState()

    val categorias = listOf(
        "Todas",
        "Electrónicos",
        "Muebles",
        "Ropa y Accesorios",
        "Deportes y Fitness",
        "Libros y Revistas",
        "Juguetes y Juegos",
        "Hogar y Jardín",
        "Herramientas",
        "Vehículos y Accesorios",
        "Arte y Manualidades",
        "Música e Instrumentos",
        "Mascotas y Accesorios",
        "Alimentos y Bebidas",
        "Salud y Belleza",
        "Otro"
    )

    LaunchedEffect(Unit) {
        if (drawerState.isOpen) {
            drawerState.close()
        }
    }

    fun reloadPosts() {
        if (!token.isNullOrBlank()) {
            GlobalRumMonitor.get().addAction(
                type = RumActionType.TAP,
                name = "reload_publications",
                attributes = mapOf(
                    "current_posts_count" to publicaciones.size
                )
            )
            postViewModel.forceRefresh(token!!)
        }
    }

    LaunchedEffect(token) {
        if (!token.isNullOrBlank()) {
            postViewModel.loadPosts(token!!)
            authViewModel.loadUserProfile()
        }
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            chatViewModel.listenToUserChats(currentUserId)
        }
    }

    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Open) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }
// 🔥 DATADOG: Actualizar info del usuario cuando carga el perfil
    LaunchedEffect(userProfile) {
        if (userProfile != null) {
            GlobalRumMonitor.get().apply {
                addAttribute("user_name", userProfile!!.name)
                addAttribute("user_email", userProfile!!.email)
                addAttribute("user_role", userRole ?: "user")
                addAttribute("has_profile_picture", (userProfile!!.profilePicUrl?.isNotEmpty() == true).toString())
                addAttribute("posts_count", publicaciones.count { it.userId == currentUserId }.toString())
            }

            Log.d("DatadogRUM", "👤 Perfil de usuario actualizado en Datadog")
        }
    }
    // 🔥 DATADOG: Track errores al cargar publicaciones
    LaunchedEffect(error) {
        if (!error.isNullOrBlank()) {
            GlobalRumMonitor.get().addAction(
                type = RumActionType.CUSTOM,
                name = "load_posts_error",
                attributes = mapOf(
                    "error_message" to error,
                    "user_id" to currentUserId
                )
            )

            Log.e("DatadogRUM", "❌ Error cargando publicaciones: $error")
        }
    }
    fun processLogout() {
        coroutineScope.launch {
            try {
                isLoggingOut = true
                // 🔥 DATADOG: Registrar logout
                GlobalRumMonitor.get().addAction(
                    type = RumActionType.TAP,
                    name = "logout",
                    attributes = mapOf(
                        "user_id" to currentUserId,
                        "source" to "home_screen"
                    )
                )
                onLogout()
                delay(500)

                // 🔥 DATADOG: Limpiar atributos del usuario
                GlobalRumMonitor.get().apply {
                    removeAttribute("user_id")
                    removeAttribute("user_email")
                    removeAttribute("user_name")
                    removeAttribute("user_role")
                    removeAttribute("login_method")
                    removeAttribute("has_profile_picture")
                    removeAttribute("posts_count")
                }

                Log.d("DatadogRUM", "👤 Atributos de usuario eliminados de Datadog")

                isLoggingOut = false
                showLogoutDialog = false
                showSuccessDialog = true
            } catch (_: Exception) {
                isLoggingOut = false
                Toast.makeText(
                    context,
                    "Error al cerrar sesión. Intenta nuevamente.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    LoadingModal(
        isVisible = isLoggingOut,
        message = "Cerrando sesión...",
        timeoutSeconds = 5
    )

    val filteredPublicaciones by remember(publicaciones, searchQuery, selectedCategory) {
        derivedStateOf {
            var filtered = publicaciones

            if (searchQuery.isNotEmpty()) {
                val query = searchQuery.trim().lowercase()
                filtered = filtered.filter { publicacion ->
                    publicacion.titulo.lowercase().contains(query) ||
                            publicacion.categoria.lowercase().contains(query)
                }
            }

            if (selectedCategory != "Todas") {
                filtered = filtered.filter { it.categoria == selectedCategory }
            }

            filtered
        }
    }

    val totalUnreadCount by remember(userChats, currentUserId) {
        derivedStateOf {
            userChats.sumOf { chat ->
                chat.unreadCount[currentUserId] ?: 0
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                modifier = Modifier.widthIn(max = screenWidth * 0.85f)
            ) {
                DrawerContent(
                    onHomeClick = {
                        GlobalRumMonitor.get().addAction(
                            type = RumActionType.TAP,
                            name = "drawer_navigation",
                            attributes = mapOf("destination" to "home")
                        )
                        scope.launch { drawerState.close() }
                    },
                    onMessagesClick = {
                        GlobalRumMonitor.get().addAction(
                            type = RumActionType.TAP,
                            name = "drawer_navigation",
                            attributes = mapOf("destination" to "chat_list")
                        )
                        scope.launch {
                            drawerState.close()
                            navController.navigate("chat_list")
                        }
                    },
                    onUsersClick = {
                        GlobalRumMonitor.get().addAction(
                            type = RumActionType.TAP,
                            name = "drawer_navigation",
                            attributes = mapOf("destination" to "admin_users")
                        )
                        scope.launch {
                            drawerState.close()
                            navController.navigate("admin_users")
                        }
                    },
                    onConfigClick = {
                        GlobalRumMonitor.get().addAction(
                            type = RumActionType.TAP,
                            name = "drawer_navigation",
                            attributes = mapOf("destination" to "settings")
                        )
                        scope.launch {
                            drawerState.close()
                            navController.navigate("settings")
                        }
                    },
                    userProfile = userProfile,
                    unreadMessagesCount = totalUnreadCount,
                    isAdmin = isAdmin
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
                                onClick = {
                                    scope.launch {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        drawerState.open()
                                    }
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (totalUnreadCount > 0) {
                                            Badge(
                                                containerColor = Color(0xFFE53935),
                                                modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (totalUnreadCount > 9) "9+" else totalUnreadCount.toString(),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Menú",
                                        tint = Color(0xFF2D3748)
                                    )
                                }
                            }

                            Image(
                                painter = painterResource(id = R.drawable.la_troca_logo_2),
                                contentDescription = "Logo La Troca",
                                modifier = Modifier.size(60.dp)
                            )

                            IconButton(
                                onClick = {
                                    if (!isLoggingOut) {
                                        showLogoutDialog = true
                                    }
                                },
                                modifier = Modifier.size(48.dp),
                                enabled = !isLoggingOut
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Cerrar sesión",
                                    tint = if (!isLoggingOut) Color(0xFFE53935) else Color(0xFFCBD5E0)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { GlobalRumMonitor.get().addAction(
                        type = RumActionType.TAP,
                            name = "create_new_publication",
                        attributes = mapOf(
                            "source" to "home_fab"
                        )
                    )
                        navController.navigate("newPublication") },
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Nueva publicación",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF7FAFC))
            ) {
                // Campo de búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { newQuery ->
                        searchQuery = newQuery

                        // Track la acción de búsqueda
                        if (newQuery.isNotEmpty()) {
                            GlobalRumMonitor.get().addAction(
                                type = RumActionType.CUSTOM,
                                name = "search_publications",
                                attributes = mapOf(
                                    "search_query" to newQuery,
                                    "query_length" to newQuery.length
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = {
                        Text(
                            "Buscar publicaciones...",
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

                // Filtro de Categoría - Más pegado al input
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    OutlinedButton(
                        onClick = { showCategoryMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedCategory != "Todas")
                                Color(0xFFE53935).copy(alpha = 0.1f)
                            else Color.White,
                            contentColor = if (selectedCategory != "Todas")
                                Color(0xFFE53935)
                            else Color(0xFF2D3748)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 1.5.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (selectedCategory != "Todas")
                                    Color(0xFFE53935)
                                else Color(0xFFE2E8F0)
                            )
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Categoría",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (selectedCategory == "Todas")
                                        "Categoría"
                                    else selectedCategory,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (selectedCategory != "Todas")
                                        FontWeight.Bold
                                    else FontWeight.Normal
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false },
                        modifier = Modifier
                            .background(Color.White)
                            .heightIn(max = 400.dp)
                    ) {
                        categorias.forEach { categoria ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (categoria == selectedCategory) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFFE53935),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        } else {
                                            Spacer(modifier = Modifier.width(26.dp))
                                        }
                                        Text(
                                            text = categoria,
                                            fontSize = 14.sp,
                                            fontWeight = if (categoria == selectedCategory)
                                                FontWeight.Bold
                                            else FontWeight.Normal,
                                            color = if (categoria == selectedCategory)
                                                Color(0xFFE53935)
                                            else Color(0xFF2D3748)
                                        )
                                    }
                                },
                                onClick = {
                                    val previousCategory = selectedCategory  // ⬅️ Guardar antes
                                    selectedCategory = categoria
                                    showCategoryMenu = false
                                    // Track selección de categoría
                                    GlobalRumMonitor.get().addAction(
                                        type = RumActionType.TAP,
                                        name = "filter_by_category",
                                        attributes = mapOf(
                                            "category" to categoria,
                                            "previous_category" to previousCategory
                                        )
                                    )
                                },
                                modifier = Modifier.background(
                                    if (categoria == selectedCategory)
                                        Color(0xFFE53935).copy(alpha = 0.05f)
                                    else Color.Transparent
                                )
                            )
                        }
                    }
                }

                // Chips de filtros activos - Rediseñados para evitar desbordamiento
                if (selectedCategory != "Todas" || searchQuery.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Filtros activos:",
                                fontSize = 12.sp,
                                color = Color(0xFF718096),
                                fontWeight = FontWeight.Medium
                            )

                            TextButton(
                                onClick = {
                                    selectedCategory = "Todas"
                                    searchQuery = ""
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "Limpiar todo",
                                    fontSize = 11.sp,
                                    color = Color(0xFFE53935),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Chips en una fila con scroll horizontal si es necesario
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (searchQuery.isNotEmpty()) {
                                FilterChip(
                                    selected = true,
                                    onClick = { searchQuery = "" },
                                    label = {
                                        Text(
                                            "Búsqueda: \"${searchQuery.take(15)}${if(searchQuery.length > 15) "..." else ""}\"",
                                            fontSize = 11.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFE53935).copy(alpha = 0.1f),
                                        selectedLabelColor = Color(0xFFE53935)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = Color(0xFFE53935)
                                    )
                                )
                            }

                            if (selectedCategory != "Todas") {
                                FilterChip(
                                    selected = true,
                                    onClick = { selectedCategory = "Todas" },
                                    label = { Text(selectedCategory, fontSize = 11.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFE53935).copy(alpha = 0.1f),
                                        selectedLabelColor = Color(0xFFE53935)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = Color(0xFFE53935)
                                    )
                                )
                            }
                        }
                    }
                }

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
                                    text = "Cargando publicaciones...",
                                    color = Color(0xFF718096),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    !error.isNullOrBlank() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Error al cargar publicaciones",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2D3748),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = error ?: "Error desconocido",
                                    fontSize = 14.sp,
                                    color = Color(0xFF718096),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { reloadPosts() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE53935)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Reintentar",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    token.isNullOrBlank() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Sin autenticación",
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Sesión inválida",
                                    fontSize = 18.sp,
                                    color = Color(0xFF2D3748),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Por favor, inicia sesión nuevamente",
                                    fontSize = 14.sp,
                                    color = Color(0xFF718096),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { onLogout() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE53935)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Ir a Login", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    filteredPublicaciones.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.not_found),
                                    contentDescription = "Sin publicaciones",
                                    tint = Color(0xFFCBD5E0),
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty() || selectedCategory != "Todas")
                                        "No se encontraron resultados"
                                    else
                                        "No hay publicaciones disponibles",
                                    fontSize = 16.sp,
                                    color = Color(0xFF2D3748),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty() || selectedCategory != "Todas")
                                        "Intenta con otros filtros"
                                    else
                                        "Sé el primero en publicar algo",
                                    fontSize = 14.sp,
                                    color = Color(0xFF718096),
                                    textAlign = TextAlign.Center
                                )

                                if (searchQuery.isEmpty() && selectedCategory == "Todas") {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    OutlinedButton(
                                        onClick = { reloadPosts() },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFFE53935)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        border = ButtonDefaults.outlinedButtonBorder.copy(
                                            width = 2.dp
                                        ),
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Recargar",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            state = gridState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 88.dp
                            )
                        ) {
                            item(span = { GridItemSpan(2) }) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Publicaciones (${filteredPublicaciones.size})",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2D3748)
                                    )

                                    IconButton(
                                        onClick = { reloadPosts() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Recargar",
                                            tint = Color(0xFF718096),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            items(
                                items = filteredPublicaciones,
                                key = { it.id }
                            ) { publicacion ->
                                val esPropia = publicacion.userId == currentUserId

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
// Track click en publicación
                                        GlobalRumMonitor.get().addAction(
                                            type = RumActionType.TAP,
                                            name = "view_publication",
                                            attributes = mapOf(
                                                "publication_id" to publicacion.id,
                                                "publication_title" to publicacion.titulo,
                                                "publication_category" to publicacion.categoria,
                                                "is_own_post" to esPropia
                                            )
                                        )

                                        navController.navigate("publicationDetail/${publicacion.id}")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLoggingOut) showLogoutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "¿Cerrar sesión?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que quieres cerrar sesión?",
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                // 🆕 Layout responsivo para botones
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { processLogout() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53E3E)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoggingOut
                    ) {
                        Text(
                            "Sí, cerrar sesión",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { showLogoutDialog = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF2D3748)
                        ),
                        enabled = !isLoggingOut
                    ) {
                        Text(
                            "Cancelar",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            dismissButton = {}, // Vacío porque los botones están en confirmButton
            containerColor = Color.White,
            tonalElevation = 4.dp,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f) // 90% del ancho en pantallas pequeñas
                .padding(horizontal = 16.dp)
        )
    }

// 2️⃣ Diálogo de Éxito (mejorado con responsividad)
    if (showSuccessDialog) {
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
                    "¡Sesión cerrada!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    "Has cerrado sesión exitosamente",
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {},
            dismissButton = {},
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(horizontal = 16.dp)
        )
    }
}

@ExperimentalMaterial3Api
@Composable
fun DrawerContent(
    onHomeClick: () -> Unit,
    onMessagesClick: () -> Unit,
    onUsersClick: () -> Unit,
    onConfigClick: () -> Unit,
    userProfile: UserProfileResponse?,
    unreadMessagesCount: Int = 0,
    isAdmin: Boolean = false
) {
    LaunchedEffect(isAdmin) {
        Log.d("DrawerContent", "🎨 Recomposición - isAdmin: $isAdmin")
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE53935))
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!userProfile?.profilePicUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = userProfile.profilePicUrl,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Perfil",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = userProfile?.name ?: "Cargando...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = userProfile?.email ?: "",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = Color(0xFFFFD700),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            Text(
                                text = "⭐ ADMIN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D3748),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        NavigationDrawerItem(
            label = { Text("Home", fontSize = 16.sp) },
            icon = { Icon(Icons.Default.Home, "Home") },
            selected = false,
            onClick = onHomeClick,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent
            )
        )

        NavigationDrawerItem(
            label = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mensajes", fontSize = 16.sp)
                    if (unreadMessagesCount > 0) {
                        Badge(
                            containerColor = Color(0xFFE53935),
                            contentColor = Color.White
                        ) {
                            Text(
                                text = if (unreadMessagesCount > 9) "9+" else unreadMessagesCount.toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            icon = { Icon(Icons.Default.Chat, "Mensajes") },
            selected = false,
            onClick = onMessagesClick,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent
            )
        )

        if (isAdmin) {
            NavigationDrawerItem(
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Usuarios", fontSize = 16.sp)
                        Surface(
                            color = Color(0xFFFFD700).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "ADMIN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB300),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "Usuarios"
                    )
                },
                selected = false,
                onClick = onUsersClick,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color.Transparent
                )
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Divider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = Color(0xFFE2E8F0)
        )

        NavigationDrawerItem(
            label = { Text("Configuración", fontSize = 16.sp) },
            icon = { Icon(Icons.Default.Settings, "Configuración") },
            selected = false,
            onClick = onConfigClick,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
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
            .heightIn(min = 240.dp, max = 280.dp),
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
                    placeholder = painterResource(id = R.drawable.image_not_found),
                    error = painterResource(id = R.drawable.image_not_found)
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    color = Color(0xFFE53935),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = publicacion.categoria,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }

                if (esPropia) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(28.dp),
                        color = Color(0xFF4CAF50),
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Tu publicación",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = publicacion.titulo,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D3748),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = publicacion.descripcion,
                fontSize = 12.sp,
                color = Color(0xFF718096),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ubication_img),
                    contentDescription = "Ubicación",
                    modifier = Modifier.size(12.dp)
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

            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935)
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    "Ver detalles",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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
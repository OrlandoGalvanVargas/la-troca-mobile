package com.troca.latroca.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.troca.latroca.domain.models.AuthResult
import com.troca.latroca.ui.viewmodels.AuthViewModel
import com.troca.latroca.ui.viewmodels.PostViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    postViewModel: PostViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val userProfile by authViewModel.userProfile.collectAsState()
    val updateProfileState by authViewModel.updateProfileState.collectAsState()
    val changePasswordState by authViewModel.changePasswordState.collectAsState()

    var nombre by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var ubicacion by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageFile by remember { mutableStateOf<File?>(null) }

    var nombreError by remember { mutableStateOf("") }
    var bioError by remember { mutableStateOf("") }
    var ubicacionError by remember { mutableStateOf("") }
    var isGettingLocation by remember { mutableStateOf(false) }

    var imagenSegura by remember { mutableStateOf<Boolean?>(null) }
    var isValidatingImage by remember { mutableStateOf(false) }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showPasswordWarning by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isChangingPassword by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userProfile) {
        userProfile?.let { profile ->
            nombre = profile.name
            bio = profile.bio ?: ""
            ubicacion = profile.location?.manual ?: ""
            latitude = profile.location?.latitude ?: 0.0
            longitude = profile.location?.longitude ?: 0.0
        }
    }

    LaunchedEffect(selectedImageUri) {
        val token = authViewModel.getToken() ?: return@LaunchedEffect
        selectedImageUri?.let {
            isValidatingImage = true
            imagenSegura = null
            imagenSegura = postViewModel.analyzeImage(context, token, it).first
            isValidatingImage = false
        }
    }

    LaunchedEffect(changePasswordState) {
        when (val state = changePasswordState) {
            is AuthResult.Success -> {
                isChangingPassword = false
                showPasswordDialog = false
                showSuccessDialog = true
                authViewModel.resetChangePasswordState()

                delay(1000)
                onLogout()
            }
            is AuthResult.Error -> {
                isChangingPassword = false
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                authViewModel.resetChangePasswordState()
            }
            is AuthResult.Loading -> {
                isChangingPassword = true
            }
            else -> {}
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val file = File(context.cacheDir, "profile_${System.currentTimeMillis()}.jpg")
                inputStream?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                selectedImageFile = file
            } catch (_: Exception) {
                Toast.makeText(context, "Error al cargar imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            getLocation(context) { lat, lon, address ->
                latitude = lat
                longitude = lon
                ubicacion = address
                isGettingLocation = false
            }
        } else {
            isGettingLocation = false
            Toast.makeText(context, "Permisos de ubicación denegados", Toast.LENGTH_SHORT).show()
        }
    }

    fun validateNombre(): Boolean {
        nombreError = when {
            nombre.isBlank() -> "El nombre es obligatorio"
            nombre.length < 3 -> "El nombre debe tener al menos 3 caracteres"
            nombre.length > 25 -> "El nombre no puede exceder 25 caracteres"
            else -> ""
        }
        return nombreError.isEmpty()
    }

    fun validateBio(): Boolean {
        val lineBreaks = bio.count { it == '\n' }
        val wordCount = bio.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
        bioError = when {
            bio.isBlank() -> "La biografía es obligatoria"
            wordCount < 3 -> "La biografía debe tener al menos 3 palabras"
            bio.length > 130 -> "La biografía no puede exceder 130 caracteres"
            lineBreaks > 3 -> "Máximo 4 saltos de línea permitidos"
            else -> ""
        }
        return bioError.isEmpty()
    }

    fun validateUbicacion(): Boolean {
        ubicacionError = when {
            ubicacion.isBlank() -> "La ubicación es obligatoria"
            ubicacion.length < 3 -> "La ubicación debe tener al menos 3 caracteres"
            else -> ""
        }
        return ubicacionError.isEmpty()
    }

    fun validatePassword(): Boolean {
        passwordError = when {
            newPassword.isBlank() -> "La contraseña es requerida"
            newPassword.length < 8 -> "Mínimo 8 caracteres"
            confirmPassword.isBlank() -> "Confirma tu contraseña"
            newPassword != confirmPassword -> "Las contraseñas no coinciden"
            else -> ""
        }
        return passwordError.isEmpty()
    }

    fun requestLocation() {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                isGettingLocation = true
                getLocation(context) { lat, lon, address ->
                    latitude = lat
                    longitude = lon
                    ubicacion = address
                    isGettingLocation = false
                }
            }
            else -> {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    LaunchedEffect(updateProfileState) {
        when (val state = updateProfileState) {
            is AuthResult.Success -> {
                Toast.makeText(context, state.data, Toast.LENGTH_SHORT).show()
                authViewModel.resetUpdateProfileState()
                navController.popBackStack()
            }
            is AuthResult.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                authViewModel.resetUpdateProfileState()
            }
            else -> {}
        }
    }

    if (showPasswordWarning) {
        AlertDialog(
            onDismissRequest = { showPasswordWarning = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFFA726),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Importante",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    "Al cambiar tu contraseña, tu sesión se cerrará automáticamente. " +
                            "Deberás iniciar sesión nuevamente con tu nueva contraseña.",
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPasswordWarning = false
                        showPasswordDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Entendido", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordWarning = false }) {
                    Text("Cancelar", color = Color(0xFF718096))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isChangingPassword) {
                    showPasswordDialog = false
                    newPassword = ""
                    confirmPassword = ""
                    passwordError = ""
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    "Cambiar Contraseña",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            passwordError = ""
                        },
                        label = { Text("Nueva contraseña") },
                        placeholder = { Text("Mínimo 8 caracteres") },
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible)
                                        Icons.Default.Visibility
                                    else
                                        Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Ocultar" else "Mostrar"
                                )
                            }
                        },
                        singleLine = true,
                        isError = passwordError.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE53935),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            cursorColor = Color(0xFFE53935)
                        ),
                        enabled = !isChangingPassword
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            passwordError = ""
                        },
                        label = { Text("Confirmar contraseña") },
                        placeholder = { Text("Repite tu contraseña") },
                        visualTransformation = if (confirmPasswordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible)
                                        Icons.Default.Visibility
                                    else
                                        Icons.Default.VisibilityOff,
                                    contentDescription = if (confirmPasswordVisible) "Ocultar" else "Mostrar"
                                )
                            }
                        },
                        singleLine = true,
                        isError = passwordError.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE53935),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            cursorColor = Color(0xFFE53935)
                        ),
                        enabled = !isChangingPassword
                    )

                    if (passwordError.isNotBlank()) {
                        Text(
                            text = passwordError,
                            color = Color.Red,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (validatePassword()) {
                            authViewModel.changePassword(newPassword)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isChangingPassword
                ) {
                    if (isChangingPassword) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Cambiar", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasswordDialog = false
                        newPassword = ""
                        confirmPassword = ""
                        passwordError = ""
                    },
                    enabled = !isChangingPassword
                ) {
                    Text("Cancelar", color = Color(0xFF718096))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

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
                    "¡Contraseña actualizada!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Tu contraseña se cambió correctamente.",
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Cerrando sesión...",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = Color(0xFF718096)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color(0xFFE53935),
                        strokeWidth = 3.dp
                    )
                }
            },
            confirmButton = {},
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Editar Perfil",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFFE53935)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF7FAFC))
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Foto de perfil:",
                color = Color(0xFFE53935),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color(0xFFE53935), CircleShape)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Nueva foto",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (!userProfile?.profilePicUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = userProfile?.profilePicUrl,
                        contentDescription = "Foto actual",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Sin foto",
                        modifier = Modifier.size(60.dp),
                        tint = Color(0xFF718096)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.BottomEnd)
                        .background(Color(0xFFE53935), CircleShape)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Cambiar foto",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
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
                            text = "Imagen con contenido inapropiado",
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

            Spacer(modifier = Modifier.height(32.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Nombre",
                        color = Color(0xFFE53935),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color(0xFF718096),
                        modifier = Modifier.size(18.dp)
                    )
                }

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { newText ->
                        val formattedText = formatNombreText(nombre, newText)
                        nombre = formattedText
                        validateNombre()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    isError = nombreError.isNotBlank(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (nombreError.isNotBlank()) Color.Red else Color(0xFFE53935),
                        unfocusedBorderColor = if (nombreError.isNotBlank()) Color.Red else Color(0xFFE2E8F0),
                        focusedTextColor = Color(0xFF2D3748),
                        unfocusedTextColor = Color(0xFF2D3748),
                        cursorColor = Color(0xFFE53935)
                    ),
                    placeholder = {
                        Text("Ingresa tu nombre", color = Color(0xFFA0AEC0))
                    },
                    supportingText = {
                        Text(
                            text = "${nombre.length}/25",
                            fontSize = 11.sp,
                            color = Color(0xFF718096)
                        )
                    }
                )

                if (nombreError.isNotBlank()) {
                    Text(
                        text = nombreError,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Biografía",
                        color = Color(0xFFE53935),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color(0xFF718096),
                        modifier = Modifier.size(18.dp)
                    )
                }

                OutlinedTextField(
                    value = bio,
                    onValueChange = { newText ->
                        val formattedText = formatBioText(bio, newText)
                        if (formattedText.length <= 130) {
                            bio = formattedText
                            validateBio()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 150.dp),
                    shape = RoundedCornerShape(12.dp),
                    isError = bioError.isNotBlank(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (bioError.isNotBlank()) Color.Red else Color(0xFFE53935),
                        unfocusedBorderColor = if (bioError.isNotBlank()) Color.Red else Color(0xFFE2E8F0),
                        focusedTextColor = Color(0xFF2D3748),
                        unfocusedTextColor = Color(0xFF2D3748),
                        cursorColor = Color(0xFFE53935)
                    ),
                    placeholder = {
                        Text("Cuéntanos sobre ti...", color = Color(0xFFA0AEC0))
                    },
                    maxLines = 5
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (bioError.isNotBlank()) {
                        Text(
                            text = bioError,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${bio.length}/130",
                            color = Color(0xFF718096),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Ubicación",
                        color = Color(0xFFE53935),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color(0xFF718096),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "Escribe tu ubicación o usa el GPS",
                    fontSize = 14.sp,
                    color = Color(0xFF718096),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = ubicacion,
                        onValueChange = {
                            ubicacion = it
                            validateUbicacion()
                        },
                        enabled = false,
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Ubicación",
                                tint = Color(0xFF718096)
                            )
                        },
                        isError = ubicacionError.isNotBlank(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (ubicacionError.isNotBlank()) Color.Red else Color(0xFFE53935),
                            unfocusedBorderColor = if (ubicacionError.isNotBlank()) Color.Red else Color(0xFFE2E8F0),
                            focusedTextColor = Color(0xFF2D3748),
                            unfocusedTextColor = Color(0xFF2D3748),
                            cursorColor = Color(0xFFE53935),
                            disabledTextColor = Color(0xFF2D3748),
                            disabledPlaceholderColor = Color(0xFFA0AEC0),
                            disabledLeadingIconColor = Color(0xFFA0AEC0),
                            disabledTrailingIconColor = Color(0xFFA0AEC0)
                        ),
                        placeholder = {
                            Text("Ingresa tu ubicación", color = Color(0xFFA0AEC0))
                        }
                    )

                    OutlinedButton(
                        onClick = { requestLocation() },
                        modifier = Modifier
                            .height(56.dp)
                            .width(80.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFF7FAFC)
                        ),
                        enabled = !isGettingLocation
                    ) {
                        if (isGettingLocation) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFFE53935)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Usar GPS",
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                if (ubicacionError.isNotBlank()) {
                    Text(
                        text = ubicacionError,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Contraseña",
                        color = Color(0xFFE53935),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF718096),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "Actualiza tu contraseña de forma segura",
                    fontSize = 14.sp,
                    color = Color(0xFF718096),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = "••••••••",
                        onValueChange = {},
                        enabled = false,
                        readOnly = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Contraseña",
                                tint = Color(0xFF718096)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = Color(0xFFE2E8F0),
                            disabledTextColor = Color(0xFF718096),
                            disabledLeadingIconColor = Color(0xFF718096)
                        )
                    )

                    Button(
                        onClick = { showPasswordWarning = true },
                        modifier = Modifier
                            .height(48.dp)
                            .width(90.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            "Cambiar",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            val isImageSafe = imagenSegura != false && !isValidatingImage
            val canSave = isImageSafe

            Button(
                onClick = {
                    if (validateNombre() && validateBio() && validateUbicacion() && canSave) {
                        if (latitude == 0.0 || longitude == 0.0) {
                            Toast.makeText(
                                context,
                                "Por favor, obtén tu ubicación GPS",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        authViewModel.updateProfile(
                            nombre = nombre.takeIf { it != userProfile?.name },
                            bio = bio.takeIf { it != userProfile?.bio },
                            ubicacion = ubicacion,
                            latitude = latitude,
                            longitude = longitude,
                            imageFile = selectedImageFile
                        )
                    } else {
                        Toast.makeText(
                            context,
                            when {
                                !canSave -> "La imagen contiene contenido inapropiado o está validándose"
                                else -> "Por favor, completa correctamente todos los campos"
                            },
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canSave) Color(0xFFE53935) else Color(0xFFB0BEC5),
                    disabledContainerColor = Color(0xFFB0BEC5)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = updateProfileState !is AuthResult.Loading && canSave
            ) {
                if (updateProfileState is AuthResult.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Guardar Cambios",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun getLocation(
    context: android.content.Context,
    onLocationReceived: (latitude: Double, longitude: Double, address: String) -> Unit
) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    try {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val geocoder = Geocoder(context, Locale.getDefault())
                try {
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    val address = if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        "${addr.locality ?: ""}, ${addr.adminArea ?: ""}, ${addr.countryName ?: ""}".trim(',', ' ')
                    } else {
                        "${location.latitude}, ${location.longitude}"
                    }
                    onLocationReceived(location.latitude, location.longitude, address)
                } catch (_: Exception) {
                    onLocationReceived(location.latitude, location.longitude, "Ubicación desconocida")
                }
            } else {
                Toast.makeText(context, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Error al obtener ubicación", Toast.LENGTH_SHORT).show()
        }
    } catch (_: SecurityException) {
        Toast.makeText(context, "Sin permisos de ubicación", Toast.LENGTH_SHORT).show()
    }
}

private fun formatBioText(currentText: String, newText: String): String {
    if (newText.isEmpty()) return ""

    if (newText.length < currentText.length) {
        return newText
    }

    val isAddingNewLine = newText.endsWith('\n') && !currentText.endsWith('\n')
    val hasContent = currentText.any { it != ' ' && it != '\n' }

    if (!hasContent) {
        if (newText.first().isWhitespace()) {
            return ""
        }
    }

    if (isAddingNewLine) {
        return currentText
    }

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

private fun formatNombreText(currentText: String, newText: String): String {
    if (newText.isEmpty()) return ""

    if (newText.length < currentText.length) {
        return newText
    }

    if (newText.length > 25) {
        return currentText
    }

    val hasContent = currentText.any { it != ' ' }

    if (!hasContent) {
        if (newText.first().isWhitespace()) {
            return currentText
        }
    }

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
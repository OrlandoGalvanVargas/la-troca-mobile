package com.troca.latroca.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.troca.latroca.data.repository.ChatRepository
import com.troca.latroca.ui.viewmodels.AuthViewModel
import com.troca.latroca.ui.viewmodels.ChatViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.res.painterResource
import com.troca.latroca.R
import androidx.compose.foundation.Image
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.TextButton
import androidx.core.content.ContextCompat
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatConversationScreen(
    navController: NavController,
    chatId: String,
    otherUserName: String,
    otherUserId: String,
    chatViewModel: ChatViewModel,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val currentUserId = authViewModel.getUserId()
    val currentUserName = authViewModel.userProfile.value?.name ?: "Usuario"

    var showNotificationBanner by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            showNotificationBanner = !granted
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        showNotificationBanner = !isGranted

        if (isGranted) {
            Toast.makeText(context, "Notificaciones activadas", Toast.LENGTH_SHORT).show()
        }
    }

    val messages by chatViewModel.messages.collectAsState()
    val isOtherUserTyping by chatViewModel.isOtherUserTyping.collectAsState()
    val error by chatViewModel.error.collectAsState()

    var messageText by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(chatId) {
        chatViewModel.listenToMessages(chatId, currentUserId)
        chatViewModel.listenToTypingStatus(chatId, otherUserId)
    }



    DisposableEffect(Unit) {
        onDispose {
            chatViewModel.clearMessages()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            chatViewModel.clearError()
        }
    }
    LaunchedEffect(messages.size) {
        listState.animateScrollToItem(0)
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Eliminar conversación",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D3748)
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas eliminar esta conversación?\n\n" +
                            "Si el otro usuario aún tiene el chat, podrá enviarte mensajes " +
                            "y la conversación se reactivará.",
                    color = Color(0xFF4A5568),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        chatViewModel.hideChatForUser(
                            chatId = chatId,
                            userId = currentUserId
                        ) {
                            Toast.makeText(
                                context,
                                "Conversación eliminada",
                                Toast.LENGTH_SHORT
                            ).show()
                            navController.popBackStack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "Sí, eliminar",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "Cancelar",
                        color = Color(0xFF718096),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = otherUserName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color(0xFF2D3748)
                        )
                        if (isOtherUserTyping) {
                            Text(
                                text = "escribiendo...",
                                fontSize = 12.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
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
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar conversación",
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
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.chat_icon),
                                contentDescription = "Icono de chat vacío",
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No hay mensajes aún",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF718096)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Envía el primer mensaje a $otherUserName",
                                fontSize = 14.sp,
                                color = Color(0xFF718096),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 80.dp
                        ),
                        verticalArrangement = Arrangement.Bottom,  // ← Los nuevos mensajes van abajo
                        reverseLayout = true                       // ← Invierte el orden visual
                    ) {
                        items(
                            items = messages,
                            key = { it.id }
                        ) { message ->
                            MessageBubble(
                                message = message,
                                isCurrentUser = message.senderId == currentUserId,
                                chatRepository = ChatRepository()
                            )
                        }
                    }
                }
                if (showNotificationBanner) {
                    NotificationBanner(
                        onActivateNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onDismiss = {
                            showNotificationBanner = false
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = {
                            messageText = it
                            if (it.isNotBlank()) {
                                chatViewModel.setTypingStatus(chatId, currentUserId, true)
                            } else {
                                chatViewModel.setTypingStatus(chatId, currentUserId, false)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp, max = 100.dp),
                        placeholder = {
                            Text(
                                "Escribe un mensaje...",
                                color = Color(0xFFB0BEC5),
                                fontSize = 14.sp
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE53935),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedTextColor = Color(0xFF2D3748),
                            unfocusedTextColor = Color(0xFF2D3748),
                            cursorColor = Color(0xFFE53935)
                        ),
                        maxLines = 3,
                        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
                    )

                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                val token = authViewModel.getToken() ?: ""
                                chatViewModel.sendMessage(
                                    chatId = chatId,
                                    senderId = currentUserId,
                                    senderName = currentUserName,
                                    text = messageText,
                                    receiverId = otherUserId,
                                    token = token
                                )
                                messageText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (messageText.isNotBlank()) Color(0xFFE53935) else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Enviar",
                            tint = if (messageText.isNotBlank()) Color.White else Color(0xFF718096),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationBanner(
    onActivateNotifications: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF0F9FF),
        border = BorderStroke(1.dp, Color(0xFFE0F2FE)),
        shadowElevation = 6.dp,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notificaciones",
                    tint = Color(0xFF0EA5E9),
                    modifier = Modifier
                        .size(32.dp)
                        .padding(end = 12.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notificaciones desactivadas",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0369A1),
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Actívalas para recibir alertas de nuevos mensajes",
                        color = Color(0xFF475569),
                        fontSize = 13.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(start = 12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE2E8F0),
                        contentColor = Color(0xFF475569)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        "Ahora no",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = onActivateNotifications,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0EA5E9),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        "Activar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: com.troca.latroca.data.models.ChatMessage,
    isCurrentUser: Boolean,
    chatRepository: ChatRepository
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            color = if (isCurrentUser) Color(0xFFE53935) else Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.text,
                    color = if (isCurrentUser) Color.White else Color(0xFF2D3748),
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = chatRepository.formatTimestamp(message.timestamp),
                        fontSize = 11.sp,
                        color = if (isCurrentUser) Color.White.copy(alpha = 0.8f) else Color(0xFF718096)
                    )

                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (message.read) "✓✓" else "✓",
                            fontSize = 12.sp,
                            color = if (message.read) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
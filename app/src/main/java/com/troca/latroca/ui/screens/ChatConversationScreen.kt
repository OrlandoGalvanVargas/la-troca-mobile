package com.troca.latroca.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.latroca.ui.utils.clickableOnce
import com.troca.latroca.data.repository.ChatRepository
import com.troca.latroca.ui.components.LoadingModal
import com.troca.latroca.ui.viewmodels.AuthViewModel
import com.troca.latroca.ui.viewmodels.ChatViewModel
import kotlinx.coroutines.launch

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
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentUserId = authViewModel.getUserId()
    val currentUserName = authViewModel.userProfile.value?.name ?: "Usuario"

    val messages by chatViewModel.messages.collectAsState()
    val isOtherUserTyping by chatViewModel.isOtherUserTyping.collectAsState()
    val error by chatViewModel.error.collectAsState()

    var messageText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var isInitialLoading by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 🔥 Observar ciclo de vida para pausar/resumir listeners
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Log.d("ChatConversation", "ON_RESUME - Iniciando listeners")
                    chatViewModel.listenToMessages(chatId, currentUserId)
                    chatViewModel.listenToTypingStatus(chatId, otherUserId)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d("ChatConversation", "ON_PAUSE - Pausando listeners")
                    chatViewModel.setTypingStatus(chatId, currentUserId, false)
                }
                Lifecycle.Event.ON_STOP -> {
                    Log.d("ChatConversation", "ON_STOP")
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            Log.d("ChatConversation", "onDispose - Limpiando")
            lifecycleOwner.lifecycle.removeObserver(observer)
            chatViewModel.setTypingStatus(chatId, currentUserId, false)
            chatViewModel.clearMessages()
        }
    }

    // 🔥 Iniciar listeners solo una vez
    LaunchedEffect(chatId) {
        try {
            Log.d("ChatConversation", "Iniciando listeners para chatId: $chatId")
            chatViewModel.listenToMessages(chatId, currentUserId)
            chatViewModel.listenToTypingStatus(chatId, otherUserId)

            // Dar tiempo para que carguen los mensajes
            kotlinx.coroutines.delay(1000)
            isInitialLoading = false
        } catch (e: Exception) {
            Log.e("ChatConversation", "Error al iniciar listeners: ${e.message}")
            isInitialLoading = false
            Toast.makeText(
                context,
                "Error al cargar el chat: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // 🔥 Auto-scroll al último mensaje con manejo de errores
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && !isInitialLoading) {
            try {
                coroutineScope.launch {
                    // Esperar un poco para que el LazyColumn se actualice
                    kotlinx.coroutines.delay(100)
                    listState.animateScrollToItem(messages.size - 1)
                }
            } catch (e: Exception) {
                Log.e("ChatConversation", "Error al hacer scroll: ${e.message}")
            }
        }
    }

    // 🔥 Mostrar errores con Toast
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            chatViewModel.clearError()
        }
    }

    // 🚀 LoadingModal para envío de mensajes
    LoadingModal(
        isVisible = isSending,
        message = "Enviando mensaje..."
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
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
                    IconButton(
                        onClick = { navController.popBackStack() },
                        enabled = !isSending
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = if (isSending) Color(0xFFCBD5E0) else Color(0xFFE53935)
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
                .background(Color(0xFFF7FAFC))
                .padding(paddingValues)
        ) {
            when {
                isInitialLoading -> {
                    // Estado de carga inicial
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFFE53935))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Cargando mensajes...",
                                color = Color(0xFF718096),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                messages.isEmpty() && !isInitialLoading -> {
                    // Sin mensajes
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "👋",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No hay mensajes aún",
                                fontSize = 16.sp,
                                color = Color(0xFF718096),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Envía el primer mensaje a $otherUserName",
                                fontSize = 14.sp,
                                color = Color(0xFF718096)
                            )
                        }
                    }
                }
                else -> {
                    // Lista de mensajes
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 16.dp,
                            end = 16.dp,
                            bottom = 80.dp // 🔥 Espacio para el input field
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = messages,
                            key = { it.id } // 🔥 Importante para performance
                        ) { message ->
                            MessageBubble(
                                message = message,
                                isCurrentUser = message.senderId == currentUserId,
                                chatRepository = ChatRepository()
                            )
                        }

                        // Espacio final para mejor UX
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // 🔥 Input field fijo en la parte inferior
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.White),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .heightIn(min = 56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = {
                            if (!isSending) {
                                messageText = it
                                // Indicar que está escribiendo
                                try {
                                    if (it.isNotBlank()) {
                                        chatViewModel.setTypingStatus(chatId, currentUserId, true)
                                    } else {
                                        chatViewModel.setTypingStatus(chatId, currentUserId, false)
                                    }
                                } catch (e: Exception) {
                                    Log.e("ChatConversation", "Error al actualizar typing status: ${e.message}")
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp, max = 120.dp),
                        placeholder = { Text("Escribe un mensaje...", color = Color(0xFFB0BEC5)) },
                        shape = RoundedCornerShape(24.dp),
                        enabled = !isSending,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE53935),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            disabledBorderColor = Color(0xFFE2E8F0),
                            disabledTextColor = Color(0xFF718096)
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = when {
                                    isSending -> Color(0xFFB0BEC5)
                                    messageText.isNotBlank() -> Color(0xFFE53935)
                                    else -> Color(0xFFE2E8F0)
                                },
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickableOnce(enabled = messageText.isNotBlank() && !isSending) {
                                if (messageText.isNotBlank() && !isSending) {
                                    isSending = true
                                    val textToSend = messageText
                                    messageText = ""

                                    try {
                                        chatViewModel.sendMessage(
                                            chatId = chatId,
                                            senderId = currentUserId,
                                            senderName = currentUserName,
                                            text = textToSend,
                                            receiverId = otherUserId
                                        )

                                        // Resetear typing status
                                        chatViewModel.setTypingStatus(chatId, currentUserId, false)

                                        // Simular delay de envío
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(500)
                                            isSending = false
                                        }
                                    } catch (e: Exception) {
                                        Log.e("ChatConversation", "Error al enviar mensaje: ${e.message}")
                                        Toast.makeText(
                                            context,
                                            "Error al enviar mensaje",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        messageText = textToSend // Restaurar el mensaje
                                        isSending = false
                                    }
                                }
                            }
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Enviar",
                                tint = if (messageText.isNotBlank()) Color.White else Color(0xFF718096)
                            )
                        }
                    }
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
                        text = try {
                            chatRepository.formatTimestamp(message.timestamp)
                        } catch (e: Exception) {
                            "Ahora"
                        },
                        fontSize = 11.sp,
                        color = if (isCurrentUser) Color.White.copy(alpha = 0.8f) else Color.Gray
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
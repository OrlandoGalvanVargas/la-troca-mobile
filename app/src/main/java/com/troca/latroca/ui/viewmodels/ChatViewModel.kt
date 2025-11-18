package com.troca.latroca.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.troca.latroca.data.models.Chat
import com.troca.latroca.data.models.ChatListItem
import com.troca.latroca.data.models.ChatMessage
import com.troca.latroca.data.repository.ChatRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _userChats = MutableStateFlow<List<Chat>>(emptyList())
    val userChats: StateFlow<List<Chat>> = _userChats.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isOtherUserTyping = MutableStateFlow(false)
    val isOtherUserTyping: StateFlow<Boolean> = _isOtherUserTyping.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentChatId = MutableStateFlow<String?>(null)

    private var messagesListenerJob: Job? = null
    private var chatsListenerJob: Job? = null
    private var typingListenerJob: Job? = null
    private var typingDebounceJob: Job? = null

    fun getOrCreateChat(
        currentUserId: String,
        currentUserName: String,
        otherUserId: String,
        otherUserName: String,
        postId: String,
        postTitle: String,
        postImageUrl: String,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val result = chatRepository.getOrCreateChat(
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    otherUserId = otherUserId,
                    otherUserName = otherUserName,
                    postId = postId,
                    postTitle = postTitle,
                    postImageUrl = postImageUrl
                )

                _isLoading.value = false

                result.onSuccess { chatId ->
                    _currentChatId.value = chatId
                    Log.d("ChatViewModel", "Chat obtenido/creado: $chatId")
                    onSuccess(chatId)
                }.onFailure { exception ->
                    _error.value = "Error al crear chat: ${exception.message}"
                    Log.e("ChatViewModel", "Error creating chat", exception)
                }
            } catch (e: CancellationException) {
                Log.d("ChatViewModel", "getOrCreateChat cancelado")
                throw e
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Error inesperado: ${e.message}"
                Log.e("ChatViewModel", "Unexpected error", e)
            }
        }
    }

    fun listenToMessages(chatId: String, currentUserId: String) {
        messagesListenerJob?.cancel()

        messagesListenerJob = viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "Iniciando listener de mensajes para: $chatId")

                chatRepository.getMessagesFlow(chatId)
                    .catch { e ->
                        if (e !is CancellationException) {
                            Log.e("ChatViewModel", "Error en flow de mensajes", e)
                            _error.value = "Error al cargar mensajes: ${e.message}"
                            emit(emptyList())
                        }
                    }
                    .collect { messages ->
                        _messages.value = messages
                        Log.d("ChatViewModel", "Mensajes actualizados: ${messages.size}")

                        if (messages.isNotEmpty()) {
                            markMessagesAsRead(chatId, currentUserId)
                        }
                    }
            } catch (e: CancellationException) {
                Log.d("ChatViewModel", "Listener de mensajes cancelado")
                throw e
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error listening to messages", e)
                _error.value = "Error al cargar mensajes: ${e.message}"
            }
        }
    }

    fun listenToUserChats(userId: String) {
        chatsListenerJob?.cancel()

        chatsListenerJob = viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "Iniciando listener de chats para: $userId")

                chatRepository.getUserChatsFlow(userId)
                    .catch { e ->
                        if (e !is CancellationException) {
                            Log.e("ChatViewModel", "Error en flow de chats", e)
                            _error.value = "Error al cargar chats: ${e.message}"
                            emit(emptyList())
                        }
                    }
                    .collect { chats ->
                        _userChats.value = chats
                        Log.d("ChatViewModel", "Chats actualizados: ${chats.size}")
                    }
            } catch (e: CancellationException) {
                Log.d("ChatViewModel", "Listener de chats cancelado")
                throw e
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error listening to chats", e)
                _error.value = "Error al cargar chats: ${e.message}"
            }
        }
    }

    fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        text: String,
        receiverId: String,
        token: String
    ) {
        if (text.isBlank()) return

        viewModelScope.launch {
            try {
                setTypingStatus(chatId, senderId, false)

                val result = chatRepository.sendMessage(
                    chatId = chatId,
                    senderId = senderId,
                    senderName = senderName,
                    text = text.trim(),
                    receiverId = receiverId,
                    token = token
                )

                result.onFailure { exception ->
                    _error.value = "Error al enviar mensaje: ${exception.message}"
                    Log.e("ChatViewModel", "Error sending message", exception)
                }
            } catch (e: CancellationException) {
                Log.d("ChatViewModel", "Envío de mensaje cancelado")
                throw e
            } catch (e: Exception) {
                _error.value = "Error al enviar: ${e.message}"
                Log.e("ChatViewModel", "Unexpected error sending message", e)
            }
        }
    }

    private fun markMessagesAsRead(chatId: String, userId: String) {
        viewModelScope.launch {
            try {
                chatRepository.markMessagesAsRead(chatId, userId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error marking messages as read", e)
            }
        }
    }

    fun setTypingStatus(chatId: String, userId: String, isTyping: Boolean) {
        typingDebounceJob?.cancel()

        viewModelScope.launch {
            try {
                chatRepository.setTypingStatus(chatId, userId, isTyping)

                if (isTyping) {
                    typingDebounceJob = launch {
                        delay(3000)
                        chatRepository.setTypingStatus(chatId, userId, false)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error setting typing status", e)
            }
        }
    }

    fun listenToTypingStatus(chatId: String, otherUserId: String) {
        typingListenerJob?.cancel()

        typingListenerJob = viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "Iniciando listener de typing para: $otherUserId")

                chatRepository.getTypingStatusFlow(chatId, otherUserId)
                    .catch { e ->
                        if (e !is CancellationException) {
                            Log.e("ChatViewModel", "Error en flow de typing status", e)
                            emit(false)
                        }
                    }
                    .collect { isTyping ->
                        _isOtherUserTyping.value = isTyping
                    }
            } catch (e: CancellationException) {
                Log.d("ChatViewModel", "Listener de typing cancelado")
                _isOtherUserTyping.value = false
                throw e
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error listening to typing status", e)
            }
        }
    }

    fun hideChatForUser(chatId: String, userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val result = chatRepository.hideChat(chatId, userId)

                _isLoading.value = false

                result.onSuccess {
                    Log.d("ChatViewModel", "Chat ocultado exitosamente")
                    onSuccess()
                }.onFailure { exception ->
                    _error.value = "Error al ocultar chat: ${exception.message}"
                    Log.e("ChatViewModel", "Error hiding chat", exception)
                }
            } catch (e: CancellationException) {
                _isLoading.value = false
                throw e
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Error inesperado: ${e.message}"
                Log.e("ChatViewModel", "Unexpected error hiding chat", e)
            }
        }
    }

    fun deleteChatsForPost(postId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "Eliminando chats del post: $postId")
                val result = chatRepository.deleteChatsForPost(postId)

                result.onSuccess {
                    Log.d("ChatViewModel", "Chats del post eliminados exitosamente")
                    onSuccess()
                }.onFailure { exception ->
                    Log.e("ChatViewModel", "Error deleting chats for post", exception)
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Unexpected error deleting chats for post", e)
            }
        }
    }

    fun stopListeners() {
        Log.d("ChatViewModel", "Deteniendo todos los listeners")
        messagesListenerJob?.cancel()
        chatsListenerJob?.cancel()
        typingListenerJob?.cancel()
        typingDebounceJob?.cancel()


    }

    fun clearError() {
        _error.value = null
    }

    fun clearMessages() {
        Log.d("ChatViewModel", "Limpiando mensajes y listeners")
        _messages.value = emptyList()
        _currentChatId.value = null
        _isOtherUserTyping.value = false
        stopListeners()
    }

    fun getChatListItems(chats: List<Chat>, currentUserId: String): List<ChatListItem> {
        return chats.map { chat ->
            val otherUserId = chat.participants.firstOrNull { it != currentUserId } ?: ""
            val otherUserName = chat.participantNames[otherUserId] ?: "Usuario"
            val unreadCount = chat.unreadCount[currentUserId] ?: 0

            ChatListItem(
                chatId = chat.id,
                otherUserId = otherUserId,
                otherUserName = otherUserName,
                postTitle = chat.postTitle,
                postImageUrl = chat.postImageUrl,
                lastMessage = chat.lastMessage,
                lastMessageTime = chatRepository.formatTimestamp(chat.lastMessageTimestamp),
                unreadCount = unreadCount,
                timestamp = chat.lastMessageTimestamp
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ChatViewModel", "ViewModel cleared")
        stopListeners()
    }
}
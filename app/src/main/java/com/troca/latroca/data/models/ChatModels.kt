package com.troca.latroca.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

// 💬 Modelo para un mensaje individual
data class ChatMessage(
    @DocumentId
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val read: Boolean = false,
    val type: String = "text" // "text", "image", "file"
)

// 📝 Modelo para la conversación/chat
data class Chat(
    @DocumentId
    val id: String = "",
    val participants: List<String> = emptyList(), // [userId1, userId2]
    val participantNames: Map<String, String> = emptyMap(), // {userId: userName}
    val postId: String = "",
    val postTitle: String = "",
    val postImageUrl: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val lastMessage: String = "",
    val lastMessageSenderId: String = "",
    @ServerTimestamp
    val lastMessageTimestamp: Timestamp? = null,
    val unreadCount: Map<String, Int> = emptyMap() // {userId: count}
)

// 🔄 Estado de "escribiendo..."
data class TypingStatus(
    val userId: String = "",
    val isTyping: Boolean = false,
    @ServerTimestamp
    val timestamp: Timestamp? = null
)

// 📱 Modelo para UI de lista de chats
data class ChatListItem(
    val chatId: String,
    val otherUserId: String,
    val otherUserName: String,
    val postTitle: String,
    val postImageUrl: String,
    val lastMessage: String,
    val lastMessageTime: String,
    val unreadCount: Int,
    val timestamp: Timestamp?
)
package com.troca.latroca.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class ChatMessage(
    @DocumentId
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val read: Boolean = false,
    val type: String = "text"
)

data class Chat(
    @DocumentId
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val postId: String = "",
    val postTitle: String = "",
    val postImageUrl: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val lastMessage: String = "",
    val lastMessageSenderId: String = "",
    @ServerTimestamp
    val lastMessageTimestamp: Timestamp? = null,
    val unreadCount: Map<String, Int> = emptyMap(),
    val hiddenFor: Map<String, Boolean> = emptyMap()
)

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
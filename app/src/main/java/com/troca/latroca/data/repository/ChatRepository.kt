package com.troca.latroca.data.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.troca.latroca.data.models.Chat
import com.troca.latroca.data.models.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val chatsCollection = db.collection("chats")
    private val messagesCollection = db.collection("messages")

    private suspend fun sendPushNotificationViaBackend(
        receiverId: String,
        senderName: String,
        messageText: String,
        chatId: String,
        senderId: String,
        token: String
    ) {
        return withContext(Dispatchers.IO) {
            try {
                val userDoc = db.collection("users").document(receiverId).get().await()
                val receiverFcmToken = userDoc.getString("fcmToken")

                if (receiverFcmToken.isNullOrEmpty()) {
                    Log.w("ChatRepository", "Usuario $receiverId no tiene FCM token")
                    return@withContext
                }

                val json = JSONObject().apply {
                    put("receiverFcmToken", receiverFcmToken)
                    put("senderName", senderName)
                    put("messageText", messageText)
                    put("chatId", chatId)
                    put("senderId", senderId)
                }

                val client = OkHttpClient()
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = json.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("https://la-troca-backend-staging.onrender.com/api/Chat/send-notification") // Reemplaza con tu URL
                    .post(body)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    Log.d("ChatRepository", "Notificación enviada exitosamente")
                } else {
                    Log.e("ChatRepository", "Error enviando notificación: ${response.code}")
                }

                response.close()

            } catch (e: Exception) {
                Log.e("ChatRepository", "Error enviando notificación push", e)
            }
        }
    }

    suspend fun getOrCreateChat(
        currentUserId: String,
        currentUserName: String,
        otherUserId: String,
        otherUserName: String,
        postId: String,
        postTitle: String,
        postImageUrl: String
    ): Result<String> {
        return try {
            val existingChats = chatsCollection
                .whereArrayContains("participants", currentUserId)
                .whereEqualTo("postId", postId)
                .limit(10) // Limitar resultados
                .get()
                .await()

            val existingChat = existingChats.documents.firstOrNull { doc ->
                val participants = doc.get("participants") as? List<*>
                participants?.containsAll(listOf(currentUserId, otherUserId)) == true
            }

            if (existingChat != null) {
                val chatId = existingChat.id
                Log.d("ChatRepository", "Chat existente encontrado: $chatId")

                val hiddenFor = existingChat.get("hiddenFor") as? Map<*, *> ?: emptyMap<String, Boolean>()

                if (hiddenFor[currentUserId] == true) {
                    chatsCollection.document(chatId).update(
                        "hiddenFor.$currentUserId", false
                    ).await()
                    Log.d("ChatRepository", "Chat reactivado para usuario: $currentUserId")
                }

                Result.success(chatId)
            } else {
                val chatData = hashMapOf(
                    "participants" to listOf(currentUserId, otherUserId),
                    "participantNames" to mapOf(
                        currentUserId to currentUserName,
                        otherUserId to otherUserName
                    ),
                    "postId" to postId,
                    "postTitle" to postTitle,
                    "postImageUrl" to postImageUrl,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "lastMessage" to "",
                    "lastMessageSenderId" to "",
                    "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                    "unreadCount" to mapOf(
                        currentUserId to 0,
                        otherUserId to 0
                    ),
                    "hiddenFor" to mapOf(
                        currentUserId to false,
                        otherUserId to false
                    )
                )

                val newChat = chatsCollection.add(chatData).await()
                Log.d("ChatRepository", "Nuevo chat creado: ${newChat.id}")
                Result.success(newChat.id)
            }
        } catch (e: FirebaseFirestoreException) {
            Log.e("ChatRepository", "FirebaseFirestoreException: ${e.code} - ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error creating/getting chat", e)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        text: String,
        receiverId: String,
        token: String
    ): Result<Unit> {
        return try {
            val messageData = hashMapOf(
                "senderId" to senderId,
                "senderName" to senderName,
                "text" to text,
                "timestamp" to FieldValue.serverTimestamp(),
                "read" to false,
                "type" to "text"
            )

            val messageRef = messagesCollection
                .document(chatId)
                .collection("messages")
                .add(messageData)
                .await()

            Log.d("ChatRepository", "Mensaje enviado: ${messageRef.id}")

            val chatDoc = chatsCollection.document(chatId).get().await()
            val hiddenFor = chatDoc.get("hiddenFor") as? Map<*, *> ?: emptyMap<String, Boolean>()

            val updateMap = mutableMapOf(
                "lastMessage" to text,
                "lastMessageSenderId" to senderId,
                "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                "unreadCount.$receiverId" to FieldValue.increment(1)
            )

            if (hiddenFor[senderId] == true) {
                updateMap["hiddenFor.$senderId"] = false
                Log.d("ChatRepository", "Chat reactivado para emisor: $senderId")
            }

            if (hiddenFor[receiverId] == true) {
                updateMap["hiddenFor.$receiverId"] = false
                Log.d("ChatRepository", "Chat reactivado para receptor: $receiverId")
            }

            chatsCollection.document(chatId).update(updateMap).await()
            sendPushNotificationViaBackend(
                receiverId = receiverId,
                senderName = senderName,
                messageText = text,
                chatId = chatId,
                senderId = senderId,
                token = token
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending message", e)
            Result.failure(e)
        }
    }

    suspend fun updateFcmToken(fcmToken: String, jwtToken: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("fcmToken", fcmToken)
                }

                val client = OkHttpClient()
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = json.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("https://la-troca-backend-staging.onrender.com/api/Chat/update-fcm-token")
                    .post(body)
                    .addHeader("Authorization", "Bearer $jwtToken")
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    Log.d("ChatRepository", "FCM token actualizado exitosamente")
                    Result.success(Unit)
                } else {
                    Log.e("ChatRepository", "Error actualizando FCM token: ${response.code}")
                    Result.failure(Exception("Error ${response.code}"))
                }
            } catch (e: Exception) {
                Log.e("ChatRepository", "Error actualizando FCM token", e)
                Result.failure(e)
            }
        }
    }

    suspend fun removeFcmToken(jwtToken: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                val request = Request.Builder()
                    .url("https://la-troca-backend-staging.onrender.com/api/Chat/remove-fcm-token")
                    .post("".toRequestBody())
                    .addHeader("Authorization", "Bearer $jwtToken")
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    Log.d("ChatRepository", "FCM token eliminado exitosamente")
                    Result.success(Unit)
                } else {
                    Log.e("ChatRepository", "Error eliminando FCM token: ${response.code}")
                    Result.failure(Exception("Error ${response.code}"))
                }
            } catch (e: Exception) {
                Log.e("ChatRepository", "Error eliminando FCM token", e)
                Result.failure(e)
            }
        }
    }

    fun getMessagesFlow(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

        try {
            listenerRegistration = messagesCollection
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("ChatRepository", "Error listening to messages: ${error.message}", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val messages = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                            } catch (e: Exception) {
                                Log.e("ChatRepository", "Error parsing message: ${e.message}")
                                null
                            }
                        }
                        Log.d("ChatRepository", "Mensajes recibidos: ${messages.size}")
                        trySend(messages)
                    }
                }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error setting up messages listener", e)
            close(e)
        }

        awaitClose {
            Log.d("ChatRepository", "Cerrando listener de mensajes")
            listenerRegistration?.remove()
        }
    }

    suspend fun markMessagesAsRead(chatId: String, userId: String): Result<Unit> {
        return try {
            val unreadMessages = messagesCollection
                .document(chatId)
                .collection("messages")
                .whereEqualTo("read", false)
                .whereNotEqualTo("senderId", userId)
                .limit(50)
                .get()
                .await()

            if (unreadMessages.documents.isEmpty()) {
                return Result.success(Unit)
            }

            val batch = db.batch()
            unreadMessages.documents.forEach { doc ->
                batch.update(doc.reference, "read", true)
            }
            batch.commit().await()

            chatsCollection.document(chatId).update(
                "unreadCount.$userId", 0
            ).await()

            Log.d("ChatRepository", "Mensajes marcados como leídos: ${unreadMessages.size()}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error marking messages as read", e)
            Result.failure(e)
        }
    }

    suspend fun setTypingStatus(chatId: String, userId: String, isTyping: Boolean): Result<Unit> {
        return try {
            val typingData = hashMapOf(
                "userId" to userId,
                "isTyping" to isTyping,
                "timestamp" to FieldValue.serverTimestamp()
            )

            chatsCollection
                .document(chatId)
                .collection("typing")
                .document(userId)
                .set(typingData)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getTypingStatusFlow(chatId: String, otherUserId: String): Flow<Boolean> = callbackFlow {
        var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

        try {
            listenerRegistration = chatsCollection
                .document(chatId)
                .collection("typing")
                .document(otherUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(false)
                        return@addSnapshotListener
                    }

                    val isTyping = if (snapshot != null && snapshot.exists()) {
                        snapshot.getBoolean("isTyping") ?: false
                    } else {
                        false
                    }

                    Log.d("ChatRepository", "Typing status para $otherUserId: $isTyping")
                    trySend(isTyping)
                }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error setting up typing listener", e)
            trySend(false)
        }

        awaitClose {
            listenerRegistration?.remove()
        }
    }

    suspend fun hideChat(chatId: String, userId: String): Result<Unit> {
        return try {
            chatsCollection.document(chatId).update(
                "hiddenFor.$userId", true
            ).await()

            Log.d("ChatRepository", "Chat ocultado para usuario: $userId")

            val chatDoc = chatsCollection.document(chatId).get().await()
            val hiddenFor = chatDoc.get("hiddenFor") as? Map<*, *>

            val allHidden = hiddenFor?.values?.all { it == true } == true
            if (allHidden) {
                deleteChatPermanently(chatId)
                Log.d("ChatRepository", "Chat eliminado completamente (ambos lo ocultaron)")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error hiding chat", e)
            Result.failure(e)
        }
    }

    private suspend fun deleteChatPermanently(chatId: String) {
        try {
            val messages = messagesCollection
                .document(chatId)
                .collection("messages")
                .limit(500)
                .get()
                .await()

            if (messages.documents.isNotEmpty()) {
                val batch = db.batch()
                messages.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }

            val typingDocs = chatsCollection
                .document(chatId)
                .collection("typing")
                .get()
                .await()

            if (typingDocs.documents.isNotEmpty()) {
                val batch = db.batch()
                typingDocs.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }

            chatsCollection.document(chatId).delete().await()

            Log.d("ChatRepository", "Chat eliminado permanentemente: $chatId")
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error deleting chat permanently", e)
        }
    }

    suspend fun deleteChatsForPost(postId: String): Result<Unit> {
        return try {
            val chatsToDelete = chatsCollection
                .whereEqualTo("postId", postId)
                .get()
                .await()

            Log.d("ChatRepository", "Encontrados ${chatsToDelete.size()} chats para post: $postId")

            chatsToDelete.documents.forEach { doc ->
                deleteChatPermanently(doc.id)
            }

            Log.d("ChatRepository", "Todos los chats del post eliminados: $postId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error deleting chats for post", e)
            Result.failure(e)
        }
    }

    fun getUserChatsFlow(userId: String): Flow<List<Chat>> = callbackFlow {
        var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

        try {
            listenerRegistration = chatsCollection
                .whereArrayContains("participants", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        if (error.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                            Log.e("ChatRepository", "❌ ÍNDICE FALTANTE. Crea el índice en Firebase Console")
                            Log.e("ChatRepository", "Colección: chats")
                            Log.e("ChatRepository", "Campos: participants (Arrays), lastMessageTimestamp (Descending)")
                        }
                        Log.e("ChatRepository", "Error listening to chats: ${error.message}", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val chats = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(Chat::class.java)?.copy(id = doc.id)
                            } catch (e: Exception) {
                                Log.e("ChatRepository", "Error parsing chat: ${e.message}")
                                null
                            }
                        }
                            .filter { chat ->
                                val isHidden = chat.hiddenFor[userId] ?: false
                                !isHidden
                            }
                            .sortedByDescending { chat ->
                                chat.lastMessageTimestamp?.seconds ?: 0
                            }

                        Log.d("ChatRepository", "Chats recibidos (no ocultos): ${chats.size}")
                        trySend(chats)
                    }
                }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error setting up chats listener", e)
            close(e)
        }

        awaitClose {
            Log.d("ChatRepository", "Cerrando listener de chats")
            listenerRegistration?.remove()
        }
    }

    @Suppress("DEPRECATION")
    fun formatTimestamp(timestamp: com.google.firebase.Timestamp?): String {
        if (timestamp == null) return ""

        return try {
            val date = timestamp.toDate()
            val now = Calendar.getInstance()
            val messageDate = Calendar.getInstance().apply { time = date }

            when {
                now.get(Calendar.DAY_OF_YEAR) == messageDate.get(Calendar.DAY_OF_YEAR) &&
                        now.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR) -> {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                }
                now.get(Calendar.DAY_OF_YEAR) - messageDate.get(Calendar.DAY_OF_YEAR) == 1 &&
                        now.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR) -> {
                    "Ayer"
                }
                now.get(Calendar.WEEK_OF_YEAR) == messageDate.get(Calendar.WEEK_OF_YEAR) &&
                        now.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR) -> {
                    SimpleDateFormat("EEEE", Locale("es", "ES")).format(date)
                        .replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale("es", "ES"))
                            else it.toString() }
                }
                else -> {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
                }
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error formatting timestamp", e)
            ""
        }
    }
}
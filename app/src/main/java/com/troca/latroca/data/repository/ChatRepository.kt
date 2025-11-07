package com.troca.latroca.data.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.troca.latroca.data.models.Chat
import com.troca.latroca.data.models.ChatMessage
import com.troca.latroca.data.models.TypingStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val chatsCollection = db.collection("chats")
    private val messagesCollection = db.collection("messages")

    // 📝 Crear o obtener un chat existente
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
            // 🔍 Buscar chat existente de forma más eficiente
            val existingChats = chatsCollection
                .whereArrayContains("participants", currentUserId)
                .whereEqualTo("postId", postId)
                .limit(10) // Limitar resultados
                .get()
                .await()

            // Filtrar en el cliente para encontrar el chat con ambos usuarios
            val existingChat = existingChats.documents.firstOrNull { doc ->
                val participants = doc.get("participants") as? List<*>
                participants?.containsAll(listOf(currentUserId, otherUserId)) == true
            }

            if (existingChat != null) {
                Log.d("ChatRepository", "Chat existente encontrado: ${existingChat.id}")
                Result.success(existingChat.id)
            } else {
                // Crear nuevo chat
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

    // 💬 Enviar mensaje
    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        text: String,
        receiverId: String
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

            // Agregar mensaje a la subcolección
            val messageRef = messagesCollection
                .document(chatId)
                .collection("messages")
                .add(messageData)
                .await()

            Log.d("ChatRepository", "Mensaje enviado: ${messageRef.id}")

            // Actualizar el último mensaje en el chat
            chatsCollection.document(chatId).update(
                mapOf(
                    "lastMessage" to text,
                    "lastMessageSenderId" to senderId,
                    "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                    "unreadCount.$receiverId" to FieldValue.increment(1)
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending message", e)
            Result.failure(e)
        }
    }

    // 📖 Escuchar mensajes en tiempo real
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
                        // No cerrar el flow, solo enviar lista vacía
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

    // 📋 Obtener lista de chats del usuario (SIN orderBy para evitar índice)
    fun getUserChatsFlow(userId: String): Flow<List<Chat>> = callbackFlow {
        var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

        try {
            listenerRegistration = chatsCollection
                .whereArrayContains("participants", userId)
                // 🔥 REMOVIDO orderBy para evitar error de índice
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
                        }.sortedByDescending { chat ->
                            // Ordenar en el cliente por timestamp
                            chat.lastMessageTimestamp?.seconds ?: 0
                        }

                        Log.d("ChatRepository", "Chats recibidos: ${chats.size}")
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

    // ✅ Marcar mensajes como leídos
    suspend fun markMessagesAsRead(chatId: String, userId: String): Result<Unit> {
        return try {
            // Obtener mensajes no leídos
            val unreadMessages = messagesCollection
                .document(chatId)
                .collection("messages")
                .whereEqualTo("read", false)
                .whereNotEqualTo("senderId", userId)
                .limit(50) // Limitar para evitar leer demasiados documentos
                .get()
                .await()

            if (unreadMessages.documents.isEmpty()) {
                return Result.success(Unit)
            }

            // Marcar cada mensaje como leído
            val batch = db.batch()
            unreadMessages.documents.forEach { doc ->
                batch.update(doc.reference, "read", true)
            }
            batch.commit().await()

            // Resetear contador de no leídos
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

    // ✍️ Actualizar estado de "escribiendo..."
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
            // No loguear errores de typing status, son menos críticos
            Result.failure(e)
        }
    }

    // 👀 Escuchar estado de "escribiendo..."
    fun getTypingStatusFlow(chatId: String, otherUserId: String): Flow<Boolean> = callbackFlow {
        var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

        try {
            listenerRegistration = chatsCollection
                .document(chatId)
                .collection("typing")
                .document(otherUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // No cerrar el flow, solo enviar false
                        trySend(false)
                        return@addSnapshotListener
                    }

                    val typingStatus = snapshot?.toObject(TypingStatus::class.java)
                    trySend(typingStatus?.isTyping ?: false)
                }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error setting up typing listener", e)
            trySend(false)
        }

        awaitClose {
            listenerRegistration?.remove()
        }
    }

    // 🗑️ Eliminar chat
    suspend fun deleteChat(chatId: String): Result<Unit> {
        return try {
            // Eliminar mensajes en lotes
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

            // Eliminar documentos de typing
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

            // Eliminar chat principal
            chatsCollection.document(chatId).delete().await()

            Log.d("ChatRepository", "Chat eliminado: $chatId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error deleting chat", e)
            Result.failure(e)
        }
    }

    // 🕐 Formatear timestamp para UI
    fun formatTimestamp(timestamp: com.google.firebase.Timestamp?): String {
        if (timestamp == null) return ""

        return try {
            val date = timestamp.toDate()
            val now = Calendar.getInstance()
            val messageDate = Calendar.getInstance().apply { time = date }

            when {
                // Hoy
                now.get(Calendar.DAY_OF_YEAR) == messageDate.get(Calendar.DAY_OF_YEAR) &&
                        now.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR) -> {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                }
                // Ayer
                now.get(Calendar.DAY_OF_YEAR) - messageDate.get(Calendar.DAY_OF_YEAR) == 1 &&
                        now.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR) -> {
                    "Ayer"
                }
                // Esta semana
                now.get(Calendar.WEEK_OF_YEAR) == messageDate.get(Calendar.WEEK_OF_YEAR) &&
                        now.get(Calendar.YEAR) == messageDate.get(Calendar.YEAR) -> {
                    SimpleDateFormat("EEEE", Locale("es", "ES")).format(date).capitalize()
                }
                // Otro
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
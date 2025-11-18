package com.troca.latroca.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.troca.latroca.MainActivity
import com.troca.latroca.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "chat_notifications"
        private const val CHANNEL_NAME = "Mensajes de Chat"
        private const val CHANNEL_DESCRIPTION = "Notificaciones de mensajes nuevos"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🆕 Nuevo FCM token generado: ${token.take(20)}...")

        val sharedPref = getSharedPreferences("fcm_prefs", MODE_PRIVATE)
        sharedPref.edit {
            putString("fcm_token", token)
        }

        Log.d(TAG, "Token guardado localmente. Se enviará al backend cuando el usuario inicie sesión.")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "📬 Mensaje recibido de: ${message.from}")

        if (isAppInForeground()) {
            Log.d(TAG, "App en primer plano - No mostrar notificación")
            return
        }

        val data = message.data
        val senderName = data["senderName"] ?: "Nuevo mensaje"
        val messageText = data["messageText"] ?: ""
        val chatId = data["chatId"] ?: ""
        val senderId = data["senderId"] ?: ""

        Log.d(TAG, "📊 Datos recibidos:")
        Log.d(TAG, "  - senderName: $senderName")
        Log.d(TAG, "  - messageText: $messageText")
        Log.d(TAG, "  - chatId: $chatId")
        Log.d(TAG, "  - senderId: $senderId")

        if (messageText.isNotEmpty() && chatId.isNotEmpty()) {
            showNotification(senderName, messageText, chatId, senderId)
        } else {
            Log.w(TAG, "⚠️ Datos incompletos, no se mostrará notificación")
        }
    }

    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        val runningProcesses = activityManager.runningAppProcesses ?: return false

        return runningProcesses.any { processInfo ->
            processInfo.processName == packageName &&
                    processInfo.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }
    }

    private fun showNotification(
        senderName: String,
        messageText: String,
        chatId: String,
        senderId: String
    ) {
        createNotificationChannel()

        Log.d(TAG, "🔔 Creando notificación para chat: $chatId")

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP

            putExtra("openChat", true)
            putExtra("chatId", chatId)
            putExtra("senderId", senderId)
            putExtra("senderName", senderName)

            Log.d(TAG, "📦 Intent configurado con extras:")
            Log.d(TAG, "  - openChat: true")
            Log.d(TAG, "  - chatId: $chatId")
            Log.d(TAG, "  - senderId: $senderId")
            Log.d(TAG, "  - senderName: $senderName")
        }

        val requestCode = System.currentTimeMillis().toInt()

        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(senderName)
            .setContentText(messageText)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val notificationId = chatId.hashCode()
        Log.d(TAG, "✅ Mostrando notificación con ID: $notificationId, requestCode: $requestCode")

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
    }
}
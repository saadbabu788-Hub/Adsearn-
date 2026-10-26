package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AdsearnFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "New FCM Registration Token generated: ${token.take(12)}***")
        
        // Save token and register device locally
        FcmPushManager.registerDeviceToken(applicationContext, token)

        // Automatically subscribe this device to the app's global announcements topic
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(GLOBAL_ANNOUNCEMENTS_TOPIC)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.i(TAG, "Successfully subscribed to topic: $GLOBAL_ANNOUNCEMENTS_TOPIC")
                    } else {
                        Log.w(TAG, "Failed to subscribe to topic: ${task.exception?.message}")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing to FCM topic", e)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(TAG, "FCM message received from: ${remoteMessage.from}, ID: ${remoteMessage.messageId}")

        // Check for duplicates
        val broadcastId = remoteMessage.data["broadcast_id"] ?: remoteMessage.messageId ?: ""
        if (broadcastId.isNotBlank() && FcmPushManager.isDuplicateMessage(applicationContext, broadcastId)) {
            Log.i(TAG, "Skipping duplicate FCM message ID: $broadcastId")
            return
        }
        if (broadcastId.isNotBlank()) {
            FcmPushManager.markMessageReceived(applicationContext, broadcastId)
        }

        // Extract title and body from notification payload or data payload
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Adsearn Announcement"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["message"]
            ?: remoteMessage.data["body"]
            ?: "New notification from Adsearn admin."

        showGlobalNotification(title, body, broadcastId)
    }

    private fun showGlobalNotification(title: String, message: String, broadcastId: String) {
        val context = applicationContext
        createNotificationChannel(context)

        // Intent to launch MainActivity when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("fcm_broadcast_id", broadcastId)
            putExtra("from_fcm", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            broadcastId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(context, GLOBAL_ANNOUNCEMENTS_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setColor(Color.parseColor("#8B5CF6")) // PurplePrimary

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = if (broadcastId.isNotBlank()) broadcastId.hashCode() else System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, notificationBuilder.build())

            // Record into delivery log
            val prefs = ScheduledNotificationPrefs(context)
            prefs.recordDeliveryLog(
                slotId = 99, // 99 indicates Global FCM push
                title = title,
                isPreview = false
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display FCM notification", e)
        }
    }

    companion object {
        private const val TAG = "AdsearnFCM"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Global Announcements"
                val descriptionText = "Centralized push notifications and administrative broadcasts from Adsearn."
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(GLOBAL_ANNOUNCEMENTS_CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    enableLights(true)
                    lightColor = Color.parseColor("#FFD700")
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 150, 250)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}

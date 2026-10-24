package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.SPIN_CHANNEL_ID

object DailySpinNotificationHelper {

    private const val PREFS_NAME = "spin_notification_prefs"
    private const val KEY_LAST_NOTIFIED_DATE = "last_notified_date"
    private const val NOTIFICATION_ID = 7001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Spin Reminders"
            val descriptionText = "Notifications for available free daily spins & bonus rewards"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(SPIN_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun notifyDailySpinsAvailable(context: Context, spinsRemaining: Int, currentDate: String) {
        if (spinsRemaining <= 0 || currentDate.isBlank()) return

        // Prevent duplicate daily notifications
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastNotified = prefs.getString(KEY_LAST_NOTIFIED_DATE, null)
        if (lastNotified == currentDate) {
            return
        }

        // Check runtime permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val spinText = if (spinsRemaining == 1) {
            "You have 1 free daily spin ready! Spin the wheel to win coins & unlock bonus cash ads."
        } else {
            "You have 2 free daily spins ready today! Spin the wheel to win instant coins & bonus rewards."
        }

        val builder = NotificationCompat.Builder(context, SPIN_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🎰 Daily Spin Wheel Ready ($spinsRemaining Available)")
            .setContentText(spinText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(spinText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID, builder.build())
            // Mark as notified for this date
            prefs.edit().putString(KEY_LAST_NOTIFIED_DATE, currentDate).apply()
        } catch (_: SecurityException) {
            // Permission was revoked at runtime
        }
    }
}

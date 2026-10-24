package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.SCHEDULED_NOTIFICATIONS_CHANNEL_ID
import com.example.TIMEZONE_ASIA_KOLKATA
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DailyNotificationScheduler {

    private const val TAG = "DailyScheduler"
    const val ACTION_SCHEDULED_NOTIFICATION = "com.example.action.SCHEDULED_NOTIFICATION"
    const val EXTRA_SLOT_ID = "EXTRA_SLOT_ID"
    const val BASE_REQUEST_CODE = 8100

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Offers & Reminders"
            val descriptionText = "Scheduled daily notifications at 9:00 AM, 12:00 PM, and 5:00 PM (Asia/Kolkata)"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(SCHEDULED_NOTIFICATIONS_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun rescheduleAll(context: Context) {
        val prefs = ScheduledNotificationPrefs(context)
        if (!prefs.isMasterEnabled) {
            cancelAll(context)
            Log.d(TAG, "Master notifications disabled. Cancelled all daily alarms.")
            return
        }

        prefs.getAllSlotConfigs().forEach { slot ->
            if (slot.isEnabled) {
                scheduleSlot(context, slot)
            } else {
                cancelSlot(context, slot.id)
            }
        }
    }

    fun scheduleSlot(context: Context, slot: NotificationSlotConfig) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerMillis = computeNextTriggerEpochMillis(slot.hour, slot.minute)

        val intent = Intent(context, ScheduledNotificationReceiver::class.java).apply {
            action = ACTION_SCHEDULED_NOTIFICATION
            putExtra(EXTRA_SLOT_ID, slot.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            BASE_REQUEST_CODE + slot.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
            Log.d(TAG, "Scheduled slot ${slot.id} for trigger at $triggerMillis ms")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm for slot ${slot.id}: ${e.message}")
        }
    }

    fun cancelSlot(context: Context, slotId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ScheduledNotificationReceiver::class.java).apply {
            action = ACTION_SCHEDULED_NOTIFICATION
            putExtra(EXTRA_SLOT_ID, slotId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            BASE_REQUEST_CODE + slotId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun cancelAll(context: Context) {
        listOf(1, 2, 3).forEach { cancelSlot(context, it) }
    }

    fun computeNextTriggerEpochMillis(hour: Int, minute: Int): Long {
        val kolkataZone = try {
            ZoneId.of(TIMEZONE_ASIA_KOLKATA)
        } catch (_: Exception) {
            ZoneId.systemDefault()
        }
        val nowKolkata = ZonedDateTime.now(kolkataZone)
        var targetDateTime = nowKolkata
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)

        // If target time is equal or earlier than now, schedule for the next day
        if (!targetDateTime.isAfter(nowKolkata)) {
            targetDateTime = targetDateTime.plusDays(1)
        }
        return targetDateTime.toInstant().toEpochMilli()
    }

    fun getNextFormattedTime(hour: Int, minute: Int): String {
        val triggerMillis = computeNextTriggerEpochMillis(hour, minute)
        val zone = try { ZoneId.of(TIMEZONE_ASIA_KOLKATA) } catch (_: Exception) { ZoneId.systemDefault() }
        val dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(triggerMillis), zone)
        val formatter = DateTimeFormatter.ofPattern("MMM dd, hh:mm a", Locale.US)
        return dateTime.format(formatter) + " IST"
    }

    fun getTodayKolkataDateString(): String {
        val zone = try { ZoneId.of(TIMEZONE_ASIA_KOLKATA) } catch (_: Exception) { ZoneId.systemDefault() }
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
        return ZonedDateTime.now(zone).format(formatter)
    }

    fun sendTestNotification(context: Context, slotId: Int) {
        val prefs = ScheduledNotificationPrefs(context)
        val slot = prefs.getSlotConfig(slotId)
        dispatchNotification(
            context = context,
            slot = slot,
            isPreview = true
        )
    }

    fun dispatchNotification(
        context: Context,
        slot: NotificationSlotConfig,
        isPreview: Boolean = false
    ) {
        // Android 13+ runtime permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (status != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Notification permission not granted; cannot dispatch slot ${slot.id}")
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATED_FROM_NOTIFICATION", true)
            putExtra("EXTRA_NOTIFICATION_SLOT_ID", slot.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            9000 + slot.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isPreview) "[PREVIEW] ${slot.title}" else slot.title

        val builder = NotificationCompat.Builder(context, SCHEDULED_NOTIFICATIONS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(slot.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(slot.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(BASE_REQUEST_CODE + slot.id, builder.build())

            // Record log in preferences
            val prefs = ScheduledNotificationPrefs(context)
            prefs.recordDeliveryLog(slot.id, title, isPreview)
            Log.d(TAG, "Dispatched notification for slot ${slot.id} (isPreview=$isPreview)")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while notifying slot ${slot.id}: ${e.message}")
        }
    }
}

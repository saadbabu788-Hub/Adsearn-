package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "Received intent action: $action. Restoring daily notification schedules.")
        try {
            DailyNotificationScheduler.createNotificationChannel(context)
            DailyNotificationScheduler.rescheduleAll(context)
        } catch (e: Exception) {
            Log.e("BootReceiver", "Error restoring notification schedules: ${e.message}")
        }
    }
}

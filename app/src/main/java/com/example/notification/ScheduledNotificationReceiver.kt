package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ScheduledNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val slotId = intent.getIntExtra(DailyNotificationScheduler.EXTRA_SLOT_ID, -1)
        if (slotId !in 1..3) {
            Log.w("ScheduledNotifReceiver", "Received broadcast with invalid slot ID: $slotId")
            return
        }

        val prefs = ScheduledNotificationPrefs(context)
        if (!prefs.isMasterEnabled) {
            Log.d("ScheduledNotifReceiver", "Master scheduled notifications are disabled. Ignoring trigger for slot $slotId.")
            return
        }

        val slot = prefs.getSlotConfig(slotId)
        if (!slot.isEnabled) {
            Log.d("ScheduledNotifReceiver", "Slot $slotId is disabled. Ignoring trigger.")
            return
        }

        // Check for duplicate firing on the same Kolkata date
        val todayDate = DailyNotificationScheduler.getTodayKolkataDateString()
        if (prefs.hasFiredToday(slotId, todayDate)) {
            Log.d("ScheduledNotifReceiver", "Slot $slotId already fired today ($todayDate). Skipping duplicate.")
            // Still re-arm for next day
            DailyNotificationScheduler.scheduleSlot(context, slot)
            return
        }

        // Mark as fired for today
        prefs.recordFiredToday(slotId, todayDate)

        // Dispatch notification
        DailyNotificationScheduler.dispatchNotification(context, slot, isPreview = false)

        // Reschedule for next day
        DailyNotificationScheduler.scheduleSlot(context, slot)
    }
}

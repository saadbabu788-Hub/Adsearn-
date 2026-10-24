package com.example.notification

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class ScheduledNotificationPrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "scheduled_notification_prefs"
        private const val KEY_MASTER_ENABLED = "master_scheduled_enabled"
        private const val KEY_ADMIN_PIN = "admin_access_pin"
        private const val DEFAULT_ADMIN_PIN = "7788"
        private const val KEY_DELIVERY_LOGS = "delivery_logs_json"

        // Slot keys prefix
        private const val PREFIX_SLOT_HOUR = "slot_hour_"
        private const val PREFIX_SLOT_MINUTE = "slot_minute_"
        private const val PREFIX_SLOT_TITLE = "slot_title_"
        private const val PREFIX_SLOT_MESSAGE = "slot_message_"
        private const val PREFIX_SLOT_ENABLED = "slot_enabled_"
        private const val PREFIX_LAST_FIRED_DATE = "last_fired_date_slot_"

        // Default configurations
        val DEFAULT_SLOT_1 = NotificationSlotConfig(
            id = 1,
            hour = 9,
            minute = 0,
            title = "🎁 Check Your Rewards!",
            message = "Open Adsearn and check out your rewards! Your reward offer is waiting for you. Tap to see the details.",
            isEnabled = true
        )

        val DEFAULT_SLOT_2 = NotificationSlotConfig(
            id = 2,
            hour = 12,
            minute = 0,
            title = "😂 Babu, Mujhe Paise Ki Zarurat Hai!",
            message = "Babu, mujhe paison ki zarurat hai! 😂 App khol lo, kuchh interesting mil jaaye toh baat bane!",
            isEnabled = true
        )

        val DEFAULT_SLOT_3 = NotificationSlotConfig(
            id = 3,
            hour = 17,
            minute = 0,
            title = "🌮 Kya Momos Khaoge?",
            message = "Kya momos khaoge? 😋 Paise kahan se laoge? 😂 Adsearn kholo aur apne rewards check karo!",
            isEnabled = true
        )
    }

    var isMasterEnabled: Boolean
        get() = prefs.getBoolean(KEY_MASTER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MASTER_ENABLED, value).apply()

    var adminPin: String
        get() = prefs.getString(KEY_ADMIN_PIN, DEFAULT_ADMIN_PIN) ?: DEFAULT_ADMIN_PIN
        set(value) = prefs.edit().putString(KEY_ADMIN_PIN, value).apply()

    fun getSlotConfig(slotId: Int): NotificationSlotConfig {
        val defaultSlot = when (slotId) {
            1 -> DEFAULT_SLOT_1
            2 -> DEFAULT_SLOT_2
            3 -> DEFAULT_SLOT_3
            else -> DEFAULT_SLOT_1
        }

        val hour = prefs.getInt(PREFIX_SLOT_HOUR + slotId, defaultSlot.hour)
        val minute = prefs.getInt(PREFIX_SLOT_MINUTE + slotId, defaultSlot.minute)
        val title = prefs.getString(PREFIX_SLOT_TITLE + slotId, defaultSlot.title) ?: defaultSlot.title
        val message = prefs.getString(PREFIX_SLOT_MESSAGE + slotId, defaultSlot.message) ?: defaultSlot.message
        val isEnabled = prefs.getBoolean(PREFIX_SLOT_ENABLED + slotId, defaultSlot.isEnabled)

        return NotificationSlotConfig(
            id = slotId,
            hour = hour,
            minute = minute,
            title = title,
            message = message,
            isEnabled = isEnabled
        )
    }

    fun getAllSlotConfigs(): List<NotificationSlotConfig> {
        return listOf(getSlotConfig(1), getSlotConfig(2), getSlotConfig(3))
    }

    fun saveSlotConfig(config: NotificationSlotConfig) {
        prefs.edit()
            .putInt(PREFIX_SLOT_HOUR + config.id, config.hour)
            .putInt(PREFIX_SLOT_MINUTE + config.id, config.minute)
            .putString(PREFIX_SLOT_TITLE + config.id, config.title)
            .putString(PREFIX_SLOT_MESSAGE + config.id, config.message)
            .putBoolean(PREFIX_SLOT_ENABLED + config.id, config.isEnabled)
            .apply()
    }

    fun setSlotEnabled(slotId: Int, enabled: Boolean) {
        prefs.edit().putBoolean(PREFIX_SLOT_ENABLED + slotId, enabled).apply()
    }

    fun hasFiredToday(slotId: Int, currentDateFormatted: String): Boolean {
        val lastFired = prefs.getString(PREFIX_LAST_FIRED_DATE + slotId, null)
        return lastFired == currentDateFormatted
    }

    fun recordFiredToday(slotId: Int, currentDateFormatted: String) {
        prefs.edit().putString(PREFIX_LAST_FIRED_DATE + slotId, currentDateFormatted).apply()
    }

    fun recordDeliveryLog(slotId: Int, title: String, isPreview: Boolean) {
        try {
            val logs = getDeliveryLogs().toMutableList()
            logs.add(
                0,
                NotificationLogEntry(
                    timestampMillis = System.currentTimeMillis(),
                    slotId = slotId,
                    title = title,
                    isPreview = isPreview
                )
            )
            val trimmed = logs.take(20)
            val jsonArray = JSONArray()
            for (entry in trimmed) {
                val obj = JSONObject().apply {
                    put("timestampMillis", entry.timestampMillis)
                    put("slotId", entry.slotId)
                    put("title", entry.title)
                    put("isPreview", entry.isPreview)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_DELIVERY_LOGS, jsonArray.toString()).apply()
        } catch (_: Exception) {
        }
    }

    fun getDeliveryLogs(): List<NotificationLogEntry> {
        val raw = prefs.getString(KEY_DELIVERY_LOGS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<NotificationLogEntry>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    NotificationLogEntry(
                        timestampMillis = obj.optLong("timestampMillis"),
                        slotId = obj.optInt("slotId"),
                        title = obj.optString("title"),
                        isPreview = obj.optBoolean("isPreview")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clearLogs() {
        prefs.edit().remove(KEY_DELIVERY_LOGS).apply()
    }
}

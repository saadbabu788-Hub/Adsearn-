package com.example.notification

data class NotificationSlotConfig(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val title: String,
    val message: String,
    val isEnabled: Boolean = true
) {
    val formattedTime: String
        get() {
            val period = if (hour < 12) "AM" else "PM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            return String.format("%02d:%02d %s", displayHour, minute, period)
        }
}

data class NotificationLogEntry(
    val timestampMillis: Long,
    val slotId: Int,
    val title: String,
    val isPreview: Boolean
)

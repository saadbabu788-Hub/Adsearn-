package com.example.notification

import java.util.UUID

const val GLOBAL_ANNOUNCEMENTS_TOPIC = "adsearn_global_announcements"
const val GLOBAL_ANNOUNCEMENTS_CHANNEL_ID = "adsearn_global_announcements_channel"

data class DeviceRegistration(
    val deviceToken: String,
    val deviceId: String,
    val deviceModel: String,
    val androidVersion: String,
    val appVersion: String,
    val registeredAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    val isValid: Boolean = true
)

data class GlobalPushBroadcast(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val targetTopic: String = GLOBAL_ANNOUNCEMENTS_TOPIC,
    val timestamp: Long = System.currentTimeMillis(),
    val sentBy: String = "Admin",
    val recipientDeviceCount: Int = 1,
    val status: String = "Dispatched",
    val deliveryReport: String? = null
)

data class FcmConfigStatus(
    val isFirebaseReady: Boolean = false,
    val fcmToken: String? = null,
    val subscribedTopic: String = GLOBAL_ANNOUNCEMENTS_TOPIC,
    val registeredDeviceCount: Int = 0,
    val isBackendConfigured: Boolean = false,
    val backendEndpoint: String? = null
)

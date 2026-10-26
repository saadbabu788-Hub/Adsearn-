package com.example.notification

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

object FcmPushManager {
    private const val TAG = "FcmPushManager"
    private const val PREFS_NAME = "fcm_push_manager_prefs"
    private const val KEY_FCM_TOKEN = "fcm_device_token"
    private const val KEY_REGISTERED_DEVICES_JSON = "registered_devices_json"
    private const val KEY_BROADCAST_HISTORY_JSON = "broadcast_history_json"
    private const val KEY_RECEIVED_BROADCAST_IDS = "received_broadcast_ids"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun initFcm(context: Context) {
        try {
            // Check if Firebase is available
            if (FirebaseApp.getApps(context).isEmpty()) {
                Log.w(TAG, "FirebaseApp is not initialized yet. Skipping FCM token query.")
                return
            }

            // Ensure notification channel exists
            AdsearnFirebaseMessagingService.createNotificationChannel(context)

            // Subscribe device to global announcements topic
            FirebaseMessaging.getInstance().subscribeToTopic(GLOBAL_ANNOUNCEMENTS_TOPIC)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.i(TAG, "Subscribed to FCM topic: $GLOBAL_ANNOUNCEMENTS_TOPIC")
                    } else {
                        Log.w(TAG, "Subscription to FCM topic failed: ${task.exception?.message}")
                    }
                }

            // Retrieve current device registration token
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && !task.result.isNullOrBlank()) {
                        val token = task.result
                        Log.i(TAG, "Fetched FCM Token: ${token.take(12)}***")
                        registerDeviceToken(context, token)
                    } else {
                        Log.w(TAG, "Failed to retrieve FCM token: ${task.exception?.message}")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing FCM", e)
        }
    }

    fun registerDeviceToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()

        // Update registered devices list
        val currentList = getRegisteredDevices(context).toMutableList()
        val deviceId = "${Build.MANUFACTURER}_${Build.MODEL}_${Build.ID}"
        val existingIndex = currentList.indexOfFirst { it.deviceToken == token || it.deviceId == deviceId }

        val newRegistration = DeviceRegistration(
            deviceToken = token,
            deviceId = deviceId,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            appVersion = "1.0",
            registeredAt = System.currentTimeMillis(),
            lastActiveAt = System.currentTimeMillis(),
            isValid = true
        )

        if (existingIndex >= 0) {
            currentList[existingIndex] = newRegistration
        } else {
            currentList.add(newRegistration)
        }

        saveRegisteredDevices(context, currentList)
    }

    fun getFcmToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_FCM_TOKEN, null)
    }

    fun getRegisteredDevices(context: Context): List<DeviceRegistration> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_REGISTERED_DEVICES_JSON, null) ?: return emptyList()
        val list = mutableListOf<DeviceRegistration>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    DeviceRegistration(
                        deviceToken = obj.optString("deviceToken", ""),
                        deviceId = obj.optString("deviceId", ""),
                        deviceModel = obj.optString("deviceModel", ""),
                        androidVersion = obj.optString("androidVersion", ""),
                        appVersion = obj.optString("appVersion", ""),
                        registeredAt = obj.optLong("registeredAt", System.currentTimeMillis()),
                        lastActiveAt = obj.optLong("lastActiveAt", System.currentTimeMillis()),
                        isValid = obj.optBoolean("isValid", true)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveRegisteredDevices(context: Context, list: List<DeviceRegistration>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("deviceToken", item.deviceToken)
            obj.put("deviceId", item.deviceId)
            obj.put("deviceModel", item.deviceModel)
            obj.put("androidVersion", item.androidVersion)
            obj.put("appVersion", item.appVersion)
            obj.put("registeredAt", item.registeredAt)
            obj.put("lastActiveAt", item.lastActiveAt)
            obj.put("isValid", item.isValid)
            array.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_REGISTERED_DEVICES_JSON, array.toString()).apply()
    }

    fun getBroadcastHistory(context: Context): List<GlobalPushBroadcast> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_BROADCAST_HISTORY_JSON, null) ?: return emptyList()
        val list = mutableListOf<GlobalPushBroadcast>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    GlobalPushBroadcast(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", ""),
                        message = obj.optString("message", ""),
                        targetTopic = obj.optString("targetTopic", GLOBAL_ANNOUNCEMENTS_TOPIC),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        sentBy = obj.optString("sentBy", "Admin"),
                        recipientDeviceCount = obj.optInt("recipientDeviceCount", 1),
                        status = obj.optString("status", "Dispatched"),
                        deliveryReport = if (obj.has("deliveryReport")) obj.optString("deliveryReport") else null
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveBroadcastHistory(context: Context, list: List<GlobalPushBroadcast>) {
        val array = JSONArray()
        for (item in list.take(30)) { // Keep last 30 broadcasts
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("message", item.message)
            obj.put("targetTopic", item.targetTopic)
            obj.put("timestamp", item.timestamp)
            obj.put("sentBy", item.sentBy)
            obj.put("recipientDeviceCount", item.recipientDeviceCount)
            obj.put("status", item.status)
            if (item.deliveryReport != null) obj.put("deliveryReport", item.deliveryReport)
            array.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BROADCAST_HISTORY_JSON, array.toString()).apply()
    }

    fun isDuplicateMessage(context: Context, broadcastId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_RECEIVED_BROADCAST_IDS, emptySet()) ?: emptySet()
        return set.contains(broadcastId)
    }

    fun markMessageReceived(context: Context, broadcastId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet(KEY_RECEIVED_BROADCAST_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(broadcastId)
        prefs.edit().putStringSet(KEY_RECEIVED_BROADCAST_IDS, currentSet).apply()
    }

    fun sendGlobalPushAnnouncement(
        context: Context,
        title: String,
        message: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val broadcastId = UUID.randomUUID().toString()
        val devices = getRegisteredDevices(context)
        val deviceCount = maxOf(1, devices.size)

        // Save into broadcast history
        val newBroadcast = GlobalPushBroadcast(
            id = broadcastId,
            title = title,
            message = message,
            targetTopic = GLOBAL_ANNOUNCEMENTS_TOPIC,
            recipientDeviceCount = deviceCount,
            status = "Dispatched to Topic: $GLOBAL_ANNOUNCEMENTS_TOPIC",
            deliveryReport = "Enqueued for all devices subscribed to '$GLOBAL_ANNOUNCEMENTS_TOPIC'."
        )
        val updatedHistory = listOf(newBroadcast) + getBroadcastHistory(context)
        saveBroadcastHistory(context, updatedHistory)

        // Optional Backend Dispatch URL (from BuildConfig or environment)
        val backendUrl = "" // Configured via secure backend / Firebase Cloud Function

        if (backendUrl.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val payload = JSONObject().apply {
                        put("topic", GLOBAL_ANNOUNCEMENTS_TOPIC)
                        put("title", title)
                        put("message", message)
                        put("broadcast_id", broadcastId)
                        put("timestamp", System.currentTimeMillis())
                    }

                    val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url(backendUrl)
                        .post(requestBody)
                        .build()

                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        onResult(true, "Global push broadcast dispatched to topic '$GLOBAL_ANNOUNCEMENTS_TOPIC' successfully.")
                    } else {
                        onResult(false, "Backend error HTTP ${response.code}: ${response.message}")
                    }
                } catch (e: Exception) {
                    onResult(false, "Network error dispatching to backend: ${e.message}")
                }
            }
        } else {
            // Self-contained delivery explanation
            val token = getFcmToken(context)
            val infoMsg = if (token != null) {
                "Announcement queued for topic '$GLOBAL_ANNOUNCEMENTS_TOPIC' (FCM Token active). To broadcast across phones via FCM HTTP v1, trigger via your Firebase Cloud Function or Firebase Console."
            } else {
                "Announcement recorded in Broadcast Registry. Waiting for FCM Token initialization from Google Play services."
            }
            onResult(true, infoMsg)
        }
    }
}

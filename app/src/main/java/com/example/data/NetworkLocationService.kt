package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class NetworkLocationInfo(
    val ip: String,
    val country: String,
    val flagEmoji: String,
    val region: String,
    val city: String,
    val isp: String,
    val timezone: String,
    val serverDate: String = ""
)

sealed interface NetworkLocationState {
    data object Loading : NetworkLocationState
    data class Success(val data: NetworkLocationInfo) : NetworkLocationState
    data class Error(val message: String) : NetworkLocationState
}

class NetworkLocationService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun fetchNetworkLocation(): NetworkLocationState = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://ipwho.is/")
            .header("User-Agent", "Adsearn-Android-Telemetry/1.0")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext NetworkLocationState.Error("HTTP error ${response.code}")
                }
                val bodyString = response.body?.string()
                    ?: return@withContext NetworkLocationState.Error("Empty network telemetry response")

                val json = JSONObject(bodyString)
                val success = json.optBoolean("success", false)
                if (!success) {
                    val msg = json.optString("message", "IP lookup failed")
                    return@withContext NetworkLocationState.Error(msg)
                }

                val ip = json.optString("ip", "Unknown")
                val country = json.optString("country", "Unknown")
                val region = json.optString("region", "Unknown")
                val city = json.optString("city", "Unknown")

                // Extract flag emoji if available, or generate from country_code
                val flagObj = json.optJSONObject("flag")
                val flagEmoji = flagObj?.optString("emoji")?.ifEmpty { null }
                    ?: getFlagEmoji(json.optString("country_code", ""))

                val connectionObj = json.optJSONObject("connection")
                val isp = connectionObj?.optString("isp")?.ifEmpty { null }
                    ?: connectionObj?.optString("org")?.ifEmpty { null }
                    ?: "Unknown ISP"

                val timezoneObj = json.optJSONObject("timezone")
                val timezone = timezoneObj?.optString("id")?.ifEmpty { null }
                    ?: timezoneObj?.optString("utc")?.ifEmpty { null }
                    ?: "UTC"

                val rawTime = timezoneObj?.optString("current_time", "") ?: ""
                val serverDate = if (rawTime.length >= 10 && rawTime.startsWith("20")) {
                    rawTime.substring(0, 10)
                } else {
                    val dateHeader = response.header("Date")
                    parseHttpDate(dateHeader) ?: getReliableUtcDate()
                }

                NetworkLocationState.Success(
                    NetworkLocationInfo(
                        ip = ip,
                        country = country,
                        flagEmoji = flagEmoji,
                        region = region,
                        city = city,
                        isp = isp,
                        timezone = timezone,
                        serverDate = serverDate
                    )
                )
            }
        } catch (e: Exception) {
            NetworkLocationState.Error(e.message ?: "Failed to connect to network service")
        }
    }

    private fun parseHttpDate(httpDate: String?): String? {
        if (httpDate.isNullOrBlank()) return null
        return try {
            val rfc1123 = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", java.util.Locale.US)
            val d = rfc1123.parse(httpDate)
            if (d != null) {
                val outFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                outFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                outFormat.format(d)
            } else null
        } catch (t: Throwable) {
            null
        }
    }

    fun getReliableUtcDate(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date())
    }

    private fun getFlagEmoji(countryCode: String): String {
        if (countryCode.length != 2) return "🌐"
        val firstChar = Character.codePointAt(countryCode.uppercase(), 0) - 0x41 + 0x1F1E6
        val secondChar = Character.codePointAt(countryCode.uppercase(), 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    }
}

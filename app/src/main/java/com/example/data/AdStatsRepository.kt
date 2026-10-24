package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ESTIMATED_ECPM_USD
import com.example.MIN_WITHDRAWAL_INR
import com.example.REWARD_COINS_PER_AD
import com.example.REWARD_INR_PER_AD
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

val Context.adStatsDataStore: DataStore<Preferences> by preferencesDataStore(name = "ad_stats")

enum class WithdrawalStatus {
    PENDING,
    APPROVED,
    PROCESSING,
    PAID,
    REJECTED;

    val displayName: String
        get() = when (this) {
            PENDING -> "Pending"
            APPROVED -> "Approved"
            PROCESSING -> "Processing"
            PAID -> "Paid"
            REJECTED -> "Rejected"
        }
}

data class WithdrawalRequest(
    val id: String = UUID.randomUUID().toString(),
    val amountInr: Double,
    val upiId: String,
    val email: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: WithdrawalStatus = WithdrawalStatus.PENDING,
    val note: String? = null
) {
    val maskedUpi: String
        get() {
            val parts = upiId.split("@")
            if (parts.size != 2) {
                return if (upiId.length > 4) upiId.take(2) + "***" + upiId.takeLast(2) else "***"
            }
            val user = parts[0]
            val handle = parts[1]
            val maskedUser = when {
                user.length <= 3 -> "${user.take(1)}***"
                user.length <= 5 -> "${user.take(2)}***"
                else -> "${user.take(2)}***${user.takeLast(2)}"
            }
            return "$maskedUser@$handle"
        }
}

data class AdStats(
    val totalClicks: Int = 0,
    val rewardedWatched: Int = 0,
    val interstitialsShown: Int = 0,
    val adsSkipped: Int = 0,
    val totalCoinsEarned: Int = 0,
    val loadSuccess: Int = 0,
    val loadFailed: Int = 0,
    val totalRupeesEarned: Double = 0.0,
    val availableRupeesBalance: Double = 0.0,
    val withdrawalRequests: List<WithdrawalRequest> = emptyList(),
    val lastSpinDate: String = "",
    val spinsUsedToday: Int = 0,
    val lastSpinRewardCoins: Int = 0
) {
    val totalImpressions: Int
        get() = rewardedWatched + interstitialsShown

    val spinsRemainingToday: Int
        get() = maxOf(0, com.example.MAX_DAILY_SPINS - spinsUsedToday)

    val ctrPercent: Double
        get() = if (totalImpressions > 0) {
            (totalClicks.toDouble() / totalImpressions.toDouble()) * 100.0
        } else {
            0.0
        }

    val estimatedEarningsUsd: Double
        get() = (totalImpressions.toDouble() / 1000.0) * ESTIMATED_ECPM_USD

    val isEligibleForWithdrawal: Boolean
        get() = availableRupeesBalance >= MIN_WITHDRAWAL_INR
}

class AdStatsRepository(private val context: Context) {

    private object PreferencesKeys {
        val TOTAL_CLICKS = intPreferencesKey("total_clicks")
        val REWARDED_WATCHED = intPreferencesKey("rewarded_watched")
        val INTERSTITIALS_SHOWN = intPreferencesKey("interstitials_shown")
        val ADS_SKIPPED = intPreferencesKey("ads_skipped")
        val TOTAL_COINS_EARNED = intPreferencesKey("total_coins_earned")
        val LOAD_SUCCESS = intPreferencesKey("load_success")
        val LOAD_FAILED = intPreferencesKey("load_failed")
        val TOTAL_RUPEES_EARNED = doublePreferencesKey("total_rupees_earned")
        val AVAILABLE_RUPEES_BALANCE = doublePreferencesKey("available_rupees_balance")
        val WITHDRAWAL_REQUESTS_JSON = stringPreferencesKey("withdrawal_requests_json")
        val LAST_SPIN_DATE = stringPreferencesKey("last_spin_date")
        val SPINS_USED_TODAY = intPreferencesKey("spins_used_today")
        val LAST_SPIN_REWARD_COINS = intPreferencesKey("last_spin_reward_coins")
    }

    val statsFlow: Flow<AdStats> = context.adStatsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val rewardedWatched = preferences[PreferencesKeys.REWARDED_WATCHED] ?: 0
            val defaultTotalRupees = rewardedWatched * REWARD_INR_PER_AD
            val totalRupeesEarned = preferences[PreferencesKeys.TOTAL_RUPEES_EARNED] ?: defaultTotalRupees
            val availableBalance = preferences[PreferencesKeys.AVAILABLE_RUPEES_BALANCE] ?: totalRupeesEarned
            val requestsJson = preferences[PreferencesKeys.WITHDRAWAL_REQUESTS_JSON]
            val requests = parseWithdrawalRequests(requestsJson)

            val lastSpinDate = preferences[PreferencesKeys.LAST_SPIN_DATE] ?: ""
            val spinsUsedToday = preferences[PreferencesKeys.SPINS_USED_TODAY] ?: 0
            val lastSpinRewardCoins = preferences[PreferencesKeys.LAST_SPIN_REWARD_COINS] ?: 0

            AdStats(
                totalClicks = preferences[PreferencesKeys.TOTAL_CLICKS] ?: 0,
                rewardedWatched = rewardedWatched,
                interstitialsShown = preferences[PreferencesKeys.INTERSTITIALS_SHOWN] ?: 0,
                adsSkipped = preferences[PreferencesKeys.ADS_SKIPPED] ?: 0,
                totalCoinsEarned = preferences[PreferencesKeys.TOTAL_COINS_EARNED] ?: 0,
                loadSuccess = preferences[PreferencesKeys.LOAD_SUCCESS] ?: 0,
                loadFailed = preferences[PreferencesKeys.LOAD_FAILED] ?: 0,
                totalRupeesEarned = totalRupeesEarned,
                availableRupeesBalance = availableBalance,
                withdrawalRequests = requests,
                lastSpinDate = lastSpinDate,
                spinsUsedToday = spinsUsedToday,
                lastSpinRewardCoins = lastSpinRewardCoins
            )
        }

    suspend fun recordClick() {
        context.adStatsDataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.TOTAL_CLICKS] ?: 0
            preferences[PreferencesKeys.TOTAL_CLICKS] = current + 1
        }
    }

    suspend fun recordRewardedCompleted() {
        context.adStatsDataStore.edit { preferences ->
            val currentRewarded = preferences[PreferencesKeys.REWARDED_WATCHED] ?: 0
            val currentCoins = preferences[PreferencesKeys.TOTAL_COINS_EARNED] ?: 0
            val currentTotalRupees = preferences[PreferencesKeys.TOTAL_RUPEES_EARNED] ?: (currentRewarded * REWARD_INR_PER_AD)
            val currentAvailable = preferences[PreferencesKeys.AVAILABLE_RUPEES_BALANCE] ?: currentTotalRupees

            preferences[PreferencesKeys.REWARDED_WATCHED] = currentRewarded + 1
            preferences[PreferencesKeys.TOTAL_COINS_EARNED] = currentCoins + REWARD_COINS_PER_AD
            preferences[PreferencesKeys.TOTAL_RUPEES_EARNED] = currentTotalRupees + REWARD_INR_PER_AD
            preferences[PreferencesKeys.AVAILABLE_RUPEES_BALANCE] = currentAvailable + REWARD_INR_PER_AD
        }
    }

    suspend fun recordInterstitialCompleted() {
        context.adStatsDataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.INTERSTITIALS_SHOWN] ?: 0
            preferences[PreferencesKeys.INTERSTITIALS_SHOWN] = current + 1
        }
    }

    suspend fun recordAdSkipped() {
        context.adStatsDataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.ADS_SKIPPED] ?: 0
            preferences[PreferencesKeys.ADS_SKIPPED] = current + 1
        }
    }

    suspend fun recordLoadSuccess() {
        context.adStatsDataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.LOAD_SUCCESS] ?: 0
            preferences[PreferencesKeys.LOAD_SUCCESS] = current + 1
        }
    }

    suspend fun recordLoadFailed() {
        context.adStatsDataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.LOAD_FAILED] ?: 0
            preferences[PreferencesKeys.LOAD_FAILED] = current + 1
        }
    }

    suspend fun submitWithdrawal(
        amount: Double,
        upiId: String,
        email: String?
    ): Result<WithdrawalRequest> {
        val cleanUpi = upiId.trim()
        val upiRegex = Regex("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z0-9.\\-_]{2,64}$")
        if (!upiRegex.matches(cleanUpi)) {
            return Result.failure(IllegalArgumentException("Please enter a valid UPI ID (e.g. mobile@upi, username@okhdfcbank)"))
        }

        val cleanEmail = email?.trim()?.ifEmpty { null }
        if (cleanEmail != null) {
            val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
            if (!emailRegex.matches(cleanEmail)) {
                return Result.failure(IllegalArgumentException("Please enter a valid email address or leave it empty."))
            }
        }

        if (amount < MIN_WITHDRAWAL_INR) {
            return Result.failure(IllegalArgumentException("Minimum withdrawal amount is ₹${MIN_WITHDRAWAL_INR.toInt()}."))
        }

        var resultRequest: WithdrawalRequest? = null
        var failureReason: String? = null

        context.adStatsDataStore.edit { preferences ->
            val currentRewarded = preferences[PreferencesKeys.REWARDED_WATCHED] ?: 0
            val currentTotalRupees = preferences[PreferencesKeys.TOTAL_RUPEES_EARNED] ?: (currentRewarded * REWARD_INR_PER_AD)
            val available = preferences[PreferencesKeys.AVAILABLE_RUPEES_BALANCE] ?: currentTotalRupees

            if (available < amount) {
                failureReason = "Requested amount ₹$amount exceeds your available balance of ₹$available."
                return@edit
            }

            val newAvailable = available - amount
            preferences[PreferencesKeys.AVAILABLE_RUPEES_BALANCE] = newAvailable
            preferences[PreferencesKeys.TOTAL_RUPEES_EARNED] = currentTotalRupees

            val currentRequests = parseWithdrawalRequests(preferences[PreferencesKeys.WITHDRAWAL_REQUESTS_JSON])
            val newReq = WithdrawalRequest(
                id = UUID.randomUUID().toString(),
                amountInr = amount,
                upiId = cleanUpi,
                email = cleanEmail,
                timestamp = System.currentTimeMillis(),
                status = WithdrawalStatus.PENDING,
                note = "Submitted and pending verification."
            )

            val updatedList = listOf(newReq) + currentRequests
            preferences[PreferencesKeys.WITHDRAWAL_REQUESTS_JSON] = serializeWithdrawalRequests(updatedList)
            resultRequest = newReq
        }

        return if (resultRequest != null) {
            Result.success(resultRequest!!)
        } else {
            Result.failure(IllegalStateException(failureReason ?: "Failed to process withdrawal request."))
        }
    }

    suspend fun updateWithdrawalStatus(requestId: String, newStatus: WithdrawalStatus) {
        context.adStatsDataStore.edit { preferences ->
            val currentRequests = parseWithdrawalRequests(preferences[PreferencesKeys.WITHDRAWAL_REQUESTS_JSON])
            var refundAmount = 0.0

            val updatedList = currentRequests.map { req ->
                if (req.id == requestId) {
                    if (newStatus == WithdrawalStatus.REJECTED && req.status != WithdrawalStatus.REJECTED) {
                        refundAmount += req.amountInr
                    }
                    req.copy(status = newStatus)
                } else {
                    req
                }
            }

            if (refundAmount > 0.0) {
                val currentAvailable = preferences[PreferencesKeys.AVAILABLE_RUPEES_BALANCE] ?: 0.0
                preferences[PreferencesKeys.AVAILABLE_RUPEES_BALANCE] = currentAvailable + refundAmount
            }

            preferences[PreferencesKeys.WITHDRAWAL_REQUESTS_JSON] = serializeWithdrawalRequests(updatedList)
        }
    }

    suspend fun syncDailySpins(currentDate: String) {
        if (currentDate.isBlank()) return
        context.adStatsDataStore.edit { preferences ->
            val lastDate = preferences[PreferencesKeys.LAST_SPIN_DATE] ?: ""
            if (lastDate != currentDate) {
                preferences[PreferencesKeys.LAST_SPIN_DATE] = currentDate
                preferences[PreferencesKeys.SPINS_USED_TODAY] = 0
            }
        }
    }

    suspend fun recordSpinResult(coins: Int, currentDate: String): Boolean {
        var success = false
        context.adStatsDataStore.edit { preferences ->
            val lastDate = preferences[PreferencesKeys.LAST_SPIN_DATE] ?: ""
            val usedToday = if (lastDate == currentDate) {
                preferences[PreferencesKeys.SPINS_USED_TODAY] ?: 0
            } else {
                0
            }

            if (usedToday >= com.example.MAX_DAILY_SPINS) {
                success = false
                return@edit
            }

            preferences[PreferencesKeys.LAST_SPIN_DATE] = currentDate
            preferences[PreferencesKeys.SPINS_USED_TODAY] = usedToday + 1
            preferences[PreferencesKeys.LAST_SPIN_REWARD_COINS] = coins

            val currentCoins = preferences[PreferencesKeys.TOTAL_COINS_EARNED] ?: 0
            preferences[PreferencesKeys.TOTAL_COINS_EARNED] = currentCoins + coins
            success = true
        }
        return success
    }

    suspend fun resetStats() {
        context.adStatsDataStore.edit { preferences ->
            preferences[PreferencesKeys.TOTAL_CLICKS] = 0
            preferences[PreferencesKeys.REWARDED_WATCHED] = 0
            preferences[PreferencesKeys.INTERSTITIALS_SHOWN] = 0
            preferences[PreferencesKeys.ADS_SKIPPED] = 0
            preferences[PreferencesKeys.TOTAL_COINS_EARNED] = 0
            preferences[PreferencesKeys.LOAD_SUCCESS] = 0
            preferences[PreferencesKeys.LOAD_FAILED] = 0
            preferences[PreferencesKeys.TOTAL_RUPEES_EARNED] = 0.0
            preferences[PreferencesKeys.AVAILABLE_RUPEES_BALANCE] = 0.0
            preferences[PreferencesKeys.SPINS_USED_TODAY] = 0
            preferences[PreferencesKeys.LAST_SPIN_REWARD_COINS] = 0
        }
    }

    private fun serializeWithdrawalRequests(requests: List<WithdrawalRequest>): String {
        val array = JSONArray()
        for (r in requests) {
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("amountInr", r.amountInr)
            obj.put("upiId", r.upiId)
            if (r.email != null) obj.put("email", r.email)
            obj.put("timestamp", r.timestamp)
            obj.put("status", r.status.name)
            if (r.note != null) obj.put("note", r.note)
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseWithdrawalRequests(jsonStr: String?): List<WithdrawalRequest> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        val list = mutableListOf<WithdrawalRequest>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val amountInr = obj.optDouble("amountInr", 0.0)
                val upiId = obj.optString("upiId", "")
                val email = if (obj.has("email")) obj.optString("email") else null
                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                val statusStr = obj.optString("status", WithdrawalStatus.PENDING.name)
                val status = try {
                    WithdrawalStatus.valueOf(statusStr)
                } catch (e: Exception) {
                    WithdrawalStatus.PENDING
                }
                val note = if (obj.has("note")) obj.optString("note") else null
                list.add(
                    WithdrawalRequest(
                        id = id,
                        amountInr = amountInr,
                        upiId = upiId,
                        email = email,
                        timestamp = timestamp,
                        status = status,
                        note = note
                    )
                )
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }
        return list
    }
}

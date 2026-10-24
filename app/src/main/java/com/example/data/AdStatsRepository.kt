package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
    val note: String? = null,
    val refunded: Boolean = false
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
    val lastSpinRewardCoins: Int = 0,
    val coinRedemptions: List<CoinRedemptionTransaction> = emptyList(),
    val referralState: ReferralState = ReferralState(),
    val referralSettings: ReferralCampaignSettings = ReferralCampaignSettings(),
    val gamesCatalog: List<AdminGameItem> = DefaultGamesCatalog.getDefaultGames(),
    val appVersionInfo: AppVersionInfo = AppVersionInfo(),
    val auditLogs: List<AdminAuditLog> = emptyList()
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

    val isEligibleToRedeemCoins: Boolean
        get() = totalCoinsEarned >= CoinRedemptionTransaction.COINS_PER_UNIT
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

        // New feature keys
        val COIN_REDEMPTIONS_JSON = stringPreferencesKey("coin_redemptions_json")
        val MY_REFERRAL_CODE = stringPreferencesKey("my_referral_code")
        val REFERRAL_STATE_JSON = stringPreferencesKey("referral_state_json")
        val REFERRAL_SETTINGS_JSON = stringPreferencesKey("referral_settings_json")
        val GAMES_CATALOG_JSON = stringPreferencesKey("games_catalog_json")
        val APP_VERSION_INFO_JSON = stringPreferencesKey("app_version_info_json")
        val ADMIN_AUDIT_LOGS_JSON = stringPreferencesKey("admin_audit_logs_json")
        val ADMIN_PASSWORD_HASH = stringPreferencesKey("admin_password_hash")
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

            val redemptions = parseCoinRedemptions(preferences[PreferencesKeys.COIN_REDEMPTIONS_JSON])

            var myCode = preferences[PreferencesKeys.MY_REFERRAL_CODE] ?: ""
            if (myCode.isBlank()) {
                myCode = generateReferralCode()
            }
            val refState = parseReferralState(preferences[PreferencesKeys.REFERRAL_STATE_JSON], myCode)
            val refSettings = parseReferralSettings(preferences[PreferencesKeys.REFERRAL_SETTINGS_JSON])
            val games = parseGamesCatalog(preferences[PreferencesKeys.GAMES_CATALOG_JSON])
            val appVersion = parseAppVersionInfo(preferences[PreferencesKeys.APP_VERSION_INFO_JSON])
            val auditLogs = parseAuditLogs(preferences[PreferencesKeys.ADMIN_AUDIT_LOGS_JSON])

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
                lastSpinRewardCoins = lastSpinRewardCoins,
                coinRedemptions = redemptions,
                referralState = refState,
                referralSettings = refSettings,
                gamesCatalog = games,
                appVersionInfo = appVersion,
                auditLogs = auditLogs
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

    // --- COIN EARNING & REDEEM SYSTEM (20 coins = ₹3) ---
    suspend fun redeemCoins(coinsToRedeem: Int): Result<CoinRedemptionTransaction> {
        if (!CoinRedemptionTransaction.isValidMultiples(coinsToRedeem)) {
            return Result.failure(
                IllegalArgumentException("Coins to redeem must be in multiples of 20 (e.g. 20 coins = ₹3, 40 coins = ₹6, 100 coins = ₹15).")
            )
        }

        val rupeesEarned = CoinRedemptionTransaction.calculateRupees(coinsToRedeem)
        var resultTransaction: CoinRedemptionTransaction? = null
        var failureMessage: String? = null

        context.adStatsDataStore.edit { preferences ->
            val currentCoins = preferences[PreferencesKeys.TOTAL_COINS_EARNED] ?: 0
            if (currentCoins < coinsToRedeem) {
                failureMessage = "Insufficient coins! You have $currentCoins coins, but tried to redeem $coinsToRedeem."
                return@edit
            }

            val currentAvailableRupees = preferences[PreferencesKeys.AVAILABLE_RUPEES_BALANCE] ?: 0.0
            val currentTotalRupees = preferences[PreferencesKeys.TOTAL_RUPEES_EARNED] ?: 0.0

            // Deduct coins and credit equivalent rupees atomically
            preferences[PreferencesKeys.TOTAL_COINS_EARNED] = currentCoins - coinsToRedeem
            preferences[PreferencesKeys.AVAILABLE_RUPEES_BALANCE] = currentAvailableRupees + rupeesEarned
            preferences[PreferencesKeys.TOTAL_RUPEES_EARNED] = currentTotalRupees + rupeesEarned

            val existingRedemptions = parseCoinRedemptions(preferences[PreferencesKeys.COIN_REDEMPTIONS_JSON])
            val newTx = CoinRedemptionTransaction(
                id = UUID.randomUUID().toString(),
                coinsRedeemed = coinsToRedeem,
                rupeesCredited = rupeesEarned,
                timestamp = System.currentTimeMillis(),
                status = "Completed"
            )
            preferences[PreferencesKeys.COIN_REDEMPTIONS_JSON] = serializeCoinRedemptions(listOf(newTx) + existingRedemptions)
            resultTransaction = newTx
        }

        return if (resultTransaction != null) {
            recordAuditLogInternal("COIN_REDEEM", "User redeemed $coinsToRedeem coins for ₹${rupeesEarned.toInt()} Cash.")
            Result.success(resultTransaction!!)
        } else {
            Result.failure(IllegalStateException(failureMessage ?: "Failed to process coin redemption."))
        }
    }

    // --- WALLET & WITHDRAWAL SYSTEM (Minimum ₹500, UPI ID Required) ---
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
            recordAuditLogInternal("WITHDRAWAL_SUBMIT", "User submitted withdrawal of ₹${amount.toInt()} to UPI ${cleanUpi.take(4)}***.")
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
                    val shouldRefund = (newStatus == WithdrawalStatus.REJECTED && !req.refunded)
                    if (shouldRefund) {
                        refundAmount += req.amountInr
                    }
                    req.copy(
                        status = newStatus,
                        refunded = if (shouldRefund) true else req.refunded,
                        note = if (newStatus == WithdrawalStatus.REJECTED) {
                            "Rejected by Admin. Reserved ₹${req.amountInr.toInt()} refunded to wallet balance."
                        } else req.note
                    )
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
        recordAuditLogInternal("WITHDRAWAL_STATUS_UPDATE", "Request $requestId updated to ${newStatus.displayName}.")
    }

    // --- REFERRAL & EARN SYSTEM ---
    suspend fun applyReferralCode(codeToApply: String): Result<Double> {
        val cleanCode = codeToApply.trim().uppercase()
        if (cleanCode.length < 5) {
            return Result.failure(IllegalArgumentException("Invalid referral code length."))
        }

        var rewardGiven = 0.0
        var errorMsg: String? = null

        context.adStatsDataStore.edit { preferences ->
            var myCode = preferences[PreferencesKeys.MY_REFERRAL_CODE] ?: ""
            if (myCode.isBlank()) {
                myCode = generateReferralCode()
                preferences[PreferencesKeys.MY_REFERRAL_CODE] = myCode
            }

            if (cleanCode.equals(myCode, ignoreCase = true)) {
                errorMsg = "You cannot refer yourself! Please share your code with friends."
                return@edit
            }

            val refState = parseReferralState(preferences[PreferencesKeys.REFERRAL_STATE_JSON], myCode)
            if (refState.appliedReferrerCode != null) {
                errorMsg = "You have already applied a referral code (${refState.appliedReferrerCode})."
                return@edit
            }

            val refSettings = parseReferralSettings(preferences[PreferencesKeys.REFERRAL_SETTINGS_JSON])
            val isFirst = !refState.firstReferralClaimed
            val rewardAmount = if (isFirst) refSettings.firstReferralRewardInr else refSettings.subsequentReferralRewardInr

            val newRecord = ReferralRecord(
                id = UUID.randomUUID().toString(),
                friendCode = cleanCode,
                rewardInr = rewardAmount,
                isFirstReferral = isFirst,
                timestamp = System.currentTimeMillis(),
                status = "Credited"
            )

            val updatedState = refState.copy(
                totalReferralsCount = refState.totalReferralsCount + 1,
                firstReferralClaimed = true,
                totalReferralRupeesCredited = refState.totalReferralRupeesCredited + rewardAmount,
                referralHistory = listOf(newRecord) + refState.referralHistory,
                appliedReferrerCode = cleanCode
            )

            // Credit the wallet atomically
            val currentAvailable = preferences[PreferencesKeys.AVAILABLE_RUPEES_BALANCE] ?: 0.0
            val currentTotal = preferences[PreferencesKeys.TOTAL_RUPEES_EARNED] ?: 0.0
            preferences[PreferencesKeys.AVAILABLE_RUPEES_BALANCE] = currentAvailable + rewardAmount
            preferences[PreferencesKeys.TOTAL_RUPEES_EARNED] = currentTotal + rewardAmount
            preferences[PreferencesKeys.REFERRAL_STATE_JSON] = serializeReferralState(updatedState)
            rewardGiven = rewardAmount
        }

        return if (rewardGiven > 0.0) {
            recordAuditLogInternal("REFERRAL_APPLIED", "Applied referral code $cleanCode. Credited ₹${rewardGiven.toInt()} reward.")
            Result.success(rewardGiven)
        } else {
            Result.failure(IllegalStateException(errorMsg ?: "Failed to apply referral code."))
        }
    }

    suspend fun updateReferralSettings(settings: ReferralCampaignSettings) {
        context.adStatsDataStore.edit { preferences ->
            preferences[PreferencesKeys.REFERRAL_SETTINGS_JSON] = serializeReferralSettings(settings)
        }
        recordAuditLogInternal(
            "UPDATE_REFERRAL_SETTINGS",
            "1st Referral: ₹${settings.firstReferralRewardInr.toInt()}, Subsequent: ₹${settings.subsequentReferralRewardInr.toInt()}"
        )
    }

    // --- GAMES CATALOG MANAGEMENT ---
    suspend fun addOrUpdateGame(game: AdminGameItem) {
        context.adStatsDataStore.edit { preferences ->
            val games = parseGamesCatalog(preferences[PreferencesKeys.GAMES_CATALOG_JSON])
            val existingIndex = games.indexOfFirst { it.id == game.id }
            val updated = if (existingIndex >= 0) {
                games.toMutableList().apply { set(existingIndex, game) }
            } else {
                games + game
            }
            preferences[PreferencesKeys.GAMES_CATALOG_JSON] = serializeGamesCatalog(updated)
        }
        recordAuditLogInternal("UPDATE_GAME", "Game ${game.title} (ID: ${game.id}) saved.")
    }

    suspend fun toggleGamePublished(gameId: String, isPublished: Boolean) {
        context.adStatsDataStore.edit { preferences ->
            val games = parseGamesCatalog(preferences[PreferencesKeys.GAMES_CATALOG_JSON])
            val updated = games.map { if (it.id == gameId) it.copy(isPublished = isPublished) else it }
            preferences[PreferencesKeys.GAMES_CATALOG_JSON] = serializeGamesCatalog(updated)
        }
        recordAuditLogInternal("TOGGLE_GAME_PUBLISHED", "Game ID $gameId published state: $isPublished")
    }

    suspend fun deleteGame(gameId: String) {
        context.adStatsDataStore.edit { preferences ->
            val games = parseGamesCatalog(preferences[PreferencesKeys.GAMES_CATALOG_JSON])
            val updated = games.filterNot { it.id == gameId }
            preferences[PreferencesKeys.GAMES_CATALOG_JSON] = serializeGamesCatalog(updated)
        }
        recordAuditLogInternal("DELETE_GAME", "Game ID $gameId removed.")
    }

    suspend fun awardGameCoins(coins: Int, gameTitle: String) {
        context.adStatsDataStore.edit { preferences ->
            val currentCoins = preferences[PreferencesKeys.TOTAL_COINS_EARNED] ?: 0
            preferences[PreferencesKeys.TOTAL_COINS_EARNED] = currentCoins + coins
        }
        recordAuditLogInternal("AWARD_GAME_COINS", "Awarded +$coins coins for playing $gameTitle.")
    }

    // --- APP UPDATE SYSTEM ---
    suspend fun updateAppVersionInfo(info: AppVersionInfo) {
        context.adStatsDataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_VERSION_INFO_JSON] = serializeAppVersionInfo(info)
        }
        recordAuditLogInternal("UPDATE_APP_VERSION", "Latest Version: ${info.latestVersionName} (Code: ${info.latestVersionCode})")
    }

    // --- AUDIT LOG & SECURITY ---
    suspend fun recordAuditLog(action: String, details: String) {
        recordAuditLogInternal(action, details)
    }

    private suspend fun recordAuditLogInternal(action: String, details: String) {
        try {
            context.adStatsDataStore.edit { preferences ->
                val logs = parseAuditLogs(preferences[PreferencesKeys.ADMIN_AUDIT_LOGS_JSON])
                val newLog = AdminAuditLog(action = action, details = details)
                val updated = listOf(newLog) + logs.take(49) // Keep last 50
                preferences[PreferencesKeys.ADMIN_AUDIT_LOGS_JSON] = serializeAuditLogs(updated)
            }
        } catch (_: Exception) {}
    }

    suspend fun updateAdminPassword(newPasswordPlain: String) {
        val newHash = AdminSecurity.hashWithSalt(newPasswordPlain)
        context.adStatsDataStore.edit { preferences ->
            preferences[PreferencesKeys.ADMIN_PASSWORD_HASH] = newHash
        }
        recordAuditLogInternal("ADMIN_PASSWORD_CHANGED", "Admin password hash updated successfully.")
    }

    // --- DAILY SPIN PERSISTENCE ---
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

    // --- HELPERS & SERIALIZERS ---
    private fun generateReferralCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val randomSuffix = (1..5).map { alphabet.random() }.joinToString("")
        return "ADSEARN-$randomSuffix"
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
            obj.put("refunded", r.refunded)
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
                val refunded = obj.optBoolean("refunded", false)
                list.add(
                    WithdrawalRequest(
                        id = id,
                        amountInr = amountInr,
                        upiId = upiId,
                        email = email,
                        timestamp = timestamp,
                        status = status,
                        note = note,
                        refunded = refunded
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun serializeCoinRedemptions(list: List<CoinRedemptionTransaction>): String {
        val array = JSONArray()
        for (t in list) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("coinsRedeemed", t.coinsRedeemed)
            obj.put("rupeesCredited", t.rupeesCredited)
            obj.put("timestamp", t.timestamp)
            obj.put("status", t.status)
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseCoinRedemptions(jsonStr: String?): List<CoinRedemptionTransaction> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        val list = mutableListOf<CoinRedemptionTransaction>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CoinRedemptionTransaction(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        coinsRedeemed = obj.optInt("coinsRedeemed", 0),
                        rupeesCredited = obj.optDouble("rupeesCredited", 0.0),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        status = obj.optString("status", "Completed")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun serializeReferralState(state: ReferralState): String {
        val obj = JSONObject()
        obj.put("myReferralCode", state.myReferralCode)
        obj.put("totalReferralsCount", state.totalReferralsCount)
        obj.put("firstReferralClaimed", state.firstReferralClaimed)
        obj.put("totalReferralRupeesCredited", state.totalReferralRupeesCredited)
        if (state.appliedReferrerCode != null) obj.put("appliedReferrerCode", state.appliedReferrerCode)

        val historyArray = JSONArray()
        for (h in state.referralHistory) {
            val ho = JSONObject()
            ho.put("id", h.id)
            ho.put("friendCode", h.friendCode)
            ho.put("rewardInr", h.rewardInr)
            ho.put("isFirstReferral", h.isFirstReferral)
            ho.put("timestamp", h.timestamp)
            ho.put("status", h.status)
            historyArray.put(ho)
        }
        obj.put("history", historyArray)
        return obj.toString()
    }

    private fun parseReferralState(jsonStr: String?, defaultCode: String): ReferralState {
        if (jsonStr.isNullOrBlank()) {
            return ReferralState(myReferralCode = defaultCode)
        }
        return try {
            val obj = JSONObject(jsonStr)
            val history = mutableListOf<ReferralRecord>()
            val histArr = obj.optJSONArray("history")
            if (histArr != null) {
                for (i in 0 until histArr.length()) {
                    val ho = histArr.getJSONObject(i)
                    history.add(
                        ReferralRecord(
                            id = ho.optString("id", UUID.randomUUID().toString()),
                            friendCode = ho.optString("friendCode", ""),
                            rewardInr = ho.optDouble("rewardInr", 0.0),
                            isFirstReferral = ho.optBoolean("isFirstReferral", false),
                            timestamp = ho.optLong("timestamp", System.currentTimeMillis()),
                            status = ho.optString("status", "Credited")
                        )
                    )
                }
            }
            ReferralState(
                myReferralCode = obj.optString("myReferralCode", defaultCode),
                totalReferralsCount = obj.optInt("totalReferralsCount", 0),
                firstReferralClaimed = obj.optBoolean("firstReferralClaimed", false),
                totalReferralRupeesCredited = obj.optDouble("totalReferralRupeesCredited", 0.0),
                referralHistory = history,
                appliedReferrerCode = if (obj.has("appliedReferrerCode")) obj.optString("appliedReferrerCode") else null
            )
        } catch (_: Exception) {
            ReferralState(myReferralCode = defaultCode)
        }
    }

    private fun serializeReferralSettings(s: ReferralCampaignSettings): String {
        val obj = JSONObject()
        obj.put("firstReferralRewardInr", s.firstReferralRewardInr)
        obj.put("subsequentReferralRewardInr", s.subsequentReferralRewardInr)
        obj.put("minEligibilityRequirement", s.minEligibilityRequirement)
        return obj.toString()
    }

    private fun parseReferralSettings(jsonStr: String?): ReferralCampaignSettings {
        if (jsonStr.isNullOrBlank()) return ReferralCampaignSettings()
        return try {
            val obj = JSONObject(jsonStr)
            ReferralCampaignSettings(
                firstReferralRewardInr = obj.optDouble("firstReferralRewardInr", 50.0),
                subsequentReferralRewardInr = obj.optDouble("subsequentReferralRewardInr", 5.0),
                minEligibilityRequirement = obj.optString("minEligibilityRequirement", "Friend registers and completes at least 1 ad")
            )
        } catch (_: Exception) {
            ReferralCampaignSettings()
        }
    }

    private fun serializeGamesCatalog(list: List<AdminGameItem>): String {
        val array = JSONArray()
        for (g in list) {
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("title", g.title)
            obj.put("description", g.description)
            obj.put("iconEmoji", g.iconEmoji)
            obj.put("gameType", g.gameType)
            obj.put("rewardCoins", g.rewardCoins)
            obj.put("isPublished", g.isPublished)
            obj.put("launchDestination", g.launchDestination)
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseGamesCatalog(jsonStr: String?): List<AdminGameItem> {
        if (jsonStr.isNullOrBlank()) return DefaultGamesCatalog.getDefaultGames()
        val list = mutableListOf<AdminGameItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AdminGameItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", ""),
                        description = obj.optString("description", ""),
                        iconEmoji = obj.optString("iconEmoji", "🎮"),
                        gameType = obj.optString("gameType", "CUSTOM"),
                        rewardCoins = obj.optInt("rewardCoins", 10),
                        isPublished = obj.optBoolean("isPublished", true),
                        launchDestination = obj.optString("launchDestination", "IN_APP")
                    )
                )
            }
        } catch (_: Exception) {
            return DefaultGamesCatalog.getDefaultGames()
        }
        return if (list.isEmpty()) DefaultGamesCatalog.getDefaultGames() else list
    }

    private fun serializeAppVersionInfo(v: AppVersionInfo): String {
        val obj = JSONObject()
        obj.put("latestVersionCode", v.latestVersionCode)
        obj.put("latestVersionName", v.latestVersionName)
        obj.put("releaseNotes", v.releaseNotes)
        obj.put("downloadUrl", v.downloadUrl)
        obj.put("isMandatory", v.isMandatory)
        return obj.toString()
    }

    private fun parseAppVersionInfo(jsonStr: String?): AppVersionInfo {
        if (jsonStr.isNullOrBlank()) return AppVersionInfo()
        return try {
            val obj = JSONObject(jsonStr)
            AppVersionInfo(
                latestVersionCode = obj.optInt("latestVersionCode", 1),
                latestVersionName = obj.optString("latestVersionName", "1.0"),
                releaseNotes = obj.optString("releaseNotes", ""),
                downloadUrl = obj.optString("downloadUrl", "https://github.com/adsearn/releases/latest"),
                isMandatory = obj.optBoolean("isMandatory", false)
            )
        } catch (_: Exception) {
            AppVersionInfo()
        }
    }

    private fun serializeAuditLogs(logs: List<AdminAuditLog>): String {
        val array = JSONArray()
        for (l in logs) {
            val obj = JSONObject()
            obj.put("id", l.id)
            obj.put("action", l.action)
            obj.put("details", l.details)
            obj.put("timestamp", l.timestamp)
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseAuditLogs(jsonStr: String?): List<AdminAuditLog> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        val list = mutableListOf<AdminAuditLog>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AdminAuditLog(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        action = obj.optString("action", ""),
                        details = obj.optString("details", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}

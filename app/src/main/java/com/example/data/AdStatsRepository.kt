package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ESTIMATED_ECPM_USD
import com.example.REWARD_COINS_PER_AD
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.adStatsDataStore: DataStore<Preferences> by preferencesDataStore(name = "ad_stats")

data class AdStats(
    val totalClicks: Int = 0,
    val rewardedWatched: Int = 0,
    val interstitialsShown: Int = 0,
    val adsSkipped: Int = 0,
    val totalCoinsEarned: Int = 0,
    val loadSuccess: Int = 0,
    val loadFailed: Int = 0
) {
    val totalImpressions: Int
        get() = rewardedWatched + interstitialsShown

    val ctrPercent: Double
        get() = if (totalImpressions > 0) {
            (totalClicks.toDouble() / totalImpressions.toDouble()) * 100.0
        } else {
            0.0
        }

    val estimatedEarningsUsd: Double
        get() = (totalImpressions.toDouble() / 1000.0) * ESTIMATED_ECPM_USD
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
            AdStats(
                totalClicks = preferences[PreferencesKeys.TOTAL_CLICKS] ?: 0,
                rewardedWatched = preferences[PreferencesKeys.REWARDED_WATCHED] ?: 0,
                interstitialsShown = preferences[PreferencesKeys.INTERSTITIALS_SHOWN] ?: 0,
                adsSkipped = preferences[PreferencesKeys.ADS_SKIPPED] ?: 0,
                totalCoinsEarned = preferences[PreferencesKeys.TOTAL_COINS_EARNED] ?: 0,
                loadSuccess = preferences[PreferencesKeys.LOAD_SUCCESS] ?: 0,
                loadFailed = preferences[PreferencesKeys.LOAD_FAILED] ?: 0
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
            preferences[PreferencesKeys.REWARDED_WATCHED] = currentRewarded + 1
            preferences[PreferencesKeys.TOTAL_COINS_EARNED] = currentCoins + REWARD_COINS_PER_AD
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

    suspend fun resetStats() {
        context.adStatsDataStore.edit { preferences ->
            preferences[PreferencesKeys.TOTAL_CLICKS] = 0
            preferences[PreferencesKeys.REWARDED_WATCHED] = 0
            preferences[PreferencesKeys.INTERSTITIALS_SHOWN] = 0
            preferences[PreferencesKeys.ADS_SKIPPED] = 0
            preferences[PreferencesKeys.TOTAL_COINS_EARNED] = 0
            preferences[PreferencesKeys.LOAD_SUCCESS] = 0
            preferences[PreferencesKeys.LOAD_FAILED] = 0
        }
    }
}

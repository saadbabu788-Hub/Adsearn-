package com.example.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ads.AdPlacementState
import com.example.ads.UiMessage
import com.example.ads.UnityAdsManager
import com.example.ads.UnitySdkStatus
import com.example.data.AdStats
import com.example.data.AdStatsRepository
import com.example.data.NetworkLocationService
import com.example.data.NetworkLocationState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AdStatsRepository(application)
    private val locationService = NetworkLocationService()
    val adsManager = UnityAdsManager(application, repository, viewModelScope)

    val adStats: StateFlow<AdStats> = repository.statsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AdStats()
    )

    val sdkStatus: StateFlow<UnitySdkStatus> = adsManager.sdkStatus
    val rewardedState: StateFlow<AdPlacementState> = adsManager.rewardedState
    val interstitialState: StateFlow<AdPlacementState> = adsManager.interstitialState
    val isWaitingForRewarded: StateFlow<Boolean> = adsManager.isWaitingForRewarded
    val isWaitingForInterstitial: StateFlow<Boolean> = adsManager.isWaitingForInterstitial
    val lastRawError: StateFlow<String?> = adsManager.lastRawError

    private val _networkLocationState = MutableStateFlow<NetworkLocationState>(NetworkLocationState.Loading)
    val networkLocationState: StateFlow<NetworkLocationState> = _networkLocationState.asStateFlow()

    private val _sessionTimeSeconds = MutableStateFlow(0)
    val sessionTimeSeconds: StateFlow<Int> = _sessionTimeSeconds.asStateFlow()

    private val _showCoinBurst = MutableStateFlow(false)
    val showCoinBurst: StateFlow<Boolean> = _showCoinBurst.asStateFlow()

    private val _showWithdrawalSheet = MutableStateFlow(false)
    val showWithdrawalSheet: StateFlow<Boolean> = _showWithdrawalSheet.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 10)
    val snackbarMessages: SharedFlow<UiMessage> = _snackbarMessages.asSharedFlow()

    init {
        // Initialize Unity Ads SDK
        adsManager.initialize()

        // Fetch real network location
        fetchNetworkLocation()

        // Start live session timer
        startSessionTimer()

        // Forward ads manager messages and trigger coin burst on reward
        viewModelScope.launch {
            adsManager.userMessages.collect { msg ->
                _snackbarMessages.emit(msg)
                if (msg is UiMessage.Success && msg.isReward) {
                    _showCoinBurst.value = true
                }
            }
        }
    }

    fun watchRewardedAd(activity: Activity) {
        adsManager.requestWatchRewardedAd(activity)
    }

    fun showInterstitialAd(activity: Activity) {
        adsManager.requestShowInterstitialAd(activity)
    }

    fun reloadAds() {
        adsManager.reloadAllAds()
    }

    fun fetchNetworkLocation() {
        viewModelScope.launch {
            _networkLocationState.value = NetworkLocationState.Loading
            _networkLocationState.value = locationService.fetchNetworkLocation()
        }
    }

    fun resetStats() {
        viewModelScope.launch {
            repository.resetStats()
            _snackbarMessages.emit(UiMessage.Info("All ad counters reset to 0."))
        }
    }

    fun openWithdrawalSheet() {
        _showWithdrawalSheet.value = true
    }

    fun closeWithdrawalSheet() {
        _showWithdrawalSheet.value = false
    }

    fun submitWithdrawal(
        amount: Double,
        upiId: String,
        email: String?,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.submitWithdrawal(amount, upiId, email)
            result.fold(
                onSuccess = { req ->
                    val msg = "Your withdrawal request for ₹${req.amountInr.toInt()} has been submitted successfully and is pending review."
                    _snackbarMessages.emit(UiMessage.Success(msg))
                    onComplete(true, msg)
                },
                onFailure = { err ->
                    val errorMsg = err.message ?: "Failed to submit withdrawal request."
                    _snackbarMessages.emit(UiMessage.Error(errorMsg))
                    onComplete(false, errorMsg)
                }
            )
        }
    }

    fun onCoinBurstFinished() {
        _showCoinBurst.value = false
    }

    private fun startSessionTimer() {
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _sessionTimeSeconds.value += 1
            }
        }
    }

    fun formattedSessionTime(): String {
        val totalSecs = _sessionTimeSeconds.value
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}

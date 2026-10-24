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
import com.example.data.AdminGameItem
import com.example.data.AppVersionInfo
import com.example.data.NetworkLocationService
import com.example.data.NetworkLocationState
import com.example.data.ReferralCampaignSettings
import com.example.data.WithdrawalStatus
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

    private val _showRedeemCoinsSheet = MutableStateFlow(false)
    val showRedeemCoinsSheet: StateFlow<Boolean> = _showRedeemCoinsSheet.asStateFlow()

    private val _isAdminAuthenticated = MutableStateFlow(false)
    val isAdminAuthenticated: StateFlow<Boolean> = _isAdminAuthenticated.asStateFlow()

    private val _showAdminLoginDialog = MutableStateFlow(false)
    val showAdminLoginDialog: StateFlow<Boolean> = _showAdminLoginDialog.asStateFlow()

    private val _showAdminDashboard = MutableStateFlow(false)
    val showAdminDashboard: StateFlow<Boolean> = _showAdminDashboard.asStateFlow()

    private val _dismissedUpdateVersionCode = MutableStateFlow(0)
    val dismissedUpdateVersionCode: StateFlow<Int> = _dismissedUpdateVersionCode.asStateFlow()

    private val _isSpinning = MutableStateFlow(false)
    val isSpinning: StateFlow<Boolean> = _isSpinning.asStateFlow()

    private val _showAutoAdPrompt = MutableStateFlow(false)
    val showAutoAdPrompt: StateFlow<Boolean> = _showAutoAdPrompt.asStateFlow()

    private val _autoAdCountdown = MutableStateFlow(3)
    val autoAdCountdown: StateFlow<Int> = _autoAdCountdown.asStateFlow()

    private var autoAdJob: kotlinx.coroutines.Job? = null

    private val _snackbarMessages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 10)
    val snackbarMessages: SharedFlow<UiMessage> = _snackbarMessages.asSharedFlow()

    init {
        // Initialize Unity Ads SDK
        adsManager.initialize()

        // Fetch real network location
        fetchNetworkLocation()

        // Sync initial daily spin state with reliable date
        viewModelScope.launch {
            val date = locationService.getReliableUtcDate()
            repository.syncDailySpins(date)
        }

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
            val result = locationService.fetchNetworkLocation()
            _networkLocationState.value = result
            if (result is NetworkLocationState.Success && result.data.serverDate.isNotBlank()) {
                repository.syncDailySpins(result.data.serverDate)
            }
        }
    }

    fun getEffectiveDate(): String {
        val net = _networkLocationState.value
        return if (net is NetworkLocationState.Success && net.data.serverDate.isNotBlank()) {
            net.data.serverDate
        } else {
            locationService.getReliableUtcDate()
        }
    }

    fun triggerSpin(
        activity: Activity,
        coinsWon: Int,
        startWheelAnimation: (Int) -> Unit
    ) {
        if (_isSpinning.value) return
        val currentStats = adStats.value
        if (currentStats.spinsRemainingToday <= 0) {
            _snackbarMessages.tryEmit(
                UiMessage.Info("0 spins remaining today. Daily free spins reset tomorrow at 00:00 UTC!")
            )
            return
        }

        val isSecondSpin = (currentStats.spinsUsedToday == 1)

        // Pre-warm and load the rewarded ad before the second spin finishes
        if (isSecondSpin) {
            adsManager.loadRewardedAd()
        }

        _isSpinning.value = true
        startWheelAnimation(coinsWon)

        viewModelScope.launch {
            delay(3900)

            val currentDate = getEffectiveDate()
            val recorded = repository.recordSpinResult(coinsWon, currentDate)
            if (recorded) {
                _snackbarMessages.emit(
                    UiMessage.Success(
                        message = "🎉 You won +$coinsWon Coins from Daily Spin!",
                        isReward = true
                    )
                )
                _showCoinBurst.value = true
            }

            _isSpinning.value = false

            if (isSecondSpin && recorded) {
                launchAutoAdFlow(activity)
            }
        }
    }

    private fun launchAutoAdFlow(activity: Activity) {
        autoAdJob?.cancel()
        _showAutoAdPrompt.value = true
        _autoAdCountdown.value = 3

        autoAdJob = viewModelScope.launch {
            for (sec in 3 downTo 1) {
                _autoAdCountdown.value = sec
                delay(1000)
            }
            _autoAdCountdown.value = 0
            if (_showAutoAdPrompt.value) {
                _showAutoAdPrompt.value = false
                executeAutoRewardedAd(activity)
            }
        }
    }

    fun confirmWatchAutoAd(activity: Activity) {
        autoAdJob?.cancel()
        _showAutoAdPrompt.value = false
        executeAutoRewardedAd(activity)
    }

    fun dismissAutoAdPrompt() {
        autoAdJob?.cancel()
        _showAutoAdPrompt.value = false
    }

    private fun executeAutoRewardedAd(activity: Activity) {
        if (adsManager.rewardedState.value is AdPlacementState.Ready) {
            adsManager.showRewardedAdIfReady(activity)
        } else {
            _snackbarMessages.tryEmit(
                UiMessage.Info("No ad available right now. Please try again later.")
            )
        }
    }

    fun checkDailyReminderNotification(context: android.content.Context) {
        val remaining = adStats.value.spinsRemainingToday
        val currentDate = getEffectiveDate()
        com.example.notification.DailySpinNotificationHelper.notifyDailySpinsAvailable(context, remaining, currentDate)
    }

    fun resetStats() {
        viewModelScope.launch {
            repository.resetStats()
            _snackbarMessages.emit(UiMessage.Info("All ad counters & spin limits reset."))
        }
    }

    // --- WITHDRAWAL ACTIONS ---
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

    // --- COIN REDEMPTION (20 coins = ₹3) ---
    fun openRedeemCoinsSheet() {
        _showRedeemCoinsSheet.value = true
    }

    fun closeRedeemCoinsSheet() {
        _showRedeemCoinsSheet.value = false
    }

    fun redeemCoins(
        coins: Int,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.redeemCoins(coins)
            result.fold(
                onSuccess = { tx ->
                    val msg = "Converted ${tx.coinsRedeemed} Coins into ₹${tx.rupeesCredited.toInt()} Cash!"
                    _snackbarMessages.emit(UiMessage.Success(msg, isReward = true))
                    _showCoinBurst.value = true
                    onComplete(true, msg)
                },
                onFailure = { err ->
                    val errorMsg = err.message ?: "Failed to redeem coins."
                    _snackbarMessages.emit(UiMessage.Error(errorMsg))
                    onComplete(false, errorMsg)
                }
            )
        }
    }

    // --- REFERRAL & EARN ACTIONS ---
    fun applyReferralCode(
        code: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.applyReferralCode(code)
            result.fold(
                onSuccess = { amount ->
                    val msg = "${amount.toInt()}"
                    _snackbarMessages.emit(UiMessage.Success("Referral bonus of ₹${amount.toInt()} credited to your wallet!", isReward = true))
                    _showCoinBurst.value = true
                    onComplete(true, msg)
                },
                onFailure = { err ->
                    val errString = err.message ?: "Failed to apply referral code."
                    _snackbarMessages.emit(UiMessage.Error(errString))
                    onComplete(false, errString)
                }
            )
        }
    }

    fun updateReferralSettings(settings: ReferralCampaignSettings) {
        viewModelScope.launch {
            repository.updateReferralSettings(settings)
            _snackbarMessages.emit(UiMessage.Success("Referral settings updated."))
        }
    }

    // --- GAMES ACTIONS ---
    fun addOrUpdateGame(game: AdminGameItem) {
        viewModelScope.launch {
            repository.addOrUpdateGame(game)
            _snackbarMessages.emit(UiMessage.Success("Game '${game.title}' saved."))
        }
    }

    fun toggleGamePublished(gameId: String, isPublished: Boolean) {
        viewModelScope.launch {
            repository.toggleGamePublished(gameId, isPublished)
        }
    }

    fun deleteGame(gameId: String) {
        viewModelScope.launch {
            repository.deleteGame(gameId)
            _snackbarMessages.emit(UiMessage.Info("Game removed."))
        }
    }

    fun awardGameCoins(coins: Int, gameTitle: String) {
        viewModelScope.launch {
            repository.awardGameCoins(coins, gameTitle)
            _snackbarMessages.emit(UiMessage.Success("+$coins Coins earned from $gameTitle!", isReward = true))
            _showCoinBurst.value = true
        }
    }

    // --- ADMIN AUTH & CONTROL ---
    fun openAdminLogin() {
        if (_isAdminAuthenticated.value) {
            _showAdminDashboard.value = true
        } else {
            _showAdminLoginDialog.value = true
        }
    }

    fun dismissAdminLogin() {
        _showAdminLoginDialog.value = false
    }

    fun onAdminLoginSuccess() {
        _isAdminAuthenticated.value = true
        _showAdminLoginDialog.value = false
        _showAdminDashboard.value = true
        viewModelScope.launch {
            _snackbarMessages.emit(UiMessage.Success("Admin access granted."))
        }
    }

    fun dismissAdminDashboard() {
        _showAdminDashboard.value = false
    }

    fun logoutAdmin() {
        _isAdminAuthenticated.value = false
        _showAdminDashboard.value = false
        viewModelScope.launch {
            _snackbarMessages.emit(UiMessage.Info("Admin logged out successfully."))
        }
    }

    fun updateWithdrawalStatus(requestId: String, status: WithdrawalStatus) {
        viewModelScope.launch {
            repository.updateWithdrawalStatus(requestId, status)
            _snackbarMessages.emit(UiMessage.Success("Withdrawal status updated to ${status.displayName}."))
        }
    }

    fun updateAppVersionInfo(info: AppVersionInfo) {
        viewModelScope.launch {
            repository.updateAppVersionInfo(info)
            _snackbarMessages.emit(UiMessage.Success("App version information updated."))
        }
    }

    fun dismissUpdateDialog(versionCode: Int) {
        _dismissedUpdateVersionCode.value = versionCode
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

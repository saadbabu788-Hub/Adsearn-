package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.INTERSTITIAL_PLACEMENT_ID
import com.example.REWARD_COINS_PER_AD
import com.example.REWARD_INR_PER_AD
import com.example.REWARDED_PLACEMENT_ID
import com.example.TEST_MODE
import com.example.UNITY_GAME_ID
import com.example.data.AdStatsRepository
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "UnityAdsManager"

sealed interface UnitySdkStatus {
    data object Uninitialized : UnitySdkStatus
    data object Initializing : UnitySdkStatus
    data object Initialized : UnitySdkStatus
    data class Failed(val errorCode: String, val rawMessage: String) : UnitySdkStatus
}

sealed interface AdPlacementState {
    data object NotLoaded : AdPlacementState
    data object Loading : AdPlacementState
    data object Ready : AdPlacementState
    data object Showing : AdPlacementState
    data class Failed(val errorCode: String, val rawMessage: String) : AdPlacementState
}

sealed interface UiMessage {
    data class Success(val message: String, val isReward: Boolean = false) : UiMessage
    data class Info(val message: String) : UiMessage
    data class Error(val message: String) : UiMessage
}

class UnityAdsManager(
    private val context: Context,
    private val repository: AdStatsRepository,
    private val scope: CoroutineScope
) {
    private val _sdkStatus = MutableStateFlow<UnitySdkStatus>(UnitySdkStatus.Uninitialized)
    val sdkStatus: StateFlow<UnitySdkStatus> = _sdkStatus.asStateFlow()

    private val _rewardedState = MutableStateFlow<AdPlacementState>(AdPlacementState.NotLoaded)
    val rewardedState: StateFlow<AdPlacementState> = _rewardedState.asStateFlow()

    private val _interstitialState = MutableStateFlow<AdPlacementState>(AdPlacementState.NotLoaded)
    val interstitialState: StateFlow<AdPlacementState> = _interstitialState.asStateFlow()

    private val _userMessages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 10)
    val userMessages: SharedFlow<UiMessage> = _userMessages.asSharedFlow()

    private val _isWaitingForRewarded = MutableStateFlow(false)
    val isWaitingForRewarded: StateFlow<Boolean> = _isWaitingForRewarded.asStateFlow()

    private val _isWaitingForInterstitial = MutableStateFlow(false)
    val isWaitingForInterstitial: StateFlow<Boolean> = _isWaitingForInterstitial.asStateFlow()

    private val _lastRawError = MutableStateFlow<String?>(null)
    val lastRawError: StateFlow<String?> = _lastRawError.asStateFlow()

    private var rewardedRetryJob: Job? = null
    private var interstitialRetryJob: Job? = null

    private var pendingRewardedJob: Job? = null
    private var pendingInterstitialJob: Job? = null

    fun initialize() {
        if (_sdkStatus.value is UnitySdkStatus.Initializing || _sdkStatus.value is UnitySdkStatus.Initialized) {
            return
        }

        _sdkStatus.value = UnitySdkStatus.Initializing
        Log.d(TAG, "Initializing Unity Ads with Game ID: $UNITY_GAME_ID, Test Mode: $TEST_MODE")

        try {
            UnityAds.initialize(
                context.applicationContext,
                UNITY_GAME_ID,
                TEST_MODE,
                object : IUnityAdsInitializationListener {
                    override fun onInitializationComplete() {
                        Log.d(TAG, "Unity Ads Initialization Complete")
                        _sdkStatus.value = UnitySdkStatus.Initialized
                        _lastRawError.value = null

                        // Auto-load both Rewarded and Interstitial ads
                        loadRewardedAd()
                        loadInterstitialAd()
                    }

                    override fun onInitializationFailed(
                        error: UnityAds.UnityAdsInitializationError?,
                        message: String?
                    ) {
                        val errCode = error?.name ?: "UNKNOWN_INIT_ERROR"
                        val rawMsg = message ?: "No message provided by Unity Ads SDK"
                        Log.e(TAG, "Unity Ads Init Failed: [$errCode] $rawMsg")
                        _sdkStatus.value = UnitySdkStatus.Failed(errCode, rawMsg)
                        _lastRawError.value = "Init error [$errCode]: $rawMsg"
                        _userMessages.tryEmit(UiMessage.Error("Unity Ads Init Error: [$errCode] $rawMsg"))
                    }
                }
            )
        } catch (t: Throwable) {
            Log.e(TAG, "UnityAds.initialize exception", t)
            _sdkStatus.value = UnitySdkStatus.Failed("INIT_EXCEPTION", t.message ?: "Exception initializing SDK")
            _lastRawError.value = "Init exception: ${t.message}"
        }
    }

    fun reloadAllAds() {
        if (_sdkStatus.value !is UnitySdkStatus.Initialized) {
            initialize()
            return
        }
        loadRewardedAd()
        loadInterstitialAd()
    }

    fun loadRewardedAd() {
        rewardedRetryJob?.cancel()
        _rewardedState.value = AdPlacementState.Loading
        Log.d(TAG, "Loading Rewarded Ad: $REWARDED_PLACEMENT_ID")

        try {
            UnityAds.load(
                REWARDED_PLACEMENT_ID,
                object : IUnityAdsLoadListener {
                    override fun onUnityAdsAdLoaded(placementId: String?) {
                        Log.d(TAG, "Rewarded Ad Loaded: $placementId")
                        _rewardedState.value = AdPlacementState.Ready
                        scope.launch { repository.recordLoadSuccess() }
                    }

                    override fun onUnityAdsFailedToLoad(
                        placementId: String?,
                        error: UnityAds.UnityAdsLoadError?,
                        message: String?
                    ) {
                        val errCode = error?.name ?: "UNKNOWN_LOAD_ERROR"
                        val rawMsg = message ?: "No message provided"
                        Log.e(TAG, "Rewarded Ad Failed to Load: [$errCode] $rawMsg")
                        _rewardedState.value = AdPlacementState.Failed(errCode, rawMsg)
                        _lastRawError.value = "Rewarded Load Error [$errCode]: $rawMsg"
                        scope.launch { repository.recordLoadFailed() }

                        // Retry after 5 seconds repeatedly
                        rewardedRetryJob = scope.launch {
                            delay(5000)
                            if (_rewardedState.value !is AdPlacementState.Ready &&
                                _rewardedState.value !is AdPlacementState.Showing
                            ) {
                                loadRewardedAd()
                            }
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            Log.e(TAG, "UnityAds.load rewarded exception", t)
            _rewardedState.value = AdPlacementState.Failed("LOAD_EXCEPTION", t.message ?: "Exception loading ad")
        }
    }

    fun loadInterstitialAd() {
        interstitialRetryJob?.cancel()
        _interstitialState.value = AdPlacementState.Loading
        Log.d(TAG, "Loading Interstitial Ad: $INTERSTITIAL_PLACEMENT_ID")

        try {
            UnityAds.load(
                INTERSTITIAL_PLACEMENT_ID,
                object : IUnityAdsLoadListener {
                    override fun onUnityAdsAdLoaded(placementId: String?) {
                        Log.d(TAG, "Interstitial Ad Loaded: $placementId")
                        _interstitialState.value = AdPlacementState.Ready
                        scope.launch { repository.recordLoadSuccess() }
                    }

                    override fun onUnityAdsFailedToLoad(
                        placementId: String?,
                        error: UnityAds.UnityAdsLoadError?,
                        message: String?
                    ) {
                        val errCode = error?.name ?: "UNKNOWN_LOAD_ERROR"
                        val rawMsg = message ?: "No message provided"
                        Log.e(TAG, "Interstitial Ad Failed to Load: [$errCode] $rawMsg")
                        _interstitialState.value = AdPlacementState.Failed(errCode, rawMsg)
                        _lastRawError.value = "Interstitial Load Error [$errCode]: $rawMsg"
                        scope.launch { repository.recordLoadFailed() }

                        // Retry after 5 seconds repeatedly
                        interstitialRetryJob = scope.launch {
                            delay(5000)
                            if (_interstitialState.value !is AdPlacementState.Ready &&
                                _interstitialState.value !is AdPlacementState.Showing
                            ) {
                                loadInterstitialAd()
                            }
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            Log.e(TAG, "UnityAds.load interstitial exception", t)
            _interstitialState.value = AdPlacementState.Failed("LOAD_EXCEPTION", t.message ?: "Exception loading ad")
        }
    }

    fun requestWatchRewardedAd(activity: Activity) {
        if (_isWaitingForRewarded.value || _rewardedState.value is AdPlacementState.Showing) {
            return
        }

        if (_rewardedState.value is AdPlacementState.Ready) {
            showRewardedAd(activity)
            return
        }

        // Ad is still loading or not loaded. Show loading progress inside button and wait up to 30s.
        _isWaitingForRewarded.value = true
        if (_rewardedState.value !is AdPlacementState.Loading) {
            loadRewardedAd()
        }

        pendingRewardedJob?.cancel()
        pendingRewardedJob = scope.launch {
            val result = withTimeoutOrNull(30_000) {
                _rewardedState.first { it is AdPlacementState.Ready }
            }
            _isWaitingForRewarded.value = false
            if (result != null) {
                showRewardedAd(activity)
            } else {
                _userMessages.emit(UiMessage.Info("No ad available right now, please try again"))
            }
        }
    }

    fun requestShowInterstitialAd(activity: Activity) {
        if (_isWaitingForInterstitial.value || _interstitialState.value is AdPlacementState.Showing) {
            return
        }

        if (_interstitialState.value is AdPlacementState.Ready) {
            showInterstitialAd(activity)
            return
        }

        // Ad is still loading or not loaded. Show loading progress inside button and wait up to 30s.
        _isWaitingForInterstitial.value = true
        if (_interstitialState.value !is AdPlacementState.Loading) {
            loadInterstitialAd()
        }

        pendingInterstitialJob?.cancel()
        pendingInterstitialJob = scope.launch {
            val result = withTimeoutOrNull(30_000) {
                _interstitialState.first { it is AdPlacementState.Ready }
            }
            _isWaitingForInterstitial.value = false
            if (result != null) {
                showInterstitialAd(activity)
            } else {
                _userMessages.emit(UiMessage.Info("No ad available right now, please try again"))
            }
        }
    }

    private fun showRewardedAd(activity: Activity) {
        _rewardedState.value = AdPlacementState.Showing
        Log.d(TAG, "Showing Rewarded Ad: $REWARDED_PLACEMENT_ID")

        try {
            UnityAds.show(
                activity,
                REWARDED_PLACEMENT_ID,
                UnityAdsShowOptions(),
                object : IUnityAdsShowListener {
                    override fun onUnityAdsShowFailure(
                        placementId: String?,
                        error: UnityAds.UnityAdsShowError?,
                        message: String?
                    ) {
                        val errCode = error?.name ?: "UNKNOWN_SHOW_ERROR"
                        val rawMsg = message ?: "No message provided"
                        Log.e(TAG, "Rewarded Show Failure: [$errCode] $rawMsg")
                        _rewardedState.value = AdPlacementState.Failed(errCode, rawMsg)
                        _lastRawError.value = "Rewarded Show Error [$errCode]: $rawMsg"
                        _userMessages.tryEmit(UiMessage.Error("Ad Show Failed: [$errCode] $rawMsg"))

                        // Silently load next ad
                        loadRewardedAd()
                    }

                    private var rewardHandled = false

                    override fun onUnityAdsShowStart(placementId: String?) {
                        Log.d(TAG, "Rewarded Ad Started: $placementId")
                        _lastRawError.value = null
                        rewardHandled = false
                    }

                    override fun onUnityAdsShowClick(placementId: String?) {
                        Log.d(TAG, "Rewarded Ad Clicked: $placementId")
                        scope.launch { repository.recordClick() }
                    }

                    override fun onUnityAdsShowComplete(
                        placementId: String?,
                        state: UnityAds.UnityAdsShowCompletionState?
                    ) {
                        Log.d(TAG, "Rewarded Ad Complete: state = $state")
                        when (state) {
                            UnityAds.UnityAdsShowCompletionState.COMPLETED -> {
                                if (!rewardHandled) {
                                    rewardHandled = true
                                    scope.launch {
                                        repository.recordRewardedCompleted()
                                        _userMessages.emit(
                                            UiMessage.Success(
                                                message = "Congratulations! You earned +$REWARD_COINS_PER_AD Coins & ₹${REWARD_INR_PER_AD.toInt()} Cash!",
                                                isReward = true
                                            )
                                        )
                                    }
                                }
                            }
                            UnityAds.UnityAdsShowCompletionState.SKIPPED -> {
                                scope.launch {
                                    repository.recordAdSkipped()
                                    _userMessages.emit(UiMessage.Info("Ad was skipped. No coins or money awarded."))
                                }
                            }
                            null -> {
                                // Non-standard completion
                            }
                        }

                        _rewardedState.value = AdPlacementState.NotLoaded
                        // Silently load next ad
                        loadRewardedAd()
                    }
                }
            )
        } catch (t: Throwable) {
            Log.e(TAG, "UnityAds.show rewarded exception", t)
            _rewardedState.value = AdPlacementState.Failed("SHOW_EXCEPTION", t.message ?: "Exception showing ad")
            _lastRawError.value = "Show exception: ${t.message}"
            loadRewardedAd()
        }
    }

    private fun showInterstitialAd(activity: Activity) {
        _interstitialState.value = AdPlacementState.Showing
        Log.d(TAG, "Showing Interstitial Ad: $INTERSTITIAL_PLACEMENT_ID")

        try {
            UnityAds.show(
                activity,
                INTERSTITIAL_PLACEMENT_ID,
                UnityAdsShowOptions(),
                object : IUnityAdsShowListener {
                    override fun onUnityAdsShowFailure(
                        placementId: String?,
                        error: UnityAds.UnityAdsShowError?,
                        message: String?
                    ) {
                        val errCode = error?.name ?: "UNKNOWN_SHOW_ERROR"
                        val rawMsg = message ?: "No message provided"
                        Log.e(TAG, "Interstitial Show Failure: [$errCode] $rawMsg")
                        _interstitialState.value = AdPlacementState.Failed(errCode, rawMsg)
                        _lastRawError.value = "Interstitial Show Error [$errCode]: $rawMsg"
                        _userMessages.tryEmit(UiMessage.Error("Interstitial Show Failed: [$errCode] $rawMsg"))

                        // Silently load next ad
                        loadInterstitialAd()
                    }

                    override fun onUnityAdsShowStart(placementId: String?) {
                        Log.d(TAG, "Interstitial Ad Started: $placementId")
                        _lastRawError.value = null
                    }

                    override fun onUnityAdsShowClick(placementId: String?) {
                        Log.d(TAG, "Interstitial Ad Clicked: $placementId")
                        scope.launch { repository.recordClick() }
                    }

                    override fun onUnityAdsShowComplete(
                        placementId: String?,
                        state: UnityAds.UnityAdsShowCompletionState?
                    ) {
                        Log.d(TAG, "Interstitial Ad Complete: state = $state")
                        if (state == UnityAds.UnityAdsShowCompletionState.SKIPPED) {
                            scope.launch { repository.recordAdSkipped() }
                        } else if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                            scope.launch { repository.recordInterstitialCompleted() }
                        }

                        _interstitialState.value = AdPlacementState.NotLoaded
                        // Silently load next ad
                        loadInterstitialAd()
                    }
                }
            )
        } catch (t: Throwable) {
            Log.e(TAG, "UnityAds.show interstitial exception", t)
            _interstitialState.value = AdPlacementState.Failed("SHOW_EXCEPTION", t.message ?: "Exception showing ad")
            _lastRawError.value = "Show exception: ${t.message}"
            loadInterstitialAd()
        }
    }
}

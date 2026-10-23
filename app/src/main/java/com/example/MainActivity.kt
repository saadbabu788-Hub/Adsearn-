package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ads.UiMessage
import com.example.ui.MainViewModel
import com.example.ui.components.AdActionButtons
import com.example.ui.components.AdPerformanceCard
import com.example.ui.components.AdsEngineStatusCard
import com.example.ui.components.AppFooter
import com.example.ui.components.CoinBalanceCard
import com.example.ui.components.CoinConfettiOverlay
import com.example.ui.components.DeviceSessionCard
import com.example.ui.components.HeaderCard
import com.example.ui.components.NetworkLocationCard
import com.example.ui.components.WithdrawalSheet
import com.example.ui.theme.CosmicNavy
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RubyError
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AdsearnApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdsearnApp(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val adStats by viewModel.adStats.collectAsStateWithLifecycle()
    val sdkStatus by viewModel.sdkStatus.collectAsStateWithLifecycle()
    val rewardedState by viewModel.rewardedState.collectAsStateWithLifecycle()
    val interstitialState by viewModel.interstitialState.collectAsStateWithLifecycle()
    val isWaitingForRewarded by viewModel.isWaitingForRewarded.collectAsStateWithLifecycle()
    val isWaitingForInterstitial by viewModel.isWaitingForInterstitial.collectAsStateWithLifecycle()
    val lastRawError by viewModel.lastRawError.collectAsStateWithLifecycle()
    val networkLocationState by viewModel.networkLocationState.collectAsStateWithLifecycle()
    val showCoinBurst by viewModel.showCoinBurst.collectAsStateWithLifecycle()
    val showWithdrawalSheet by viewModel.showWithdrawalSheet.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { msg ->
            when (msg) {
                is UiMessage.Success -> snackbarHostState.showSnackbar(msg.message)
                is UiMessage.Info -> snackbarHostState.showSnackbar(msg.message)
                is UiMessage.Error -> snackbarHostState.showSnackbar(msg.message)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeepPurpleDark,
                        Color(0xFF110A26),
                        CosmicNavy,
                        Color(0xFF060410)
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = TextPrimary,
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = SurfaceDark,
                        contentColor = TextPrimary,
                        actionColor = EmeraldSuccess,
                        dismissActionContentColor = RubyError
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp)
                        .verticalScroll(rememberScrollState())
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header with Logo and Title
                    HeaderCard()

                    // Coin & Rupee Balance Card with Withdraw Button
                    CoinBalanceCard(
                        coins = adStats.totalCoinsEarned,
                        availableRupees = adStats.availableRupeesBalance,
                        onWithdrawClick = { viewModel.openWithdrawalSheet() }
                    )

                    // Big Action Buttons for Rewarded and Interstitial ads
                    AdActionButtons(
                        isWaitingForRewarded = isWaitingForRewarded,
                        isWaitingForInterstitial = isWaitingForInterstitial,
                        onWatchRewardedClick = {
                            activity?.let { viewModel.watchRewardedAd(it) }
                        },
                        onShowInterstitialClick = {
                            activity?.let { viewModel.showInterstitialAd(it) }
                        }
                    )

                    // Ads Engine Status Card
                    AdsEngineStatusCard(
                        sdkStatus = sdkStatus,
                        rewardedState = rewardedState,
                        interstitialState = interstitialState,
                        lastRawError = lastRawError,
                        onReloadClick = { viewModel.reloadAds() }
                    )

                    // Ad Performance Grid & Estimated Revenue
                    AdPerformanceCard(adStats = adStats)

                    // Real Network & Location Telemetry Card
                    NetworkLocationCard(
                        state = networkLocationState,
                        onRefreshClick = { viewModel.fetchNetworkLocation() }
                    )

                    // Device Specs & Live Session Card
                    DeviceSessionCard(
                        sessionTime = viewModel.formattedSessionTime(),
                        sdkStatus = sdkStatus,
                        onResetStats = { viewModel.resetStats() }
                    )

                    // Footer with Package Name and Mode
                    AppFooter()
                }

                // Coin Burst Particle Overlay on Reward
                CoinConfettiOverlay(
                    show = showCoinBurst,
                    onFinished = { viewModel.onCoinBurstFinished() }
                )

                // Withdrawal BottomSheet
                if (showWithdrawalSheet) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    WithdrawalSheet(
                        availableBalance = adStats.availableRupeesBalance,
                        withdrawalRequests = adStats.withdrawalRequests,
                        sheetState = sheetState,
                        onDismiss = { viewModel.closeWithdrawalSheet() },
                        onSubmitWithdrawal = { amount, upiId, email, onComplete ->
                            viewModel.submitWithdrawal(amount, upiId, email, onComplete)
                        }
                    )
                }
            }
        }
    }
}

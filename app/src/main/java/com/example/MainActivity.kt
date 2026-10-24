package com.example

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ads.UiMessage
import com.example.ui.MainViewModel
import com.example.ui.components.AdActionButtons
import com.example.ui.components.AdPerformanceCard
import com.example.ui.components.AdminDashboardSheet
import com.example.ui.components.AdminLoginDialog
import com.example.ui.components.AdsEngineStatusCard
import com.example.ui.components.AppFooter
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.CoinBalanceCard
import com.example.ui.components.CoinConfettiOverlay
import com.example.ui.components.DailySpinCard
import com.example.ui.components.DeviceSessionCard
import com.example.ui.components.GamesSection
import com.example.ui.components.HeaderCard
import com.example.ui.components.NetworkLocationCard
import com.example.ui.components.RedeemCoinsSheet
import com.example.ui.components.ReferralSection
import com.example.ui.components.WithdrawalSheet
import com.example.ui.theme.CosmicNavy
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.RubyError
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

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
    val showRedeemCoinsSheet by viewModel.showRedeemCoinsSheet.collectAsStateWithLifecycle()
    val isSpinning by viewModel.isSpinning.collectAsStateWithLifecycle()
    val showAutoAdPrompt by viewModel.showAutoAdPrompt.collectAsStateWithLifecycle()
    val autoAdCountdown by viewModel.autoAdCountdown.collectAsStateWithLifecycle()

    val showAdminLoginDialog by viewModel.showAdminLoginDialog.collectAsStateWithLifecycle()
    val showAdminDashboard by viewModel.showAdminDashboard.collectAsStateWithLifecycle()
    val dismissedUpdateCode by viewModel.dismissedUpdateVersionCode.collectAsStateWithLifecycle()

    // Navigation Tab state: 0 = Home, 1 = Games, 2 = Referral
    var currentNavTab by remember { mutableIntStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Handle Android system back button when on sub-tabs
    BackHandler(enabled = currentNavTab != 0) {
        currentNavTab = 0
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.checkDailyReminderNotification(context)
            com.example.notification.DailyNotificationScheduler.rescheduleAll(context)
        }
    }

    LaunchedEffect(Unit) {
        com.example.notification.DailyNotificationScheduler.rescheduleAll(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.checkDailyReminderNotification(context)
        }
    }

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
            },
            bottomBar = {
                // Sleek, modern Bottom Navigation Bar
                NavigationBar(
                    containerColor = Color(0xEB130B2A),
                    contentColor = TextPrimary,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                ) {
                    NavigationBarItem(
                        selected = currentNavTab == 0,
                        onClick = { currentNavTab = 0 },
                        icon = {
                            Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(22.dp))
                        },
                        label = {
                            Text("Home", fontWeight = if (currentNavTab == 0) FontWeight.Bold else FontWeight.Medium)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GoldCoin,
                            selectedTextColor = GoldCoin,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = PurplePrimary.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.testTag("nav_home")
                    )

                    NavigationBarItem(
                        selected = currentNavTab == 1,
                        onClick = { currentNavTab = 1 },
                        icon = {
                            Icon(Icons.Default.Casino, contentDescription = "Games", modifier = Modifier.size(22.dp))
                        },
                        label = {
                            Text("Games", fontWeight = if (currentNavTab == 1) FontWeight.Bold else FontWeight.Medium)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GoldCoin,
                            selectedTextColor = GoldCoin,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = PurplePrimary.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.testTag("nav_games")
                    )

                    NavigationBarItem(
                        selected = currentNavTab == 2,
                        onClick = { currentNavTab = 2 },
                        icon = {
                            Icon(Icons.Default.People, contentDescription = "Refer & Earn", modifier = Modifier.size(22.dp))
                        },
                        label = {
                            Text("Referral", fontWeight = if (currentNavTab == 2) FontWeight.Bold else FontWeight.Medium)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GoldCoin,
                            selectedTextColor = GoldCoin,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = PurplePrimary.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.testTag("nav_referral")
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
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header with Logo, Title, and Admin Shortcut
                    HeaderCard(
                        onAdminClick = { viewModel.openAdminLogin() }
                    )

                    // Switch between Home, Games, and Referral screens
                    when (currentNavTab) {
                        0 -> {
                            // --- HOME SCREEN (100% PRESERVED EXISTING CONTENT) ---

                            // Coin & Rupee Balance Card with Clickable Coins to Redeem
                            CoinBalanceCard(
                                coins = adStats.totalCoinsEarned,
                                availableRupees = adStats.availableRupeesBalance,
                                onWithdrawClick = { viewModel.openWithdrawalSheet() },
                                onRedeemCoinsClick = { viewModel.openRedeemCoinsSheet() }
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

                            // Games Section with Daily Spin Wheel & Auto Rewarded Ad
                            DailySpinCard(
                                adStats = adStats,
                                rewardedState = rewardedState,
                                isSpinning = isSpinning,
                                showAutoAdPrompt = showAutoAdPrompt,
                                autoAdCountdown = autoAdCountdown,
                                onSpinTriggered = { act, coins, onStartAnim ->
                                    viewModel.triggerSpin(act, coins, onStartAnim)
                                },
                                onConfirmWatchAutoAd = { act ->
                                    viewModel.confirmWatchAutoAd(act)
                                },
                                onDismissAutoAdPrompt = {
                                    viewModel.dismissAutoAdPrompt()
                                }
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

                            // Footer with Package Name, Mode, and "Adsearn Admin Login" link
                            AppFooter(
                                onAdminClick = { viewModel.openAdminLogin() }
                            )
                        }

                        1 -> {
                            // --- DEDICATED GAMES SECTION ---
                            GamesSection(
                                adStats = adStats,
                                rewardedState = rewardedState,
                                isSpinning = isSpinning,
                                showAutoAdPrompt = showAutoAdPrompt,
                                autoAdCountdown = autoAdCountdown,
                                onSpinTriggered = { act, coins, onStartAnim ->
                                    viewModel.triggerSpin(act, coins, onStartAnim)
                                },
                                onConfirmWatchAutoAd = { act ->
                                    viewModel.confirmWatchAutoAd(act)
                                },
                                onDismissAutoAdPrompt = {
                                    viewModel.dismissAutoAdPrompt()
                                },
                                onAwardCoins = { coins, gameTitle ->
                                    viewModel.awardGameCoins(coins, gameTitle)
                                }
                            )

                            AppFooter(
                                onAdminClick = { viewModel.openAdminLogin() }
                            )
                        }

                        2 -> {
                            // --- DEDICATED REFERRAL & EARN SECTION ---
                            ReferralSection(
                                referralState = adStats.referralState,
                                campaignSettings = adStats.referralSettings,
                                onApplyReferralCode = { code, onComplete ->
                                    viewModel.applyReferralCode(code, onComplete)
                                }
                            )

                            AppFooter(
                                onAdminClick = { viewModel.openAdminLogin() }
                            )
                        }
                    }
                }

                // Coin Burst Particle Overlay on Reward
                CoinConfettiOverlay(
                    show = showCoinBurst,
                    onFinished = { viewModel.onCoinBurstFinished() }
                )

                // Coin Redemption BottomSheet
                if (showRedeemCoinsSheet) {
                    val redeemSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    RedeemCoinsSheet(
                        availableCoins = adStats.totalCoinsEarned,
                        redemptionHistory = adStats.coinRedemptions,
                        sheetState = redeemSheetState,
                        onDismiss = { viewModel.closeRedeemCoinsSheet() },
                        onConfirmRedeem = { coins, onComplete ->
                            viewModel.redeemCoins(coins, onComplete)
                        }
                    )
                }

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

                // Admin Password Login Dialog (Password field and Login button only)
                if (showAdminLoginDialog) {
                    AdminLoginDialog(
                        onDismiss = { viewModel.dismissAdminLogin() },
                        onLoginSuccess = { viewModel.onAdminLoginSuccess() }
                    )
                }

                // Comprehensive Admin Dashboard BottomSheet
                if (showAdminDashboard) {
                    val adminDashboardSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    AdminDashboardSheet(
                        adStats = adStats,
                        sheetState = adminDashboardSheetState,
                        onDismiss = { viewModel.dismissAdminDashboard() },
                        onLogout = { viewModel.logoutAdmin() },
                        onUpdateWithdrawalStatus = { id, status ->
                            viewModel.updateWithdrawalStatus(id, status)
                        },
                        onUpdateReferralSettings = { settings ->
                            viewModel.updateReferralSettings(settings)
                        },
                        onAddOrUpdateGame = { game ->
                            viewModel.addOrUpdateGame(game)
                        },
                        onToggleGamePublished = { id, isPublished ->
                            viewModel.toggleGamePublished(id, isPublished)
                        },
                        onDeleteGame = { id ->
                            viewModel.deleteGame(id)
                        },
                        onUpdateAppVersionInfo = { info ->
                            viewModel.updateAppVersionInfo(info)
                        }
                    )
                }

                // App Update Available Dialog
                val versionInfo = adStats.appVersionInfo
                val currentVersionCode = 1
                if (versionInfo.isUpdateAvailable(currentVersionCode) && dismissedUpdateCode != versionInfo.latestVersionCode) {
                    AppUpdateDialog(
                        versionInfo = versionInfo,
                        onDismiss = { viewModel.dismissUpdateDialog(versionInfo.latestVersionCode) }
                    )
                }
            }
        }
    }
}

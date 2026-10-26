package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdStats
import com.example.data.AdminGameItem
import com.example.data.AppVersionInfo
import com.example.data.ReferralCampaignSettings
import com.example.data.WithdrawalRequest
import com.example.data.WithdrawalStatus
import com.example.notification.DailyNotificationScheduler
import com.example.notification.FcmPushManager
import com.example.notification.GLOBAL_ANNOUNCEMENTS_TOPIC
import com.example.notification.NotificationSlotConfig
import com.example.notification.ScheduledNotificationPrefs
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CosmicNavy
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.GlassCardFill
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.GoldCoinDark
import com.example.ui.theme.GoldCoinLight
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleSecondary
import com.example.ui.theme.RubyError
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardSheet(
    adStats: AdStats,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onLogout: () -> Unit,
    onUpdateWithdrawalStatus: (String, WithdrawalStatus) -> Unit,
    onUpdateReferralSettings: (ReferralCampaignSettings) -> Unit,
    onAddOrUpdateGame: (AdminGameItem) -> Unit,
    onToggleGamePublished: (String, Boolean) -> Unit,
    onDeleteGame: (String) -> Unit,
    onUpdateAppVersionInfo: (AppVersionInfo) -> Unit
) {
    val context = LocalContext.current
    val notifPrefs = remember { ScheduledNotificationPrefs(context) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Withdrawals", "Referrals", "Games", "Notifications", "Version", "Audit Logs")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DeepPurpleDark,
        dragHandle = null,
        modifier = Modifier.fillMaxHeight(0.95f).testTag("admin_dashboard_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Dashboard Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(listOf(PurplePrimary, Color(0xFF1B0E3E)))
                            )
                            .border(1.dp, GoldCoin.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Shield",
                            tint = GoldCoin,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Adsearn Admin Console",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Authenticated • Role: Super Administrator",
                            color = EmeraldSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Logout button
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("admin_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = RubyError
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("admin_dashboard_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Tabs Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0x22FFFFFF),
                contentColor = TextPrimary,
                edgePadding = 8.dp,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                color = if (selectedTab == index) GoldCoinLight else TextSecondary,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedTab) {
                    0 -> WithdrawalsManagementTab(
                        requests = adStats.withdrawalRequests,
                        onUpdateStatus = onUpdateWithdrawalStatus
                    )
                    1 -> ReferralSettingsTab(
                        currentSettings = adStats.referralSettings,
                        history = adStats.referralState.referralHistory,
                        onSave = onUpdateReferralSettings
                    )
                    2 -> GamesManagementTab(
                        games = adStats.gamesCatalog,
                        onAddOrUpdate = onAddOrUpdateGame,
                        onTogglePublished = onToggleGamePublished,
                        onDelete = onDeleteGame
                    )
                    3 -> AdminNotificationsTab(
                        prefs = notifPrefs
                    )
                    4 -> AppVersionManagementTab(
                        currentInfo = adStats.appVersionInfo,
                        onSave = onUpdateAppVersionInfo
                    )
                    5 -> AuditLogsTab(
                        logs = adStats.auditLogs
                    )
                }
            }
        }
    }
}

// ---------------- 1. WITHDRAWALS TAB ----------------
@Composable
private fun WithdrawalsManagementTab(
    requests: List<WithdrawalRequest>,
    onUpdateStatus: (String, WithdrawalStatus) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Withdrawal Requests (${requests.size})",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Min ₹500 • Instant Refund on Reject",
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        if (requests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No withdrawal requests submitted yet.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            requests.forEach { req ->
                val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(req.timestamp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GlassCardFill),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(GlassCardBorder, Color.Transparent)))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "₹${req.amountInr.toInt()}",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            val badgeColor = when (req.status) {
                                WithdrawalStatus.PENDING -> AmberWarning
                                WithdrawalStatus.APPROVED -> CyanAccent
                                WithdrawalStatus.PROCESSING -> PurpleSecondary
                                WithdrawalStatus.PAID -> EmeraldSuccess
                                WithdrawalStatus.REJECTED -> RubyError
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(badgeColor.copy(alpha = 0.2f))
                                    .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(req.status.displayName, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = "UPI: ${req.upiId}" + if (!req.email.isNullOrBlank()) " • Email: ${req.email}" else "",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = "Submitted: $dateStr",
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        if (!req.note.isNullOrBlank()) {
                            Text(
                                text = "Note: ${req.note}",
                                color = if (req.status == WithdrawalStatus.REJECTED) RubyError else TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Action Buttons for Admin to change status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (req.status != WithdrawalStatus.APPROVED && req.status != WithdrawalStatus.PAID) {
                                Button(
                                    onClick = { onUpdateStatus(req.id, WithdrawalStatus.APPROVED) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent.copy(alpha = 0.8f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(34.dp)
                                ) {
                                    Text("Approve", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (req.status != WithdrawalStatus.PAID && req.status != WithdrawalStatus.REJECTED) {
                                Button(
                                    onClick = { onUpdateStatus(req.id, WithdrawalStatus.PAID) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(34.dp)
                                ) {
                                    Text("Mark Paid", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (req.status != WithdrawalStatus.REJECTED && req.status != WithdrawalStatus.PAID) {
                                Button(
                                    onClick = { onUpdateStatus(req.id, WithdrawalStatus.REJECTED) },
                                    colors = ButtonDefaults.buttonColors(containerColor = RubyError.copy(alpha = 0.85f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(34.dp)
                                ) {
                                    Text("Reject & Refund", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- 2. REFERRALS TAB ----------------
@Composable
private fun ReferralSettingsTab(
    currentSettings: ReferralCampaignSettings,
    history: List<com.example.data.ReferralRecord>,
    onSave: (ReferralCampaignSettings) -> Unit
) {
    val context = LocalContext.current
    var firstRewardInput by remember(currentSettings) { mutableStateOf(currentSettings.firstReferralRewardInr.toInt().toString()) }
    var subsequentRewardInput by remember(currentSettings) { mutableStateOf(currentSettings.subsequentReferralRewardInr.toInt().toString()) }
    var minEligibilityInput by remember(currentSettings) { mutableStateOf(currentSettings.minEligibilityRequirement) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GlassCardFill),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(GlassCardBorder, Color.Transparent)))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Referral Reward Campaigns",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Configure reward amounts for first-time referrals vs. subsequent referrals. Default campaign is ₹50 first, ₹5 subsequent (can be set to ₹10).",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                OutlinedTextField(
                    value = firstRewardInput,
                    onValueChange = { firstRewardInput = it.filter { c -> c.isDigit() } },
                    label = { Text("1st Referral Reward (₹)") },
                    placeholder = { Text("Default: 50") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = subsequentRewardInput,
                    onValueChange = { subsequentRewardInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Subsequent Referrals Reward (₹)") },
                    placeholder = { Text("Default: 5 (or 10)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = minEligibilityInput,
                    onValueChange = { minEligibilityInput = it },
                    label = { Text("Eligibility Terms Display") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Button(
                    onClick = {
                        val f = firstRewardInput.toDoubleOrNull() ?: 50.0
                        val s = subsequentRewardInput.toDoubleOrNull() ?: 5.0
                        onSave(
                            ReferralCampaignSettings(
                                firstReferralRewardInr = f,
                                subsequentReferralRewardInr = s,
                                minEligibilityRequirement = minEligibilityInput.trim()
                            )
                        )
                        Toast.makeText(context, "Referral settings updated!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Campaign Settings", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Referral History
        Text(
            text = "Referral Attributions Log (${history.size})",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        if (history.isEmpty()) {
            Text(text = "No referral claims recorded yet.", color = TextMuted, fontSize = 12.sp)
        } else {
            history.forEach { record ->
                val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(record.timestamp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x18FFFFFF))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Code: ${record.friendCode} • ${if (record.isFirstReferral) "1st Referral" else "Subsequent"}",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(text = dateStr, color = TextMuted, fontSize = 10.sp)
                        }
                        Text(
                            text = "+₹${record.rewardInr.toInt()}",
                            color = EmeraldSuccess,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ---------------- 3. GAMES TAB ----------------
@Composable
private fun GamesManagementTab(
    games: List<AdminGameItem>,
    onAddOrUpdate: (AdminGameItem) -> Unit,
    onTogglePublished: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    val context = LocalContext.current
    var editingGame by remember { mutableStateOf<AdminGameItem?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Managed Games (${games.size})",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = { isCreatingNew = true },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Game", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        games.forEach { game ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GlassCardFill),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(GlassCardBorder, Color.Transparent)))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = game.iconEmoji, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = game.title,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Reward: +${game.rewardCoins} Coins • Type: ${game.gameType}",
                                    color = GoldCoinLight,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = game.isPublished,
                            onCheckedChange = { onTogglePublished(game.id, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldSuccess,
                                checkedTrackColor = EmeraldSuccess.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceDark
                            )
                        )
                    }

                    Text(
                        text = game.description,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { editingGame = game },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", fontSize = 11.sp)
                        }

                        if (game.id != "spin_wheel") {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { onDelete(game.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RubyError, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit/Create Dialog
    if (editingGame != null || isCreatingNew) {
        val target = editingGame ?: AdminGameItem(
            title = "",
            description = "",
            iconEmoji = "🎯",
            gameType = "CUSTOM",
            rewardCoins = 10
        )
        var title by remember(target) { mutableStateOf(target.title) }
        var desc by remember(target) { mutableStateOf(target.description) }
        var emoji by remember(target) { mutableStateOf(target.iconEmoji) }
        var reward by remember(target) { mutableStateOf(target.rewardCoins.toString()) }

        AlertDialog(
            onDismissRequest = {
                editingGame = null
                isCreatingNew = false
            },
            title = {
                Text(
                    text = if (isCreatingNew) "Add New Game" else "Edit Game",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Game Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { emoji = it },
                        label = { Text("Icon / Emoji") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reward,
                        onValueChange = { reward = it.filter { c -> c.isDigit() } },
                        label = { Text("Reward Coins") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val coins = reward.toIntOrNull() ?: 10
                        val updated = target.copy(
                            title = title.trim().ifEmpty { "Game" },
                            description = desc.trim(),
                            iconEmoji = emoji.trim().ifEmpty { "🎮" },
                            rewardCoins = coins
                        )
                        onAddOrUpdate(updated)
                        editingGame = null
                        isCreatingNew = false
                        Toast.makeText(context, "Game saved!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) {
                    Text("Save Game")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    editingGame = null
                    isCreatingNew = false
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF1E153D),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ---------------- 4. NOTIFICATIONS TAB ----------------
@Composable
private fun AdminNotificationsTab(
    prefs: ScheduledNotificationPrefs
) {
    val context = LocalContext.current
    var isMasterEnabled by remember { mutableStateOf(prefs.isMasterEnabled) }
    var slots by remember { mutableStateOf(prefs.getAllSlotConfigs()) }
    var logs by remember { mutableStateOf(prefs.getDeliveryLogs()) }
    var editingSlot by remember { mutableStateOf<NotificationSlotConfig?>(null) }

    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMessage by remember { mutableStateOf("") }
    var isSendingBroadcast by remember { mutableStateOf(false) }
    var broadcastFeedback by remember { mutableStateOf<String?>(null) }
    var broadcastSuccess by remember { mutableStateOf(false) }
    var broadcastHistory by remember { mutableStateOf(FcmPushManager.getBroadcastHistory(context)) }
    val fcmToken = remember { FcmPushManager.getFcmToken(context) }
    val registeredDevices = remember { FcmPushManager.getRegisteredDevices(context) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. GLOBAL PUSH NOTIFICATION COMPOSER (FCM) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GlassCardFill),
            shape = RoundedCornerShape(18.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.verticalGradient(listOf(GoldCoin.copy(alpha = 0.5f), GlassCardBorder))
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GoldCoin.copy(alpha = 0.2f))
                                .border(1.dp, GoldCoin.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = GoldCoin,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Global Push Notification Broadcast",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Firebase Cloud Messaging (FCM Topic: $GLOBAL_ANNOUNCEMENTS_TOPIC)",
                                color = GoldCoinLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmeraldSuccess.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${maxOf(1, registeredDevices.size)} Active Devices",
                            color = EmeraldSuccess,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Broadcast an announcement to all eligible users worldwide who have installed this app. Sent via FCM to open, backgrounded, or closed devices.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                // Title Input
                OutlinedTextField(
                    value = broadcastTitle,
                    onValueChange = {
                        broadcastTitle = it
                        broadcastFeedback = null
                    },
                    label = { Text("Notification Title") },
                    placeholder = { Text("e.g. 🎁 Special Weekend Bonus Unlocked!") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("fcm_broadcast_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = GlassCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Message Body Input
                OutlinedTextField(
                    value = broadcastMessage,
                    onValueChange = {
                        broadcastMessage = it
                        broadcastFeedback = null
                    },
                    label = { Text("Notification Message") },
                    placeholder = { Text("e.g. Open Adsearn now to claim extra coins and spin the wheel!") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("fcm_broadcast_message_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = GlassCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Live Android Lock Screen / Status Bar Notification Preview
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "LIVE DEVICE NOTIFICATION PREVIEW",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF261D47))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PurplePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Adsearn • now",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "FCM Push",
                                        color = GoldCoin,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = broadcastTitle.ifBlank { "Notification Title Preview" },
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = broadcastMessage.ifBlank { "Notification message body will appear here on user phones..." },
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Feedback Banner
                if (broadcastFeedback != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (broadcastSuccess) EmeraldSuccess.copy(alpha = 0.15f) else RubyError.copy(alpha = 0.15f))
                            .border(1.dp, if (broadcastSuccess) EmeraldSuccess.copy(alpha = 0.4f) else RubyError.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = if (broadcastSuccess) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (broadcastSuccess) EmeraldSuccess else RubyError,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = broadcastFeedback ?: "",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Send to All Users Button
                Button(
                    onClick = {
                        if (broadcastTitle.isBlank() || broadcastMessage.isBlank()) {
                            broadcastFeedback = "Please fill in both Title and Message before sending."
                            broadcastSuccess = false
                            return@Button
                        }

                        isSendingBroadcast = true
                        broadcastFeedback = null
                        FcmPushManager.sendGlobalPushAnnouncement(
                            context = context,
                            title = broadcastTitle.trim(),
                            message = broadcastMessage.trim()
                        ) { success, msg ->
                            isSendingBroadcast = false
                            broadcastSuccess = success
                            broadcastFeedback = msg
                            if (success) {
                                broadcastHistory = FcmPushManager.getBroadcastHistory(context)
                                broadcastTitle = ""
                                broadcastMessage = ""
                            }
                        }
                    },
                    enabled = !isSendingBroadcast && broadcastTitle.isNotBlank() && broadcastMessage.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("send_global_push_button")
                ) {
                    if (isSendingBroadcast) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Broadcasting to FCM...", color = Color.Black, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send to All Users (Global Broadcast)", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- BROADCAST HISTORY & DELIVERY REPORTING ---
        if (broadcastHistory.isNotEmpty()) {
            Text(
                text = "Recent Global Broadcasts (${broadcastHistory.size})",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                broadcastHistory.take(5).forEach { item ->
                    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x18FFFFFF))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.title,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(EmeraldSuccess.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(item.status, color = EmeraldSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = item.message,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 2
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Target: ${item.targetTopic} • By ${item.sentBy}",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = dateStr,
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. 3X DAILY SCHEDULED NOTIFICATIONS (PRESERVED) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GlassCardFill),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(GlassCardBorder, Color.Transparent)))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "3x Daily Scheduled Notifications",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "9:00 AM, 12:00 PM, 5:00 PM IST (Asia/Kolkata)",
                            color = GoldCoin,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = isMasterEnabled,
                        onCheckedChange = { checked ->
                            isMasterEnabled = checked
                            prefs.isMasterEnabled = checked
                            DailyNotificationScheduler.rescheduleAll(context)
                            slots = prefs.getAllSlotConfigs()
                            Toast.makeText(context, if (checked) "Schedule enabled" else "Schedule disabled", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldSuccess)
                    )
                }
            }
        }

        slots.forEach { slot ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GlassCardFill),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(GlassCardBorder, Color.Transparent)))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Slot #${slot.id}: ${slot.formattedTime} IST",
                            color = GoldCoin,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = slot.isEnabled,
                            onCheckedChange = { enabled ->
                                prefs.setSlotEnabled(slot.id, enabled)
                                DailyNotificationScheduler.rescheduleAll(context)
                                slots = prefs.getAllSlotConfigs()
                            }
                        )
                    }

                    Text(text = slot.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(text = slot.message, color = TextSecondary, fontSize = 12.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                DailyNotificationScheduler.sendTestNotification(context, slot.id)
                                logs = prefs.getDeliveryLogs()
                                Toast.makeText(context, "Test notification triggered!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Preview / Test", fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { editingSlot = slot },
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // Edit Slot Dialog
    if (editingSlot != null) {
        val current = editingSlot!!
        var h by remember(current) { mutableStateOf(current.hour.toString()) }
        var m by remember(current) { mutableStateOf(current.minute.toString()) }
        var t by remember(current) { mutableStateOf(current.title) }
        var msg by remember(current) { mutableStateOf(current.message) }

        AlertDialog(
            onDismissRequest = { editingSlot = null },
            title = { Text("Edit Notification Slot #${current.id}", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = h,
                            onValueChange = { h = it.filter { c -> c.isDigit() } },
                            label = { Text("Hour (0-23)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = m,
                            onValueChange = { m = it.filter { c -> c.isDigit() } },
                            label = { Text("Minute (0-59)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(value = t, onValueChange = { t = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = msg, onValueChange = { msg = it }, label = { Text("Message") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hourInt = h.toIntOrNull() ?: current.hour
                        val minInt = m.toIntOrNull() ?: current.minute
                        val updated = current.copy(hour = hourInt, minute = minInt, title = t.trim(), message = msg.trim())
                        prefs.saveSlotConfig(updated)
                        DailyNotificationScheduler.scheduleSlot(context, updated)
                        slots = prefs.getAllSlotConfigs()
                        editingSlot = null
                        Toast.makeText(context, "Slot #${updated.id} saved & scheduled!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) {
                    Text("Save Slot")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSlot = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF1E153D),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ---------------- 5. VERSION TAB ----------------
@Composable
private fun AppVersionManagementTab(
    currentInfo: AppVersionInfo,
    onSave: (AppVersionInfo) -> Unit
) {
    val context = LocalContext.current
    var versionCode by remember(currentInfo) { mutableStateOf(currentInfo.latestVersionCode.toString()) }
    var versionName by remember(currentInfo) { mutableStateOf(currentInfo.latestVersionName) }
    var releaseNotes by remember(currentInfo) { mutableStateOf(currentInfo.releaseNotes) }
    var downloadUrl by remember(currentInfo) { mutableStateOf(currentInfo.downloadUrl) }
    var isMandatory by remember(currentInfo) { mutableStateOf(currentInfo.isMandatory) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassCardFill),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(GlassCardBorder, Color.Transparent)))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "App Version & In-App Update System",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "When the latest version code set here exceeds the installed app's version code (1), the app displays an update notification to users.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            OutlinedTextField(
                value = versionCode,
                onValueChange = { versionCode = it.filter { c -> c.isDigit() } },
                label = { Text("Latest Version Code (e.g. 2)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = versionName,
                onValueChange = { versionName = it },
                label = { Text("Latest Version Name (e.g. 1.1)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = downloadUrl,
                onValueChange = { downloadUrl = it },
                label = { Text("Verified Download / APK URL") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = releaseNotes,
                onValueChange = { releaseNotes = it },
                label = { Text("Release Notes / Changelog") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Mandatory Update (Enforce before using)", color = TextPrimary, fontSize = 13.sp)
                Switch(checked = isMandatory, onCheckedChange = { isMandatory = it })
            }

            Button(
                onClick = {
                    val code = versionCode.toIntOrNull() ?: 1
                    onSave(
                        AppVersionInfo(
                            latestVersionCode = code,
                            latestVersionName = versionName.trim().ifEmpty { "1.0" },
                            releaseNotes = releaseNotes.trim(),
                            downloadUrl = downloadUrl.trim(),
                            isMandatory = isMandatory
                        )
                    )
                    Toast.makeText(context, "Version info updated!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Publish Version Info", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---------------- 6. AUDIT LOGS TAB ----------------
@Composable
private fun AuditLogsTab(
    logs: List<com.example.data.AdminAuditLog>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "System Security & Action Logs (${logs.size})",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        if (logs.isEmpty()) {
            Text(text = "No audit log records found.", color = TextMuted, fontSize = 12.sp)
        } else {
            logs.forEach { log ->
                val dateStr = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault()).format(Date(log.timestamp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x18FFFFFF))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = log.action, color = GoldCoinLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = dateStr, color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Text(text = log.details, color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

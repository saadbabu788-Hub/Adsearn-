package com.example.ui.components

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CoinRedemptionTransaction
import com.example.ui.theme.CosmicNavy
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepPurpleDark
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.GlassCardFill
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.GoldCoinLight
import com.example.ui.theme.RubyError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedeemCoinsSheet(
    availableCoins: Int,
    redemptionHistory: List<CoinRedemptionTransaction>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onConfirmRedeem: (Int, (Boolean, String) -> Unit) -> Unit
) {
    // Initial selectable amount: minimum 20 or max available multiple of 20
    val maxMultiplesOf20 = (availableCoins / 20) * 20
    var selectedCoinsToRedeem by remember(availableCoins) {
        mutableIntStateOf(if (maxMultiplesOf20 >= 20) 20 else 0)
    }

    var isProcessing by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val calculatedRupees = CoinRedemptionTransaction.calculateRupees(selectedCoinsToRedeem)
    val isValidSelection = selectedCoinsToRedeem >= 20 &&
            selectedCoinsToRedeem <= availableCoins &&
            (selectedCoinsToRedeem % 20 == 0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DeepPurpleDark,
        dragHandle = null,
        modifier = Modifier.fillMaxHeight(0.92f).testTag("redeem_coins_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
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
                            .background(GoldCoin.copy(alpha = 0.2f))
                            .border(1.dp, GoldCoin.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Redeem Coins",
                            tint = GoldCoin,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Redeem Coins to Cash",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Conversion Rate: 20 Coins = ₹3.00 Cash",
                            color = EmeraldSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_redeem_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            // Available Coins Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0x356366F1), Color(0x258B5CF6), CosmicNavy)
                        )
                    )
                    .border(1.5.dp, GoldCoin.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AVAILABLE COIN BALANCE",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(GoldCoin.copy(alpha = 0.15f))
                                .border(1.dp, GoldCoin.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Max Redeemable: $maxMultiplesOf20 Coins",
                                color = GoldCoinLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$availableCoins",
                            color = TextPrimary,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Coins",
                            color = GoldCoin,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }

                    Text(
                        text = if (availableCoins >= 20) {
                            "You can convert your coins directly into eligible wallet cash in multiples of 20."
                        } else {
                            "You need at least 20 coins to redeem. Watch ads or spin the wheel to earn more!"
                        },
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Status Banners
            AnimatedVisibility(visible = successMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(EmeraldSuccess.copy(alpha = 0.15f))
                        .border(1.dp, EmeraldSuccess.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = successMessage ?: "", color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }

            AnimatedVisibility(visible = errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(RubyError.copy(alpha = 0.15f))
                        .border(1.dp, RubyError.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = RubyError, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = errorMessage ?: "", color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }

            // Coin Selection & Conversion Calculator Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassCardFill)
                    .border(1.dp, GlassCardBorder, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "SELECT AMOUNT TO REDEEM (MULTIPLES OF 20)",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Stepper / Counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                if (selectedCoinsToRedeem >= 40) {
                                    selectedCoinsToRedeem -= 20
                                }
                            },
                            enabled = selectedCoinsToRedeem > 20 && !isProcessing,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease 20 coins", tint = TextPrimary)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$selectedCoinsToRedeem",
                                color = TextPrimary,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Coins Selected",
                                color = GoldCoinLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(
                            onClick = {
                                if (selectedCoinsToRedeem + 20 <= maxMultiplesOf20) {
                                    selectedCoinsToRedeem += 20
                                }
                            },
                            enabled = selectedCoinsToRedeem + 20 <= maxMultiplesOf20 && !isProcessing,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase 20 coins", tint = TextPrimary)
                        }
                    }

                    // Quick Multiples Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(20, 40, 100).forEach { amount ->
                            val isAllowed = amount <= maxMultiplesOf20
                            val isSelected = selectedCoinsToRedeem == amount
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isSelected -> GoldCoin.copy(alpha = 0.25f)
                                            isAllowed -> Color(0x1AFFFFFF)
                                            else -> Color(0x0AFFFFFF)
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) GoldCoin else Color(0x22FFFFFF),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable(enabled = isAllowed && !isProcessing) {
                                        selectedCoinsToRedeem = amount
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$amount Coins",
                                        color = if (isSelected) GoldCoin else if (isAllowed) TextPrimary else TextMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "= ₹${(amount / 20) * 3}",
                                        color = if (isSelected) EmeraldSuccess else TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Max Button
                        if (maxMultiplesOf20 > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selectedCoinsToRedeem == maxMultiplesOf20) EmeraldSuccess.copy(alpha = 0.25f) else Color(0x1AFFFFFF)
                                    )
                                    .border(
                                        1.dp,
                                        if (selectedCoinsToRedeem == maxMultiplesOf20) EmeraldSuccess else Color(0x22FFFFFF),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable(enabled = !isProcessing) {
                                        selectedCoinsToRedeem = maxMultiplesOf20
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "MAX",
                                        color = EmeraldSuccess,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "$maxMultiplesOf20",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Calculation Payout Preview Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(EmeraldSuccess.copy(alpha = 0.12f))
                            .border(1.dp, EmeraldSuccess.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("You will receive in wallet:", color = TextSecondary, fontSize = 12.sp)
                                    Text("Instant cash credit", color = TextMuted, fontSize = 10.sp)
                                }
                            }

                            Text(
                                text = "₹${calculatedRupees.toInt()}.00",
                                color = EmeraldSuccess,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Redeem Action Button
                    Button(
                        onClick = {
                            if (!isValidSelection) return@Button
                            isProcessing = true
                            errorMessage = null
                            successMessage = null
                            onConfirmRedeem(selectedCoinsToRedeem) { success, msg ->
                                isProcessing = false
                                if (success) {
                                    successMessage = "🎉 Successfully redeemed $selectedCoinsToRedeem coins for ₹${calculatedRupees.toInt()} Cash credited to your wallet!"
                                    selectedCoinsToRedeem = 0
                                } else {
                                    errorMessage = msg
                                }
                            }
                        },
                        enabled = isValidSelection && !isProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("confirm_redeem_coins_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSuccess,
                            disabledContainerColor = Color(0x3310B981)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing Redemption...", color = Color.White)
                        } else {
                            Text(
                                text = if (isValidSelection) "Redeem $selectedCoinsToRedeem Coins for ₹${calculatedRupees.toInt()}" else "Select at least 20 Coins",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isValidSelection) Color.Black else TextMuted
                            )
                        }
                    }
                }
            }

            // Redemption History Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassCardFill)
                    .border(1.dp, GlassCardBorder, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "COIN REDEMPTION HISTORY",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    if (redemptionHistory.isEmpty()) {
                        Text(
                            text = "No coin redemptions yet. Convert your coins above in multiples of 20.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            redemptionHistory.forEach { item ->
                                val dateStr = remember(item.timestamp) {
                                    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x18FFFFFF))
                                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "-${item.coinsRedeemed} Coins ➔ +₹${item.rupeesCredited.toInt()} Cash",
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = dateStr,
                                                color = TextMuted,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(EmeraldSuccess.copy(alpha = 0.2f))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(item.status, color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

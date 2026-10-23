package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MIN_WITHDRAWAL_INR
import com.example.REWARD_COINS_PER_AD
import com.example.REWARD_INR_PER_AD
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassCardBorderStrong
import com.example.ui.theme.GlassCardFill
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.GoldCoinDark
import com.example.ui.theme.GoldCoinLight
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleSecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CoinBalanceCard(
    coins: Int,
    availableRupees: Double = 0.0,
    onWithdrawClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val animatedCoins by animateIntAsState(
        targetValue = coins,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "CoinCountAnimation"
    )

    val animatedRupees by animateIntAsState(
        targetValue = availableRupees.toInt(),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "RupeesCountAnimation"
    )

    val isEligible = availableRupees >= MIN_WITHDRAWAL_INR

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0x3D2C1C60),
                        GlassCardFill,
                        Color(0x28381D46)
                    )
                )
            )
            .border(1.5.dp, GlassCardBorderStrong, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header row with reward badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "REWARD BALANCE",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(GoldCoin.copy(alpha = 0.15f))
                            .border(1.dp, GoldCoin.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+$REWARD_COINS_PER_AD Coins",
                            color = GoldCoinLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmeraldSuccess.copy(alpha = 0.15f))
                            .border(1.dp, EmeraldSuccess.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+₹${REWARD_INR_PER_AD.toInt()} / Ad",
                            color = EmeraldSuccess,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Balances & Coin Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Cash Balance Primary Display
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "₹$animatedRupees",
                            color = TextPrimary,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cash",
                            color = EmeraldSuccess,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = GoldCoin,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$animatedCoins Coins earned",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Glowing Gold Coin Icon Badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    GoldCoinLight.copy(alpha = 0.35f),
                                    GoldCoinDark.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(
                            2.dp,
                            Brush.linearGradient(listOf(GoldCoinLight, GoldCoinDark)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Gold Coin",
                        tint = GoldCoin,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Accessible Withdraw Action Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isEligible) {
                            Brush.horizontalGradient(
                                listOf(EmeraldSuccess.copy(alpha = 0.25f), Color(0x33064E3B))
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(Color(0x22FFFFFF), Color(0x11FFFFFF))
                            )
                        }
                    )
                    .border(
                        1.dp,
                        if (isEligible) EmeraldSuccess.copy(alpha = 0.6f) else Color(0x25FFFFFF),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onWithdrawClick() }
                    .testTag("withdraw_button")
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = "Withdraw",
                            tint = if (isEligible) EmeraldSuccess else GoldCoinLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Withdraw Funds",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isEligible) "Ready to payout via UPI" else "Min. ₹${MIN_WITHDRAWAL_INR.toInt()} (Need ₹${(MIN_WITHDRAWAL_INR - availableRupees).coerceAtLeast(0.0).toInt()} more)",
                                color = if (isEligible) EmeraldSuccess else TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isEligible) EmeraldSuccess else PurplePrimary.copy(alpha = 0.3f)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (isEligible) "Withdraw Now" else "View Status",
                            color = if (isEligible) Color.Black else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

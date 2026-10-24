package com.example.ui.components

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MAX_DAILY_SPINS
import com.example.REWARD_COINS_PER_AD
import com.example.REWARD_INR_PER_AD
import com.example.ads.AdPlacementState
import com.example.data.AdStats
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.GlassCardFill
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.RubyError
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun DailySpinCard(
    adStats: AdStats,
    rewardedState: AdPlacementState,
    isSpinning: Boolean,
    showAutoAdPrompt: Boolean,
    autoAdCountdown: Int,
    onSpinTriggered: (Activity, Int, (Int) -> Unit) -> Unit,
    onConfirmWatchAutoAd: (Activity) -> Unit,
    onDismissAutoAdPrompt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val currentRotation = remember { Animatable(0f) }
    var baseRotation by remember { mutableFloatStateOf(0f) }

    val spinsRemaining = adStats.spinsRemainingToday
    val spinsUsed = adStats.spinsUsedToday
    val isSecondSpin = (spinsUsed == 1)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GlassCardFill)
            .border(1.dp, GlassCardBorder, RoundedCornerShape(20.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Games",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Games",
                            color = Color(0xFFA78BFA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x3310B981))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "DAILY",
                                color = EmeraldSuccess,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Daily Spin Wheel",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Spins Remaining Badge
            val badgeColor = when {
                spinsRemaining == 2 -> EmeraldSuccess
                spinsRemaining == 1 -> AmberWarning
                else -> TextMuted
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = badgeColor.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (spinsRemaining > 0) Icons.Default.Stars else Icons.Default.LockClock,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "$spinsRemaining / $MAX_DAILY_SPINS Left",
                        color = badgeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Informative guidance subtext
        val statusText = when {
            spinsRemaining == 2 -> "🎁 2 free spins available today! Spin once, then spin again to unlock your automatic bonus ad."
            spinsRemaining == 1 -> "⚡ 1 spin remaining today! Completing this 2nd spin automatically attempts a bonus rewarded ad."
            else -> "🔒 All 2 daily spins used. Daily limits reset according to server UTC date at 00:00."
        }
        Text(
            text = statusText,
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Spin Wheel Visual Component
        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glowing outer rim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF1E1B4B), Color(0xFF0F0E2A))
                        )
                    )
                    .border(
                        width = 4.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                AmberWarning,
                                Color(0xFF8B5CF6),
                                EmeraldSuccess,
                                Color(0xFFEC4899),
                                AmberWarning
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // The rotating Wheel Canvas
            Canvas(
                modifier = Modifier
                    .size(216.dp)
                    .rotate(currentRotation.value)
            ) {
                val canvasSize = size.minDimension
                val radius = canvasSize / 2f
                val sweepAngle = 360f / SPIN_SECTORS.size

                for (sector in SPIN_SECTORS) {
                    val startAngle = sector.index * sweepAngle
                    // Draw colored sector slice
                    drawArc(
                        color = sector.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        size = Size(canvasSize, canvasSize),
                        topLeft = Offset(0f, 0f)
                    )

                    // Draw slice divider line
                    val dividerRad = Math.toRadians(startAngle.toDouble())
                    val endX = center.x + radius * Math.cos(dividerRad).toFloat()
                    val endY = center.y + radius * Math.sin(dividerRad).toFloat()
                    drawLine(
                        color = Color(0x66FFFFFF),
                        start = center,
                        end = Offset(endX, endY),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // Sector text labels
                drawIntoCanvas { canvas ->
                    val nativeCanvas = canvas.nativeCanvas
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = radius * 0.18f
                        isAntiAlias = true
                        isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        setShadowLayer(5f, 0f, 2f, android.graphics.Color.BLACK)
                    }

                    for (sector in SPIN_SECTORS) {
                        val sectorCenterAngle = sector.index * sweepAngle + (sweepAngle / 2f)
                        nativeCanvas.save()
                        nativeCanvas.rotate(sectorCenterAngle, center.x, center.y)
                        nativeCanvas.drawText(
                            sector.label,
                            center.x,
                            center.y - (radius * 0.58f),
                            paint
                        )
                        nativeCanvas.restore()
                    }
                }

                // Outer border inside canvas
                drawCircle(
                    color = Color(0xFFFBBF24),
                    radius = radius,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Top Pointer (Needle pointing down at 12 o'clock / 270°)
            Canvas(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(28.dp, 32.dp)
            ) {
                val trianglePath = Path().apply {
                    moveTo(size.width / 2f, size.height) // tip pointing down into wheel
                    lineTo(0f, 0f)
                    lineTo(size.width, 0f)
                    close()
                }
                drawPath(trianglePath, color = RubyError)
                drawPath(trianglePath, color = Color.White, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
            }

            // Center Golden Hub
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFFEF08A), Color(0xFFD97706), Color(0xFF78350F))
                        )
                    )
                    .border(2.dp, Color.White, CircleShape)
                    .shadow(6.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "WIN",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        // Last Spin Reward Banner if won
        if (adStats.lastSpinRewardCoins > 0) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0x2210B981),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4410B981)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏆 Last Win: +${adStats.lastSpinRewardCoins} Coins Credited!",
                        color = EmeraldSuccess,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Action Button: Spin Now
        val buttonEnabled = !isSpinning && spinsRemaining > 0 && activity != null

        Button(
            onClick = {
                if (!buttonEnabled || activity == null) return@Button
                // Pick target sector
                val targetIndex = Random.nextInt(SPIN_SECTORS.size)
                val targetSector = SPIN_SECTORS[targetIndex]

                // Math to align targetSectorCenter with top needle (270 degrees)
                val sweepAngle = 360f / SPIN_SECTORS.size
                val targetCenter = targetIndex * sweepAngle + (sweepAngle / 2f)
                val targetModAngle = (270f - targetCenter + 360f) % 360f

                // Add full revolutions (5 full turns)
                val fullTurns = 360f * (5 + Random.nextInt(2))
                val targetTotalRotation = baseRotation + fullTurns + targetModAngle

                onSpinTriggered(activity, targetSector.coins) { wonCoins ->
                    scope.launch {
                        currentRotation.animateTo(
                            targetValue = targetTotalRotation,
                            animationSpec = androidx.compose.animation.core.tween(
                                durationMillis = 3800,
                                easing = CubicBezierEasing(0.12f, 0.8f, 0.15f, 1.0f)
                            )
                        )
                        baseRotation = targetTotalRotation % 360f
                    }
                }
            },
            enabled = buttonEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("daily_spin_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSecondSpin) Color(0xFFD97706) else Color(0xFF6366F1),
                disabledContainerColor = Color(0xFF262338)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isSpinning) Icons.Default.Casino else if (isSecondSpin) Icons.Default.Tv else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (buttonEnabled) Color.White else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = when {
                        isSpinning -> "Spinning Wheel..."
                        spinsRemaining == 2 -> "Spin Wheel (1st Free Spin)"
                        spinsRemaining == 1 -> "Spin Wheel (2nd Spin → Auto Ad)"
                        else -> "All Spins Used Today (2/2)"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (buttonEnabled) Color.White else TextMuted
                )
            }
        }
    }

    // Auto Rewarded Ad Dialog / Confirmation Prompt (Required disclosure & policy compliance)
    if (showAutoAdPrompt) {
        AlertDialog(
            onDismissRequest = { onDismissAutoAdPrompt() },
            containerColor = SurfaceDark,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x3310B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "2nd Spin Bonus Unlocked!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Congratulations on completing your 2nd daily spin! As promised, your high-value sponsor rewarded video is ready.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x22F59E0B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44F59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "🎁 Verified Sponsor Reward:",
                                color = AmberWarning,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• ₹${REWARD_INR_PER_AD.toInt()} Cash credited to eligible balance\n• +$REWARD_COINS_PER_AD Coins credited to coin wallet",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Countdown bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (autoAdCountdown > 0) "Showing automatically in ${autoAdCountdown}s..." else "Launching sponsor video...",
                            color = Color(0xFFA78BFA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { autoAdCountdown / 3f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = EmeraldSuccess,
                        trackColor = Color(0xFF2E2A4A)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        activity?.let { onConfirmWatchAutoAd(it) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("auto_ad_confirm_button")
                ) {
                    Text(
                        text = "Watch Ad Now",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onDismissAutoAdPrompt() },
                    modifier = Modifier.testTag("auto_ad_skip_button")
                ) {
                    Text(
                        text = "Skip",
                        color = TextMuted
                    )
                }
            }
        )
    }
}

package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.GoldCoinDark
import com.example.ui.theme.GoldCoinLight
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleSecondary

@Composable
fun AdActionButtons(
    isWaitingForRewarded: Boolean,
    isWaitingForInterstitial: Boolean,
    onWatchRewardedClick: () -> Unit,
    onShowInterstitialClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Rewarded Ad Button with Gift icon wiggle
        AnimatedActionButton(
            title = if (isWaitingForRewarded) "Loading ad..." else "Watch Rewarded Ad (+10 Coins)",
            icon = Icons.Default.CardGiftcard,
            isLoading = isWaitingForRewarded,
            iconAnimationType = IconAnimType.WIGGLE,
            primaryColor = GoldCoinDark,
            secondaryColor = PurplePrimary,
            accentColor = GoldCoin,
            testTag = "watch_rewarded_button",
            onClick = onWatchRewardedClick
        )

        // Interstitial Ad Button with TV icon bounce
        AnimatedActionButton(
            title = if (isWaitingForInterstitial) "Loading ad..." else "Show Interstitial Ad",
            icon = Icons.Default.Tv,
            isLoading = isWaitingForInterstitial,
            iconAnimationType = IconAnimType.BOUNCE,
            primaryColor = PurplePrimary,
            secondaryColor = Color(0xFF4C1D95),
            accentColor = PurpleSecondary,
            testTag = "show_interstitial_button",
            onClick = onShowInterstitialClick
        )
    }
}

private enum class IconAnimType {
    WIGGLE, BOUNCE
}

@Composable
private fun AnimatedActionButton(
    title: String,
    icon: ImageVector,
    isLoading: Boolean,
    iconAnimationType: IconAnimType,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Spring press bounce animation: scale to 0.94 on press
    val scale = if (isPressed) 0.94f else 1.0f

    val infiniteTransition = rememberInfiniteTransition(label = "ButtonIdleEffects")

    // Gentle pulsing glow
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowPulse"
    )

    // Moving shimmer gradient
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerGradient"
    )

    // Icon wiggle (for gift) or bounce (for TV)
    val iconRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2500
                0f at 0
                0f at 1600
                -12f at 1800
                12f at 2000
                -8f at 2200
                0f at 2400
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "IconWiggle"
    )

    val iconBounceY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2200
                0f at 0
                0f at 1400
                -6f at 1600
                0f at 1800
                -3f at 1950
                0f at 2100
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "IconBounce"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.8.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = glowAlpha),
                        primaryColor.copy(alpha = glowAlpha * 0.8f),
                        accentColor.copy(alpha = glowAlpha)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.85f),
                        secondaryColor.copy(alpha = 0.90f),
                        Color(0xFF13092A)
                    ),
                    start = Offset(shimmerOffset * 0.2f, 0f),
                    end = Offset(shimmerOffset, 120f)
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag)
            .padding(vertical = 18.dp, horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = GoldCoinLight,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Loading ad...",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            } else {
                Box(
                    modifier = Modifier.graphicsLayer {
                        when (iconAnimationType) {
                            IconAnimType.WIGGLE -> rotationZ = iconRotation
                            IconAnimType.BOUNCE -> translationY = iconBounceY
                        }
                    }
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

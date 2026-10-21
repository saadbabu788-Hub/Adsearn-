package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdStats
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.GlassCardFill
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleSecondary
import com.example.ui.theme.RubyError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun AdPerformanceCard(
    adStats: AdStats,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(GlassCardFill)
            .border(1.dp, GlassCardBorder, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "AD PERFORMANCE & TELEMETRY",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            // 2-Column Grid of Telemetry Tiles
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        title = "Total Clicks",
                        value = adStats.totalClicks,
                        icon = Icons.Default.TouchApp,
                        iconTint = CyanAccent,
                        gradientColors = listOf(Color(0x3006B6D4), Color(0x150E0926)),
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        title = "CTR",
                        displayString = String.format(Locale.US, "%.1f%%", adStats.ctrPercent),
                        icon = Icons.Default.Percent,
                        iconTint = PurpleSecondary,
                        gradientColors = listOf(Color(0x308B5CF6), Color(0x150E0926)),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        title = "Rewarded Watched",
                        value = adStats.rewardedWatched,
                        icon = Icons.Default.OndemandVideo,
                        iconTint = EmeraldSuccess,
                        gradientColors = listOf(Color(0x3010B981), Color(0x150E0926)),
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        title = "Interstitials Shown",
                        value = adStats.interstitialsShown,
                        icon = Icons.Default.Tv,
                        iconTint = PurplePrimary,
                        gradientColors = listOf(Color(0x307C3AED), Color(0x150E0926)),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        title = "Ads Skipped",
                        value = adStats.adsSkipped,
                        icon = Icons.Default.SkipNext,
                        iconTint = Color(0xFFF97316),
                        gradientColors = listOf(Color(0x30F97316), Color(0x150E0926)),
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        title = "Coins Earned",
                        value = adStats.totalCoinsEarned,
                        icon = Icons.Default.MonetizationOn,
                        iconTint = GoldCoin,
                        gradientColors = listOf(Color(0x35FFD700), Color(0x150E0926)),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        title = "Load Success",
                        value = adStats.loadSuccess,
                        icon = Icons.Default.CheckCircle,
                        iconTint = EmeraldSuccess,
                        gradientColors = listOf(Color(0x3010B981), Color(0x150E0926)),
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        title = "Load Failed",
                        value = adStats.loadFailed,
                        icon = Icons.Default.Close,
                        iconTint = RubyError,
                        gradientColors = listOf(Color(0x30EF4444), Color(0x150E0926)),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Estimated Earnings Full-Width Tile
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0x35059669), Color(0x200D9488), Color(0x150E0926))
                        )
                    )
                    .border(1.dp, EmeraldSuccess.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(EmeraldSuccess.copy(alpha = 0.2f))
                            .border(1.dp, EmeraldSuccess.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = "Estimated Earnings",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "ESTIMATED REVENUE",
                            color = EmeraldSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "$%.4f USD", adStats.estimatedEarningsUsd),
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Estimated (check Unity Dashboard for real earnings)",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: Int? = null,
    displayString: String? = null,
    icon: ImageVector,
    iconTint: Color,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateIntAsState(
        targetValue = value ?: 0,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "MetricAnimation"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(gradientColors))
            .border(1.dp, iconTint.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = displayString ?: "$animatedValue",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

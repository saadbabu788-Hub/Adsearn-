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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.AdPlacementState
import com.example.ads.UnitySdkStatus
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.GlassCardFill
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.RubyError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AdsEngineStatusCard(
    sdkStatus: UnitySdkStatus,
    rewardedState: AdPlacementState,
    interstitialState: AdPlacementState,
    lastRawError: String?,
    onReloadClick: () -> Unit,
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
            // Header Row with Reload button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ADS ENGINE STATUS",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                // Reload Ads button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(PurplePrimary.copy(alpha = 0.15f))
                        .border(1.dp, PurplePrimary.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .clickable { onReloadClick() }
                        .testTag("reload_ads_button")
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload Ads",
                        tint = PurplePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reload Ads",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Status List with Colored Dots
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // SDK Status
                val (sdkDotColor, sdkLabel) = when (sdkStatus) {
                    is UnitySdkStatus.Initialized -> EmeraldSuccess to "Initialized (v4.21.0)"
                    is UnitySdkStatus.Initializing -> AmberWarning to "Initializing..."
                    is UnitySdkStatus.Failed -> RubyError to "Init Failed [${sdkStatus.errorCode}]"
                    is UnitySdkStatus.Uninitialized -> Color(0xFF94A3B8) to "Uninitialized"
                }
                StatusRow(title = "Unity Ads SDK", statusText = sdkLabel, dotColor = sdkDotColor)

                // Rewarded Status
                val (rewardedDotColor, rewardedLabel) = when (rewardedState) {
                    is AdPlacementState.Ready -> EmeraldSuccess to "Ready"
                    is AdPlacementState.Loading -> AmberWarning to "Loading..."
                    is AdPlacementState.Showing -> PurplePrimary to "Showing Ad"
                    is AdPlacementState.Failed -> RubyError to "Failed (${rewardedState.errorCode})"
                    is AdPlacementState.NotLoaded -> Color(0xFF94A3B8) to "Not Loaded"
                }
                StatusRow(title = "Rewarded Ad (Video)", statusText = rewardedLabel, dotColor = rewardedDotColor)

                // Interstitial Status
                val (interstitialDotColor, interstitialLabel) = when (interstitialState) {
                    is AdPlacementState.Ready -> EmeraldSuccess to "Ready"
                    is AdPlacementState.Loading -> AmberWarning to "Loading..."
                    is AdPlacementState.Showing -> PurplePrimary to "Showing Ad"
                    is AdPlacementState.Failed -> RubyError to "Failed (${interstitialState.errorCode})"
                    is AdPlacementState.NotLoaded -> Color(0xFF94A3B8) to "Not Loaded"
                }
                StatusRow(title = "Interstitial Ad", statusText = interstitialLabel, dotColor = interstitialDotColor)
            }

            // Area for RAW Unity error text
            AnimatedVisibility(visible = lastRawError != null) {
                if (lastRawError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(RubyError.copy(alpha = 0.12f))
                            .border(1.dp, RubyError.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = "Error",
                                tint = RubyError,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "RAW SDK LOG / ERROR",
                                    color = RubyError,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = lastRawError,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusRow(
    title: String,
    statusText: String,
    dotColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Text(
            text = statusText,
            color = dotColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

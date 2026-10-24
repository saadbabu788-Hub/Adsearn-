package com.example.ui.components

import android.app.Activity
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
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.AdPlacementState
import com.example.data.AdStats
import com.example.data.AdminGameItem
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassCardBorder
import com.example.ui.theme.GlassCardFill
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.GoldCoinLight
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.RubyError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun GamesSection(
    adStats: AdStats,
    rewardedState: AdPlacementState,
    isSpinning: Boolean,
    showAutoAdPrompt: Boolean,
    autoAdCountdown: Int,
    onSpinTriggered: (Activity, Int, (Int) -> Unit) -> Unit,
    onConfirmWatchAutoAd: (Activity) -> Unit,
    onDismissAutoAdPrompt: () -> Unit,
    onAwardCoins: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeMiniGame by remember { mutableStateOf<AdminGameItem?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Daily Spin Card (Preserved & prominent)
        DailySpinCard(
            adStats = adStats,
            rewardedState = rewardedState,
            isSpinning = isSpinning,
            showAutoAdPrompt = showAutoAdPrompt,
            autoAdCountdown = autoAdCountdown,
            onSpinTriggered = onSpinTriggered,
            onConfirmWatchAutoAd = onConfirmWatchAutoAd,
            onDismissAutoAdPrompt = onDismissAutoAdPrompt
        )

        // Additional Admin-Managed Games Catalog
        val publishedGames = remember(adStats.gamesCatalog) {
            adStats.gamesCatalog.filter { it.isPublished && it.id != "spin_wheel" }
        }

        if (publishedGames.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "MORE EARNING GAMES (${publishedGames.size})",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                Text(
                    text = "Play & Win Coins",
                    color = GoldCoinLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            publishedGames.forEach { game ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GlassCardFill),
                    shape = RoundedCornerShape(18.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(GlassCardBorder, Color.Transparent)))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(PurplePrimary.copy(alpha = 0.2f))
                                    .border(1.dp, GoldCoin.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = game.iconEmoji, fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = game.title,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = game.description,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = GoldCoin,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "+${game.rewardCoins} Coins on Win",
                                        color = GoldCoinLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = { activeMiniGame = game },
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Mini-Game Dialog
    if (activeMiniGame != null) {
        val game = activeMiniGame!!
        when (game.gameType) {
            "MATH_QUIZ" -> MathQuizDialog(game = game, onDismiss = { activeMiniGame = null }, onAwardCoins = onAwardCoins)
            "LUCKY_DICE" -> LuckyDiceDialog(game = game, onDismiss = { activeMiniGame = null }, onAwardCoins = onAwardCoins)
            "SCRATCH_CARD" -> ScratchCardDialog(game = game, onDismiss = { activeMiniGame = null }, onAwardCoins = onAwardCoins)
            else -> GenericPlayDialog(game = game, onDismiss = { activeMiniGame = null }, onAwardCoins = onAwardCoins)
        }
    }
}

// ---------------- MINI GAME 1: MATH QUIZ ----------------
@Composable
private fun MathQuizDialog(
    game: AdminGameItem,
    onDismiss: () -> Unit,
    onAwardCoins: (Int, String) -> Unit
) {
    var num1 by remember { mutableIntStateOf(Random.nextInt(5, 30)) }
    var num2 by remember { mutableIntStateOf(Random.nextInt(3, 20)) }
    var score by remember { mutableIntStateOf(0) }
    var round by remember { mutableIntStateOf(1) }
    val maxRounds = 5
    var isGameOver by remember { mutableStateOf(false) }

    val correctAnswer = num1 + num2
    val options = remember(round) {
        val list = mutableListOf(correctAnswer)
        while (list.size < 4) {
            val fake = correctAnswer + Random.nextInt(-8, 9)
            if (fake != correctAnswer && fake > 0 && !list.contains(fake)) {
                list.add(fake)
            }
        }
        list.shuffled()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("🧮 Math Speed Challenge", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            if (isGameOver) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (score >= 4) "🎉 Excellent Job!" else "Nice Try!",
                        color = if (score >= 4) EmeraldSuccess else GoldCoinLight,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "You scored $score out of $maxRounds correct answers.",
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    if (score >= 4) {
                        Text(
                            text = "Reward Credited: +${game.rewardCoins} Coins!",
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Question $round / $maxRounds", color = TextMuted, fontSize = 12.sp)
                        Text(text = "Score: $score", color = EmeraldSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x22FFFFFF))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$num1 + $num2 = ?",
                            color = TextPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { opt ->
                            Button(
                                onClick = {
                                    if (opt == correctAnswer) score += 1
                                    if (round < maxRounds) {
                                        round += 1
                                        num1 = Random.nextInt(5, 30)
                                        num2 = Random.nextInt(3, 20)
                                    } else {
                                        isGameOver = true
                                        if (score + (if (opt == correctAnswer) 1 else 0) >= 4) {
                                            onAwardCoins(game.rewardCoins, game.title)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x336366F1)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(text = "$opt", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isGameOver) {
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)) {
                    Text("Collect & Close", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isGameOver) {
                TextButton(onClick = onDismiss) {
                    Text("Exit Game", color = TextSecondary)
                }
            }
        },
        containerColor = Color(0xFF1E153D),
        shape = RoundedCornerShape(18.dp)
    )
}

// ---------------- MINI GAME 2: LUCKY 7 DICE ----------------
@Composable
private fun LuckyDiceDialog(
    game: AdminGameItem,
    onDismiss: () -> Unit,
    onAwardCoins: (Int, String) -> Unit
) {
    var dice1 by remember { mutableIntStateOf(3) }
    var dice2 by remember { mutableIntStateOf(4) }
    var selectedPrediction by remember { mutableStateOf<String?>(null) } // "UNDER", "SEVEN", "OVER"
    var isRolling by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var hasWon by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎲 Lucky 7 Dice Roll", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Predict if the total of two dice is Under 7, Lucky 7, or Over 7!",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Dice Display
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "$dice1", color = Color.Black, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    }
                    Text("+", color = TextMuted, fontSize = 24.sp)
                    Box(
                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "$dice2", color = Color.Black, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    }
                }

                Text(
                    text = "Total: ${dice1 + dice2}",
                    color = GoldCoin,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                if (resultText != null) {
                    Text(
                        text = resultText ?: "",
                        color = if (hasWon) EmeraldSuccess else RubyError,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Prediction options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("UNDER" to "< 7", "SEVEN" to "= 7", "OVER" to "> 7").forEach { (pred, label) ->
                        val isSel = selectedPrediction == pred
                        Button(
                            onClick = { selectedPrediction = pred },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) PurplePrimary else Color(0x22FFFFFF)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label, color = if (isSel) Color.White else TextSecondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedPrediction == null) return@Button
                    isRolling = true
                    dice1 = Random.nextInt(1, 7)
                    dice2 = Random.nextInt(1, 7)
                    val sum = dice1 + dice2
                    isRolling = false

                    val won = when (selectedPrediction) {
                        "UNDER" -> sum < 7
                        "SEVEN" -> sum == 7
                        "OVER" -> sum > 7
                        else -> false
                    }
                    hasWon = won
                    if (won) {
                        resultText = "🎉 You Won! +${game.rewardCoins} Coins credited!"
                        onAwardCoins(game.rewardCoins, game.title)
                    } else {
                        resultText = "Total was $sum. Better luck next roll!"
                    }
                },
                enabled = selectedPrediction != null && !isRolling,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
            ) {
                Text("Roll Dice", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = TextSecondary) }
        },
        containerColor = Color(0xFF1E153D),
        shape = RoundedCornerShape(18.dp)
    )
}

// ---------------- MINI GAME 3: SCRATCH CARD ----------------
@Composable
private fun ScratchCardDialog(
    game: AdminGameItem,
    onDismiss: () -> Unit,
    onAwardCoins: (Int, String) -> Unit
) {
    var isScratched by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎫 Golden Scratch Card", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Tap on the card below to reveal your secret coin prize!",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isScratched) {
                                Brush.radialGradient(listOf(EmeraldSuccess.copy(alpha = 0.3f), Color(0xFF042F2E)))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFF94A3B8), Color(0xFF475569)))
                            }
                        )
                        .border(2.dp, GoldCoin, RoundedCornerShape(14.dp))
                        .clickable(!isScratched) {
                            isScratched = true
                            onAwardCoins(game.rewardCoins, game.title)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isScratched) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🎉 WINNER! 🎉", color = GoldCoin, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text(text = "+${game.rewardCoins} COINS", color = EmeraldSuccess, fontWeight = FontWeight.Black, fontSize = 26.sp)
                            Text(text = "Credited to your balance", color = TextSecondary, fontSize = 11.sp)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Casino, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                            Text(text = "TAP TO SCRATCH", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)) {
                Text("Close")
            }
        },
        containerColor = Color(0xFF1E153D),
        shape = RoundedCornerShape(18.dp)
    )
}

// Generic play fallback
@Composable
private fun GenericPlayDialog(
    game: AdminGameItem,
    onDismiss: () -> Unit,
    onAwardCoins: (Int, String) -> Unit
) {
    var hasClaimed by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(game.title, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = game.description, color = TextSecondary, fontSize = 13.sp)
                Text(text = "Prize: +${game.rewardCoins} Coins", color = EmeraldSuccess, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!hasClaimed) {
                        hasClaimed = true
                        onAwardCoins(game.rewardCoins, game.title)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
            ) {
                Text(if (hasClaimed) "Done" else "Play & Claim +${game.rewardCoins} Coins", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close", color = TextSecondary) } },
        containerColor = Color(0xFF1E153D),
        shape = RoundedCornerShape(18.dp)
    )
}

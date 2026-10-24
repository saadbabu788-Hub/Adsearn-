package com.example.data

import java.util.UUID

data class AdminGameItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val iconEmoji: String,
    val gameType: String, // "SPIN", "MATH_QUIZ", "LUCKY_DICE", "SCRATCH_CARD"
    val rewardCoins: Int,
    val isPublished: Boolean = true,
    val launchDestination: String = "IN_APP"
)

object DefaultGamesCatalog {
    fun getDefaultGames(): List<AdminGameItem> = listOf(
        AdminGameItem(
            id = "spin_wheel",
            title = "Daily Spin Wheel",
            description = "Spin the lucky wheel twice a day to win up to 50 Coins and unlock bonus sponsor ads!",
            iconEmoji = "🎡",
            gameType = "SPIN",
            rewardCoins = 50,
            isPublished = true,
            launchDestination = "SPIN"
        ),
        AdminGameItem(
            id = "math_quiz",
            title = "Math Speed Quiz",
            description = "Solve quick arithmetic questions in 30 seconds. Score 5+ to earn bonus coins!",
            iconEmoji = "🧮",
            gameType = "MATH_QUIZ",
            rewardCoins = 15,
            isPublished = true,
            launchDestination = "MATH_QUIZ"
        ),
        AdminGameItem(
            id = "lucky_dice",
            title = "Lucky 7 Dice Roll",
            description = "Predict if the sum of 2 rolled dice is Below 7, Lucky 7, or Above 7 to win coins!",
            iconEmoji = "🎲",
            gameType = "LUCKY_DICE",
            rewardCoins = 20,
            isPublished = true,
            launchDestination = "LUCKY_DICE"
        ),
        AdminGameItem(
            id = "scratch_card",
            title = "Golden Scratch Card",
            description = "Scratch off the silver coat with your finger to reveal instant coin prizes!",
            iconEmoji = "🎫",
            gameType = "SCRATCH_CARD",
            rewardCoins = 25,
            isPublished = true,
            launchDestination = "SCRATCH_CARD"
        )
    )
}

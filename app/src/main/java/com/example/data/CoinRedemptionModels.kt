package com.example.data

import java.util.UUID

data class CoinRedemptionTransaction(
    val id: String = UUID.randomUUID().toString(),
    val coinsRedeemed: Int,
    val rupeesCredited: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Completed"
) {
    companion object {
        const val COINS_PER_UNIT = 20
        const val INR_PER_UNIT = 3.0

        fun calculateRupees(coins: Int): Double {
            if (coins < COINS_PER_UNIT) return 0.0
            val units = coins / COINS_PER_UNIT
            return units * INR_PER_UNIT
        }

        fun isValidMultiples(coins: Int): Boolean {
            return coins >= COINS_PER_UNIT && (coins % COINS_PER_UNIT == 0)
        }
    }
}

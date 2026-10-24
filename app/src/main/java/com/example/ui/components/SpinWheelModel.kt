package com.example.ui.components

import androidx.compose.ui.graphics.Color

data class SpinSector(
    val index: Int,
    val coins: Int,
    val label: String,
    val color: Color,
    val isJackpot: Boolean = false
)

val SPIN_SECTORS = listOf(
    SpinSector(0, 10, "10", Color(0xFF4F46E5)),
    SpinSector(1, 25, "25", Color(0xFF059669)),
    SpinSector(2, 5, "5", Color(0xFFD97706)),
    SpinSector(3, 50, "50", Color(0xFFDB2777)),
    SpinSector(4, 15, "15", Color(0xFF7C3AED)),
    SpinSector(5, 20, "20", Color(0xFF0891B2)),
    SpinSector(6, 100, "100 👑", Color(0xFFE11D48), isJackpot = true),
    SpinSector(7, 30, "30", Color(0xFF0D9488))
)

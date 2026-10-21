package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.GoldCoinDark
import com.example.ui.theme.GoldCoinLight
import com.example.ui.theme.PurpleSecondary
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(
    val initialAngle: Float,
    val initialSpeed: Float,
    val radius: Float,
    val color: Color,
    val isCoin: Boolean,
    val rotationSpeed: Float
)

@Composable
fun CoinConfettiOverlay(
    show: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!show) return

    val progress = remember { Animatable(0f) }

    val particles = remember {
        val colors = listOf(GoldCoin, GoldCoinLight, GoldCoinDark, PurpleSecondary, Color.White)
        List(35) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 450f + 250f
            val radius = Random.nextFloat() * 6f + 5f
            val color = colors.random()
            val isCoin = Random.nextBoolean()
            val rotationSpeed = (Random.nextFloat() - 0.5f) * 10f
            Particle(angle, speed, radius, color, isCoin, rotationSpeed)
        }
    }

    LaunchedEffect(show) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2000, easing = FastOutLinearInEasing)
        )
        onFinished()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 3f
        val t = progress.value
        val alpha = (1f - t).coerceIn(0f, 1f)
        val gravity = 900f * t * t

        particles.forEach { particle ->
            val dist = particle.initialSpeed * t
            val x = centerX + cos(particle.initialAngle) * dist
            val y = centerY + sin(particle.initialAngle) * dist + gravity

            if (particle.isCoin) {
                drawCoinParticle(
                    x = x,
                    y = y,
                    radius = particle.radius,
                    color = particle.color.copy(alpha = alpha)
                )
            } else {
                drawStarParticle(
                    x = x,
                    y = y,
                    radius = particle.radius * 0.8f,
                    color = particle.color.copy(alpha = alpha)
                )
            }
        }
    }
}

private fun DrawScope.drawCoinParticle(
    x: Float,
    y: Float,
    radius: Float,
    color: Color
) {
    // Outer coin circle
    drawCircle(
        color = color,
        radius = radius,
        center = Offset(x, y)
    )
    // Inner coin rim
    drawCircle(
        color = Color.White.copy(alpha = color.alpha * 0.7f),
        radius = radius * 0.5f,
        center = Offset(x, y)
    )
}

private fun DrawScope.drawStarParticle(
    x: Float,
    y: Float,
    radius: Float,
    color: Color
) {
    val r = radius * 1.3f
    drawLine(
        color = color,
        start = Offset(x - r, y),
        end = Offset(x + r, y),
        strokeWidth = 3f
    )
    drawLine(
        color = color,
        start = Offset(x, y - r),
        end = Offset(x, y + r),
        strokeWidth = 3f
    )
}

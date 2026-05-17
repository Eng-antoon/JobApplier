package com.aplicator.jobapplier.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        Color(0xFF004182),
        Color(0xFF0A66C2),
        Color(0xFF0073B1),
        Color(0xFF004182),
    ),
    content: @Composable () -> Unit = {}
) {
    val transition = rememberInfiniteTransition(label = "gradientRotation")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "gradientAngle",
    )

    val radians = Math.toRadians(angle.toDouble())
    val offsetX = (cos(radians) * 1000).toFloat()
    val offsetY = (sin(radians) * 1000).toFloat()

    val brush = Brush.linearGradient(
        colors = colors,
        start = Offset(500f + offsetX, 500f + offsetY),
        end = Offset(500f - offsetX, 500f - offsetY),
    )

    Box(modifier = modifier.fillMaxSize().background(brush)) {
        content()
    }
}

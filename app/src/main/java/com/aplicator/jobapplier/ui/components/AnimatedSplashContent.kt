package com.aplicator.jobapplier.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplicator.jobapplier.ui.theme.PlusJakartaSans
import kotlinx.coroutines.delay

@Composable
fun AnimatedSplashContent(modifier: Modifier = Modifier) {
    val logoScale = remember { Animatable(0.8f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, tween(600))
        logoScale.animateTo(1f, tween(800))
        delay(200)
        textAlpha.animateTo(1f, tween(500))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splashPulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulseAlpha",
    )

    AnimatedGradientBackground(modifier = modifier) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Pulse ring
                    Canvas(modifier = Modifier.size(120.dp)) {
                        drawCircle(
                            color = Color.White.copy(alpha = pulseAlpha),
                            radius = (size.minDimension / 2f) * pulseRadius,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }

                    // Logo circle
                    Canvas(
                        modifier = Modifier
                            .size(80.dp)
                            .graphicsLayer {
                                scaleX = logoScale.value
                                scaleY = logoScale.value
                                alpha = logoAlpha.value
                            }
                    ) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.15f),
                            radius = size.minDimension / 2f,
                        )
                        drawCircle(
                            color = Color.White,
                            radius = size.minDimension / 2f,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                        // Letter "J" stylized
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val letterSize = size.minDimension * 0.3f

                        drawLine(
                            color = Color.White,
                            start = androidx.compose.ui.geometry.Offset(
                                centerX + letterSize * 0.15f,
                                centerY - letterSize * 0.6f,
                            ),
                            end = androidx.compose.ui.geometry.Offset(
                                centerX + letterSize * 0.15f,
                                centerY + letterSize * 0.3f,
                            ),
                            strokeWidth = 3.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        )
                        drawArc(
                            color = Color.White,
                            startAngle = 0f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(
                                centerX - letterSize * 0.35f,
                                centerY + letterSize * 0.05f,
                            ),
                            size = androidx.compose.ui.geometry.Size(
                                letterSize * 0.5f,
                                letterSize * 0.5f,
                            ),
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "JobApplier",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    color = Color.White,
                    modifier = Modifier.graphicsLayer { alpha = textAlpha.value },
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your AI-powered career companion",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = PlusJakartaSans,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.graphicsLayer { alpha = textAlpha.value },
                )
            }
        }
    }
}

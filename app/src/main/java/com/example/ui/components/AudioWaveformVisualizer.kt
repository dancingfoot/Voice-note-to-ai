package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun AudioWaveformVisualizer(
    isListening: Boolean,
    isSpeaking: Boolean,
    isProcessing: Boolean,
    audioRms: Float,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.onBackground
    val subtleColor = MaterialTheme.colorScheme.outline

    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")

    // Processing scanner wave
    val scannerOffset by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanner"
    )

    // Speaking rhythm animation
    val speakingPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "speakingPhase"
    )

    // Smooth audio RMS animation
    val animatedRms = remember { Animatable(0f) }
    LaunchedEffect(audioRms) {
        animatedRms.animateTo(
            targetValue = audioRms,
            animationSpec = tween(durationMillis = 70, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .testTag("audio_waveform_visualizer"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(60.dp)) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f
            val barCount = 33
            val spacing = width / barCount.toFloat()
            val barWidth = (spacing * 0.45f).coerceAtLeast(3f)

            when {
                isListening -> {
                    // Responsive dynamic voice equalizer
                    val currentLevel = animatedRms.value
                    for (i in 0 until barCount) {
                        val normalizedIdx = (i - barCount / 2f) / (barCount / 2f)
                        val bellFactor = (1f - normalizedIdx * normalizedIdx).coerceAtLeast(0.15f)
                        val dynamicHeight = (10f + (currentLevel * 48f * bellFactor)).coerceIn(8f, height - 6f)
                        val x = i * spacing + (spacing - barWidth) / 2f
                        val y = centerY - dynamicHeight / 2f

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, dynamicHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                    }
                }

                isSpeaking -> {
                    // Sine pulse wave indicating active synthesized speech playback
                    for (i in 0 until barCount) {
                        val wave = (sin(speakingPhase + (i * 0.35f)) + 1f) / 2f
                        val barH = 10f + (wave * 34f)
                        val x = i * spacing + (spacing - barWidth) / 2f
                        val y = centerY - barH / 2f

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                    }
                }

                isProcessing -> {
                    // Scanning bar pulse indicating AI thinking / endpoint inference
                    for (i in 0 until barCount) {
                        val normX = i.toFloat() / barCount.toFloat()
                        val distance = kotlin.math.abs(normX - scannerOffset)
                        val intensity = (1f - distance * 3.5f).coerceIn(0.12f, 1f)
                        val barH = 8f + (intensity * 30f)
                        val x = i * spacing + (spacing - barWidth) / 2f
                        val y = centerY - barH / 2f

                        val color = if (intensity > 0.4f) barColor else subtleColor
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                    }
                }

                else -> {
                    // Idle minimal baseline with gentle center tick
                    for (i in 0 until barCount) {
                        val isCenter = kotlin.math.abs(i - barCount / 2) <= 1
                        val barH = if (isCenter) 12f else 4f
                        val x = i * spacing + (spacing - barWidth) / 2f
                        val y = centerY - barH / 2f

                        drawRoundRect(
                            color = if (isCenter) barColor else subtleColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                    }
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyGreenDark
import com.example.ui.theme.SpotifyGreenLight

@Composable
fun WaveformVisualizer(
    isAnimating: Boolean,
    amplitude: Int = 50,
    modifier: Modifier = Modifier,
    barCount: Int = 32
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        val totalWidth = size.width
        val barWidth = (totalWidth / (barCount * 1.4f)).coerceIn(3.5f, 10f)
        val spaceWidth = barWidth * 0.4f
        val maxHeight = size.height

        val gradient = Brush.verticalGradient(
            colors = listOf(
                SpotifyGreenLight,
                SpotifyGreen,
                SpotifyGreenDark
            ),
            startY = 0f,
            endY = maxHeight
        )

        for (i in 0 until barCount) {
            val x = i * (barWidth + spaceWidth)
            val sinVal = kotlin.math.sin(phase + (i * 0.35f))
            val cosVal = kotlin.math.cos(phase * 1.2f + (i * 0.2f))
            
            val baseScale = if (isAnimating) {
                val ampFactor = (amplitude / 100f).coerceIn(0.25f, 1.0f)
                val combinedWave = ((sinVal + cosVal + 2f) / 4f) * pulse
                (0.2f + 0.8f * combinedWave) * ampFactor
            } else {
                0.12f
            }

            val barHeight = (maxHeight * baseScale).coerceIn(6f, maxHeight)
            val y = (maxHeight - barHeight) / 2f

            drawRoundRect(
                brush = gradient,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}


package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.AssistantState
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import kotlin.math.sin

@Composable
fun AudioWaveformVisualizer(
    amplitude: Float,
    state: AssistantState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val barCount = 32

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val barSpacing = width / barCount
        val barWidth = barSpacing * 0.55f

        val baseColors = when (state) {
            AssistantState.DISCONNECTED -> listOf(
                NeonCyan.copy(alpha = 0.2f),
                ElectricViolet.copy(alpha = 0.1f)
            )
            AssistantState.CONNECTING -> listOf(
                NeonCyan.copy(alpha = 0.6f),
                CyberMagenta.copy(alpha = 0.5f)
            )
            AssistantState.LISTENING -> listOf(
                NeonCyan,
                NeonCyan.copy(alpha = 0.6f)
            )
            AssistantState.THINKING -> listOf(
                ElectricViolet,
                CyberMagenta
            )
            AssistantState.SPEAKING -> listOf(
                CyberMagenta,
                NeonCyan
            )
        }

        val brush = Brush.verticalGradient(
            colors = listOf(
                baseColors.first(),
                baseColors.last()
            ),
            startY = 0f,
            endY = height
        )

        for (i in 0 until barCount) {
            val progress = i.toFloat() / barCount
            val x = i * barSpacing + (barSpacing - barWidth) / 2f

            // Calculate height influenced by state and real audio amplitude
            val sineWave = (sin(progress * 8f + phase) * 0.5f + 0.5f)
            val centerWeight = 1f - kotlin.math.abs(progress - 0.5f) * 1.5f
            val smoothWeight = centerWeight.coerceIn(0.2f, 1f)

            val targetHeight = when (state) {
                AssistantState.DISCONNECTED -> 6.dp.toPx()
                AssistantState.CONNECTING -> (12.dp.toPx() + sineWave * 18.dp.toPx()) * smoothWeight
                AssistantState.LISTENING -> {
                    val activeH = 8.dp.toPx() + (amplitude * 55.dp.toPx() * smoothWeight)
                    activeH + (sineWave * 8.dp.toPx() * amplitude)
                }
                AssistantState.THINKING -> (16.dp.toPx() + (sin(progress * 12f - phase * 1.5f) * 0.5f + 0.5f) * 26.dp.toPx()) * smoothWeight
                AssistantState.SPEAKING -> {
                    val speechH = 10.dp.toPx() + (amplitude * 60.dp.toPx() * smoothWeight)
                    speechH + (sineWave * 12.dp.toPx() * (amplitude + 0.2f))
                }
            }.coerceIn(4.dp.toPx(), height - 6.dp.toPx())

            val top = centerY - targetHeight / 2f

            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, top),
                size = Size(barWidth, targetHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

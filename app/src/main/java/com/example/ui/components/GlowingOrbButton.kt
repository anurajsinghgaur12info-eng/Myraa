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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AssistantState
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlowPurple
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.VoidBlack

@Composable
fun GlowingOrbButton(
    state: AssistantState,
    amplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    // Ambient breathing pulse
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // Orbital ring continuous rotation
    val rotationDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Dynamic scale driven by audio amplitude
    val audioScaleAnim = remember { Animatable(1f) }
    LaunchedEffect(amplitude) {
        val target = 1f + (amplitude * 0.35f)
        audioScaleAnim.animateTo(
            targetValue = target,
            animationSpec = tween(60, easing = LinearEasing)
        )
    }

    val totalScale = when (state) {
        AssistantState.DISCONNECTED -> 0.94f
        AssistantState.CONNECTING -> breathingScale
        AssistantState.LISTENING -> audioScaleAnim.value * breathingScale
        AssistantState.THINKING -> breathingScale * 1.02f
        AssistantState.SPEAKING -> audioScaleAnim.value * 1.06f
    }

    val primaryGlow = when (state) {
        AssistantState.DISCONNECTED -> NeonCyan.copy(alpha = 0.25f)
        AssistantState.CONNECTING -> NeonCyan
        AssistantState.LISTENING -> NeonCyan
        AssistantState.THINKING -> ElectricViolet
        AssistantState.SPEAKING -> CyberMagenta
    }

    val secondaryGlow = when (state) {
        AssistantState.DISCONNECTED -> ElectricViolet.copy(alpha = 0.15f)
        AssistantState.CONNECTING -> CyberMagenta
        AssistantState.LISTENING -> GlowPurple
        AssistantState.THINKING -> NeonCyan
        AssistantState.SPEAKING -> ElectricViolet
    }

    Box(
        modifier = modifier
            .size(240.dp)
            .scale(totalScale),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing energy aura layers
        Canvas(modifier = Modifier.size(230.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // Outer subtle glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = if (state == AssistantState.DISCONNECTED) 0.08f else 0.35f),
                        secondaryGlow.copy(alpha = if (state == AssistantState.DISCONNECTED) 0.03f else 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.width / 2f
                ),
                radius = size.width / 2f,
                center = center
            )

            // Ambient ring 1
            if (state != AssistantState.DISCONNECTED) {
                drawCircle(
                    color = primaryGlow.copy(alpha = 0.4f),
                    radius = (size.width / 2f) - 14.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.8.dp.toPx())
                )
                // Ambient ring 2
                drawCircle(
                    color = secondaryGlow.copy(alpha = 0.3f),
                    radius = (size.width / 2f) - 26.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
        }

        // Rotating Cybernetic Neural Ring
        Canvas(
            modifier = Modifier
                .size(190.dp)
                .rotate(if (state == AssistantState.THINKING) rotationDeg * 2f else rotationDeg)
        ) {
            val strokeW = 2.5.dp.toPx()
            val radius = (size.width / 2f) - 6.dp.toPx()
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        primaryGlow,
                        Color.Transparent,
                        secondaryGlow,
                        Color.Transparent,
                        primaryGlow
                    )
                ),
                startAngle = 0f,
                sweepAngle = 280f,
                useCenter = false,
                style = Stroke(width = strokeW),
                topLeft = Offset(6.dp.toPx(), 6.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
        }

        // Core Orb (Interactive Button)
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SurfaceDark,
                            VoidBlack
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = primaryGlow),
                    onClick = onClick
                )
                .testTag("orb_power_button"),
            contentAlignment = Alignment.Center
        ) {
            // Internal Holographic Depth
            Canvas(modifier = Modifier.size(140.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryGlow.copy(alpha = if (state == AssistantState.DISCONNECTED) 0.15f else 0.5f),
                            secondaryGlow.copy(alpha = if (state == AssistantState.DISCONNECTED) 0.05f else 0.25f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = 65.dp.toPx()
                    ),
                    radius = 65.dp.toPx(),
                    center = center
                )
            }

            // Central Dynamic Icon
            when (state) {
                AssistantState.DISCONNECTED -> {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Connect Myraa",
                        tint = NeonCyan.copy(alpha = 0.8f),
                        modifier = Modifier.size(46.dp)
                    )
                }
                AssistantState.CONNECTING -> {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Connecting",
                        tint = NeonCyan,
                        modifier = Modifier
                            .size(46.dp)
                            .rotate(rotationDeg)
                    )
                }
                AssistantState.LISTENING -> {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Listening",
                        tint = NeonCyan,
                        modifier = Modifier.size(50.dp)
                    )
                }
                AssistantState.THINKING -> {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Thinking",
                        tint = ElectricViolet,
                        modifier = Modifier.size(48.dp)
                    )
                }
                AssistantState.SPEAKING -> {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Tap to interrupt",
                        tint = CyberMagenta,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssistantState
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TranscriptOverlay(
    visible: Boolean,
    state: AssistantState,
    userText: String,
    myraaText: String,
    modifier: Modifier = Modifier
) {
    val displayText = when (state) {
        AssistantState.LISTENING -> {
            if (userText.isNotBlank()) "You: \"$userText\"" else "Listening to your voice..."
        }
        AssistantState.THINKING -> {
            if (userText.isNotBlank()) "\"$userText\"" else "Thinking..."
        }
        AssistantState.SPEAKING -> {
            if (myraaText.isNotBlank()) myraaText else "Speaking..."
        }
        AssistantState.CONNECTING -> "Establishing neural link..."
        AssistantState.DISCONNECTED -> "Tap the orb to start talking with Myraa"
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        GlassmorphicCard(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayText,
                    color = if (state == AssistantState.SPEAKING) CyberMagenta else if (state == AssistantState.LISTENING) NeonCyan else TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    maxLines = 3
                )
            }
        }
    }
}

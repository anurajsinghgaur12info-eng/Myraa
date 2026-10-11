package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.AssistantState
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.GlowingOrbButton
import com.example.ui.components.ToolActionBanner
import com.example.ui.components.TranscriptOverlay
import com.example.ui.components.VoiceSelectorSheet
import com.example.ui.theme.CyberMagenta
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.MyraaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyraaMainScreen(
    viewModel: MyraaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val assistantState by viewModel.assistantState.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()
    val currentTranscript by viewModel.currentTranscript.collectAsState()
    val userTranscript by viewModel.userTranscript.collectAsState()
    val selectedVoice by viewModel.selectedVoice.collectAsState()
    val selectedTone by viewModel.selectedTone.collectAsState()
    val executedTools by viewModel.executedTools.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val showVoiceSettings by viewModel.showVoiceSettings.collectAsState()
    val showTranscriptHUD by viewModel.showTranscriptHUD.collectAsState()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            viewModel.connectAndStart()
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                VoidBlack,
                SurfaceDark.copy(alpha = 0.85f),
                VoidBlack
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        // Decorative ambient top/bottom glows
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(350.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(320.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            CyberMagenta.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            TopBarSection(
                assistantState = assistantState,
                voiceName = selectedVoice.name,
                toneTitle = selectedTone.title,
                showTranscript = showTranscriptHUD,
                onVoiceClick = { viewModel.toggleVoiceSettings(true) },
                onTranscriptToggle = { viewModel.toggleTranscriptHUD() }
            )

            // Center: Waveform & Central Holographic Orb
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Status Subtitle
                Text(
                    text = assistantState.description,
                    color = when (assistantState) {
                        AssistantState.SPEAKING -> CyberMagenta
                        AssistantState.LISTENING -> NeonCyan
                        AssistantState.THINKING -> ElectricViolet
                        AssistantState.CONNECTING -> NeonCyan
                        AssistantState.DISCONNECTED -> TextMuted
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Central Orb Button
                GlowingOrbButton(
                    state = assistantState,
                    amplitude = amplitude,
                    onClick = {
                        if (!hasMicPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.onOrbClicked()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Waveform Visualizer
                AudioWaveformVisualizer(
                    amplitude = amplitude,
                    state = assistantState,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Bottom Area: Subtitles HUD, Tool Banner, Suggestions & Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tool Execution Banner
                ToolActionBanner(
                    tool = executedTools.firstOrNull()
                )

                // Transcript Floating HUD
                TranscriptOverlay(
                    visible = showTranscriptHUD,
                    state = assistantState,
                    userText = userTranscript,
                    myraaText = currentTranscript
                )

                // Quick Prompt Chips
                QuickPromptChips(
                    assistantState = assistantState,
                    onPromptSelected = { prompt ->
                        if (!hasMicPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.sendDirectPrompt(prompt)
                        }
                    }
                )

                // Active Session Controls
                if (assistantState != AssistantState.DISCONNECTED) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassmorphicCard(
                            shape = CircleShape,
                            borderColor = CyberMagenta,
                            modifier = Modifier.clickable { viewModel.disconnect() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = "End call",
                                    tint = CyberMagenta,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "End Session",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Error message banner
        if (errorMessage != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = SurfaceDark,
                contentColor = TextPrimary,
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss", color = NeonCyan)
                    }
                }
            ) {
                Text(errorMessage ?: "")
            }
        }

        // Voice Selector Modal Sheet
        if (showVoiceSettings) {
            VoiceSelectorSheet(
                sheetState = sheetState,
                selectedVoice = selectedVoice,
                selectedTone = selectedTone,
                onVoiceSelected = { viewModel.selectVoice(it) },
                onToneSelected = { viewModel.selectTone(it) },
                onDismiss = { viewModel.toggleVoiceSettings(false) }
            )
        }
    }
}

@Composable
private fun TopBarSection(
    assistantState: AssistantState,
    voiceName: String,
    toneTitle: String,
    showTranscript: Boolean,
    onVoiceClick: () -> Unit,
    onTranscriptToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Title & Status Dot
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        when (assistantState) {
                            AssistantState.SPEAKING -> CyberMagenta
                            AssistantState.LISTENING -> NeonTeal
                            AssistantState.THINKING -> WarningAmber
                            AssistantState.CONNECTING -> NeonCyan
                            AssistantState.DISCONNECTED -> TextMuted
                        }
                    )
            )
            Column {
                Text(
                    text = "MYRAA",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "LIVE AI COMPANION",
                    color = NeonCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Voice Persona Selector Chip
            GlassmorphicCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .clickable { onVoiceClick() }
                    .testTag("voice_selector_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Voice settings",
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "$voiceName",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Transcript Toggle Button
            IconButton(
                onClick = onTranscriptToggle,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceGlass)
                    .testTag("transcript_toggle_button")
            ) {
                Icon(
                    imageVector = if (showTranscript) Icons.Default.Subtitles else Icons.Default.SubtitlesOff,
                    contentDescription = "Toggle Subtitles",
                    tint = if (showTranscript) NeonCyan else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickPromptChips(
    assistantState: AssistantState,
    onPromptSelected: (String) -> Unit
) {
    val prompts = listOf(
        "Open YouTube",
        "Tell me a witty joke",
        "How is the weather today?",
        "Who created you?",
        "Save a note: meeting at 3pm"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        prompts.forEach { prompt ->
            GlassmorphicCard(
                shape = RoundedCornerShape(14.dp),
                borderColor = GlassBorder,
                modifier = Modifier.clickable { onPromptSelected(prompt) }
            ) {
                Text(
                    text = prompt,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

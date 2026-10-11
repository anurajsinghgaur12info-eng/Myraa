package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayer
import com.example.audio.AudioRecorder
import com.example.audio.WavUtils
import com.example.gemini.GeminiLiveService
import com.example.gemini.GeminiVoiceResponse
import com.example.model.AssistantState
import com.example.model.PersonaTone
import com.example.model.ToolExecution
import com.example.model.VoiceDefaults
import com.example.model.VoiceOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MyraaViewModel(application: Application) : AndroidViewModel(application) {

    private val geminiService = GeminiLiveService(application)

    // State flows
    private val _assistantState = MutableStateFlow(AssistantState.DISCONNECTED)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _currentTranscript = MutableStateFlow("")
    val currentTranscript: StateFlow<String> = _currentTranscript.asStateFlow()

    private val _userTranscript = MutableStateFlow("")
    val userTranscript: StateFlow<String> = _userTranscript.asStateFlow()

    private val _selectedVoice = MutableStateFlow(VoiceDefaults.AVAILABLE_VOICES.first())
    val selectedVoice: StateFlow<VoiceOption> = _selectedVoice.asStateFlow()

    private val _selectedTone = MutableStateFlow(VoiceDefaults.AVAILABLE_TONES.first())
    val selectedTone: StateFlow<PersonaTone> = _selectedTone.asStateFlow()

    private val _executedTools = MutableStateFlow<List<ToolExecution>>(emptyList())
    val executedTools: StateFlow<List<ToolExecution>> = _executedTools.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _showVoiceSettings = MutableStateFlow(false)
    val showVoiceSettings: StateFlow<Boolean> = _showVoiceSettings.asStateFlow()

    private val _showTranscriptHUD = MutableStateFlow(true)
    val showTranscriptHUD: StateFlow<Boolean> = _showTranscriptHUD.asStateFlow()

    // Audio Components
    private val audioPlayer = AudioPlayer(
        onPlaybackAmplitudeChanged = { amp ->
            _amplitude.value = amp
        },
        onPlaybackFinished = {
            if (_assistantState.value == AssistantState.SPEAKING) {
                // Return to listening for continuous voice conversation
                startListening()
            }
        }
    )

    private val audioRecorder = AudioRecorder(
        onAmplitudeChanged = { amp ->
            if (_assistantState.value == AssistantState.LISTENING) {
                _amplitude.value = amp
            }
        },
        onSpeechUtteranceReady = { pcmBytes ->
            viewModelScope.launch {
                handleUserSpeechUtterance(pcmBytes)
            }
        }
    )

    // Android Speech Recognizer for real-time live captions
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null

    // Android TTS fallback
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        initSpeechRecognizer()
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        tts = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    engine.language = Locale.US
                    // Tune for young, energetic, expressive female voice
                    engine.setPitch(1.18f)
                    engine.setSpeechRate(1.05f)
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _assistantState.value = AssistantState.SPEAKING
                        }

                        override fun onDone(utteranceId: String?) {
                            viewModelScope.launch(Dispatchers.Main) {
                                _amplitude.value = 0f
                                if (_assistantState.value == AssistantState.SPEAKING) {
                                    startListening()
                                }
                            }
                        }

                        override fun onError(utteranceId: String?) {
                            viewModelScope.launch(Dispatchers.Main) {
                                _amplitude.value = 0f
                                startListening()
                            }
                        }
                    })
                    isTtsReady = true
                }
            }
        }
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(getApplication())) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplication())
            recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {
                    if (_assistantState.value == AssistantState.LISTENING) {
                        val norm = ((rmsdB + 2f) / 14f).coerceIn(0f, 1f)
                        _amplitude.value = maxOf(_amplitude.value, norm)
                    }
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    // Ignored - continuous session recovers
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        _userTranscript.value = text
                        viewModelScope.launch {
                            handleUserSpeechUtterance(null, text)
                        }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partialText = matches?.firstOrNull() ?: ""
                    if (partialText.isNotBlank()) {
                        _userTranscript.value = partialText
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    /**
     * Primary action triggered by central power / orb button
     */
    fun onOrbClicked() {
        when (_assistantState.value) {
            AssistantState.DISCONNECTED -> {
                connectAndStart()
            }
            AssistantState.SPEAKING -> {
                // Interruption: immediate interrupt and listen
                interrupt()
            }
            AssistantState.LISTENING -> {
                // Force commit/pause
                disconnect()
            }
            AssistantState.CONNECTING, AssistantState.THINKING -> {
                disconnect()
            }
        }
    }

    fun connectAndStart() {
        viewModelScope.launch {
            _assistantState.value = AssistantState.CONNECTING
            _errorMessage.value = null
            geminiService.resetSession()
            kotlinx.coroutines.delay(600) // Smooth connection visual
            startListening()
        }
    }

    private fun startListening() {
        _assistantState.value = AssistantState.LISTENING
        audioPlayer.stop()
        tts?.stop()

        // Start PCM audio recording
        audioRecorder.startRecording(viewModelScope)

        // Start live speech recognizer for instant transcription feedback
        try {
            recognizerIntent?.let { intent ->
                speechRecognizer?.startListening(intent)
            }
        } catch (e: Exception) {
            Log.w("MyraaViewModel", "Could not start speech recognizer", e)
        }
    }

    /**
     * Interrupts Myraa while speaking
     */
    fun interrupt() {
        audioPlayer.stop()
        tts?.stop()
        _amplitude.value = 0f
        startListening()
    }

    fun disconnect() {
        audioRecorder.stopRecording()
        audioPlayer.stop()
        tts?.stop()
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // Ignore
        }
        _assistantState.value = AssistantState.DISCONNECTED
        _amplitude.value = 0f
    }

    private suspend fun handleUserSpeechUtterance(pcmBytes: ByteArray?, fallbackText: String? = null) {
        if (_assistantState.value != AssistantState.LISTENING) return

        _assistantState.value = AssistantState.THINKING
        audioRecorder.stopRecording()
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // Ignore
        }

        val wavData = if (pcmBytes != null && pcmBytes.isNotEmpty()) {
            WavUtils.pcmToWav(pcmBytes)
        } else null

        val response = geminiService.sendVoiceInteraction(
            wavAudioBytes = wavData,
            transcribedText = fallbackText ?: _userTranscript.value,
            selectedVoice = _selectedVoice.value.id,
            tonePromptDescription = _selectedTone.value.promptDescription
        )

        withContext(Dispatchers.Main) {
            when (response) {
                is GeminiVoiceResponse.Success -> {
                    _currentTranscript.value = response.transcript
                    if (response.executedTools.isNotEmpty()) {
                        _executedTools.value = response.executedTools + _executedTools.value.take(4)
                    }

                    if (response.pcmAudioBytes != null && response.pcmAudioBytes.isNotEmpty()) {
                        // Play native Gemini Live audio!
                        _assistantState.value = AssistantState.SPEAKING
                        audioPlayer.playPcm(response.pcmAudioBytes, 24000, viewModelScope)
                    } else if (response.transcript.isNotBlank()) {
                        // Play via responsive voice synthesis fallback
                        _assistantState.value = AssistantState.SPEAKING
                        speakWithTts(response.transcript)
                    } else {
                        startListening()
                    }
                }
                is GeminiVoiceResponse.Error -> {
                    _errorMessage.value = response.message
                    _assistantState.value = AssistantState.SPEAKING
                    speakWithTts("Oops! Seems like my connection flickered. Try asking me again!")
                }
            }
        }
    }

    private fun speakWithTts(text: String) {
        if (isTtsReady && tts != null) {
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "myraa_reply")
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "myraa_reply")
            // Launch amplitude wave simulation for speaking
            viewModelScope.launch(Dispatchers.Default) {
                var p = 0f
                while (_assistantState.value == AssistantState.SPEAKING && tts?.isSpeaking == true) {
                    p += 0.4f
                    val amp = ((kotlin.math.sin(p) * 0.45f) + (kotlin.math.sin(p * 2.1f) * 0.25f) + 0.35f).coerceIn(0.1f, 1f)
                    withContext(Dispatchers.Main) {
                        _amplitude.value = amp
                    }
                    kotlinx.coroutines.delay(45)
                }
            }
        } else {
            // TTS not available, proceed back to listening
            viewModelScope.launch {
                kotlinx.coroutines.delay(2000)
                startListening()
            }
        }
    }

    fun selectVoice(voice: VoiceOption) {
        _selectedVoice.value = voice
    }

    fun selectTone(tone: PersonaTone) {
        _selectedTone.value = tone
    }

    fun toggleVoiceSettings(show: Boolean) {
        _showVoiceSettings.value = show
    }

    fun toggleTranscriptHUD() {
        _showTranscriptHUD.value = !_showTranscriptHUD.value
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * User quick trigger: say something directly
     */
    fun sendDirectPrompt(prompt: String) {
        viewModelScope.launch {
            _userTranscript.value = prompt
            _assistantState.value = AssistantState.THINKING
            handleUserSpeechUtterance(null, prompt)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.stopRecording()
        audioPlayer.stop()
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

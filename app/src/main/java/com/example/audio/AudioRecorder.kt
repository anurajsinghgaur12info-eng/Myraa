package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

class AudioRecorder(
    private val sampleRate: Int = 16000,
    private val onAmplitudeChanged: (Float) -> Unit,
    private val onSpeechUtteranceReady: (ByteArray) -> Unit
) {
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    @Volatile
    private var isRecording = false

    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = maxOf(
        AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat),
        2048
    )

    @SuppressLint("MissingPermission")
    fun startRecording(coroutineScope: CoroutineScope) {
        if (isRecording) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioRecorder", "AudioRecord initialization failed")
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            recordingJob = coroutineScope.launch(Dispatchers.IO) {
                val buffer = ByteArray(bufferSize)
                val currentUtterance = ByteArrayOutputStream()
                var speechFrames = 0
                var silenceFrames = 0
                val silenceThresholdFrames = 25 // ~1.5 - 2s silence to trigger utterance completion
                val speechEnergyThreshold = 0.04f

                while (isActive && isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (read > 0) {
                        val amplitude = calculateRms(buffer, read)
                        onAmplitudeChanged(amplitude)

                        if (amplitude > speechEnergyThreshold) {
                            speechFrames++
                            silenceFrames = 0
                            currentUtterance.write(buffer, 0, read)
                        } else {
                            if (speechFrames > 8) { // Has spoken at least a few words
                                currentUtterance.write(buffer, 0, read)
                                silenceFrames++
                                if (silenceFrames >= silenceThresholdFrames) {
                                    // User paused speaking! Commit utterance
                                    val utteranceBytes = currentUtterance.toByteArray()
                                    currentUtterance.reset()
                                    speechFrames = 0
                                    silenceFrames = 0
                                    if (utteranceBytes.isNotEmpty()) {
                                        onSpeechUtteranceReady(utteranceBytes)
                                    }
                                }
                            } else {
                                // Background noise, reset
                                currentUtterance.reset()
                                speechFrames = 0
                                silenceFrames = 0
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to start recording", e)
            stopRecording()
        }
    }

    /**
     * Force flushes any recorded audio buffer right now (e.g. when user taps stop/send)
     */
    fun stopRecording(): ByteArray? {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null

        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w("AudioRecorder", "Error stopping AudioRecord", e)
        } finally {
            audioRecord = null
            onAmplitudeChanged(0f)
        }
        return null
    }

    private fun calculateRms(buffer: ByteArray, readSize: Int): Float {
        var sum = 0.0
        val sampleCount = readSize / 2
        if (sampleCount == 0) return 0f

        var i = 0
        while (i < readSize - 1) {
            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
            val sampleShort = sample.toShort()
            sum += (sampleShort * sampleShort).toDouble()
            i += 2
        }

        val rms = sqrt(sum / sampleCount)
        // Normalize 0.0 to 1.0 (typical speaking RMS is between 500 and 15000)
        return (rms / 12000.0).toFloat().coerceIn(0f, 1f)
    }
}

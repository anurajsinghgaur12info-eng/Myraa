package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sqrt

class AudioPlayer(
    private val onPlaybackAmplitudeChanged: (Float) -> Unit,
    private val onPlaybackFinished: () -> Unit
) {
    private var audioTrack: AudioTrack? = null
    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null
    @Volatile
    private var isPlaying = false

    /**
     * Plays raw PCM audio data (default 24kHz 16-bit mono as output by Gemini Live / Native Audio)
     */
    fun playPcm(
        pcmData: ByteArray,
        sampleRate: Int = 24000,
        coroutineScope: CoroutineScope
    ) {
        stop()

        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = maxOf(minBufferSize, 4096)

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            isPlaying = true

            playbackJob = coroutineScope.launch(Dispatchers.IO) {
                var offset = 0
                val chunkSize = bufferSize
                while (isActive && isPlaying && offset < pcmData.size) {
                    val length = minOf(chunkSize, pcmData.size - offset)
                    val written = audioTrack?.write(pcmData, offset, length) ?: -1
                    if (written > 0) {
                        val amplitude = calculateRms(pcmData, offset, written)
                        withContext(Dispatchers.Main) {
                            onPlaybackAmplitudeChanged(amplitude)
                        }
                        offset += written
                    } else {
                        break
                    }
                }

                // Wait briefly for buffer to flush
                try {
                    audioTrack?.stop()
                } catch (e: Exception) {
                    // Ignore
                }

                withContext(Dispatchers.Main) {
                    onPlaybackAmplitudeChanged(0f)
                    isPlaying = false
                    onPlaybackFinished()
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error playing PCM audio", e)
            stop()
            onPlaybackFinished()
        }
    }

    /**
     * Plays WAV or compressed audio bytes using MediaPlayer
     */
    fun playWavOrAudioBytes(
        audioBytes: ByteArray,
        cacheDir: File,
        coroutineScope: CoroutineScope
    ) {
        stop()
        try {
            val tempFile = File(cacheDir, "myraa_speech_${System.currentTimeMillis()}.wav")
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    this@AudioPlayer.isPlaying = false
                    onPlaybackAmplitudeChanged(0f)
                    onPlaybackFinished()
                    tempFile.delete()
                }
                start()
            }
            this@AudioPlayer.isPlaying = true

            // Simulate waveform from audio stream while playing
            playbackJob = coroutineScope.launch(Dispatchers.Default) {
                var phase = 0f
                while (isActive && isPlaying && mediaPlayer?.isPlaying == true) {
                    phase += 0.35f
                    // Create natural voice modulation waveform
                    val simulatedAmp = ((kotlin.math.sin(phase) * 0.4f) + (kotlin.math.sin(phase * 2.3f) * 0.3f) + 0.3f).coerceIn(0.1f, 0.95f)
                    withContext(Dispatchers.Main) {
                        onPlaybackAmplitudeChanged(simulatedAmp)
                    }
                    kotlinx.coroutines.delay(40)
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error playing audio file", e)
            stop()
            onPlaybackFinished()
        }
    }

    /**
     * Immediately interrupts and stops any audio currently playing
     */
    fun stop() {
        isPlaying = false
        playbackJob?.cancel()
        playbackJob = null

        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // Ignore
        } finally {
            audioTrack = null
        }

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignore
        } finally {
            mediaPlayer = null
        }

        onPlaybackAmplitudeChanged(0f)
    }

    fun isPlaying(): Boolean = isPlaying

    private fun calculateRms(buffer: ByteArray, offset: Int, length: Int): Float {
        var sum = 0.0
        val sampleCount = length / 2
        if (sampleCount == 0) return 0f

        var i = offset
        val end = offset + length - 1
        while (i < end) {
            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
            val sampleShort = sample.toShort()
            sum += (sampleShort * sampleShort).toDouble()
            i += 2
        }

        val rms = sqrt(sum / sampleCount)
        return (rms / 14000.0).toFloat().coerceIn(0f, 1f)
    }
}

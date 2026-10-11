package com.example

import com.example.audio.WavUtils
import com.example.model.VoiceDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MyraaVoiceTest {

    @Test
    fun testPcmToWavHeader() {
        val pcm = ByteArray(3200) // 100ms at 16kHz 16-bit
        val wav = WavUtils.pcmToWav(pcm, sampleRate = 16000)
        // 44 byte header + 3200 byte payload
        assertEquals(3244, wav.size)
        assertEquals('R'.code.toByte(), wav[0])
        assertEquals('I'.code.toByte(), wav[1])
        assertEquals('F'.code.toByte(), wav[2])
        assertEquals('F'.code.toByte(), wav[3])
        assertEquals('W'.code.toByte(), wav[8])
        assertEquals('A'.code.toByte(), wav[9])
        assertEquals('V'.code.toByte(), wav[10])
        assertEquals('E'.code.toByte(), wav[11])
    }

    @Test
    fun testVoiceConfigurations() {
        assertTrue(VoiceDefaults.AVAILABLE_VOICES.isNotEmpty())
        val aoede = VoiceDefaults.AVAILABLE_VOICES.first { it.id == "Aoede" }
        assertEquals("Aoede", aoede.name)

        assertTrue(VoiceDefaults.AVAILABLE_TONES.isNotEmpty())
        val witty = VoiceDefaults.AVAILABLE_TONES.first { it.id == "witty" }
        assertTrue(witty.promptDescription.contains("witty", ignoreCase = true))
    }
}

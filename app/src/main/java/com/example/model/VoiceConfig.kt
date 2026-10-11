package com.example.model

data class VoiceOption(
    val id: String,
    val name: String,
    val description: String,
    val vibe: String
)

data class PersonaTone(
    val id: String,
    val title: String,
    val promptDescription: String
)

object VoiceDefaults {
    val AVAILABLE_VOICES = listOf(
        VoiceOption(
            id = "Aoede",
            name = "Aoede",
            description = "Melodic, charming, and naturally expressive",
            vibe = "Charming & Smooth"
        ),
        VoiceOption(
            id = "Kore",
            name = "Kore",
            description = "Playful, bright, and vibrant tone",
            vibe = "Playful & Vibrant"
        ),
        VoiceOption(
            id = "Leda",
            name = "Leda",
            description = "Warm, sophisticated, and soothing",
            vibe = "Warm & Sophisticated"
        ),
        VoiceOption(
            id = "Zephyr",
            name = "Zephyr",
            description = "Energetic, crisp, and agile",
            vibe = "Energetic & Crisp"
        ),
        VoiceOption(
            id = "Fenrir",
            name = "Fenrir",
            description = "Deep, steady, and resonant",
            vibe = "Resonant & Calm"
        )
    )

    val AVAILABLE_TONES = listOf(
        PersonaTone(
            id = "witty",
            title = "Witty & Charming",
            promptDescription = "Young, confident, witty, playful, and charming. Use clever remarks, light banter, and humor when appropriate. Keep answers snappy and lively."
        ),
        PersonaTone(
            id = "energetic",
            title = "Playful & Energetic",
            promptDescription = "Highly energetic, upbeat, super enthusiastic, friendly, and delightfully fun."
        ),
        PersonaTone(
            id = "empathic",
            title = "Warm & Empathetic",
            promptDescription = "Deeply supportive, emotionally aware, warm, caring, understanding, and kind."
        ),
        PersonaTone(
            id = "sharp",
            title = "Genius & Sassy",
            promptDescription = "Brilliant, razor-sharp intellect with sassy humor, clever comebacks, and confident swagger."
        )
    )
}

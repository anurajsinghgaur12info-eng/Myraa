package com.example.model

enum class AssistantState(val label: String, val description: String) {
    DISCONNECTED("Disconnected", "Tap the central orb to connect"),
    CONNECTING("Connecting", "Synchronizing neural voice link..."),
    LISTENING("Listening", "Myraa is listening to you..."),
    THINKING("Thinking", "Myraa is processing your request..."),
    SPEAKING("Speaking", "Myraa is speaking...")
}

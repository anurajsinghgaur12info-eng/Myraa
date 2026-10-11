package com.example.model

data class ToolExecution(
    val id: String = System.currentTimeMillis().toString(),
    val toolName: String,
    val summary: String,
    val details: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true
)

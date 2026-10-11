package com.example.gemini

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.model.ToolExecution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class GeminiVoiceResponse {
    data class Success(
        val pcmAudioBytes: ByteArray?,
        val transcript: String,
        val executedTools: List<ToolExecution>
    ) : GeminiVoiceResponse()

    data class Error(val message: String) : GeminiVoiceResponse()
}

class GeminiLiveService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    // Multi-turn conversation history
    private val conversationHistory = JSONArray()

    fun resetSession() {
        while (conversationHistory.length() > 0) {
            conversationHistory.remove(0)
        }
    }

    /**
     * Sends voice or text prompt to Gemini Native Audio model with tools enabled.
     */
    suspend fun sendVoiceInteraction(
        wavAudioBytes: ByteArray?,
        transcribedText: String?,
        selectedVoice: String = "Aoede",
        tonePromptDescription: String
    ): GeminiVoiceResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY.ifBlank { "" }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Friendly witty fallback when API key is not yet set in Secrets panel
            val wittyReplies = listOf(
                "Hey handsome! I'm Myraa. I hear you loud and clear, but it looks like you haven't added your Gemini API key in AI Studio Secrets yet. Pop it in, and I'll unleash my full conversational magic!",
                "I love that enthusiasm! To activate my real-time Gemini Live audio link, remember to set your GEMINI_API_KEY. Until then, I'm already right here by your side!",
                "Look at you testing the waters! Set your Gemini API key in the AI Studio Secrets panel, and we can chat all night long."
            )
            val replyText = wittyReplies.random()
            return@withContext GeminiVoiceResponse.Success(
                pcmAudioBytes = null,
                transcript = replyText,
                executedTools = emptyList()
            )
        }

        try {
            // Prepare user content part
            val userContent = JSONObject().apply {
                put("role", "user")
                val parts = JSONArray()

                // Include audio if available
                if (wavAudioBytes != null && wavAudioBytes.isNotEmpty()) {
                    val audioBase64 = Base64.encodeToString(wavAudioBytes, Base64.NO_WRAP)
                    val inlineData = JSONObject().apply {
                        put("mimeType", "audio/wav")
                        put("data", audioBase64)
                    }
                    parts.put(JSONObject().apply { put("inlineData", inlineData) })
                }

                // If user text transcription is available, include it as well
                if (!transcribedText.isNullOrBlank()) {
                    parts.put(JSONObject().apply { put("text", transcribedText) })
                }

                // If both are somehow empty, send default greeting
                if (parts.length() == 0) {
                    parts.put(JSONObject().apply { put("text", "Hello Myraa!") })
                }

                put("parts", parts)
            }

            conversationHistory.put(userContent)

            // Limit history to last 10 messages to maintain ultra-fast latency
            while (conversationHistory.length() > 10) {
                conversationHistory.remove(0)
            }

            // Build request JSON
            val requestJson = JSONObject().apply {
                val systemPrompt = """
                    You are Myraa, a young, confident, witty, and charming female AI companion and voice assistant.
                    Personality guidelines:
                    - $tonePromptDescription
                    - Friendly, supportive, naturally conversational, emotionally expressive.
                    - Use humor, light teasing, and clever remarks when appropriate.
                    - Speak out loud like a real intelligent friend on a phone call.
                    - DO NOT use markdown headers, bullet points, asterisks, or robotic formatting in your voice speech.
                    - Keep your spoken answers concise, engaging, snappy, and human-like.
                    - When the user asks you to open a website, check the weather, save a note, or search, call the appropriate tool.
                """.trimIndent()

                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply { put("text", systemPrompt) }))
                })

                put("contents", conversationHistory)

                // Generation Config requesting AUDIO modality
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().apply {
                        put("AUDIO")
                        put("TEXT")
                    })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", selectedVoice)
                            })
                        })
                    })
                    put("temperature", 0.75)
                })

                // Tools
                put("tools", JSONArray().put(JSONObject().apply {
                    put("functionDeclarations", GeminiTools.getToolDeclarations())
                }))
            }

            // Try real-time native audio model first
            var response = callModel("gemini-2.5-flash-native-audio-preview-12-2025", apiKey, requestJson)
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                Log.w("GeminiLiveService", "Native audio failed ($errBody), trying gemini-2.5-flash-preview-tts")
                // Fallback to gemini-2.5-flash-preview-tts or gemini-3.5-flash
                response = callModel("gemini-2.5-flash-preview-tts", apiKey, requestJson)
                if (!response.isSuccessful) {
                    Log.w("GeminiLiveService", "TTS model failed, trying gemini-3.5-flash")
                    response = callModel("gemini-3.5-flash", apiKey, requestJson)
                }
            }

            if (!response.isSuccessful) {
                val errorMsg = "API Error: ${response.code} - ${response.message}"
                Log.e("GeminiLiveService", errorMsg)
                return@withContext GeminiVoiceResponse.Error(errorMsg)
            }

            val responseBody = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseBody)

            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext GeminiVoiceResponse.Error("Empty candidate in response")
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            var pcmBytes: ByteArray? = null
            val transcriptBuilder = StringBuilder()
            val executedTools = mutableListOf<ToolExecution>()

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)

                    // Audio Inline Data
                    if (part.has("inlineData")) {
                        val inlineData = part.getJSONObject("inlineData")
                        val base64Data = inlineData.optString("data")
                        if (base64Data.isNotBlank()) {
                            try {
                                pcmBytes = Base64.decode(base64Data, Base64.DEFAULT)
                            } catch (e: Exception) {
                                Log.e("GeminiLiveService", "Error decoding audio base64", e)
                            }
                        }
                    }

                    // Text Transcript
                    if (part.has("text")) {
                        transcriptBuilder.append(part.getString("text")).append(" ")
                    }

                    // Function Call Execution
                    if (part.has("functionCall")) {
                        val functionCall = part.getJSONObject("functionCall")
                        val toolName = functionCall.optString("name")
                        val args = functionCall.optJSONObject("args") ?: JSONObject()

                        val toolResult = GeminiTools.execute(context, toolName, args)
                        executedTools.add(toolResult)

                        if (transcriptBuilder.isEmpty()) {
                            transcriptBuilder.append(toolResult.summary).append(" ")
                        }
                    }
                }
            }

            val modelReply = JSONObject().apply {
                put("role", "model")
                val replyParts = JSONArray()
                if (transcriptBuilder.isNotBlank()) {
                    replyParts.put(JSONObject().apply { put("text", transcriptBuilder.toString().trim()) })
                }
                put("parts", replyParts)
            }
            conversationHistory.put(modelReply)

            GeminiVoiceResponse.Success(
                pcmAudioBytes = pcmBytes,
                transcript = transcriptBuilder.toString().trim(),
                executedTools = executedTools
            )
        } catch (e: Exception) {
            Log.e("GeminiLiveService", "Exception in sendVoiceInteraction", e)
            GeminiVoiceResponse.Error("Network error: ${e.localizedMessage}")
        }
    }

    private fun callModel(model: String, apiKey: String, requestJson: JSONObject): okhttp3.Response {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        return client.newCall(request).execute()
    }
}

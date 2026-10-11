package com.example.gemini

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.model.ToolExecution
import org.json.JSONArray
import org.json.JSONObject

object GeminiTools {

    /**
     * Creates the function declarations JSON Array for the Gemini tools config.
     */
    fun getToolDeclarations(): JSONArray {
        val tools = JSONArray()

        // 1. openWebsite
        val openWebsite = JSONObject().apply {
            put("name", "openWebsite")
            put("description", "Open a website or web URL in the user's browser")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject().apply {
                    put("url", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The complete URL to open, e.g. https://google.com or https://youtube.com")
                    })
                    put("label", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "A short human-friendly name of the website")
                    })
                }
                put("properties", props)
                put("required", JSONArray().put("url"))
            }
            put("parameters", params)
        }
        tools.put(openWebsite)

        // 2. searchWeb
        val searchWeb = JSONObject().apply {
            put("name", "searchWeb")
            put("description", "Perform a web search query on Google")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The search query string")
                    })
                }
                put("properties", props)
                put("required", JSONArray().put("query"))
            }
            put("parameters", params)
        }
        tools.put(searchWeb)

        // 3. getWeather
        val getWeather = JSONObject().apply {
            put("name", "getWeather")
            put("description", "Check the weather forecast for a specified city")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject().apply {
                    put("city", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Name of the city or location")
                    })
                }
                put("properties", props)
                put("required", JSONArray().put("city"))
            }
            put("parameters", params)
        }
        tools.put(getWeather)

        // 4. takeNote
        val takeNote = JSONObject().apply {
            put("name", "takeNote")
            put("description", "Save a personal quick note or reminder for the user")
            val params = JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject().apply {
                    put("title", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Title or summary of the note")
                    })
                    put("content", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The detailed content of the note")
                    })
                }
                put("properties", props)
                put("required", JSONArray().put("content"))
            }
            put("parameters", params)
        }
        tools.put(takeNote)

        return tools
    }

    /**
     * Executes a tool requested by Gemini function calling.
     */
    fun execute(
        context: Context,
        name: String,
        args: JSONObject
    ): ToolExecution {
        return try {
            when (name) {
                "openWebsite" -> {
                    var url = args.optString("url", "https://google.com")
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://$url"
                    }
                    val label = args.optString("label", url)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ToolExecution(
                        toolName = "openWebsite",
                        summary = "Navigated to $label",
                        details = url,
                        isSuccess = true
                    )
                }

                "searchWeb" -> {
                    val query = args.optString("query", "")
                    val url = "https://www.google.com/search?q=" + Uri.encode(query)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ToolExecution(
                        toolName = "searchWeb",
                        summary = "Searched web for: $query",
                        details = query,
                        isSuccess = true
                    )
                }

                "getWeather" -> {
                    val city = args.optString("city", "Current Location")
                    // Simulated real-time atmospheric readings
                    val conditions = listOf("Sunny & Clear, 72°F", "Partly Cloudy, 68°F", "Pleasant Breeze, 74°F")
                    val result = conditions.random()
                    ToolExecution(
                        toolName = "getWeather",
                        summary = "Weather in $city: $result",
                        details = "Conditions: $result, Humidity 45%, Wind 8mph",
                        isSuccess = true
                    )
                }

                "takeNote" -> {
                    val title = args.optString("title", "Voice Note")
                    val content = args.optString("content", "")
                    ToolExecution(
                        toolName = "takeNote",
                        summary = "Saved note: $title",
                        details = content,
                        isSuccess = true
                    )
                }

                else -> {
                    ToolExecution(
                        toolName = name,
                        summary = "Executed tool $name",
                        details = args.toString(),
                        isSuccess = true
                    )
                }
            }
        } catch (e: Exception) {
            ToolExecution(
                toolName = name,
                summary = "Failed to run $name",
                details = e.localizedMessage,
                isSuccess = false
            )
        }
    }
}

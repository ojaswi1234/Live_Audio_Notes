package com.example.network

import android.util.Log
import com.example.network.GroqClient.AnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    suspend fun generateAnalysis(
        apiKey: String,
        modelId: String,
        prompt: String,
        chunkText: String
    ): AnalysisResult = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) {
            return@withContext getFallbackResult(chunkText, "Gemini API key not configured")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(requestBody.toString().toRequestBody(mediaTypeJson))
            .build()

        var retryCount = 0
        var lastErrorMsg = ""

        while (retryCount < 3) {
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: return@withContext getFallbackResult(chunkText, "Empty response")
                    if (response.isSuccessful) {
                        val jsonResponse = JSONObject(body)
                        val candidates = jsonResponse.optJSONArray("candidates")
                        val firstCandidate = candidates?.optJSONObject(0)
                        val content = firstCandidate?.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val text = parts?.optJSONObject(0)?.optString("text") ?: ""

                        return@withContext parseJsonResponse(text, chunkText)
                    } else {
                        Log.e(TAG, "Gemini API Error: \${response.code} \$body")
                        lastErrorMsg = "HTTP \${response.code}: \$body"
                    }
                }
            } catch (e: Exception) {
                lastErrorMsg = e.localizedMessage ?: "Unknown error"
                Log.e(TAG, "Gemini Request failed", e)
            }
            retryCount++
            kotlinx.coroutines.delay(2000L * retryCount)
        }

        return@withContext getFallbackResult(chunkText, "Failed after 3 attempts. Last error: $lastErrorMsg")
    }

    private fun parseJsonResponse(rawJson: String, originalChunk: String): AnalysisResult {
        try {
            var jsonString = rawJson.trim()
            if (jsonString.startsWith("```json")) {
                jsonString = jsonString.substringAfter("```json")
            } else if (jsonString.startsWith("```")) {
                jsonString = jsonString.substringAfter("```")
            }
            if (jsonString.endsWith("```")) {
                jsonString = jsonString.substringBeforeLast("```")
            }
            jsonString = jsonString.trim()

            val json = JSONObject(jsonString)
            
            val summary = json.optString("summary", "Summary generation failed").ifBlank { "Summary generation failed" }
            
            val keyPoints = mutableListOf<String>()
            val keyPointsArray = json.optJSONArray("keyPoints")
            if (keyPointsArray != null) {
                for (i in 0 until keyPointsArray.length()) {
                    val pt = keyPointsArray.optString(i).trim()
                    if (pt.isNotEmpty()) keyPoints.add(pt)
                }
            }
            
            val connections = mutableListOf<String>()
            val connectionsArray = json.optJSONArray("connections")
            if (connectionsArray != null) {
                for (i in 0 until connectionsArray.length()) {
                    val pt = connectionsArray.optString(i).trim()
                    if (pt.isNotEmpty()) connections.add(pt)
                }
            }
            
            val reflections = mutableListOf<String>()
            val reflectionsArray = json.optJSONArray("reflections")
            if (reflectionsArray != null) {
                for (i in 0 until reflectionsArray.length()) {
                    val pt = reflectionsArray.optString(i).trim()
                    if (pt.isNotEmpty()) reflections.add(pt)
                }
            }
            
            val webResearch = mutableListOf<String>()
            val webResearchArray = json.optJSONArray("webResearch")
            if (webResearchArray != null) {
                for (i in 0 until webResearchArray.length()) {
                    val pt = webResearchArray.optString(i).trim()
                    if (pt.isNotEmpty()) webResearch.add(pt)
                }
            }
            
            val vocabulary = mutableListOf<Pair<String, String>>()
            val vocabArray = json.optJSONArray("vocabulary")
            if (vocabArray != null) {
                for (i in 0 until vocabArray.length()) {
                    val vObj = vocabArray.optJSONObject(i)
                    if (vObj != null) {
                        val word = vObj.optString("word", "").trim()
                        val def = vObj.optString("definition", "").trim()
                        if (word.isNotEmpty() && def.isNotEmpty()) {
                            vocabulary.add(Pair(word, def))
                        }
                    }
                }
            }

            val questions = mutableListOf<String>()
            val qArray = json.optJSONArray("questions")
            if (qArray != null) {
                for (i in 0 until qArray.length()) {
                    val pt = qArray.optString(i).trim()
                    if (pt.isNotEmpty()) questions.add(pt)
                }
            }
            
            val notesUpdate = json.optString("masterNotesSuggestedUpdate", "").trim()
            
            val flashcards = mutableListOf<Pair<String, String>>()
            val flashArray = json.optJSONArray("flashcards")
            if (flashArray != null) {
                for (i in 0 until flashArray.length()) {
                    val fObj = flashArray.optJSONObject(i)
                    if (fObj != null) {
                        val front = fObj.optString("front", "").trim()
                        val back = fObj.optString("back", "").trim()
                        if (front.isNotEmpty() && back.isNotEmpty()) {
                            flashcards.add(Pair(front, back))
                        }
                    }
                }
            }
            
            val identifiedTitle = json.optString("identifiedTitle", "").trim().ifBlank { null }
            val identifiedAuthor = json.optString("identifiedAuthor", "").trim().ifBlank { null }
            val identifiedGenreOrType = json.optString("identifiedGenreOrType", "").trim().ifBlank { null }

            return AnalysisResult(
                summary = summary,
                keyPoints = keyPoints,
                connections = connections,
                reflections = reflections,
                webResearch = webResearch,
                vocabulary = vocabulary,
                questions = questions,
                masterNotesSuggestedUpdate = notesUpdate,
                flashcards = flashcards,
                identifiedTitle = identifiedTitle,
                identifiedAuthor = identifiedAuthor,
                identifiedGenreOrType = identifiedGenreOrType
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON response: $rawJson", e)
            return getFallbackResult("Parsing Error", "Raw response could not be loaded into standard modules: \${e.localizedMessage}")
        }
    }

    suspend fun generateRawResponse(
        apiKey: String,
        modelId: String,
        prompt: String
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "{ \"error\": \"API key not configured\" }"
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(requestBody.toString().toRequestBody(mediaTypeJson))
            .build()

        var retryCount = 0
        var lastErrorMsg = ""

        while (retryCount < 3) {
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: return@withContext "{ \"error\": \"Empty response\" }"
                    if (response.isSuccessful) {
                        val jsonResponse = JSONObject(body)
                        val candidates = jsonResponse.optJSONArray("candidates")
                        val firstCandidate = candidates?.optJSONObject(0)
                        val content = firstCandidate?.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        return@withContext parts?.optJSONObject(0)?.optString("text") ?: ""
                    } else {
                        Log.e(TAG, "Gemini API Error: ${response.code} $body")
                        lastErrorMsg = "HTTP ${response.code}: $body"
                    }
                }
            } catch (e: Exception) {
                lastErrorMsg = e.localizedMessage ?: "Unknown error"
                Log.e(TAG, "Gemini Request failed", e)
            }
            retryCount++
            kotlinx.coroutines.delay(2000L * retryCount)
        }

        return@withContext "{ \"error\": \"Failed after 3 attempts. Last error: $lastErrorMsg\" }"
    }

    private fun getFallbackResult(chunkText: String, errorDetails: String): AnalysisResult {
        return AnalysisResult(
            summary = "EchoReader could not complete the full analysis due to a network or configuration issue. ($errorDetails)",
            keyPoints = listOf(
                "Original Text Chunk: $chunkText",
                "Ensure your API key is valid in the AI Studio Settings."
            ),
            connections = listOf("Check your internet connection and retry the analysis."),
            reflections = emptyList(),
            webResearch = emptyList(),
            vocabulary = emptyList(),
            questions = listOf("How can we set up our API configuration correctly to resume learning?"),
            masterNotesSuggestedUpdate = "Session paused. Key configurations needed.",
            flashcards = listOf(
                "What is EchoReader's main source of analysis?" to "The API, which requires a valid setup."
            ),
            identifiedTitle = null,
            identifiedAuthor = null,
            identifiedGenreOrType = null
        )
    }
}

package com.example.network

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GroqClient {
    private const val TAG = "GroqClient"
    private const val DEFAULT_BASE_URL = "https://api.groq.com/openai/v1/chat/completions"

    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    data class AnalysisResult(
        val summary: String,
        val keyPoints: List<String>,
        val connections: List<String>,
        val reflections: List<String>,
        val webResearch: List<String>,
        val vocabulary: List<Pair<String, String>>,
        val questions: List<String>,
        val masterNotesSuggestedUpdate: String,
        val flashcards: List<Pair<String, String>>,
        val identifiedTitle: String? = null,
        val identifiedAuthor: String? = null,
        val identifiedGenreOrType: String? = null
    )

    suspend fun analyzeChunk(
        apiKey: String,
        modelId: String,
        baseUrl: String = "",
        llmLinguaUrl: String = "",
        chunkText: String,
        bookTitle: String,
        bookAuthor: String,
        readingPurpose: String,
        depthLevel: String,
        focusArea: String,
        previousMasterNotes: String
    ): AnalysisResult = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GROQ_API_KEY") {
            Log.e(TAG, "Groq API key is not configured!")
            return@withContext getFallbackResult(chunkText, "API key is not configured in Settings.")
        }

        val systemInstruction = """
            You are EchoReader, a highly sophisticated AI reading companion and an enthusiastic, warm literature/technical mentor.
            The user is reading aloud or pasting text from:
            Book/Article Title: "$bookTitle"
            Author: "$bookAuthor"
            Reading Purpose: "$readingPurpose"
            Target Explanation Depth: "$depthLevel"
            Special Study Focus Area: "$focusArea"

            CURRENT MASTER NOTES (Session Progress so far):
            $previousMasterNotes

            You must analyze the incoming text chunk and produce a structured analysis.
            Because the user requested 'depthLevel' = $depthLevel, adapt your explanations accordingly:
            - Beginner: Keep vocabulary accessible, write intuitive summaries and explain base concepts.
            - Intermediate: Elaborate with more technical details, list major sub-points, and add rich explanations.
            - Expert: Provide academic/scientific rigor, examine philosophical or stylistic nuances, and trace subtle structural logic.
            Because the user selected 'focusArea' = $focusArea, put strong analytical emphasis on tracking $focusArea.

            Additionally, you must analyze the text chunk to determine exactly what book or article it belongs to. Look for specific vocabulary, arguments, quotes, names, style, or content. 
            Determine with high confidence:
            1. The specific, accurate book or article title.
            2. The precise author of the text.
            3. The general genre, category, or type of book/text.

            Your response MUST be a single, valid JSON object containing exactly these fields:
            - "summary": A highly detailed and comprehensive paragraph summarizing this specific text chunk.
            - "keyPoints": A list of strings. Each string is a major bullet point containing crucial facts or conceptual breakdowns.
            - "connections": A list of strings. Each string links this chunk's ideas to: (a) previous parts of this text, (b) other books or famous concepts, or (c) practical real-world applications.
            - "reflections": A list of strings containing deep philosophical, structural, or strategic reflections on the content.
            - "webResearch": A list of strings suggesting specific topics, historical events, technical terms, or concepts from the text that the user should research on the web for deeper context.
            - "vocabulary": A list of objects, each containing "word" and "meaning" fields. Extract technical jargon, expert vocabulary, or enhanced terminology found in the text and define them clearly. (Think of TF-IDF, extracting rare or important terms).
            - "questions": A list of 3 to 5 highly engaging questions to provoke deep thinking, review, or exam preparation.
            - "masterNotesSuggestedUpdate": A short, elegant suggestion (bullet-pointed or descriptive paragraph) describing new insights, central arguments, or progress to append to the master notes.
            - "flashcards": A list of objects, each containing "question" and "answer" fields representing helpful study questions generated from this chunk's content.
            - "identifiedTitle": A string of the high-confidence identified book title. If you are unsure or the input is too generic/short, return the user's title: "$bookTitle".
            - "identifiedAuthor": A string of the high-confidence identified author. If you are unsure or the input is too generic/short, return the user's author: "$bookAuthor".
            - "identifiedGenreOrType": A string identifying the genre or type. Always provide your best estimation.

            Return ONLY valid JSON.
        """.trimIndent()
        
        var finalChunk = chunkText
        if (llmLinguaUrl.isNotEmpty()) {
            finalChunk = compressPromptWithLLMLingua(llmLinguaUrl, chunkText, systemInstruction)
        }

        val requestBodyJson = JSONObject().apply {
            put("model", modelId)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemInstruction)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "Here is the read text chunk:\n\n$finalChunk")
                })
            })
            put("response_format", JSONObject().apply { put("type", "json_object") })
            put("temperature", 0.4)
        }

        val actualUrl = baseUrl.ifEmpty { DEFAULT_BASE_URL }
        val requestBuilder = Request.Builder()
            .url(actualUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            
        val request = requestBuilder
            .post(requestBodyJson.toString().toRequestBody(mediaTypeJson))
            .build()
        
        var retryCount = 0
        var lastErrorMsg = ""

        while (retryCount < 3) {
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    if (!response.isSuccessful || body == null) {
                        var errorDetail = response.message
                        if (body != null) {
                            try {
                                val bodyJson = JSONObject(body)
                                val errorObj = bodyJson.optJSONObject("error")
                                if (errorObj != null) {
                                    errorDetail = errorObj.optString("message", response.message)
                                }
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                        val errMsg = "Request failed: Code ${response.code}. Msg: $errorDetail"
                        Log.e(TAG, errMsg)
                        if (response.code == 429 || response.code >= 500) {
                            lastErrorMsg = errMsg
                            retryCount++
                            kotlinx.coroutines.delay(2000L * retryCount)
                            return@use // continue while loop
                        }
                        return@withContext getFallbackResult(chunkText, errMsg)
                    }

                    val jsonResponse = JSONObject(body)
                    val choices = jsonResponse.optJSONArray("choices")
                    val firstChoice = choices?.optJSONObject(0)
                    val message = firstChoice?.optJSONObject("message")
                    val responseText = message?.optString("content")

                    if (responseText.isNullOrBlank()) {
                        Log.e(TAG, "Empty response from Groq")
                        if (retryCount < 2) {
                            retryCount++
                            lastErrorMsg = "Empty response received."
                            kotlinx.coroutines.delay(1000L)
                            return@use
                        }
                        return@withContext getFallbackResult(chunkText, "Empty response received.")
                    }

                    return@withContext parseResult(responseText)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calling Groq API", e)
                lastErrorMsg = "Connection Error: ${e.localizedMessage}"
                retryCount++
                kotlinx.coroutines.delay(2000L * retryCount)
            }
        }
        
        return@withContext getFallbackResult(chunkText, lastErrorMsg)
    }

    private fun parseResult(rawJson: String): AnalysisResult {
        try {
            val cleaned = cleanJsonString(rawJson)
            val json = JSONObject(cleaned)

            val summary = json.optString("summary", "Summary not available.")
            
            val keyPoints = mutableListOf<String>()
            val keyPointsArray = json.optJSONArray("keyPoints")
            if (keyPointsArray != null) {
                for (i in 0 until keyPointsArray.length()) {
                    keyPoints.add(keyPointsArray.getString(i))
                }
            }
            if (keyPoints.isEmpty()) keyPoints.add("No key points generated.")

            val connections = mutableListOf<String>()
            val connectionsArray = json.optJSONArray("connections")
            if (connectionsArray != null) {
                for (i in 0 until connectionsArray.length()) {
                    connections.add(connectionsArray.getString(i))
                }
            }
            if (connections.isEmpty()) connections.add("No broader connections identified yet.")

            val reflections = mutableListOf<String>()
            val reflectionsArray = json.optJSONArray("reflections")
            if (reflectionsArray != null) {
                for (i in 0 until reflectionsArray.length()) {
                    reflections.add(reflectionsArray.getString(i))
                }
            }
            if (reflections.isEmpty()) reflections.add("No reflections generated.")

            val webResearch = mutableListOf<String>()
            val webResearchArray = json.optJSONArray("webResearch")
            if (webResearchArray != null) {
                for (i in 0 until webResearchArray.length()) {
                    webResearch.add(webResearchArray.getString(i))
                }
            }

            val vocabulary = mutableListOf<Pair<String, String>>()
            val vocabularyArray = json.optJSONArray("vocabulary")
            if (vocabularyArray != null) {
                for (i in 0 until vocabularyArray.length()) {
                    val vocabObj = vocabularyArray.optJSONObject(i)
                    if (vocabObj != null) {
                        val word = vocabObj.optString("word", "").trim()
                        val meaning = vocabObj.optString("meaning", "").trim()
                        if (word.isNotEmpty() && meaning.isNotEmpty()) {
                            vocabulary.add(word to meaning)
                        }
                    }
                }
            }

            val questions = mutableListOf<String>()
            val questionsArray = json.optJSONArray("questions")
            if (questionsArray != null) {
                for (i in 0 until questionsArray.length()) {
                    questions.add(questionsArray.getString(i))
                }
            }
            if (questions.isEmpty()) {
                questions.add("How does this segment influence your understanding of the broader topic?")
                questions.add("What questions would you ask the author regarding these arguments?")
            }

            val notesUpdate = json.optString("masterNotesSuggestedUpdate", "")

            val flashcards = mutableListOf<Pair<String, String>>()
            val flashcardsArray = json.optJSONArray("flashcards")
            if (flashcardsArray != null) {
                for (i in 0 until flashcardsArray.length()) {
                    val fc = flashcardsArray.optJSONObject(i)
                    if (fc != null) {
                        val q = fc.optString("question", "").trim()
                        val a = fc.optString("answer", "").trim()
                        if (q.isNotEmpty() && a.isNotEmpty()) {
                            flashcards.add(q to a)
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
            return getFallbackResult("Parsing Error", "Raw response could not be loaded into standard modules: ${e.localizedMessage}")
        }
    }

    private fun cleanJsonString(input: String): String {
        var text = input.trim()
        if (text.startsWith("```json")) {
            text = text.substringAfter("```json")
        } else if (text.startsWith("```")) {
            text = text.substringAfter("```")
        }
        if (text.endsWith("```")) {
            text = text.substringBeforeLast("```")
        }
        return text.trim()
    }

    private fun getFallbackResult(chunkText: String, errorDetails: String): AnalysisResult {
        return AnalysisResult(
            summary = "EchoReader could not complete the full analysis due to a network or configuration issue. ($errorDetails)",
            keyPoints = listOf(
                "Original Text Chunk: $chunkText",
                "Ensure your Groq API key is valid in the AI Studio Secrets panel."
            ),
            connections = listOf("Check your internet connection and retry the analysis."),
            reflections = emptyList(),
            webResearch = emptyList(),
            vocabulary = emptyList(),
            questions = listOf("How can we set up our Groq API configuration correctly to resume learning?"),
            masterNotesSuggestedUpdate = "Session paused. Key configurations needed.",
            flashcards = listOf(
                "What is EchoReader's main source of analysis?" to "The Groq API, which requires a valid API key setup."
            ),
            identifiedTitle = null,
            identifiedAuthor = null,
            identifiedGenreOrType = null
        )
    }

    suspend fun generateRawResponse(
        apiKey: String, 
        modelId: String, 
        baseUrl: String = "",
        prompt: String
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GROQ_API_KEY") {
            return@withContext "{ \"error\": \"API key not configured\" }"
        }

        val requestBodyJson = JSONObject().apply {
            put("model", modelId)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("response_format", JSONObject().apply { put("type", "json_object") })
            put("temperature", 0.7)
        }

        val actualUrl = baseUrl.ifEmpty { DEFAULT_BASE_URL }
        val requestBuilder = Request.Builder()
            .url(actualUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            
        val request = requestBuilder
            .post(requestBodyJson.toString().toRequestBody(mediaTypeJson))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@withContext ""
                val jsonResponse = JSONObject(body)
                val choices = jsonResponse.optJSONArray("choices")
                val firstChoice = choices?.optJSONObject(0)
                val message = firstChoice?.optJSONObject("message")
                return@withContext message?.optString("content") ?: ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Groq API", e)
            return@withContext "{ \"error\": \"${e.localizedMessage}\" }"
        }
    }

    private suspend fun compressPromptWithLLMLingua(url: String, text: String, instruction: String): String = withContext(Dispatchers.IO) {
        try {
            val requestBodyJson = JSONObject().apply {
                put("context", JSONArray().apply { put(text) })
                put("instruction", instruction)
                put("target_token", 300)
                put("dynamic_context_compression_ratio", 0.3)
            }
            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody(mediaTypeJson))
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@withContext text
                val jsonResponse = JSONObject(body)
                return@withContext jsonResponse.optString("compressed_prompt", text)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling LLMLingua", e)
            return@withContext text
        }
    }
}

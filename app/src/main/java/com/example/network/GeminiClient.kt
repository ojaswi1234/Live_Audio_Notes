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

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    data class AnalysisResult(
        val summary: String,
        val keyPoints: List<String>,
        val connections: List<String>,
        val questions: List<String>,
        val masterNotesSuggestedUpdate: String,
        val flashcards: List<Pair<String, String>>
    )

    suspend fun analyzeChunk(
        chunkText: String,
        bookTitle: String,
        bookAuthor: String,
        readingPurpose: String,
        depthLevel: String,
        focusArea: String,
        previousMasterNotes: String
    ): AnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured!")
            return@withContext getFallbackResult(chunkText, "API key is not configured in Secrets panel.")
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

            Your response MUST be a single, valid JSON object containing exactly these fields:
            1. "summary": A crisp 2-4 sentence overview of this specific text chunk.
            2. "keyPoints": A list of strings. Each string is a major bullet point containing crucial facts, vocabulary definitions, or conceptual breakdowns.
            3. "connections": A list of strings. Each string links this chunk's ideas to: (a) previous parts of this text, (b) other books or famous concepts, or (c) practical real-world applications.
            4. "questions": A list of 3 to 5 highly engaging questions to provoke deep thinking, review, or exam preparation.
            5. "masterNotesSuggestedUpdate": A short, elegant suggestion (bullet-pointed or descriptive paragraph) describing new insights, central arguments, or progress to append to the master notes.
            6. "flashcards": A list of objects, each containing "question" and "answer" fields representing helpful study questions generated from this chunk's content.

            JSON Schema required:
            {
              "summary": "string",
              "keyPoints": ["string"],
              "connections": ["string"],
              "questions": ["string"],
              "masterNotesSuggestedUpdate": "string",
              "flashcards": [
                { "question": "string", "answer": "string" }
              ]
            }

            Do not wrap the response in markdown blocks other than a plain JSON format, and do not include any text before or after the JSON.
        """.trimIndent()

        val requestBodyJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "Here is the read text chunk:\n\n$chunkText")
                        })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemInstruction)
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.4)
            })
        }

        val request = Request.Builder()
            .url("$BASE_URL?key=$apiKey")
            .post(requestBodyJson.toString().toRequestBody(mediaTypeJson))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) {
                    val errMsg = "Request failed: Code ${response.code}. Msg: ${response.message}"
                    Log.e(TAG, errMsg)
                    return@withContext getFallbackResult(chunkText, errMsg)
                }

                val jsonResponse = JSONObject(body)
                val candidates = jsonResponse.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val firstPart = parts?.optJSONObject(0)
                val responseText = firstPart?.optString("text")

                if (responseText.isNullOrBlank()) {
                    Log.e(TAG, "Empty response from Gemini")
                    return@withContext getFallbackResult(chunkText, "Empty response received.")
                }

                return@withContext parseResult(responseText)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API", e)
            return@withContext getFallbackResult(chunkText, "Connection Error: ${e.localizedMessage}")
        }
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

            return AnalysisResult(
                summary = summary,
                keyPoints = keyPoints,
                connections = connections,
                questions = questions,
                masterNotesSuggestedUpdate = notesUpdate,
                flashcards = flashcards
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
                "Ensure your Gemini API key is valid in the AI Studio Secrets panel."
            ),
            connections = listOf("Check your internet connection and retry the analysis."),
            questions = listOf("How can we set up our Gemini API configuration correctly to resume learning?"),
            masterNotesSuggestedUpdate = "Session paused. Key configurations needed.",
            flashcards = listOf(
                "What is EchoReader's main source of analysis?" to "The Gemini API, which requires a valid API key setup."
            )
        )
    }
}

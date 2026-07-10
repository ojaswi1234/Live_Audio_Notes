package com.example.network

import android.content.Context
import android.os.PowerManager
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class LocalGemmaClient(private val context: Context) {
    private val TAG = "LocalGemmaClient"
    
    val modelFileName = "gemma_3_1b_cpu.bin"
    val modelFile: File
        get() = File(context.filesDir, modelFileName)
    
    private var llmInference: LlmInference? = null
    var isModelLoaded = false
        private set

    // We keep a flag to immediately halt processing if emergency stop is triggered.
    @Volatile
    var emergencyStopRequested = false

    fun isModelDownloaded(): Boolean {
        return modelFile.exists() && modelFile.length() > 1000 // Simple check to avoid empty files
    }

    suspend fun downloadModel(
        urlStr: String,
        onProgress: (Float) -> Unit,
        onStatus: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isModelDownloaded()) {
                onStatus("Model already installed.")
                onProgress(1f)
                return@withContext true
            }

            // Strategy: Check disk space (e.g. require at least 2GB free)
            val stat = android.os.StatFs(context.filesDir.path)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            val requiredBytes = 2L * 1024 * 1024 * 1024 // 2GB
            
            if (availableBytes < requiredBytes) {
                onStatus("Insufficient disk space. Need at least 2GB for Gemma 3.")
                return@withContext false
            }

            onStatus("Starting model download...")
            
            val url = java.net.URL(urlStr)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 60000
            connection.connect()

            if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) {
                onStatus("Server error: ${connection.responseCode}")
                return@withContext false
            }

            val fileLength = connection.contentLength
            val input: java.io.InputStream = connection.inputStream
            val tempFile = java.io.File(context.filesDir, "$modelFileName.tmp")
            val output = java.io.FileOutputStream(tempFile)

            val data = ByteArray(8192)
            var total: Long = 0
            var count: Int

            while (input.read(data).also { count = it } != -1) {
                if (emergencyStopRequested) {
                    output.close()
                    input.close()
                    tempFile.delete()
                    onStatus("Download aborted by emergency stop.")
                    return@withContext false
                }
                total += count.toLong()
                if (fileLength > 0) {
                    val progress = total.toFloat() / fileLength.toFloat()
                    onProgress(progress)
                    onStatus("Downloading... ${(progress * 100).toInt()}%")
                }
                output.write(data, 0, count)
            }

            output.flush()
            output.close()
            input.close()

            tempFile.renameTo(modelFile)
            onStatus("Download complete.")
            return@withContext true
        } catch (e: Exception) {
            onStatus("Error downloading model: ${e.message}")
            return@withContext false
        }
    }

    suspend fun loadModel() = withContext(Dispatchers.IO) {
        val modelPath = modelFile.absolutePath
        Log.d(TAG, "Attempting to load model from path: $modelPath")
        try {
            if (!modelFile.exists()) {
                Log.w(TAG, "Model file not found at $modelPath. In a real environment, you must push the Gemma 3 .bin file to the device.")
                return@withContext
            }
            
            Log.d(TAG, "Model file exists. Size: ${modelFile.length()} bytes")

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .build()

            Log.d(TAG, "Creating LlmInference with options...")
            llmInference = LlmInference.createFromOptions(context, options)
            isModelLoaded = true
            Log.i(TAG, "Gemma model loaded successfully from $modelPath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load Gemma model. This is expected if the file is a dummy file in the emulator.", e)
            llmInference = null // Ensure it triggers fallback mock logic
            isModelLoaded = false
        }
    }

    suspend fun analyzeChunkLocal(
        chunkText: String,
        bookTitle: String,
        bookAuthor: String,
        focusArea: String
    ): GeminiClient.AnalysisResult = withContext(Dispatchers.IO) {
        if (emergencyStopRequested) {
            throw Exception("Processing halted due to Emergency Stop.")
        }
        
        // Strategy to prevent overheating: check thermal status
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val thermalStatus = powerManager.currentThermalStatus
            if (thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE) {
                throw Exception("Device is overheating (Thermal Status: $thermalStatus). Emergency stop triggered automatically to protect hardware.")
            }
        }

        if (llmInference == null) {
            throw Exception("Offline AI model is not loaded or failed to initialize.")
        }

        // Construct a prompt specifically for Gemma to output JSON
        val prompt = """
            You are an expert reading assistant. Analyze this text: "$chunkText"
            The user is reading $bookTitle by $bookAuthor. Focus on: $focusArea.
            
            Return a JSON object with the following fields:
            - summary (string): Must be fully formatted in Markdown. Use large bold text for headings (e.g., `## Heading` or `### Heading`), bullet points using the dot symbol (`- ` or `* `), and bold text (`**keyword**`) for keywords and important concepts.
            - keyPoints (list of strings)
            - connections (list of strings)
            - questions (list of strings)
            - masterNotesSuggestedUpdate (string)
            - flashcards (list of objects with 'question' and 'answer')
            - identifiedTitle (string)
            - identifiedAuthor (string)
            - identifiedGenreOrType (string)
            
            JSON:
        """.trimIndent()

        // Generate response synchronously in the IO dispatcher.
        // If we wanted to prevent CPU hogging, we could use generateResponseAsync and add delays.
        // However, generateResponse is blocking and runs C++ inference. We can't yield the thread easily,
        // but we can rely on MediaPipe's internal threading.
        
        try {
            val response = llmInference!!.generateResponse(prompt)
            
            // If the user presses emergency stop mid-generation, we check here
            if (emergencyStopRequested) {
                throw Exception("Processing halted due to Emergency Stop.")
            }

            parseGemmaResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response", e)
            throw e
        }
    }

    private fun parseGemmaResponse(rawResponse: String): GeminiClient.AnalysisResult {
        // Strip markdown blocks and robustly extract the core JSON object block to prevent parsing crashes
        val jsonStart = rawResponse.indexOf("{")
        val jsonEnd = rawResponse.lastIndexOf("}")
        val cleanResponse = if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            rawResponse.substring(jsonStart, jsonEnd + 1)
        } else {
            rawResponse.replace("```json", "").replace("```", "").trim()
        }
        val json = JSONObject(cleanResponse)
        
        val summary = json.optString("summary", "N/A")
        
        val keyPointsJson = json.optJSONArray("keyPoints")
        val keyPoints = mutableListOf<String>()
        if (keyPointsJson != null) {
            for (i in 0 until keyPointsJson.length()) {
                keyPoints.add(keyPointsJson.optString(i))
            }
        }

        val connectionsJson = json.optJSONArray("connections")
        val connections = mutableListOf<String>()
        if (connectionsJson != null) {
            for (i in 0 until connectionsJson.length()) {
                connections.add(connectionsJson.optString(i))
            }
        }

        val questionsJson = json.optJSONArray("questions")
        val questions = mutableListOf<String>()
        if (questionsJson != null) {
            for (i in 0 until questionsJson.length()) {
                questions.add(questionsJson.optString(i))
            }
        }

        val notesUpdate = json.optString("masterNotesSuggestedUpdate", "")
        
        val flashcardsJson = json.optJSONArray("flashcards")
        val flashcards = mutableListOf<Pair<String, String>>()
        if (flashcardsJson != null) {
            for (i in 0 until flashcardsJson.length()) {
                val cardObj = flashcardsJson.optJSONObject(i)
                if (cardObj != null) {
                    flashcards.add(cardObj.optString("question", "") to cardObj.optString("answer", ""))
                }
            }
        }
        
        return GeminiClient.AnalysisResult(
            summary = summary,
            keyPoints = keyPoints,
            connections = connections,
            questions = questions,
            masterNotesSuggestedUpdate = notesUpdate,
            flashcards = flashcards,
            identifiedTitle = json.optString("identifiedTitle", ""),
            identifiedAuthor = json.optString("identifiedAuthor", ""),
            identifiedGenreOrType = json.optString("identifiedGenreOrType", "")
        )
    }

    suspend fun generateRawResponse(prompt: String): String = withContext(Dispatchers.IO) {
        val llmInference = llmInference
        if (llmInference == null) {
            throw Exception("Offline AI model is not loaded.")
        }
        
        var fullResponse = ""
        try {
            fullResponse = llmInference.generateResponse(prompt)
            if (emergencyStopRequested) {
                throw Exception("Generation cancelled due to emergency stop.")
            }
        } catch (e: Exception) {
            if (e.message?.contains("emergency stop") == true) throw e
            Log.e(TAG, "Inference error", e)
            throw Exception("Local AI Inference failed: ${e.message}")
        }

        val jsonStart = fullResponse.indexOf("{")
        val jsonEnd = fullResponse.lastIndexOf("}")
        
        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            return@withContext fullResponse.substring(jsonStart, jsonEnd + 1)
        }
        
        return@withContext "{}"
    }

    fun close() {
        try {
            llmInference?.close()
            llmInference = null
            isModelLoaded = false
            Log.i(TAG, "Gemma model inference closed and resources released.")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing Gemma model inference", e)
        }
    }
}

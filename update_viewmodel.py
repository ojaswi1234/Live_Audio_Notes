import re

with open("app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt", "r") as f:
    content = f.read()

# Replace properties
properties_replacement = """    private val prefs = application.getSharedPreferences("api_prefs", android.content.Context.MODE_PRIVATE)

    var userApiKey by mutableStateOf(prefs.getString("groq_api_key", "") ?: "")
        private set
        
    var userSelectedModel by mutableStateOf(prefs.getString("groq_model", "llama-3.1-8b-instant") ?: "llama-3.1-8b-instant")
        private set

    var userGeminiApiKey by mutableStateOf(prefs.getString("gemini_api_key", "") ?: "")
        private set

    fun saveModel(model: String) {
        userSelectedModel = model
        prefs.edit().putString("groq_model", model).apply()
    }

    fun saveApiKey(key: String) {
        userApiKey = key
        prefs.edit().putString("groq_api_key", key).apply()
    }

    fun deleteApiKey() {
        userApiKey = ""
        prefs.edit().remove("groq_api_key").apply()
    }

    fun saveGeminiApiKey(key: String) {
        userGeminiApiKey = key
        prefs.edit().putString("gemini_api_key", key).apply()
    }

    fun deleteGeminiApiKey() {
        userGeminiApiKey = ""
        prefs.edit().remove("gemini_api_key").apply()
    }

    val currentApiKey: String
        get() = userApiKey.takeIf { it.isNotEmpty() } ?: com.example.BuildConfig.GROQ_API_KEY
        
    val currentGeminiApiKey: String
        get() = userGeminiApiKey.takeIf { it.isNotEmpty() } ?: com.example.BuildConfig.GEMINI_API_KEY"""

content = re.sub(r'    private val prefs = application\.getSharedPreferences\("api_prefs", android\.content\.Context\.MODE_PRIVATE\)[\s\S]*?val currentApiKey: String\n\s*get\(\) = userApiKey\.takeIf \{ it\.isNotEmpty\(\) \} \?: com\.example\.BuildConfig\.GROQ_API_KEY', properties_replacement, content)

# Update imports
content = content.replace("import com.example.network.GroqClient", "import com.example.network.GroqClient\nimport com.example.network.GeminiClient")

# Update analyzeChunk call
analyze_replacement = """            val result = if (userSelectedModel.startsWith("gemini")) {
                GeminiClient.generateAnalysis(
                    apiKey = currentGeminiApiKey,
                    modelId = userSelectedModel,
                    prompt = prompt,
                    chunkText = textToProcess
                )
            } else {
                GroqClient.analyzeChunk(
                    apiKey = currentApiKey,
                    modelId = userSelectedModel,
                    prompt = prompt,
                    chunkText = textToProcess
                )
            }"""

content = re.sub(r'            val result = \s*GroqClient\.analyzeChunk\(\s*apiKey = currentApiKey,\s*modelId = userSelectedModel,\s*prompt = prompt,\s*chunkText = textToProcess\s*\)', analyze_replacement, content)

# Update GroqClient.generateRawResponse (first one)
generate_raw_1_replacement = """                val resultJsonStr = if (userSelectedModel.startsWith("gemini")) {
                    GeminiClient.generateRawResponse(
                        apiKey = currentGeminiApiKey,
                        modelId = userSelectedModel,
                        prompt = evaluationPrompt
                    )
                } else {
                    GroqClient.generateRawResponse(
                        apiKey = currentApiKey, 
                        modelId = userSelectedModel, 
                        prompt = evaluationPrompt
                    )
                }"""
content = re.sub(r'                val resultJsonStr = GroqClient\.generateRawResponse\(\s*apiKey = currentApiKey, \s*modelId = userSelectedModel, \s*prompt = evaluationPrompt\s*\)', generate_raw_1_replacement, content)

# Update GroqClient.generateRawResponse (second one)
generate_raw_2_replacement = """                val response = if (groqKeyValid) {
                    if (userSelectedModel.startsWith("gemini")) {
                        GeminiClient.generateRawResponse(
                            apiKey = currentGeminiApiKey,
                            modelId = userSelectedModel,
                            prompt = userPrompt
                        )
                    } else {
                        GroqClient.generateRawResponse(
                            apiKey = currentApiKey,
                            modelId = userSelectedModel,
                            prompt = userPrompt
                        )
                    }"""
content = re.sub(r'                val response = if \(groqKeyValid\) \{\s*GroqClient\.generateRawResponse\(\s*apiKey = currentApiKey,\s*modelId = userSelectedModel,\s*prompt = userPrompt\s*\)', generate_raw_2_replacement, content)

with open("app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt", "w") as f:
    f.write(content)

import sys

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'r') as f:
    text = f.read()

# Add GeminiClient import
text = text.replace('import com.example.network.GroqClient', 'import com.example.network.GroqClient\nimport com.example.network.GeminiClient')

# Add AI Provider state
provider_code = """
    var aiProvider by mutableStateOf(prefs.getString("ai_provider", "Gemini") ?: "Gemini")
        private set

    fun saveAiProvider(provider: String) {
        aiProvider = provider
        prefs.edit().putString("ai_provider", provider).apply()
        if (provider == "Gemini") {
            saveModel("gemini-3.5-flash")
        } else {
            saveModel("llama-3.1-8b-instant")
        }
    }

    var userGeminiApiKey by mutableStateOf(prefs.getString("gemini_api_key", "") ?: "")
        private set

    fun saveGeminiApiKey(key: String) {
        userGeminiApiKey = key
        prefs.edit().putString("gemini_api_key", key).apply()
    }

    fun deleteGeminiApiKey() {
        userGeminiApiKey = ""
        prefs.edit().remove("gemini_api_key").apply()
    }

    val currentGeminiApiKey: String
        get() = userGeminiApiKey.takeIf { it.isNotEmpty() } ?: com.example.BuildConfig.GEMINI_API_KEY
"""
text = text.replace('    var userApiKey by mutableStateOf(prefs.getString("groq_api_key", "") ?: "")', provider_code + '\n    var userApiKey by mutableStateOf(prefs.getString("groq_api_key", "") ?: "")')

# Change default model
text = text.replace('var userSelectedModel by mutableStateOf(prefs.getString("selected_model", "llama-3.1-8b-instant") ?: "llama-3.1-8b-instant")', 'var userSelectedModel by mutableStateOf(prefs.getString("selected_model", "gemini-3.5-flash") ?: "gemini-3.5-flash")')

# Change generateAnalysis to branch between Gemini and Groq
old_call = """
            val result = GroqClient.generateAnalysis(
                apiKey = currentApiKey,
                modelId = userSelectedModel,
                prompt = prompt,
                chunkText = textToProcess
            )"""

new_call = """
            val result = if (aiProvider == "Gemini") {
                GeminiClient.generateAnalysis(
                    apiKey = currentGeminiApiKey,
                    modelId = userSelectedModel,
                    prompt = prompt,
                    chunkText = textToProcess
                )
            } else {
                GroqClient.generateAnalysis(
                    apiKey = currentApiKey,
                    modelId = userSelectedModel,
                    prompt = prompt,
                    chunkText = textToProcess
                )
            }"""
text = text.replace(old_call, new_call)

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'w') as f:
    f.write(text)

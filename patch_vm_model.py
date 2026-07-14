import sys

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'r') as f:
    text = f.read()

target1 = """    var userSelectedModel by mutableStateOf(prefs.getString("groq_model", "llama-3.1-8b-instant") ?: "llama-3.1-8b-instant")
        private set"""
replacement1 = """    var userSelectedModel by mutableStateOf(prefs.getString("groq_model", null) ?: if (aiProvider == "Gemini") "gemini-2.0-flash" else "llama-3.1-8b-instant")
        private set"""
text = text.replace(target1, replacement1)

target2 = """    fun saveAiProvider(provider: String) {
        aiProvider = provider
        prefs.edit().putString("ai_provider", provider).apply()
        if (provider == "Gemini") {
            saveModel("gemini-1.5-flash")
        } else {
            saveModel("llama-3.1-8b-instant")
        }
    }"""
replacement2 = """    fun saveAiProvider(provider: String) {
        aiProvider = provider
        prefs.edit().putString("ai_provider", provider).apply()
        if (provider == "Gemini") {
            saveModel("gemini-2.0-flash")
        } else {
            saveModel("llama-3.1-8b-instant")
        }
    }"""
text = text.replace(target2, replacement2)

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'w') as f:
    f.write(text)

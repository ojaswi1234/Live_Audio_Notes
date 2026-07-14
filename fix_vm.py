import re

with open("app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt", "r") as f:
    content = f.read()

replacement = """    private val prefs = application.getSharedPreferences("api_prefs", android.content.Context.MODE_PRIVATE)

    var userApiKey by mutableStateOf(prefs.getString("groq_api_key", "") ?: "")
        private set
        
    var userSelectedModel by mutableStateOf(prefs.getString("groq_model", "llama-3.1-8b-instant") ?: "llama-3.1-8b-instant")
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

    val currentApiKey: String
        get() = userApiKey.takeIf { it.isNotEmpty() } ?: com.example.BuildConfig.GROQ_API_KEY"""

content = re.sub(r'private val prefs = application\.getSharedPreferences\("api_prefs", android\.content\.Context\.MODE_PRIVATE\)[\s\S]*?val currentApiKey: String\n\s*get\(\) = userApiKey\.takeIf \{ it\.isNotEmpty\(\) \} \?: com\.example\.BuildConfig\.GROQ_API_KEY', replacement, content)

with open("app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt", "w") as f:
    f.write(content)

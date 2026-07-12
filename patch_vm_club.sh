# Remove trailing brace temporarily
sed -i '/^}$/d' app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt

cat << 'EOF_INNER' >> app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt

    // --- Book Club / Multiplayer Simulation ---
    private val _clubMessages = androidx.compose.runtime.mutableStateListOf<com.example.viewmodel.ClubMessage>()
    val clubMessages: List<com.example.viewmodel.ClubMessage> = _clubMessages
    
    var isClubTyping by mutableStateOf(false)
        private set

    fun sendClubMessage(text: String) {
        val userMsg = ClubMessage(java.util.UUID.randomUUID().toString(), "You", text, System.currentTimeMillis())
        _clubMessages.add(userMsg)
        
        isClubTyping = true
        viewModelScope.launch {
            try {
                val groqClient = com.example.network.GroqClient()
                val sessionContext = activeSession?.title ?: "a book"
                val prompt = """
                    You are simulating an AI Book Club for the book '$sessionContext'. 
                    The user just said: "$text". 
                    Respond as one of the following personas: 'The Scholar' (deep analysis), 'The Critic' (questions everything), or 'The Enthusiast' (loves the book).
                    Format your response exactly like this:
                    [Persona Name]: [Their response]
                """.trimIndent()
                
                val response = groqClient.generateRawResponse(
                    apiKey = currentApiKey,
                    modelId = userSelectedModel,
                    prompt = prompt
                )
                
                val cleanResponse = response.replace("\"", "").trim()
                val parts = cleanResponse.split(":", limit = 2)
                
                val persona = if (parts.size == 2) parts[0].trim().replace("[", "").replace("]", "") else "The Scholar"
                val replyText = if (parts.size == 2) parts[1].trim() else cleanResponse
                
                _clubMessages.add(ClubMessage(java.util.UUID.randomUUID().toString(), persona, replyText, System.currentTimeMillis()))
            } catch (e: Exception) {
                _clubMessages.add(ClubMessage(java.util.UUID.randomUUID().toString(), "System", "Failed to connect to the club. Please check your API key.", System.currentTimeMillis()))
            } finally {
                isClubTyping = false
            }
        }
    }
}
EOF_INNER

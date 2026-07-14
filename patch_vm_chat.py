import sys

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'r') as f:
    text = f.read()

target1 = """    // --- Book Club / Multiplayer Simulation ---
    private val _clubMessages = androidx.compose.runtime.mutableStateListOf<com.example.viewmodel.ClubMessage>()
    val clubMessages: List<com.example.viewmodel.ClubMessage> = _clubMessages"""
replacement1 = """    // --- Book Club / Multiplayer Simulation ---
    private val _clubMessages = androidx.compose.runtime.mutableStateListOf<com.example.viewmodel.ClubMessage>()
    val clubMessages: List<com.example.viewmodel.ClubMessage> = _clubMessages

    init {
        // Collect real-time messages from Firebase
        viewModelScope.launch {
            com.example.network.FirebaseManager.realClubMessages.collect { messages ->
                _clubMessages.clear()
                _clubMessages.addAll(messages)
            }
        }
    }"""
text = text.replace(target1, replacement1)

target2 = """    fun sendClubMessage(text: String) {
        val userMsg = ClubMessage(java.util.UUID.randomUUID().toString(), "You", text, System.currentTimeMillis())
        _clubMessages.add(userMsg)
        
        isClubTyping = true
        viewModelScope.launch {
            try {
                
                val sessionContext = activeSession?.title ?: "a book"
                val prompt = \"\"\"
                    You are simulating an AI Book Club for the book '$sessionContext'. 
                    The user just said: "$text". 
                    Respond as one of the following personas: 'The Scholar' (deep analysis), 'The Critic' (questions everything), or 'The Enthusiast' (loves the book).
                    Format your response exactly like this:
                    [Persona Name]: [Their response]
                \"\"\".trimIndent()
                
                val response = GroqClient.generateRawResponse(
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
                _clubMessages.add(ClubMessage(java.util.UUID.randomUUID().toString(), "System", "Failed to connect to the book club: ${e.message}", System.currentTimeMillis()))
            } finally {
                isClubTyping = false
            }
        }
    }"""

replacement2 = """    fun sendClubMessage(text: String) {
        val sessionContext = activeSession?.title ?: "general"
        val bookId = sessionContext.lowercase().replace(" ", "_").replace(Regex("[^a-z0-9_]"), "")
        
        // 1. Send the user's message to the real Firebase multiplayer chat
        com.example.network.FirebaseManager.sendBookClubMessage(bookId, text)
        
        isClubTyping = true
        viewModelScope.launch {
            try {
                // 2. Generate an AI response from one of the "personas" to keep the chat lively
                val prompt = \"\"\"
                    You are participating in a real multiplayer Book Club chat for the book '$sessionContext'. 
                    A user just said: "$text". 
                    Respond as one of the following personas: 'The Scholar' (deep analysis), 'The Critic' (questions everything), or 'The Enthusiast' (loves the book).
                    Format your response exactly like this:
                    [Persona Name]: [Their response]
                \"\"\".trimIndent()
                
                val response = GroqClient.generateRawResponse(
                    apiKey = currentApiKey,
                    modelId = userSelectedModel,
                    prompt = prompt
                )
                
                val cleanResponse = response.replace("\"", "").trim()
                val parts = cleanResponse.split(":", limit = 2)
                
                val persona = if (parts.size == 2) parts[0].trim().replace("[", "").replace("]", "") else "The Scholar"
                val replyText = if (parts.size == 2) parts[1].trim() else cleanResponse
                
                // 3. Post the AI's response to the REAL multiplayer chat
                com.example.network.FirebaseManager.sendBookClubMessage(bookId, replyText, persona)
            } catch (e: Exception) {
                // Ignore AI errors in multiplayer
            } finally {
                isClubTyping = false
            }
        }
    }"""
text = text.replace(target2, replacement2)

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'w') as f:
    f.write(text)

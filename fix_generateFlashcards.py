import re

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'r') as f:
    content = f.read()

# We need to find the generateMoreFlashcards function and replace it completely
start_idx = content.find('    fun generateMoreFlashcards() {')
end_idx = content.find('    fun emergencyStop() {')

if start_idx != -1 and end_idx != -1:
    new_func = """    fun generateMoreFlashcards() {
        val session = activeSession ?: return
        if (isGeneratingFlashcards) return
        isGeneratingFlashcards = true

        viewModelScope.launch {
            try {
                val sourceText = if (session.masterNotes.isNotBlank()) session.masterNotes else "Generate general study flashcards for this topic."
                
                val prompt = \"\"\"
                    You are an expert tutor. Based on the following study notes, generate 5-10 high-quality flashcards to help the user memorize key concepts.
                    
                    Study Notes:
                    $sourceText
                    
                    Return a JSON object with the following field:
                    - flashcards (list of objects with 'question' and 'answer')
                    
                    JSON:
                \"\"\".trimIndent()
                
                val resultJsonStr = GroqClient.generateRawResponse(
                    apiKey = currentApiKey, 
                    modelId = userSelectedModel, 
                    prompt = prompt
                )

                val newCards = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    val cleanResponse = resultJsonStr.replace("```json", "").replace("```", "").trim()
                    val json = org.json.JSONObject(cleanResponse)
                    val flashcardsJson = json.optJSONArray("flashcards")
                    
                    val cards = mutableListOf<com.example.data.StudyCard>()
                    if (flashcardsJson != null) {
                        for (i in 0 until flashcardsJson.length()) {
                            val cardObj = flashcardsJson.optJSONObject(i)
                            if (cardObj != null) {
                                cards.add(com.example.data.StudyCard(
                                    sessionId = session.id,
                                    front = cardObj.optString("question", ""),
                                    back = cardObj.optString("answer", ""),
                                    isMastered = false
                                ))
                            }
                        }
                    }
                    cards
                }
                
                if (newCards.isNotEmpty()) {
                    repository.addCards(newCards)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("ViewModel", "Failed to generate flashcards", e)
            } finally {
                isGeneratingFlashcards = false
            }
        }
    }

"""
    content = content[:start_idx] + new_func + content[end_idx:]
    with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'w') as f:
        f.write(content)

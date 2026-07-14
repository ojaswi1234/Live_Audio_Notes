import re

with open("app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt", "r") as f:
    content = f.read()

# Replace variables and properties related to aiProvider
content = re.sub(r'var aiProvider by mutableStateOf.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n', '', content)
content = re.sub(r'fun saveHfApiKey[\s\S]*?get\(\) = userHfApiKey\.takeIf \{ it\.isNotEmpty\(\) \} \?: com\.example\.BuildConfig\.GEMINI_API_KEY\n', '', content)
content = re.sub(r'var userHfApiKey[\s\S]*?private set\n', '', content)

content = re.sub(r'var userSelectedModel by mutableStateOf\(.*?\)[\s\S]*?private set', 'var userSelectedModel by mutableStateOf(prefs.getString("groq_model", "llama-3.1-8b-instant") ?: "llama-3.1-8b-instant")\n        private set', content)

# Remove aiProvider completely
content = re.sub(r'val result = if \(aiProvider == "HuggingFace"\) \{[\s\S]*?\} else \{', 'val result = ', content)
content = content.replace('''                GroqClient.analyzeChunk(
                    apiKey = currentApiKey,
                    modelId = userSelectedModel,
                    chunkText = textToProcess,
                    bookTitle = session.title,
                    bookAuthor = session.author,
                    readingPurpose = session.purpose,
                    explanationDepth = session.depth,
                    focusArea = session.focus,
                    masterNotes = session.masterNotes
                )
            }''', '''                GroqClient.analyzeChunk(
                    apiKey = currentApiKey,
                    modelId = userSelectedModel,
                    chunkText = textToProcess,
                    bookTitle = session.title,
                    bookAuthor = session.author,
                    readingPurpose = session.purpose,
                    explanationDepth = session.depth,
                    focusArea = session.focus,
                    masterNotes = session.masterNotes
                )''')

chat_ai_call = """                val groqKeyValid = currentApiKey.isNotEmpty() && currentApiKey != "MY_GROQ_API_KEY"
                val geminiKeyValid = currentHfApiKey.isNotEmpty() && currentHfApiKey != "MY_GEMINI_API_KEY"
                
                val useGemini = geminiKeyValid && (!groqKeyValid || kotlin.random.Random.nextBoolean())
                
                val response = if (useGemini) {
                     val geminiModel = if (userSelectedModel.contains("gemini")) userSelectedModel else "gemini-2.0-flash"
                     com.example.network.HuggingFaceClient.generateRawResponse(
                        apiKey = currentHfApiKey,
                        modelId = geminiModel,
                        prompt = prompt
                    )
                } else if (groqKeyValid) {
                     val groqModel = if (userSelectedModel.contains("gemini")) "llama-3.1-8b-instant" else userSelectedModel
                     GroqClient.generateRawResponse(
                        apiKey = currentApiKey,
                        modelId = groqModel,
                        prompt = prompt
                    )
                } else {
                     "NO_REPLY"
                }"""

groq_chat_ai_call = """                val groqKeyValid = currentApiKey.isNotEmpty() && currentApiKey != "MY_GROQ_API_KEY"
                
                val response = if (groqKeyValid) {
                     GroqClient.generateRawResponse(
                        apiKey = currentApiKey,
                        modelId = userSelectedModel,
                        prompt = prompt
                    )
                } else {
                     "NO_REPLY"
                }"""

content = content.replace(chat_ai_call, groq_chat_ai_call)

with open("app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt", "w") as f:
    f.write(content)

package com.example.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.GroqClient
import com.example.network.GeminiClient
import com.example.speech.VoiceReaderManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import org.json.JSONArray
import java.util.Locale

enum class AppScreen {
    AUTH,
    USER_SETUP,
    API_SETUP_INSTRUCTIONS,
    API_SETUP_INPUT,
    API_KEY_MANAGER,
    GOALS_SETUP,
    SESSION_DASHBOARD,
    HISTORY_LIST,
    STUDY_QUIZ,
    PROFILE_REWARDS,
    BOOK_CLUB,
    LEADERBOARD
}

class EchoReaderViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val db = AppDatabase.getDatabase(application)
    private val repository = SessionRepository(db.bookSessionDao())
    private val voiceManager = VoiceReaderManager(application)
    private var tts: TextToSpeech? = null
    val scienceFacts = listOf(
        "When you speak while reading, both your speech and listening work in sync so that you can remember the information longer (known as the production effect).",
        "Reading aloud forces you to slow down and process the material more deeply, improving overall comprehension and focus.",
        "Hearing your own voice read the text aloud creates a distinct auditory memory trace, making it easier to recall later.",
        "Combining visual and auditory processing through reading aloud engages multiple brain regions, strengthening neural pathways.",
        "Active reading and vocalization can significantly reduce mind-wandering, keeping you anchored to the text.",
        "Self-explanation and summarizing aloud helps you identify gaps in your understanding in real-time.",
        "The physical act of articulating words aloud increases engagement and enhances cognitive function.",
        "Dual-coding theory suggests that forming both visual and auditory memories of text significantly increases long-term retention."
    )

    var currentScienceFact by mutableStateOf(scienceFacts.random())
        private set

    var showScienceFactPopup by mutableStateOf(true)
        private set

    fun dismissScienceFactPopup() {
        showScienceFactPopup = false
    }

    private val prefs = application.getSharedPreferences("api_prefs", android.content.Context.MODE_PRIVATE)


    var aiProvider by mutableStateOf(prefs.getString("ai_provider", "Gemini") ?: "Gemini")
        private set

    fun saveAiProvider(provider: String) {
        aiProvider = provider
        prefs.edit().putString("ai_provider", provider).apply()
        if (provider == "Gemini") {
            saveModel("gemini-2.0-flash")
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

    var userApiKey by mutableStateOf(prefs.getString("groq_api_key", "") ?: "")
        private set
        
    var userSelectedModel by mutableStateOf(prefs.getString("groq_model", null) ?: if (aiProvider == "Gemini") "gemini-2.0-flash" else "llama-3.1-8b-instant")
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
        get() = userApiKey.takeIf { it.isNotEmpty() } ?: com.example.BuildConfig.GROQ_API_KEY

    // Navigation State
    private val _currentScreen = MutableStateFlow(
        if (com.example.network.FirebaseManager.auth.currentUser == null) AppScreen.AUTH
        else if (prefs.getString("groq_api_key", "").isNullOrEmpty() && com.example.BuildConfig.GROQ_API_KEY == "MY_GROQ_API_KEY") AppScreen.API_SETUP_INSTRUCTIONS 
        else AppScreen.HISTORY_LIST
    )
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Database Flows
    val savedSessions = repository.allSessions
    val userStats = repository.userStats
    val userAchievements = repository.achievements

    // Active Session State
    var activeSession by mutableStateOf<BookSession?>(null)
        private set

    var activeChunks = mutableStateOf<List<TextChunk>>(emptyList())
        private set

    var activeCards = mutableStateOf<List<StudyCard>>(emptyList())
        private set

    // Processing / UI States
    var transcriptionText by mutableStateOf("")
    var typedText by mutableStateOf("")
    var isListening by mutableStateOf(false)
        private set
    var isAnalyzing by mutableStateOf(false)
        private set
    var latestAnalysis by mutableStateOf<GroqClient.AnalysisResult?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
    var isTtsPlaying by mutableStateOf(false)
        private set

    // Real-time partial text from continuous listening
    var partialTranscriptionText by mutableStateOf("")

    private val chunkProcessingQueue = kotlinx.coroutines.channels.Channel<String>(kotlinx.coroutines.channels.Channel.UNLIMITED)

    // Active Tab in the Dashboard for the current chunk analysis
    var selectedAnalysisTab by mutableStateOf(0)

    init {
        tts = TextToSpeech(application, this)
        
        viewModelScope.launch {
            for (text in chunkProcessingQueue) {
                processChunkInternal(text, isAutoBatch = true)
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        // Stop speech/audio playback when changing screens
        stopListening()
        stopSpeaking()
        errorMessage = null
        
        if (screen == AppScreen.BOOK_CLUB) {
            val sessionContext = activeSession?.title ?: "general"
            val bookId = sessionContext.lowercase().replace(" ", "_").replace(Regex("[^a-z0-9_]"), "")
            com.example.network.FirebaseManager.listenToBookClub(bookId)
        }
    }

    // --- Onboarding / Setup Actions ---
    fun startNewSession(
        title: String,
        author: String,
        purpose: String,
        depth: String,
        focus: String
    ) {
        viewModelScope.launch {
            val emptyNotes = "# EchoReader Running Notes: $title by $author\n" +
                    "**Purpose**: $purpose | **Depth**: $depth | **Focus**: $focus\n\n" +
                    "## Real-time Chapter Milestones\n*Notes will accumulate here block-by-block.*"

            com.example.viewmodel.incrementSessionsCompleted(repository)

            val newSession = BookSession(
                title = title.ifBlank { "Untitled Document" },
                author = author.ifBlank { "Unknown Author" },
                purpose = purpose,
                depth = depth,
                focus = focus,
                masterNotes = emptyNotes,
                lastTopic = "Just Started"
            )

            val sessionId = repository.saveSession(newSession)
            loadSessionById(sessionId)
            navigateTo(AppScreen.SESSION_DASHBOARD)
        }
    }

    private var chunksJob: Job? = null
    private var cardsJob: Job? = null

    fun loadSessionById(sessionId: Int) {
        chunksJob?.cancel()
        cardsJob?.cancel()

        chunksJob = viewModelScope.launch {
            val session = repository.getSession(sessionId)
            if (session != null) {
                activeSession = session
                // Collect chunks in active screen
                repository.getChunks(sessionId).collect { chunks ->
                    activeChunks.value = chunks
                }
            }
        }
        cardsJob = viewModelScope.launch {
            repository.getCards(sessionId).collect { cards ->
                activeCards.value = cards
            }
        }
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (activeSession?.id == sessionId) {
                activeSession = null
                activeChunks.value = emptyList()
                activeCards.value = emptyList()
                latestAnalysis = null
            }
        }
    }

    // --- Voice Input Controls ---
    fun toggleVoiceListening() {
        if (isListening) {
            stopListening()
        } else {
            errorMessage = null
            voiceManager.startListening(object : VoiceReaderManager.Listener {
                override fun onTranscriptionUpdate(text: String, isFinal: Boolean) {
                    if (isFinal) {
                        val newText = text.trim()
                        if (newText.isNotEmpty()) {
                            transcriptionText = if (transcriptionText.isEmpty()) newText else "$transcriptionText\n\n$newText"
                            partialTranscriptionText = ""
                            viewModelScope.launch {
                                chunkProcessingQueue.send(newText)
                            }
                        }
                    } else {
                        partialTranscriptionText = text
                    }
                }

                override fun onError(errorMessage: String) {
                    this@EchoReaderViewModel.errorMessage = errorMessage
                }

                override fun onListeningStateChanged(active: Boolean) {
                    isListening = active
                }
            })
        }
    }

    fun stopListening() {
        voiceManager.stopListening()
        isListening = false
    }

    fun clearTranscription() {
        transcriptionText = ""
    }

    // --- Analysis Execution ---
    fun processCurrentChunk() {
        val typed = typedText.trim()
        val fullTranscribed = transcriptionText.trim()
        val partialTranscribed = partialTranscriptionText.trim()
        
        val combinedTranscribed = listOf(typed, fullTranscribed, partialTranscribed)
            .filter { it.isNotEmpty() }
            .joinToString("\n\n")

        if (combinedTranscribed.isNotEmpty()) {
            viewModelScope.launch {
                chunkProcessingQueue.send(combinedTranscribed)
            }
            transcriptionText = ""
            partialTranscriptionText = ""
            typedText = ""
            return
        }

        errorMessage = "Please read aloud or type text before analyzing."
    }

    private suspend fun processChunkInternal(textToProcess: String, isAutoBatch: Boolean = false) {
        val session = activeSession ?: return

        isAnalyzing = true
        errorMessage = null
        if (!isAutoBatch) {
            stopListening()
        }
        stopSpeaking()

        try {
            // Call Groq Client
            val result = GroqClient.analyzeChunk(
                apiKey = currentApiKey,
                modelId = userSelectedModel,
                
                
                chunkText = textToProcess,
                bookTitle = session.title,
                bookAuthor = session.author,
                readingPurpose = session.purpose,
                depthLevel = session.depth,
                focusArea = session.focus,
                previousMasterNotes = session.masterNotes
            )

            latestAnalysis = result
            if (!isAutoBatch) {
                selectedAnalysisTab = 0 // Switch to summary page immediately
            }

            // Save chunk to database
            val chunkNumber = activeChunks.value.size + 1
            val chunk = TextChunk(
                sessionId = session.id,
                chunkNumber = chunkNumber,
                inputText = textToProcess,
                summary = result.summary,
                keyPointsJson = JSONArray(result.keyPoints).toString(),
                connectionsJson = JSONArray(result.connections).toString(),
                questionsJson = JSONArray(result.questions).toString()
            )
            repository.addChunk(chunk)

            // Update Session Master Notes and Progress
            var newNotes = session.masterNotes
            if (result.masterNotesSuggestedUpdate.isNotBlank()) {
                newNotes += "\n\n### Segment $chunkNumber Analysis Notes\n${result.masterNotesSuggestedUpdate}"
            }

            val newTopic = if (result.summary.isNotBlank()) {
                if (result.summary.length > 60) result.summary.substring(0, 57) + "..." else result.summary
            } else {
                session.lastTopic
            }

            // Background book metadata detection and auto-update
            var updatedTitle = session.title
            var updatedAuthor = session.author
            var updatedFocus = session.focus

            val isTitlePlaceholder = session.title.equals("Untitled Document", ignoreCase = true) || session.title.isBlank()
            val isAuthorPlaceholder = session.author.equals("Unknown Author", ignoreCase = true) || session.author.isBlank()

            if (!result.identifiedTitle.isNullOrBlank() && (isTitlePlaceholder || result.identifiedTitle != session.title)) {
                updatedTitle = result.identifiedTitle
            }
            if (!result.identifiedAuthor.isNullOrBlank() && (isAuthorPlaceholder || result.identifiedAuthor != session.author)) {
                updatedAuthor = result.identifiedAuthor
            }
            if (!result.identifiedGenreOrType.isNullOrBlank() && (session.focus.isBlank() || session.focus.equals("General", ignoreCase = true) || session.focus.equals("General / Mixed", ignoreCase = true))) {
                updatedFocus = result.identifiedGenreOrType
            }

            com.example.viewmodel.awardXp(repository, 10, "Read Segment")
            val updatedSession = session.copy(
                title = updatedTitle,
                author = updatedAuthor,
                focus = updatedFocus,
                masterNotes = newNotes,
                lastTopic = newTopic,
                lastReadTime = System.currentTimeMillis()
            )
            repository.updateSession(updatedSession)
            activeSession = updatedSession

            // Add Study Cards
            val newCards = result.flashcards.map { (q, a) ->
                StudyCard(
                    sessionId = session.id,
                    front = q,
                    back = a,
                    isMastered = false
                )
            }
            if (newCards.isNotEmpty()) {
                repository.addCards(newCards)
            }

        } catch (e: Exception) {
            errorMessage = "Failed to process: ${e.localizedMessage}"
            Log.e("ViewModel", "Process error", e)
        } finally {
            isAnalyzing = false
        }
    }

    // --- Master Notes Custom Edit ---
    fun updateMasterNotes(editedNotes: String) {
        val session = activeSession ?: return
        viewModelScope.launch {
            val updated = session.copy(masterNotes = editedNotes)
            repository.updateSession(updated)
            activeSession = updated
        }
    }

    // --- Study Quiz Controls ---
    fun toggleCardMastered(card: StudyCard) {
        viewModelScope.launch {
            repository.updateCard(card.copy(isMastered = !card.isMastered))
            if (!card.isMastered) {
                com.example.viewmodel.incrementMasteredCards(repository)
                com.example.viewmodel.awardXp(repository, 20, "Mastered Card")
            }
        }
    }

    fun deleteCard(cardId: Int) {
        viewModelScope.launch {
            repository.deleteCard(cardId)
        }
    }

    // --- Text To Speech Integration ---
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        } else {
            Log.e("EchoReaderViewModel", "TTS initialization failed")
        }
    }

    fun speak(text: String, isSeparator: Boolean = false) {
        if (text.isBlank()) return
        stopSpeaking()
        isTtsPlaying = true
        
        if (isSeparator) {
            // Even lower pitch for section headers / separators
            tts?.setPitch(0.6f)
            tts?.setSpeechRate(0.85f)
        } else {
            // Less childish than default
            tts?.setPitch(0.85f)
            tts?.setSpeechRate(0.95f)
        }
        
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "EchoReaderTTS")
    }

    fun stopSpeaking() {
        tts?.stop()
        isTtsPlaying = false
    }

    var isGeneratingFlashcards by mutableStateOf(false)
        private set

    fun generateMoreFlashcards() {
        val session = activeSession ?: return
        if (isGeneratingFlashcards) return
        isGeneratingFlashcards = true

        viewModelScope.launch {
            try {
                val sourceText = if (session.masterNotes.isNotBlank()) session.masterNotes else "Generate general study flashcards for this topic."
                
                val prompt = """
                    You are an expert tutor. Based on the following study notes, generate 5-10 high-quality flashcards to help the user memorize key concepts.
                    
                    Study Notes:
                    $sourceText
                    
                    Return a JSON object with the following field:
                    - flashcards (list of objects with 'question' and 'answer')
                    
                    JSON:
                """.trimIndent()
                
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


    private val _leaderboard = kotlinx.coroutines.flow.MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val leaderboard: kotlinx.coroutines.flow.StateFlow<List<Pair<String, Int>>> = _leaderboard

    init {
        if (com.example.network.FirebaseManager.auth.currentUser != null) {
            fetchLeaderboard()
            com.example.network.FirebaseManager.getFcmToken { token ->
                android.util.Log.d("EchoReader", "FCM Token: $token")
            }
            viewModelScope.launch {
                val cloudStats = com.example.network.FirebaseManager.fetchCloudStats()
                if (cloudStats != null) {
                    val localStats = repository.userStats.firstOrNull() ?: com.example.data.UserStats()
                    val cloudXp = (cloudStats.get("xp") as? Long)?.toInt() ?: 0
                    if (cloudXp > localStats.totalXp) {
                        val newStats = localStats.copy(
                            level = (cloudStats.get("level") as? Long)?.toInt() ?: 1,
                            totalXp = cloudXp,
                            currentStreak = (cloudStats.get("currentStreak") as? Long)?.toInt() ?: 0,
                            longestStreak = (cloudStats.get("longestStreak") as? Long)?.toInt() ?: 0,
                            flashcardsMastered = (cloudStats.get("flashcardsMastered") as? Long)?.toInt() ?: 0,
                            sessionsCompleted = (cloudStats.get("sessionsCompleted") as? Long)?.toInt() ?: 0
                        )
                        repository.saveUserStats(newStats)
                    }
                }
            }
        }
    }
    
    fun fetchLeaderboard() {
        viewModelScope.launch {
            // Fetch from real Firebase database
            val fbBoard = com.example.network.FirebaseManager.getGlobalLeaderboard()
            _leaderboard.value = fbBoard
        }
    }
    
    fun publishStatsToLeaderboard() {
        viewModelScope.launch {
            val stats = repository.userStats.firstOrNull()
            if (stats != null) {
                // Publish to real Firebase database
                com.example.network.FirebaseManager.syncUserStats(
                    level = stats.level, 
                    xp = stats.totalXp,
                    streak = stats.currentStreak,
                    longestStreak = stats.longestStreak,
                    flashcards = stats.flashcardsMastered,
                    sessions = stats.sessionsCompleted
                )
                fetchLeaderboard()
            }
        }
    }

    fun emergencyStop() {
        // Stop audio reading and recording
        stopListening()
        stopSpeaking()
        
        isAnalyzing = false
        
        errorMessage = "EMERGENCY STOP TRIGGERED: Hardware protection mode engaged."
    }

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
        voiceManager.stopListening()
    }



    // --- Book Club / Multiplayer Simulation ---
    private val _clubMessages = androidx.compose.runtime.mutableStateListOf<com.example.viewmodel.ClubMessage>()
    val clubMessages: List<com.example.viewmodel.ClubMessage> = _clubMessages
    
    private val _aiClubMessages = mutableListOf<com.example.viewmodel.ClubMessage>()
    private var _latestFirebaseMessages: List<com.example.viewmodel.ClubMessage> = emptyList()

    private fun updateCombinedMessages() {
        val combined = (_latestFirebaseMessages + _aiClubMessages).sortedBy { it.timestamp }
        _clubMessages.clear()
        _clubMessages.addAll(combined)
    }

    init {
        // Collect real-time messages from Firebase
        viewModelScope.launch {
            com.example.network.FirebaseManager.realClubMessages.collect { messages ->
                _latestFirebaseMessages = messages
                updateCombinedMessages()
            }
        }
    }
    
    var isClubTyping by mutableStateOf(false)
        private set

    var chatBlockedUntil by mutableStateOf(0L)
        private set

    fun sendClubMessage(text: String) {
        if (System.currentTimeMillis() < chatBlockedUntil) {
            val remainingMins = ((chatBlockedUntil - System.currentTimeMillis()) / 60000) + 1
            val aiMsg = ClubMessage(java.util.UUID.randomUUID().toString(), "System", "You are blocked from chatting for $remainingMins more minute(s) due to abusive behavior.", System.currentTimeMillis())
            _aiClubMessages.add(aiMsg)
            updateCombinedMessages()
            return
        }

        val sessionContext = activeSession?.title ?: "general"
        val bookId = sessionContext.lowercase().replace(" ", "_").replace(Regex("[^a-z0-9_]"), "")
        
        isClubTyping = true
        viewModelScope.launch {
            try {
                // 1. Abuse / Nonsense check
                val moderationPrompt = """
                    Analyze the following user message sent in a book club chat: "$text".
                    Is it:
                    1. ABUSIVE (toxic, offensive, harmful, cursing at people)
                    2. NONSENSE (gibberish, keyboard mashing, completely irrelevant token wasting)
                    3. FRUSTRATED (user is annoyed or upset but not directly abusive/toxic)
                    4. NORMAL (acceptable chat, questions, discussion)
                    Respond with EXACTLY one of these words: ABUSIVE, NONSENSE, FRUSTRATED, NORMAL.
                """.trimIndent()
                
                val groqKeyValid = currentApiKey.isNotEmpty() && currentApiKey != "MY_GROQ_API_KEY"
                val geminiKeyValid = currentGeminiApiKey.isNotEmpty() && currentGeminiApiKey != "MY_GEMINI_API_KEY"

                val useGeminiMod = geminiKeyValid && (!groqKeyValid || kotlin.random.Random.nextBoolean())
                val moderationResponse = if (useGeminiMod) {
                     com.example.network.GeminiClient.generateRawResponse(
                        apiKey = currentGeminiApiKey,
                        modelId = "gemini-1.5-flash",
                        prompt = moderationPrompt
                    )
                } else if (groqKeyValid) {
                     GroqClient.generateRawResponse(
                        apiKey = currentApiKey,
                        modelId = "llama-3.1-8b-instant",
                        prompt = moderationPrompt
                    )
                } else {
                     "NORMAL"
                }.trim().uppercase()

                if (moderationResponse.contains("ABUSIVE")) {
                    chatBlockedUntil = System.currentTimeMillis() + 5 * 60 * 1000 // Block for 5 minutes
                    val aiMsg = ClubMessage(java.util.UUID.randomUUID().toString(), "System Warning", "Abusive language detected. Your chat has been blocked for 5 minutes.", System.currentTimeMillis())
                    
                    // Show their message locally so they see why they were blocked
                    _aiClubMessages.add(ClubMessage(java.util.UUID.randomUUID().toString(), "You", text, System.currentTimeMillis() - 10))
                    _aiClubMessages.add(aiMsg)
                    updateCombinedMessages()
                    return@launch
                } else if (moderationResponse.contains("NONSENSE")) {
                    val aiMsg = ClubMessage(java.util.UUID.randomUUID().toString(), "System Warning", "Please keep the conversation meaningful and avoid spamming.", System.currentTimeMillis())
                    _aiClubMessages.add(ClubMessage(java.util.UUID.randomUUID().toString(), "You", text, System.currentTimeMillis() - 10))
                    _aiClubMessages.add(aiMsg)
                    updateCombinedMessages()
                    return@launch
                } else if (moderationResponse.contains("FRUSTRATED")) {
                    val aiMsg = ClubMessage(java.util.UUID.randomUUID().toString(), "System Warning", "We understand you're frustrated, but please try to keep the discussion constructive.", System.currentTimeMillis() + 100)
                    _aiClubMessages.add(aiMsg)
                    // Allow it to pass through to Firebase and AI, but we showed a warning
                }

                // Send user's message to Firebase
                com.example.network.FirebaseManager.sendBookClubMessage(bookId, text)
                
                // Generate context from recent clubMessages, compress to save tokens
                val recentHistory = _clubMessages.takeLast(6).joinToString("\n") { "${it.sender}: ${it.text}" }
                val compressedHistory = com.example.network.LocalPromptCompressor.compress(recentHistory)
                val compressedText = com.example.network.LocalPromptCompressor.compress(text)
                
                val prompt = """
                    You are participating in an AI Book Club for the book '$sessionContext'. 
                    Recent chat history (compressed):
                    $compressedHistory
                    
                    The user just said: "$compressedText". 
                    Respond to the discussion IF you feel it's relevant. If you feel a reply is unnecessary, output EXACTLY "NO_REPLY".
                    If you do reply, choose ONE OR MORE of the following personalities: 
                    'The Brainstormer' (creative, throws out ideas), 
                    'The Reasoner' (logical, analyzes the plot/themes deeply), 
                    'The Debater' (challenges points, plays devil's advocate).
                    
                    CRITICAL INSTRUCTIONS:
                    - Be concise to save tokens.
                    - Stick strictly to the book's context and the chat history, but you are allowed to go off-context to explain deep connections or concepts if the conversation goes deep.
                    - You can reply as a single personality, or you can have multiple personalities reply one after another in a conversation.
                    Format your response EXACTLY like this (one per line, NO markdown blocks):
                    [Persona Name]: [Their response]
                """.trimIndent()
                
                val useGemini = geminiKeyValid && (!groqKeyValid || kotlin.random.Random.nextBoolean())
                
                val response = if (useGemini) {
                     val geminiModel = if (userSelectedModel.contains("gemini")) userSelectedModel else "gemini-1.5-flash"
                     com.example.network.GeminiClient.generateRawResponse(
                        apiKey = currentGeminiApiKey,
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
                }
                
                val cleanResponse = response.replace("\"", "").trim()
                if (cleanResponse != "NO_REPLY") {
                    val lines = cleanResponse.split("\n")
                    
                    // Slightly increment timestamps so they appear in order
                    var timeOffset = 100L
                    for (line in lines) {
                        if (line.isNotBlank() && line.contains(":")) {
                            val parts = line.split(":", limit = 2)
                            val personaRaw = parts[0].trim()
                            val persona = personaRaw.replace("[", "").replace("]", "")
                            val replyText = parts[1].trim()
                            
                            val aiMsg = ClubMessage(
                                java.util.UUID.randomUUID().toString(), 
                                persona, 
                                replyText, 
                                System.currentTimeMillis() + timeOffset
                            )
                            _aiClubMessages.add(aiMsg)
                            timeOffset += 100L
                        }
                    }
                    
                    updateCombinedMessages()
                }
                
            } catch (e: Exception) {
                // Fail silently, it's just the AI not responding
            } finally {
                isClubTyping = false
            }
        }
    }

    // --- Authentication ---
    suspend fun loginWithGoogle(idToken: String): Boolean {
        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            com.example.network.FirebaseManager.auth.signInWithCredential(credential).await()
            val cloudStats = com.example.network.FirebaseManager.fetchCloudStats()
            if (cloudStats != null) {
                val localStats = repository.userStats.firstOrNull() ?: com.example.data.UserStats()
                val cloudXp = (cloudStats.get("xp") as? Long)?.toInt() ?: 0
                if (cloudXp > localStats.totalXp) {
                    val newStats = localStats.copy(
                        level = (cloudStats.get("level") as? Long)?.toInt() ?: 1,
                        totalXp = cloudXp,
                        currentStreak = (cloudStats.get("currentStreak") as? Long)?.toInt() ?: 0,
                        longestStreak = (cloudStats.get("longestStreak") as? Long)?.toInt() ?: 0,
                        flashcardsMastered = (cloudStats.get("flashcardsMastered") as? Long)?.toInt() ?: 0,
                        sessionsCompleted = (cloudStats.get("sessionsCompleted") as? Long)?.toInt() ?: 0
                    )
                    repository.saveUserStats(newStats)
                }
            }
            fetchLeaderboard()
            navigateTo(AppScreen.USER_SETUP)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun logout() {
        com.example.network.FirebaseManager.auth.signOut()
        navigateTo(AppScreen.AUTH)
    }

    fun completeUserSetup(name: String, age: String, interests: String, favouriteBooks: String, profilePicUri: String?) {
        viewModelScope.launch {
            com.example.network.FirebaseManager.saveUserProfile(name, age, interests, favouriteBooks, profilePicUri)
            navigateTo(AppScreen.HISTORY_LIST)
        }
    }

    suspend fun updateDisplayName(name: String): Boolean {
        return try {
            val user = com.example.network.FirebaseManager.auth.currentUser
            if (user != null) {
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user.updateProfile(profileUpdates).await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}

data class ClubMessage(val id: String, val sender: String, val text: String, val timestamp: Long)

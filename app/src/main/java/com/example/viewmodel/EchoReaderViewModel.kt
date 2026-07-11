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
import com.example.speech.VoiceReaderManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import org.json.JSONArray
import java.util.Locale

enum class AppScreen {
    API_SETUP_INSTRUCTIONS,
    API_SETUP_INPUT,
    API_KEY_MANAGER,
    ONBOARDING,
    GOALS_SETUP,
    SESSION_DASHBOARD,
    HISTORY_LIST,
    STUDY_QUIZ
}

class EchoReaderViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val db = AppDatabase.getDatabase(application)
    private val repository = SessionRepository(db.bookSessionDao())
    private val voiceManager = VoiceReaderManager(application)
    private var tts: TextToSpeech? = null

    private val prefs = application.getSharedPreferences("api_prefs", android.content.Context.MODE_PRIVATE)

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
        get() = userApiKey.takeIf { it.isNotEmpty() } ?: com.example.BuildConfig.GROQ_API_KEY

    // Navigation State
    private val _currentScreen = MutableStateFlow(
        if (prefs.getString("groq_api_key", "").isNullOrEmpty() && com.example.BuildConfig.GROQ_API_KEY == "MY_GROQ_API_KEY") AppScreen.API_SETUP_INSTRUCTIONS else AppScreen.ONBOARDING
    )
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Database Flows
    val savedSessions = repository.allSessions

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

            val newSession = BookSession(
                title = title.ifBlank { "Untitled Document" },
                author = author.ifBlank { "Unknown Author" },
                purpose = purpose,
                depth = depth,
                focus = focus,
                masterNotes = emptyNotes,
                progressPercent = 0f,
                currentChapter = "Chapter 1"
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
        if (typed.isNotEmpty()) {
            viewModelScope.launch {
                chunkProcessingQueue.send(typed)
            }
            typedText = ""
            return
        }

        val partial = partialTranscriptionText.trim()
        if (partial.isNotEmpty()) {
            viewModelScope.launch {
                chunkProcessingQueue.send(partial)
            }
            partialTranscriptionText = ""
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

            val progressIncrement = 5f
            val newProgress = (session.progressPercent + progressIncrement).coerceAtMost(100f)

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

            val updatedSession = session.copy(
                title = updatedTitle,
                author = updatedAuthor,
                focus = updatedFocus,
                masterNotes = newNotes,
                progressPercent = newProgress
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
                // Use master notes or recent chunks to generate flashcards
                val sourceText = if (session.masterNotes.isNotBlank()) session.masterNotes else "Generate general study flashcards for this topic."
                
                // Construct specific prompt to get only flashcards
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
                val cleanResponse = resultJsonStr.replace("```json", "").replace("```", "").trim()
                val json = org.json.JSONObject(cleanResponse)
                val flashcardsJson = json.optJSONArray("flashcards")
                
                val newCards = mutableListOf<com.example.data.StudyCard>()
                if (flashcardsJson != null) {
                    for (i in 0 until flashcardsJson.length()) {
                        val cardObj = flashcardsJson.optJSONObject(i)
                        if (cardObj != null) {
                            newCards.add(com.example.data.StudyCard(
                                sessionId = session.id,
                                front = cardObj.optString("question", ""),
                                back = cardObj.optString("answer", ""),
                                isMastered = false
                            ))
                        }
                    }
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
}

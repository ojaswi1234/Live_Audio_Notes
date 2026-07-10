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
import com.example.network.GeminiClient
import com.example.speech.VoiceReaderManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Locale

enum class AppScreen {
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

    // Navigation State
    private val _currentScreen = MutableStateFlow(AppScreen.ONBOARDING)
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
    var latestAnalysis by mutableStateOf<GeminiClient.AnalysisResult?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
    var isTtsPlaying by mutableStateOf(false)
        private set

    // Active Tab in the Dashboard for the current chunk analysis
    var selectedAnalysisTab by mutableStateOf(0)

    init {
        tts = TextToSpeech(application, this)
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

    fun loadSessionById(sessionId: Int) {
        viewModelScope.launch {
            val session = repository.getSession(sessionId)
            if (session != null) {
                activeSession = session
                // Collect chunks in active screen
                repository.getChunks(sessionId).collect { chunks ->
                    activeChunks.value = chunks
                }
            }
        }
        viewModelScope.launch {
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
                    transcriptionText = text
                }

                override fun onError(msg: String) {
                    errorMessage = msg
                    isListening = false
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
        val textToProcess = transcriptionText.ifBlank { typedText }.trim()
        if (textToProcess.isEmpty()) {
            errorMessage = "Please read aloud or type text before analyzing."
            return
        }

        val session = activeSession ?: return

        isAnalyzing = true
        errorMessage = null
        stopListening()
        stopSpeaking()

        viewModelScope.launch {
            try {
                // Call Gemini Client
                val result = GeminiClient.analyzeChunk(
                    chunkText = textToProcess,
                    bookTitle = session.title,
                    bookAuthor = session.author,
                    readingPurpose = session.purpose,
                    depthLevel = session.depth,
                    focusArea = session.focus,
                    previousMasterNotes = session.masterNotes
                )

                latestAnalysis = result
                selectedAnalysisTab = 0 // Switch to summary page immediately

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

                val updatedSession = session.copy(
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

                // Clean text inputs
                transcriptionText = ""
                typedText = ""

            } catch (e: Exception) {
                errorMessage = "Failed to process: ${e.localizedMessage}"
                Log.e("ViewModel", "Process error", e)
            } finally {
                isAnalyzing = false
            }
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
            tts?.language = Locale.getDefault()
        } else {
            Log.e("EchoReaderViewModel", "TTS initialization failed")
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        stopSpeaking()
        isTtsPlaying = true
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "EchoReaderTTS")
    }

    fun stopSpeaking() {
        tts?.stop()
        isTtsPlaying = false
    }

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
        voiceManager.stopListening()
    }
}

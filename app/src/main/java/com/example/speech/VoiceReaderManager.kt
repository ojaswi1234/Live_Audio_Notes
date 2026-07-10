package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class VoiceReaderManager(private val context: Context) {
    private val TAG = "VoiceReaderManager"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    
    var isListening = false
        private set
    private var shouldContinueListening = false
    private var currentListener: Listener? = null

    interface Listener {
        fun onTranscriptionUpdate(text: String, isFinal: Boolean)
        fun onError(errorMessage: String)
        fun onListeningStateChanged(active: Boolean)
    }

    fun startListening(listener: Listener) {
        mainHandler.removeCallbacksAndMessages(null)
        if (isListening && shouldContinueListening) return

        currentListener = listener
        shouldContinueListening = true

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onError("Speech recognition is not supported on this device. Please use the keyboard input instead.")
            return
        }

        startRecognizerInternal()
    }

    private fun startRecognizerInternal() {
        mainHandler.post {
            if (!shouldContinueListening) return@post

            try {
                // Ensure any existing recognizer is destroyed before creating a new one
                speechRecognizer?.destroy()
                speechRecognizer = null

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            mainHandler.post {
                                isListening = true
                                currentListener?.onListeningStateChanged(true)
                            }
                        }

                        override fun onBeginningOfSpeech() {}

                        override fun onRmsChanged(rmsdB: Float) {}

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {}

                        override fun onError(error: Int) {
                            val message = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check your mic."
                                SpeechRecognizer.ERROR_CLIENT -> "Speech recognition paused (client reset)."
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Audio permission is required."
                                SpeechRecognizer.ERROR_NETWORK -> "Network error. Speech recognition requires internet."
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout."
                                SpeechRecognizer.ERROR_NO_MATCH -> "Listening paused."
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy. Please try again."
                                SpeechRecognizer.ERROR_SERVER -> "Speech server error."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening paused."
                                else -> "Unknown voice recognition error (code $error)."
                            }
                            
                            val shouldRestart = error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS

                            mainHandler.post {
                                if (!shouldRestart) {
                                    Log.e(TAG, "Error code $error: $message")
                                    currentListener?.onError(message)
                                    shouldContinueListening = false
                                }
                                
                                isListening = false
                                currentListener?.onListeningStateChanged(false)
                                
                                if (shouldContinueListening) {
                                    // Introduce 500ms safety delay before recreating SpeechRecognizer on pause/error
                                    mainHandler.postDelayed({
                                        if (shouldContinueListening) {
                                            startRecognizerInternal()
                                        }
                                    }, 500)
                                }
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val transcription = matches?.firstOrNull()?.trim()

                            mainHandler.post {
                                if (!transcription.isNullOrEmpty()) {
                                    currentListener?.onTranscriptionUpdate(transcription, true)
                                }
                                isListening = false
                                currentListener?.onListeningStateChanged(false)
                                
                                if (shouldContinueListening) {
                                    // Introduce 300ms transition delay before starting next recognition session
                                    mainHandler.postDelayed({
                                        if (shouldContinueListening) {
                                            startRecognizerInternal()
                                        }
                                    }, 300)
                                }
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partialText = matches?.firstOrNull()?.trim()

                            mainHandler.post {
                                if (!partialText.isNullOrEmpty()) {
                                    currentListener?.onTranscriptionUpdate(partialText, false)
                                }
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                speechRecognizer?.startListening(intent)
                isListening = true
                currentListener?.onListeningStateChanged(true)

            } catch (e: Exception) {
                mainHandler.post {
                    isListening = false
                    currentListener?.onListeningStateChanged(false)
                    currentListener?.onError("Failed to start speech service: ${e.localizedMessage}")
                    shouldContinueListening = false
                }
            }
        }
    }

    fun stopListening() {
        shouldContinueListening = false
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping: ${e.localizedMessage}")
            } finally {
                speechRecognizer = null
                isListening = false
                currentListener?.onListeningStateChanged(false)
            }
        }
    }
}

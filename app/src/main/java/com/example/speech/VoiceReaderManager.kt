package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class VoiceReaderManager(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    interface Listener {
        fun onTranscriptionUpdate(text: String, isFinal: Boolean)
        fun onError(errorMessage: String)
        fun onListeningStateChanged(active: Boolean)
    }

    fun startListening(listener: Listener) {
        if (isListening) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onError("Speech recognition is not supported on this device. Please use the keyboard input instead.")
            return
        }

        try {
            // Must be run on main/UI thread
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        listener.onListeningStateChanged(true)
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check your mic."
                            SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Audio permission is required."
                            SpeechRecognizer.ERROR_NETWORK -> "Network error. Speech recognition requires internet."
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout."
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech matching. Try reading clearly."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy. Please try again."
                            SpeechRecognizer.ERROR_SERVER -> "Speech server error."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout. Speak a bit faster!"
                            else -> "An unknown speech recognition error occurred."
                        }
                        Log.e("VoiceReaderManager", "Error code $error: $message")
                        isListening = false
                        listener.onListeningStateChanged(false)
                        listener.onError(message)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val transcription = matches[0]
                            listener.onTranscriptionUpdate(transcription, true)
                        }
                        isListening = false
                        listener.onListeningStateChanged(false)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val partialText = matches[0]
                            listener.onTranscriptionUpdate(partialText, false)
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
            listener.onListeningStateChanged(true)

        } catch (e: Exception) {
            isListening = false
            listener.onListeningStateChanged(false)
            listener.onError("Failed to start speech service: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        if (!isListening) return
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("VoiceReaderManager", "Error stopping: ${e.localizedMessage}")
        } finally {
            speechRecognizer = null
            isListening = false
        }
    }
}

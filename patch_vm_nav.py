import sys

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'r') as f:
    text = f.read()

target = """    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        // Stop speech/audio playback when changing screens
        stopListening()
        stopSpeaking()
        errorMessage = null
    }"""
replacement = """    fun navigateTo(screen: AppScreen) {
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
    }"""
text = text.replace(target, replacement)

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'w') as f:
    f.write(text)

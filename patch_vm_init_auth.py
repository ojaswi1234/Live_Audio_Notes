import sys

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'r') as f:
    text = f.read()

target = """    // Navigation State
    private val _currentScreen = MutableStateFlow(
        if (prefs.getString("groq_api_key", "").isNullOrEmpty() && com.example.BuildConfig.GROQ_API_KEY == "MY_GROQ_API_KEY") AppScreen.API_SETUP_INSTRUCTIONS else AppScreen.ONBOARDING
    )
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()"""
replacement = """    // Navigation State
    private val _currentScreen = MutableStateFlow(
        if (com.example.network.FirebaseManager.auth.currentUser == null) AppScreen.AUTH
        else if (prefs.getString("groq_api_key", "").isNullOrEmpty() && com.example.BuildConfig.GROQ_API_KEY == "MY_GROQ_API_KEY") AppScreen.API_SETUP_INSTRUCTIONS 
        else AppScreen.ONBOARDING
    )
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()"""
text = text.replace(target, replacement)

target_init = """    init {
        com.example.network.FirebaseManager.auth.signInAnonymously()
            .addOnCompleteListener { task ->
                fetchLeaderboard()
                com.example.network.FirebaseManager.getFcmToken { token ->
                    android.util.Log.d("EchoReader", "FCM Token: $token")
                }
                viewModelScope.launch {
                    val cloudStats = com.example.network.FirebaseManager.fetchCloudStats()
                    if (cloudStats != null) {
                        val localStats = repository.userStats.firstOrNull() ?: com.example.data.UserStats()
                        val cloudXp = (cloudStats.get("xp") as? Long)?.toInt() ?: 0
                        // Only override local if cloud has more XP (e.g. reinstall)
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
    }"""
replacement_init = """    init {
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
    }"""
text = text.replace(target_init, replacement_init)

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'w') as f:
    f.write(text)

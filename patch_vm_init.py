import sys

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'r') as f:
    text = f.read()

target = """    init {
        com.example.network.FirebaseManager.auth.signInAnonymously()
            .addOnCompleteListener { task ->
                fetchLeaderboard()
            }
    }"""
replacement = """    init {
        com.example.network.FirebaseManager.auth.signInAnonymously()
            .addOnCompleteListener { task ->
                fetchLeaderboard()
                viewModelScope.launch {
                    val cloudStats = com.example.network.FirebaseManager.fetchCloudStats()
                    if (cloudStats != null) {
                        val localStats = repository.userStats.firstOrNull() ?: com.example.data.UserStats()
                        val cloudXp = (cloudStats["xp"] as? Long)?.toInt() ?: 0
                        // Only override local if cloud has more XP (e.g. reinstall)
                        if (cloudXp > localStats.totalXp) {
                            val newStats = localStats.copy(
                                level = (cloudStats["level"] as? Long)?.toInt() ?: 1,
                                totalXp = cloudXp,
                                currentStreak = (cloudStats["currentStreak"] as? Long)?.toInt() ?: 0,
                                longestStreak = (cloudStats["longestStreak"] as? Long)?.toInt() ?: 0,
                                flashcardsMastered = (cloudStats["flashcardsMastered"] as? Long)?.toInt() ?: 0,
                                sessionsCompleted = (cloudStats["sessionsCompleted"] as? Long)?.toInt() ?: 0
                            )
                            repository.saveUserStats(newStats)
                        }
                    }
                }
            }
    }"""
text = text.replace(target, replacement)

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'w') as f:
    f.write(text)

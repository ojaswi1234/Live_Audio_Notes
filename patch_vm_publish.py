import sys

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'r') as f:
    text = f.read()

target = """    fun publishStatsToLeaderboard() {
        viewModelScope.launch {
            val stats = repository.userStats.firstOrNull()
            if (stats != null) {
                GroqClient.updateLeaderboard("You", stats?.level ?: 1, stats?.totalXp ?: 0)
                fetchLeaderboard()
            }
        }
    }"""
replacement = """    fun publishStatsToLeaderboard() {
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
                // Fallback / mock
                GroqClient.updateLeaderboard("You", stats.level, stats.totalXp)
                fetchLeaderboard()
            }
        }
    }"""
text = text.replace(target, replacement)

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'w') as f:
    f.write(text)

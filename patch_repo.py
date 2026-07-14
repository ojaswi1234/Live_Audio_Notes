import sys

with open('app/src/main/java/com/example/data/SessionRepository.kt', 'r') as f:
    text = f.read()

target = """    suspend fun saveUserStats(stats: UserStats) {
        dao.insertUserStats(stats)
    }"""
replacement = """    suspend fun saveUserStats(stats: UserStats) {
        dao.insertUserStats(stats)
        try {
            com.example.network.FirebaseManager.syncUserStats(
                level = stats.level, 
                xp = stats.totalXp,
                streak = stats.currentStreak,
                longestStreak = stats.longestStreak,
                flashcards = stats.flashcardsMastered,
                sessions = stats.sessionsCompleted
            )
        } catch (e: Exception) {
            // Ignore if Firebase fails
        }
    }"""
text = text.replace(target, replacement)

with open('app/src/main/java/com/example/data/SessionRepository.kt', 'w') as f:
    f.write(text)

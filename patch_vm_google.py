import sys

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'r') as f:
    text = f.read()

target = """    // --- Authentication ---
    suspend fun loginWithEmail(email: String, pass: String): Boolean {
        return try {
            com.example.network.FirebaseManager.auth.signInWithEmailAndPassword(email, pass).await()
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
            navigateTo(AppScreen.ONBOARDING)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun registerWithEmail(email: String, pass: String): Boolean {
        return try {
            com.example.network.FirebaseManager.auth.createUserWithEmailAndPassword(email, pass).await()
            fetchLeaderboard()
            navigateTo(AppScreen.ONBOARDING)
            true
        } catch (e: Exception) {
            false
        }
    }"""
replacement = """    // --- Authentication ---
    suspend fun loginWithGoogle(idToken: String): Boolean {
        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            com.example.network.FirebaseManager.auth.signInWithCredential(credential).kotlinx.coroutines.tasks.await()
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
            navigateTo(AppScreen.ONBOARDING)
            true
        } catch (e: Exception) {
            false
        }
    }"""
text = text.replace(target, replacement)

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'w') as f:
    f.write(text)

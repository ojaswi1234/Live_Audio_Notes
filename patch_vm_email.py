import sys

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'r') as f:
    text = f.read()

import re

target = "    // --- Database Initialization ---"
replacement = """    // --- Authentication ---
    suspend fun loginWithEmail(email: String, pass: String): Boolean {
        return try {
            com.example.network.FirebaseManager.auth.signInWithEmailAndPassword(email, pass).kotlinx.coroutines.tasks.await()
            val cloudStats = com.example.network.FirebaseManager.fetchCloudStats()
            if (cloudStats != null) {
                val localStats = repository.userStats.kotlinx.coroutines.flow.firstOrNull() ?: com.example.data.UserStats()
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
            com.example.network.FirebaseManager.auth.createUserWithEmailAndPassword(email, pass).kotlinx.coroutines.tasks.await()
            fetchLeaderboard()
            navigateTo(AppScreen.ONBOARDING)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun logout() {
        com.example.network.FirebaseManager.auth.signOut()
        navigateTo(AppScreen.AUTH)
    }

    suspend fun updateDisplayName(name: String): Boolean {
        return try {
            val user = com.example.network.FirebaseManager.auth.currentUser
            if (user != null) {
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user.updateProfile(profileUpdates).kotlinx.coroutines.tasks.await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    // --- Database Initialization ---"""

text = text.replace(target, replacement)

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'w') as f:
    f.write(text)

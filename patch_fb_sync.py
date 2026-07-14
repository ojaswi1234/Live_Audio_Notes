import sys

with open('app/src/main/java/com/example/network/FirebaseManager.kt', 'r') as f:
    text = f.read()

# Replace old syncUserStats block
import re
text = re.sub(r'suspend fun syncUserStats\(level: Int, xp: Int\) \{.*?\} catch \(e: Exception\) \{\n            Log\.e\(TAG, "Firebase not fully configured yet", e\)\n        \}\n    \}', 
"""suspend fun syncUserStats(level: Int, xp: Int, streak: Int = 0, longestStreak: Int = 0, flashcards: Int = 0, sessions: Int = 0) {
        val user = auth.currentUser ?: return
        
        val statsData = hashMapOf(
            "displayName" to (user.displayName ?: "Anonymous Reader"),
            "level" to level,
            "xp" to xp,
            "currentStreak" to streak,
            "longestStreak" to longestStreak,
            "flashcardsMastered" to flashcards,
            "sessionsCompleted" to sessions,
            "lastActive" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        
        try {
            firestore.collection("leaderboard")
                .document(user.uid)
                .set(statsData)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Firebase not fully configured yet", e)
        }
    }
    
    suspend fun fetchCloudStats(): Map<String, Any>? {
        val user = auth.currentUser ?: return null
        return try {
            val doc = firestore.collection("leaderboard").document(user.uid).get().await()
            if (doc.exists()) doc.data else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch cloud stats", e)
            null
        }
    }""", text, flags=re.DOTALL)

with open('app/src/main/java/com/example/network/FirebaseManager.kt', 'w') as f:
    f.write(text)

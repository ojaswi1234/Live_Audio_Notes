package com.example.network

import android.util.Log
import com.example.viewmodel.ClubMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    
    // Live stream of club messages
    private val _realClubMessages = MutableStateFlow<List<ClubMessage>>(emptyList())
    val realClubMessages: StateFlow<List<ClubMessage>> = _realClubMessages
    
    fun listenToBookClub(bookId: String) {
        try {
            firestore.collection("book_clubs")
                .document(bookId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e(TAG, "Listen failed.", e)
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val messages = snapshot.documents.mapNotNull { doc ->
                            val sender = doc.getString("sender") ?: "Unknown"
                            val text = doc.getString("text") ?: ""
                            val persona = doc.getString("persona")
                            val time = doc.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()
                            ClubMessage(doc.id, sender, text, time)
                        }
                        _realClubMessages.value = messages
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase not fully configured yet", e)
        }
    }
    
    fun sendBookClubMessage(bookId: String, message: String, persona: String? = null) {
        val user = auth.currentUser
        val senderName = user?.displayName ?: "Anonymous Reader"
        
        val msgData = hashMapOf(
            "sender" to senderName,
            "text" to message,
            "persona" to persona,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        
        try {
            firestore.collection("book_clubs")
                .document(bookId)
                .collection("messages")
                .add(msgData)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase not fully configured yet", e)
        }
    }
    
    suspend fun syncUserStats(level: Int, xp: Int, streak: Int = 0, longestStreak: Int = 0, flashcards: Int = 0, sessions: Int = 0) {
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
    
    suspend fun saveUserProfile(name: String, age: String, interests: String, favouriteBooks: String, profilePicUri: String?) {
        val user = auth.currentUser ?: return
        val profileData = hashMapOf(
            "name" to name,
            "age" to age,
            "interests" to interests,
            "favouriteBooks" to favouriteBooks,
            "profilePicUri" to (profilePicUri ?: "")
        )
        try {
            firestore.collection("users").document(user.uid).set(profileData, com.google.firebase.firestore.SetOptions.merge()).await()
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name)
            if (profilePicUri != null) {
                profileUpdates.setPhotoUri(android.net.Uri.parse(profilePicUri))
            }
            user.updateProfile(profileUpdates.build()).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user profile", e)
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
    }
    
    suspend fun getGlobalLeaderboard(): List<Pair<String, Int>> {
        return try {
            val snapshot = firestore.collection("leaderboard")
                .orderBy("xp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
                
            snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("displayName") ?: "Unknown"
                val level = doc.getLong("level")?.toInt() ?: 1
                Pair(name, level)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase not fully configured yet", e)
            emptyList()
        }
    }
    
    fun getFcmToken(onToken: (String) -> Unit) {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }
                val token = task.result
                saveFcmTokenToFirestore(token)
                onToken(token)
            }
        } catch (e: Exception) {
             Log.e(TAG, "Firebase Messaging not fully configured yet", e)
        }
    }

    private fun saveFcmTokenToFirestore(token: String) {
        val user = auth.currentUser ?: return
        try {
            firestore.collection("users").document(user.uid)
                .set(hashMapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save FCM token", e)
        }
    }
}

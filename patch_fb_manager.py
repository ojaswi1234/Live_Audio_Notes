import sys

with open('app/src/main/java/com/example/network/FirebaseManager.kt', 'r') as f:
    text = f.read()

target = """    suspend fun fetchCloudStats(): Map<String, Any>? {"""
replacement = """    suspend fun saveUserProfile(name: String, age: String, interests: String, favouriteBooks: String, profilePicUri: String?) {
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

    suspend fun fetchCloudStats(): Map<String, Any>? {"""
text = text.replace(target, replacement)

with open('app/src/main/java/com/example/network/FirebaseManager.kt', 'w') as f:
    f.write(text)

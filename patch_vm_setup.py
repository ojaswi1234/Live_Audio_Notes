import sys

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'r') as f:
    text = f.read()

target = """    suspend fun updateDisplayName(name: String): Boolean {"""
replacement = """    fun completeUserSetup(name: String, age: String, interests: String, favouriteBooks: String, profilePicUri: String?) {
        viewModelScope.launch {
            com.example.network.FirebaseManager.saveUserProfile(name, age, interests, favouriteBooks, profilePicUri)
            navigateTo(AppScreen.ONBOARDING)
        }
    }

    suspend fun updateDisplayName(name: String): Boolean {"""
text = text.replace(target, replacement)

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'w') as f:
    f.write(text)

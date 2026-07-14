import sys

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'r') as f:
    text = f.read()

target = """                fetchLeaderboard()
                viewModelScope.launch {"""
replacement = """                fetchLeaderboard()
                com.example.network.FirebaseManager.getFcmToken { token ->
                    android.util.Log.d("EchoReader", "FCM Token: $token")
                }
                viewModelScope.launch {"""

text = text.replace(target, replacement)

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'w') as f:
    f.write(text)

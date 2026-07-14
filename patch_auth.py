import sys

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'r') as f:
    text = f.read()

target = """    init {
        fetchLeaderboard()
    }"""
replacement = """    init {
        com.example.network.FirebaseManager.auth.signInAnonymously()
            .addOnCompleteListener { task ->
                fetchLeaderboard()
            }
    }"""
text = text.replace(target, replacement)

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'w') as f:
    f.write(text)

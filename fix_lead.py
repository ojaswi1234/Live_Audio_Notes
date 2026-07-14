with open('app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt', 'r') as f:
    text = f.read()

import re
# Find the exact string to remove
bad_str = """        // Simulated Leaderboard Data including the current user,
        Pair("BookWorm99", 10),
        Pair("LiteratureLover", 8),
        Pair("StudyMaster", 5),
        Pair("NoviceReader", 2)
    )"""
text = text.replace(bad_str, "")

with open('app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt', 'w') as f:
    f.write(text)

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'r') as f:
    text = f.read()

text = text.replace('import androidx.lifecycle.ViewModel', 'import androidx.lifecycle.ViewModel\nimport kotlinx.coroutines.flow.firstOrNull')
text = text.replace('kotlinx.coroutines.flow.firstOrNull(repository.userStats)', 'repository.userStats.firstOrNull()')

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'w') as f:
    f.write(text)

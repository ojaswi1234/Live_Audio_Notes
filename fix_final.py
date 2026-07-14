with open('app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt', 'r') as f:
    text = f.read()

import re
text = re.sub(r'        // Simulated Leaderboard Data including the current user,\n        Pair\("BookWorm99", 10\),\n        Pair\("LiteratureLover", 8\),\n        Pair\("StudyMaster", 5\),\n        Pair\("NoviceReader", 2\)\n    \)', '', text, flags=re.MULTILINE)

with open('app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt', 'w') as f:
    f.write(text)

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'r') as f:
    text = f.read()

text = text.replace('kotlinx.coroutines.flow.firstOrNull(repository.getUserStatsFlow())', 'kotlinx.coroutines.flow.firstOrNull(repository.userStats)')

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'w') as f:
    f.write(text)

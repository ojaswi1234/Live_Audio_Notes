with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'r') as f:
    text = f.read()

import re
text = text.replace('val stats = repository.userStats.firstOrNull()\n            if (stats != null) {\n                GroqClient.updateLeaderboard("You", stats.level, stats.totalXp)',
'val stats = repository.userStats.firstOrNull()\n            if (stats != null) {\n                GroqClient.updateLeaderboard("You", stats?.level ?: 1, stats?.totalXp ?: 0)')

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'w') as f:
    f.write(text)

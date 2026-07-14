with open('app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt', 'r') as f:
    text = f.read()

import re
bad_str = """        // Simulated Leaderboard Data including the current user,
        Pair("BookWorm99", 10),
        Pair("LiteratureLover", 8),
        Pair("StudyMaster", 5),
        Pair("NoviceReader", 2)
    )"""

text = text.replace(bad_str, "")

with open('app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt', 'w') as f:
    f.write(text)

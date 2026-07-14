import re

with open('app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt', 'r') as f:
    text = f.read()

# Remove the mock
text = re.sub(r'    val mockUsers = listOf\(.*?\)', '', text, flags=re.DOTALL)
text = re.sub(r'    val leaderboard = \(mockUsers.*?\)\.sortedByDescending \{ it\.second \}', 
              '    val fetchedBoard by viewModel.leaderboard.collectAsStateWithLifecycle(initialValue = emptyList())\n    val leaderboard = (fetchedBoard.filter { it.first != "You" } + Pair("You", userLevel)).sortedByDescending { it.second }', text)

with open('app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt', 'w') as f:
    f.write(text)

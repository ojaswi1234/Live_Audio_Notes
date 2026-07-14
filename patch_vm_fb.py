import sys

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'r') as f:
    text = f.read()

target_fetch = """    fun fetchLeaderboard() {
        viewModelScope.launch {
            val board = GroqClient.getLeaderboard()
            _leaderboard.value = board
        }
    }"""
replacement_fetch = """    fun fetchLeaderboard() {
        viewModelScope.launch {
            // Try fetching from real Firebase database
            val fbBoard = com.example.network.FirebaseManager.getGlobalLeaderboard()
            if (fbBoard.isNotEmpty()) {
                _leaderboard.value = fbBoard
            } else {
                // Fallback to mock data if Firebase isn't configured by the user yet
                val board = GroqClient.getLeaderboard()
                _leaderboard.value = board
            }
        }
    }"""
text = text.replace(target_fetch, replacement_fetch)

target_publish = """    fun publishStatsToLeaderboard() {
        viewModelScope.launch {
            val stats = userStats.firstOrNull()
            if (stats != null) {
                GroqClient.updateLeaderboard("You", stats?.level ?: 1, stats?.totalXp ?: 0)
            }
        }
    }"""
replacement_publish = """    fun publishStatsToLeaderboard() {
        viewModelScope.launch {
            val stats = userStats.firstOrNull()
            if (stats != null) {
                // Publish to real Firebase database
                com.example.network.FirebaseManager.syncUserStats(stats.level, stats.totalXp)
                // Fallback / mock
                GroqClient.updateLeaderboard("You", stats.level, stats.totalXp)
            }
        }
    }"""
text = text.replace(target_publish, replacement_publish)

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'w') as f:
    f.write(text)

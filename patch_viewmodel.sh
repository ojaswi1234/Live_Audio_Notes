sed -i 's/PROFILE_REWARDS/PROFILE_REWARDS,\n    BOOK_CLUB,\n    LEADERBOARD/g' app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt

cat << 'EOF_INNER' >> app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt

data class ClubMessage(val id: String, val sender: String, val text: String, val timestamp: Long)

EOF_INNER

sed -i 's/items(sessions) { s ->/items(items = sessions, key = { it.id }) { s ->/g' app/src/main/java/com/example/ui/screens/HistoryScreen.kt
sed -i 's/items(achievements) { achievement ->/items(items = achievements, key = { it.id }) { achievement ->/g' app/src/main/java/com/example/ui/screens/ProfileScreen.kt
sed -i 's/items(chatMessages) { msg ->/items(items = chatMessages, key = { it.id }) { msg ->/g' app/src/main/java/com/example/ui/screens/BookClubScreen.kt

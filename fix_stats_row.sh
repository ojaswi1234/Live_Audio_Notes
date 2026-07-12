sed -i '/\/\/ Stats Row/,/Spacer(modifier = Modifier.height(32.dp))/c\
            // Stats Row\
            Row(\
                modifier = Modifier.fillMaxWidth(),\
                horizontalArrangement = Arrangement.SpaceEvenly\
            ) {\
                StatItem(label = "Sessions", value = (stats?.sessionsCompleted ?: 0).toString(), icon = Icons.Default.MenuBook)\
                StatItem(label = "Cards", value = flashcards.toString(), icon = Icons.Default.AutoAwesome)\
                StatItem(label = "Streak", value = currentStreak.toString(), icon = Icons.Default.LocalFireDepartment)\
                StatItem(label = "Best", value = longestStreak.toString(), icon = Icons.Default.Whatshot)\
            }\
            Spacer(modifier = Modifier.height(32.dp))' app/src/main/java/com/example/ui/screens/ProfileScreen.kt

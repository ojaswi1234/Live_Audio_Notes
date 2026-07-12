sed -i '/import androidx.compose.material.icons.filled.Star/a \
import androidx.compose.material.icons.filled.LocalFireDepartment\
import androidx.compose.material.icons.filled.Whatshot' app/src/main/java/com/example/ui/screens/ProfileScreen.kt

sed -i '/val flashcards = stats?.flashcardsMastered ?: 0/a \
    val currentStreak = stats?.currentStreak ?: 0\
    val longestStreak = stats?.longestStreak ?: 0' app/src/main/java/com/example/ui/screens/ProfileScreen.kt

# Find Stats Row
sed -i '/horizontalArrangement = Arrangement.SpaceEvenly/a \
            ) {\n                StatItem(icon = Icons.Default.MenuBook, label = "Sessions", value = stats?.sessionsCompleted?.toString() ?: "0")\n                StatItem(icon = Icons.Default.AutoAwesome, label = "Mastered", value = flashcards.toString())\n                StatItem(icon = Icons.Default.LocalFireDepartment, label = "Streak", value = currentStreak.toString())\n                StatItem(icon = Icons.Default.Whatshot, label = "Best Streak", value = longestStreak.toString())\n            }' app/src/main/java/com/example/ui/screens/ProfileScreen.kt

# Remove the old stat items
sed -i '/StatItem(icon = Icons.Default.MenuBook/,+1d' app/src/main/java/com/example/ui/screens/ProfileScreen.kt

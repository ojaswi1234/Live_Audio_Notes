sed -i 's/import androidx.compose.runtime.collectAsState/import androidx.lifecycle.compose.collectAsStateWithLifecycle/g' app/src/main/java/com/example/ui/screens/HistoryScreen.kt
sed -i 's/collectAsState(initial =/collectAsStateWithLifecycle(initialValue =/g' app/src/main/java/com/example/ui/screens/HistoryScreen.kt

sed -i 's/import androidx.compose.runtime.collectAsState/import androidx.lifecycle.compose.collectAsStateWithLifecycle/g' app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt
sed -i 's/collectAsState(initial =/collectAsStateWithLifecycle(initialValue =/g' app/src/main/java/com/example/ui/screens/LeaderboardScreen.kt

sed -i 's/import androidx.compose.runtime.collectAsState/import androidx.lifecycle.compose.collectAsStateWithLifecycle/g' app/src/main/java/com/example/ui/screens/ProfileScreen.kt
sed -i 's/collectAsState(initial =/collectAsStateWithLifecycle(initialValue =/g' app/src/main/java/com/example/ui/screens/ProfileScreen.kt

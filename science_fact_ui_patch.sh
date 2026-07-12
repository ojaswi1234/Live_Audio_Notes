sed -i '/import androidx.compose.material3.\*/a \
import androidx.compose.material.icons.filled.Lightbulb\
import androidx.compose.ui.window.Dialog' app/src/main/java/com/example/ui/screens/HistoryScreen.kt

cat << 'EOF_INNER' >> app/src/main/java/com/example/ui/screens/HistoryScreen.kt

@Composable
fun ScienceFactDialog(fact: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Lightbulb, contentDescription = "Did you know?", tint = MaterialTheme.colorScheme.primary)
        },
        title = {
            Text(text = "Did you know?")
        },
        text = {
            Text(text = fact, style = MaterialTheme.typography.bodyLarge)
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it!")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
EOF_INNER

sed -i '/val sessions by viewModel.savedSessions.collectAsState(initial = emptyList())/a \
    if (viewModel.showScienceFactPopup) {\
        ScienceFactDialog(\
            fact = viewModel.currentScienceFact,\
            onDismiss = { viewModel.dismissScienceFactPopup() }\
        )\
    }' app/src/main/java/com/example/ui/screens/HistoryScreen.kt

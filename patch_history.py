with open('app/src/main/java/com/example/ui/screens/HistoryScreen.kt', 'r') as f:
    text = f.read()

text = text.replace('import androidx.compose.material3.*', 'import androidx.compose.material3.*\nimport androidx.compose.material.icons.filled.HelpOutline\nimport com.example.ui.components.TutorStep\nimport com.example.ui.components.TutorState\nimport com.example.ui.components.tutorTarget')
text = text.replace('                    IconButton(onClick = { viewModel.navigateTo(AppScreen.LEADERBOARD) }) {', '                    IconButton(onClick = { TutorState.activeStep = TutorStep.WELCOME }) {\n                        Icon(Icons.Default.HelpOutline, contentDescription = "App Tour")\n                    }\n                    IconButton(onClick = { viewModel.navigateTo(AppScreen.LEADERBOARD) }) {')
text = text.replace('modifier = Modifier.testTag("new_session_fab"),', 'modifier = Modifier.testTag("new_session_fab").tutorTarget(TutorStep.START_SESSION),')

with open('app/src/main/java/com/example/ui/screens/HistoryScreen.kt', 'w') as f:
    f.write(text)

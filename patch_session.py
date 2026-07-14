with open('app/src/main/java/com/example/ui/screens/SessionScreen.kt', 'r') as f:
    text = f.read()

text = text.replace('import androidx.compose.material3.*', 'import androidx.compose.material3.*\nimport com.example.ui.components.TutorStep\nimport com.example.ui.components.tutorTarget')
text = text.replace('                                Box(\n                                    modifier = Modifier\n                                        .size(100.dp)\n                                        .clickable { viewModel.toggleVoiceListening() },', '                                Box(\n                                    modifier = Modifier\n                                        .size(100.dp)\n                                        .tutorTarget(TutorStep.VOICE_READER)\n                                        .clickable { viewModel.toggleVoiceListening() },')

with open('app/src/main/java/com/example/ui/screens/SessionScreen.kt', 'w') as f:
    f.write(text)

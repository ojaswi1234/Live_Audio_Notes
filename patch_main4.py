with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    text = f.read()

target = """                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background
                )"""
replacement = """                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background
                )"""
text = text.replace(target, replacement)

target2 = """                if (TutorState.activeStep != null) { TutorOverlay(viewModel = viewModel) }
                // Auto request audio permissions hook for voice triggers"""
replacement2 = """                if (TutorState.activeStep != null) { TutorOverlay(viewModel = viewModel) }
                }
                // Auto request audio permissions hook for voice triggers"""
text = text.replace(target2, replacement2)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(text)

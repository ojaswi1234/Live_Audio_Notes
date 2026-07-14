with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    text = f.read()

text = text.replace('import com.example.ui.theme.MyApplicationTheme', 'import com.example.ui.theme.MyApplicationTheme\nimport com.example.ui.components.TutorOverlay\nimport com.example.ui.components.TutorState')
text = text.replace('} // Auto request audio permissions hook for voice triggers', '}\n                if (TutorState.activeStep != null) {\n                    TutorOverlay(viewModel = viewModel)\n                }\n                // Auto request audio permissions hook for voice triggers')
text = text.replace('                }\n                // Auto request audio permissions hook for voice triggers', '                }\n                if (TutorState.activeStep != null) {\n                    TutorOverlay(viewModel = viewModel)\n                }\n                // Auto request audio permissions hook for voice triggers')

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(text)

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    text = f.read()

text = text.replace('                }\n                // Auto request audio permissions hook for voice triggers', '                }\n                if (TutorState.activeStep != null) {\n                    TutorOverlay(viewModel = viewModel)\n                }\n                // Auto request audio permissions hook for voice triggers')

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(text)

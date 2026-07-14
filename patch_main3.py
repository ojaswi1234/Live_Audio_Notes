with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    text = f.read()

target = """                }
                // Auto request audio permissions hook for voice triggers"""

replacement = """                }
                if (TutorState.activeStep != null) {
                    TutorOverlay(viewModel = viewModel)
                }
                // Auto request audio permissions hook for voice triggers"""

text = text.replace(target, replacement)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(text)

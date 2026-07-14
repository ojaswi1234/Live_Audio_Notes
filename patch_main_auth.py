import sys

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    text = f.read()

target = """                        when (screen) {
                            AppScreen.API_SETUP_INSTRUCTIONS -> {"""
replacement = """                        when (screen) {
                            AppScreen.AUTH -> {
                                AuthScreen(viewModel = viewModel)
                            }
                            AppScreen.API_SETUP_INSTRUCTIONS -> {"""

text = text.replace(target, replacement)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(text)

import sys

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    text = f.read()

target = """                            AppScreen.AUTH -> {
                                AuthScreen(viewModel = viewModel)
                            }"""
replacement = """                            AppScreen.AUTH -> {
                                AuthScreen(viewModel = viewModel)
                            }
                            AppScreen.USER_SETUP -> {
                                com.example.ui.screens.UserSetupScreen(viewModel = viewModel)
                            }"""

text = text.replace(target, replacement)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(text)

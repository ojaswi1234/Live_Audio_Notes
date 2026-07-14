import sys

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    text = f.read()

target = """                            AppScreen.ONBOARDING -> {
                                OnboardingScreen(
                                    onGetStarted = { viewModel.navigateTo(AppScreen.HISTORY_LIST) }
                                )
                            }"""
replacement = """"""
text = text.replace(target, replacement)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(text)

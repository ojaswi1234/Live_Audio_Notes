import sys

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'r') as f:
    text = f.read()

text = text.replace("enum class AppScreen {\n    AUTH,", "enum class AppScreen {\n    AUTH,\n    USER_SETUP,")
text = text.replace("navigateTo(AppScreen.ONBOARDING)", "navigateTo(AppScreen.USER_SETUP)")

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'w') as f:
    f.write(text)

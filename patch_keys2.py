import re

with open('app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt', 'r') as f:
    text = f.read()

text = text.replace('Icons.Filled.ArrowForward', 'Icons.AutoMirrored.Filled.ArrowForward')

with open('app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt', 'w') as f:
    f.write(text)

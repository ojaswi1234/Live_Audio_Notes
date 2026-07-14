import re

with open("app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt", "r") as f:
    content = f.read()

content = content.replace('userSelectedModel.startsWith("gemini") || userSelectedModel.startsWith("gemma")', 'userSelectedModel.startsWith("gemini") || userSelectedModel.startsWith("gemma")')
content = content.replace('if (userSelectedModel.startsWith("gemini"))', 'if (userSelectedModel.startsWith("gemini") || userSelectedModel.startsWith("gemma"))')

with open("app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt", "w") as f:
    f.write(content)

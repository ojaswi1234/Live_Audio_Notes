import re

with open("app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt", "r") as f:
    content = f.read()

content = content.replace("currentGeminiApiKey", "currentHfApiKey")

with open("app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt", "r") as f:
    content = f.read()

content = content.replace("import com.example.network.GeminiClient\n", "")

with open("app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt", "w") as f:
    f.write(content)

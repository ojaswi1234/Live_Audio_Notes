import re

with open("app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt", "r") as f:
    content = f.read()

# Replace Gemini instances with Hugging Face instances for provider
content = content.replace('aiProvider == "Gemini"', 'aiProvider == "HuggingFace"')
content = content.replace('aiProvider = "Gemini"', 'aiProvider = "HuggingFace"')

# Also replace currentGeminiApiKey with currentHfApiKey
content = content.replace('currentGeminiApiKey', 'currentHfApiKey')
content = content.replace('userGeminiApiKey', 'userHfApiKey')

with open("app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt", "w") as f:
    f.write(content)

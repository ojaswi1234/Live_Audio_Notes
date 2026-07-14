import re

with open("app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt", "r") as f:
    content = f.read()

content = content.replace("com.example.network.GeminiClient.generateAnalysis", "com.example.network.HuggingFaceClient.generateAnalysis")
content = content.replace("com.example.network.GeminiClient.generateRawResponse", "com.example.network.HuggingFaceClient.generateRawResponse")

with open("app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt", "w") as f:
    f.write(content)

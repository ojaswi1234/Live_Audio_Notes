import re

with open("app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt", "r") as f:
    content = f.read()

# Remove the selector for HuggingFace/Groq
content = re.sub(r'var selectedProvider by remember \{ mutableStateOf.*?\n', '', content)
content = re.sub(r'SingleChoiceSegmentedButtonRow[\s\S]*?\}', '', content)
content = re.sub(r'if \(selectedProvider == "HuggingFace"\) \{[\s\S]*?\} else \{', '', content)

with open("app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt", "w") as f:
    f.write(content)

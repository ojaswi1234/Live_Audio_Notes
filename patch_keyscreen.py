import re

with open("app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt", "r") as f:
    content = f.read()

content = content.replace('"Gemini"', '"HuggingFace"')
content = content.replace('userGeminiApiKey', 'userHfApiKey')
content = content.replace('saveGeminiApiKey', 'saveHfApiKey')
content = content.replace('MY_GEMINI_API_KEY', 'MY_HUGGINGFACE_API_KEY')

bad_models = """                listOf(
                    "gemini-2.0-flash" to "gemini-2.0-flash (Latest Fast)",
                    "gemini-1.5-pro" to "gemini-1.5-pro (Advanced)",
                    "gemini-2.5-flash" to "gemini-2.5-flash (Balanced)"
                )"""

good_models = """                listOf(
                    "meta-llama/Llama-3.2-11B-Vision-Instruct" to "Llama 3.2 11B (Fastest)",
                    "mistralai/Mistral-Nemo-Instruct-2407" to "Mistral Nemo (Balanced)",
                    "Qwen/Qwen2.5-72B-Instruct" to "Qwen 2.5 72B (Advanced)"
                )"""

content = content.replace(bad_models, good_models)

with open("app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt", "w") as f:
    f.write(content)

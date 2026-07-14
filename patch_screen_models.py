import sys

with open('app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt', 'r') as f:
    text = f.read()

target = """            val models = if (viewModel.aiProvider == "Gemini") {
                listOf(
                    "gemini-1.5-flash" to "gemini-1.5-flash (Balanced)",
                    "gemini-1.5-pro" to "gemini-1.5-pro (Advanced)",
                    "gemini-2.0-flash-exp" to "gemini-2.0-flash-exp (Experimental)"
                )
            } else {"""
replacement = """            val models = if (viewModel.aiProvider == "Gemini") {
                listOf(
                    "gemini-2.0-flash" to "gemini-2.0-flash (Fast & Latest)",
                    "gemini-2.5-flash" to "gemini-2.5-flash (Next-Gen)",
                    "gemini-1.5-flash" to "gemini-1.5-flash (Legacy Balanced)"
                )
            } else {"""
text = text.replace(target, replacement)

with open('app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt', 'w') as f:
    f.write(text)

import sys

with open('app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt', 'r') as f:
    text = f.read()

target = """            Text(
                text = if (selectedProvider == "Gemini") "Leave blank to use your default AI Studio credentials." else "Paste your Groq API key here to switch providers.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )"""

replacement = """            Text(
                text = if (selectedProvider == "Gemini") "Note: You only need to paste a key here if you want to use a DIFFERENT Gemini account. The app already has a built-in key! Leave blank to use the built-in one." else "Paste your Groq API key here to switch providers.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )"""
text = text.replace(target, replacement)

with open('app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt', 'w') as f:
    f.write(text)

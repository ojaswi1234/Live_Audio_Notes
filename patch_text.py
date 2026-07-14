import sys

with open('app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt', 'r') as f:
    text = f.read()

target = """                    } else if (defaultKey.isNotEmpty()) {
                        Text("✅ Using Built-in Project Key", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        Text("Don't worry! This is an anonymous built-in project key provided securely by AI Studio so the app works out-of-the-box. Your personal key is NOT hardcoded.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {"""

replacement = """                    } else if (defaultKey.isNotEmpty()) {
                        Text("✅ Using App Default Key", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        Text("This app is currently using a default test key so it works out of the box. Your personal AI Studio API key is NOT hardcoded anywhere.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {"""

text = text.replace(target, replacement)

with open('app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt', 'w') as f:
    f.write(text)

with open('app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt', 'r') as f:
    text = f.read()

import re
text = text.replace('Text("1. Visit Groq Console (Free)2. Sign in with Google3. Create an API Key", style = MaterialTheme.typography.bodyMedium)',
'Text("1. Visit Groq Console (Free)\\n2. Sign in with Google\\n3. Create an API Key", style = MaterialTheme.typography.bodyMedium)')
text = text.replace('Icons.Filled.ArrowForward', 'Icons.AutoMirrored.Filled.ArrowForward')

with open('app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt', 'w') as f:
    f.write(text)

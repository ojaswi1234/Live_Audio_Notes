with open('app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt', 'r') as f:
    text = f.read()

import re
text = re.sub(r'Text\("1\. Visit Groq Console \(Free\).*?style = MaterialTheme\.typography\.bodyMedium\)', 
r'Text("1. Visit Groq Console (Free)\\n2. Sign in with Google\\n3. Create an API Key", style = MaterialTheme.typography.bodyMedium)', text, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt', 'w') as f:
    f.write(text)

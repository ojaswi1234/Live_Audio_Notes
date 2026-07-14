import sys

with open('app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt', 'r') as f:
    text = f.read()
    
# Find the start of ApiKeyManagerScreen
start_idx = text.find('@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun ApiKeyManagerScreen')
if start_idx != -1:
    text = text[:start_idx]
    
with open('app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt', 'w') as f:
    f.write(text)

import re
with open('app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt', 'r') as f:
    text = f.read()

text = text.replace('import androidx.compose.material.icons.filled.ArrowForward', 'import androidx.compose.material.icons.automirrored.filled.ArrowForward')
text = text.replace('import androidx.compose.material.icons.filled.ArrowBack', 'import androidx.compose.material.icons.automirrored.filled.ArrowBack')

with open('app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt', 'w') as f:
    f.write(text)

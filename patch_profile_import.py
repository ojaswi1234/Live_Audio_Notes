import sys
import re

with open('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'r') as f:
    text = f.read()

text = text.replace("import androidx.compose.runtime.getValue", "import androidx.compose.runtime.getValue\nimport androidx.compose.runtime.setValue\nimport kotlinx.coroutines.launch")

with open('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'w') as f:
    f.write(text)

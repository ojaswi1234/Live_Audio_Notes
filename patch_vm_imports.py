import sys
import re

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'r') as f:
    text = f.read()

text = text.replace("import kotlinx.coroutines.flow.StateFlow", "import kotlinx.coroutines.flow.StateFlow\nimport kotlinx.coroutines.tasks.await")

with open('app/src/main/java/com/example/viewmodel/EchoReaderViewModel.kt', 'w') as f:
    f.write(text)

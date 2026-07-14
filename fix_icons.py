with open('app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt', 'r') as f:
    text = f.read()

text = text.replace('Icons.AutoMirrored.Filled.ArrowBack', 'Icons.Default.ArrowBack')
text = text.replace('Icons.AutoMirrored.Filled.ArrowForward', 'Icons.Default.ArrowForward')
text = text.replace('Icons.AutoMirrored.Filled.OpenInNew', 'Icons.Default.OpenInNew')
text = text.replace('import androidx.compose.material.icons.automirrored.filled.ArrowBack', 'import androidx.compose.material.icons.filled.ArrowBack')
text = text.replace('import androidx.compose.material.icons.automirrored.filled.ArrowForward', 'import androidx.compose.material.icons.filled.ArrowForward')

with open('app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt', 'w') as f:
    f.write(text)

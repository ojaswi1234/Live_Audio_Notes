sed -i 's/Icons.AutoMirrored.Filled.OpenInNew/Icons.Filled.OpenInNew/g' app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt
sed -i 's/Icons.AutoMirrored.Filled.ArrowForward/Icons.AutoMirrored.Filled.ArrowForward/g' app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt
sed -i 's/Icons.AutoMirrored.Filled.ArrowBack/Icons.AutoMirrored.Filled.ArrowBack/g' app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt
sed -i 's/HorizontalDivider(/Divider(/g' app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt
sed -i 's/import androidx.compose.material.icons.automirrored.filled.ArrowBack/import androidx.compose.material.icons.filled.ArrowBack/g' app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt
sed -i 's/import androidx.compose.material.icons.automirrored.filled.ArrowForward/import androidx.compose.material.icons.filled.ArrowForward/g' app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt
sed -i 's/import androidx.compose.material.icons.automirrored.filled.OpenInNew/import androidx.compose.material.icons.filled.OpenInNew/g' app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt

sed -i 's/Icons.AutoMirrored.Filled.MenuBook/Icons.Filled.MenuBook/g' app/src/main/java/com/example/ui/screens/ProfileScreen.kt
sed -i 's/import androidx.compose.material.icons.automirrored.filled.MenuBook/import androidx.compose.material.icons.filled.MenuBook/g' app/src/main/java/com/example/ui/screens/ProfileScreen.kt

git checkout app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt || true
git checkout app/src/main/java/com/example/ui/screens/ProfileScreen.kt || true

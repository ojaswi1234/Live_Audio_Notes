import sys

with open('app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt', 'r') as f:
    text = f.read()

target = """                    if (currentKeyForProvider.isNotEmpty()) {
                        Text("✅ Custom Key Active", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        Text("Starts with: ${currentKeyForProvider.take(7)}...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedButton(
                            onClick = {
                                if (viewModel.aiProvider == "Gemini") viewModel.deleteGeminiApiKey() else viewModel.deleteApiKey()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Remove Custom Key")
                        }
                    } else if (defaultKey.isNotEmpty()) {
                        Text("✅ AI Studio Default Key Active", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    } else {
                        Text("❌ No API Key Configured", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                    }"""

replacement = """                    if (currentKeyForProvider.isNotEmpty()) {
                        Text("✅ Custom Key Active (Provided by You)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        Text("Starts with: ${currentKeyForProvider.take(7)}...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedButton(
                            onClick = {
                                if (viewModel.aiProvider == "Gemini") viewModel.deleteGeminiApiKey() else viewModel.deleteApiKey()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Remove Custom Key")
                        }
                    } else if (defaultKey.isNotEmpty()) {
                        Text("✅ Using App's Built-in Project Key", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        Text("Don't worry! This is just a built-in project key provided securely by AI Studio so the app works out-of-the-box. Your personal key is NOT hardcoded.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("❌ No API Key Configured", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                    }
                }
            }
            
            val context = LocalContext.current
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Get Gemini Key", fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                }
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://console.groq.com/keys"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Get Groq Key", fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                }"""
text = text.replace(target, replacement)

with open('app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt', 'w') as f:
    f.write(text)

import re

with open("app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt", "r") as f:
    content = f.read()

new_screen = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeyInputScreen(viewModel: EchoReaderViewModel) {
    var inputKey by remember { mutableStateOf("") }
    var inputGeminiKey by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure API") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.API_SETUP_INSTRUCTIONS) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Model Selection",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val models = listOf(
                "llama-3.1-8b-instant" to "Groq LLaMA 3.1 8B (Fast)",
                "llama-3.3-70b-versatile" to "Groq LLaMA 3.3 70B (Better)",
                "llama-4-scout-17b" to "Groq LLaMA 4 Scout 17B",
                "gemini-2.0-flash" to "Gemini 2.0 Flash",
                "gemini-3.5-flash" to "Gemini 3.5 Flash",
                "gemini-3.1-pro-preview" to "Gemini 3.1 Pro Preview"
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                models.forEach { (id, label) ->
                    Card(
                        onClick = { viewModel.saveModel(id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (viewModel.userSelectedModel == id) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (viewModel.userSelectedModel == id) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f),
                                color = if (viewModel.userSelectedModel == id) 
                                    MaterialTheme.colorScheme.onPrimaryContainer 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (viewModel.userSelectedModel == id) {
                                Icon(
                                    Icons.Default.CheckCircle, 
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Groq Setup",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = inputKey,
                onValueChange = { inputKey = it },
                label = { Text("Groq API Key (Optional)") },
                placeholder = { Text("gsk_...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    viewModel.saveApiKey(inputKey)
                    inputKey = ""
                },
                enabled = inputKey.isNotBlank(),
                modifier = Modifier.align(Alignment.End),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Groq Key")
            }
            
            if (viewModel.userApiKey.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.VpnKey, contentDescription = "Key", tint = MaterialTheme.colorScheme.onErrorContainer)
                            Text("Groq Key is Saved", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                        }
                        
                        Text(
                            "Key ends in ...${viewModel.userApiKey.takeLast(4)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        
                        OutlinedButton(
                            onClick = { viewModel.deleteApiKey() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Key")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Gemini Setup",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = inputGeminiKey,
                onValueChange = { inputGeminiKey = it },
                label = { Text("Gemini API Key (Optional)") },
                placeholder = { Text("AIza...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    viewModel.saveGeminiApiKey(inputGeminiKey)
                    inputGeminiKey = ""
                },
                enabled = inputGeminiKey.isNotBlank(),
                modifier = Modifier.align(Alignment.End),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Gemini Key")
            }
            
            if (viewModel.userGeminiApiKey.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.VpnKey, contentDescription = "Key", tint = MaterialTheme.colorScheme.onErrorContainer)
                            Text("Gemini Key is Saved", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                        }
                        
                        Text(
                            "Key ends in ...${viewModel.userGeminiApiKey.takeLast(4)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        
                        OutlinedButton(
                            onClick = { viewModel.deleteGeminiApiKey() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Key")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}"""

content = re.sub(r'@OptIn\(ExperimentalMaterial3Api::class\)\n@Composable\nfun ApiKeyInputScreen\(viewModel: EchoReaderViewModel\) \{[\s\S]*', new_screen, content)

with open("app/src/main/java/com/example/ui/screens/ApiKeySetupScreen.kt", "w") as f:
    f.write(content)

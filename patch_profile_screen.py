import sys

with open('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'r') as f:
    text = f.read()

target = """    Scaffold(
        topBar = {"""
replacement = """    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var showNameDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var newName by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    
    val currentUser = com.example.network.FirebaseManager.auth.currentUser
    val displayName = currentUser?.displayName ?: "Reader"

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Update Name") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Display Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showNameDialog = false
                    coroutineScope.launch {
                        if (newName.isNotBlank()) {
                            val success = viewModel.updateDisplayName(newName)
                            if (success) {
                                android.widget.Toast.makeText(context, "Name updated", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {"""
text = text.replace(target, replacement)

target2 = """                Text(
                    "Bookworm",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )"""
replacement2 = """                Text(
                    displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    OutlinedButton(onClick = { showNameDialog = true }) {
                        Text("Edit Name")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { viewModel.logout() }) {
                        Text("Log Out")
                    }
                }"""
text = text.replace(target2, replacement2)

with open('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', 'w') as f:
    f.write(text)

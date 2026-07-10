package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookSession
import com.example.viewmodel.AppScreen
import com.example.viewmodel.EchoReaderViewModel
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    viewModel: EchoReaderViewModel,
    modifier: Modifier = Modifier
) {
    val session = viewModel.activeSession
    val chunks = viewModel.activeChunks.value
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Sidebar/Drawer Notes States
    var showNotesDrawer by remember { mutableStateOf(false) }
    var isEditNotesMode by remember { mutableStateOf(false) }
    var notesText by remember { mutableStateOf("") }

    // Synchronize notes text state when active session updates
    LaunchedEffect(session?.masterNotes) {
        if (session != null && !isEditNotesMode) {
            notesText = session.masterNotes
        }
    }

    // Toggle speech recognition warning
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    // Modal drawer state for Running Notes
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    if (session == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Running Master Notes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            )
                            IconButton(onClick = { isEditNotesMode = !isEditNotesMode }) {
                                Icon(
                                    imageVector = if (isEditNotesMode) Icons.Default.Save else Icons.Default.Edit,
                                    contentDescription = "Edit Notes",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (isEditNotesMode) {
                            OutlinedTextField(
                                value = notesText,
                                onValueChange = { notesText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                label = { Text("Editing Notes (Markdown supported)") }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(14.dp)
                            ) {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    Text(
                                        text = notesText,
                                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                        fontFamily = FontFamily.Serif
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateMasterNotes(notesText)
                                isEditNotesMode = false
                                Toast.makeText(context, "Master Notes updated!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Changes")
                        }

                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(notesText))
                                Toast.makeText(context, "Notes copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Notes (Markdown)")
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                session.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif
                                )
                            )
                            Text(
                                "by ${session.author}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.navigateTo(AppScreen.HISTORY_LIST) }) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Library")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                drawerState.open()
                            }
                        }) {
                            BadgedBox(
                                badge = {
                                    if (chunks.isNotEmpty()) {
                                        Badge { Text(chunks.size.toString()) }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.NoteAlt, contentDescription = "Notes")
                            }
                        }
                        IconButton(onClick = { viewModel.navigateTo(AppScreen.STUDY_QUIZ) }) {
                            Icon(Icons.Default.Quiz, contentDescription = "Quiz")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Session statistics bar
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LabelledBadge(label = "Focus: ${session.focus}", icon = Icons.Default.FilterList)
                            LabelledBadge(label = "Depth: ${session.depth}", icon = Icons.Default.CompassCalibration)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Reading Progress",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            LinearProgressIndicator(
                                progress = { session.progressPercent / 100f },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "${session.progressPercent.toInt()}%",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // Voice Listening & Typing Section
                var isVoiceMode by remember { mutableStateOf(true) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Section Header & Mode Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Active Reading Segment",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            )

                            // Mode toggle buttons
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isVoiceMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable {
                                            isVoiceMode = true
                                            viewModel.stopListening()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "Voice",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isVoiceMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (!isVoiceMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable {
                                            isVoiceMode = false
                                            viewModel.stopListening()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "Type",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isVoiceMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (isVoiceMode) {
                            // Voice recording panel
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                val scale by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = if (viewModel.isListening) 1.25f else 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "scale"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clickable { viewModel.toggleVoiceListening() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Ripple background glow
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .scale(scale)
                                            .background(
                                                if (viewModel.isListening) MaterialTheme.colorScheme.primary.copy(
                                                    alpha = 0.2f
                                                ) else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                                CircleShape
                                            )
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .background(
                                                if (viewModel.isListening) MaterialTheme.colorScheme.error
                                                else MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (viewModel.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                            contentDescription = "Mic",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = if (viewModel.isListening) "Listening... Read aloud now." else "Tap mic and read aloud",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (viewModel.isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Real-time transcribed text display
                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                            .heightIn(min = 80.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (viewModel.transcriptionText.isBlank() && viewModel.partialTranscriptionText.isBlank()) {
                                            Text(
                                                "Captured speech transcribing in real-time will appear here...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontStyle = FontStyle.Italic,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        } else {
                                            Column {
                                                if (viewModel.transcriptionText.isNotBlank()) {
                                                    Text(
                                                        viewModel.transcriptionText,
                                                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                                        fontFamily = FontFamily.Serif
                                                    )
                                                }
                                                if (viewModel.partialTranscriptionText.isNotBlank()) {
                                                    Text(
                                                        viewModel.partialTranscriptionText,
                                                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                                        fontFamily = FontFamily.Serif,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Typing manual entry
                            OutlinedTextField(
                                value = viewModel.typedText,
                                onValueChange = { viewModel.typedText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp)
                                    .testTag("type_text_input"),
                                shape = RoundedCornerShape(12.dp),
                                placeholder = { Text("Paste text from your ebook, document, or type observations here to compile insights...") },
                                label = { Text("Manual Text Block") }
                            )
                        }

                        // Chunk Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { viewModel.clearTranscription(); viewModel.typedText = "" },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Input")
                            }

                            Button(
                                onClick = { viewModel.processCurrentChunk() },
                                modifier = Modifier.testTag("process_chunk_button"),
                                enabled = !viewModel.isAnalyzing,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (viewModel.isAnalyzing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Companion Thinking...")
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = "Analyze")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Process Segment")
                                }
                            }
                        }

                        if (viewModel.isAnalyzing) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.emergencyStop() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth().testTag("emergency_stop_button")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Emergency Stop", tint = MaterialTheme.colorScheme.onError)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Emergency Stop (Hardware Protection)", color = MaterialTheme.colorScheme.onError)
                            }
                        }
                    }
                }

                // AI Companion Analysis Response Section
                val result = viewModel.latestAnalysis
                if (result != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Section Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = "Insights", tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        "Companion Insights",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Serif
                                    )
                                }

                                // TTS Audio play button and PDF Export
                                val itemToSpeak = when (viewModel.selectedAnalysisTab) {
                                    0 -> "Summary: ${result.summary}"
                                    1 -> "Key Insights: " + result.keyPoints.joinToString(". ")
                                    2 -> "Broad Connections: " + result.connections.joinToString(". ")
                                    3 -> "Discussion Questions: " + result.questions.joinToString(". ")
                                    else -> ""
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val context = LocalContext.current
                                    val bookTitle = viewModel.activeSession?.title ?: "Session"
                                    IconButton(
                                        onClick = {
                                            com.example.utils.PdfExporter.exportAnalysisToPdf(context, result, bookTitle)
                                        },
                                        modifier = Modifier.background(
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                            CircleShape
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = "Export PDF",
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (viewModel.isTtsPlaying) viewModel.stopSpeaking()
                                            else viewModel.speak(itemToSpeak)
                                        },
                                        modifier = Modifier.background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            CircleShape
                                        )
                                    ) {
                                        Icon(
                                            imageVector = if (viewModel.isTtsPlaying) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Read Aloud",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Scrollable Tabs for structured results
                            ScrollableTabRow(
                                selectedTabIndex = viewModel.selectedAnalysisTab,
                                edgePadding = 0.dp,
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Tab(
                                    selected = viewModel.selectedAnalysisTab == 0,
                                    onClick = { viewModel.selectedAnalysisTab = 0 }
                                ) {
                                    Text("Summary", modifier = Modifier.padding(vertical = 12.dp))
                                }
                                Tab(
                                    selected = viewModel.selectedAnalysisTab == 1,
                                    onClick = { viewModel.selectedAnalysisTab = 1 }
                                ) {
                                    Text("Key Points", modifier = Modifier.padding(vertical = 12.dp))
                                }
                                Tab(
                                    selected = viewModel.selectedAnalysisTab == 2,
                                    onClick = { viewModel.selectedAnalysisTab = 2 }
                                ) {
                                    Text("Connections", modifier = Modifier.padding(vertical = 12.dp))
                                }
                                Tab(
                                    selected = viewModel.selectedAnalysisTab == 3,
                                    onClick = { viewModel.selectedAnalysisTab = 3 }
                                ) {
                                    Text("Reflection", modifier = Modifier.padding(vertical = 12.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Tab content card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                when (viewModel.selectedAnalysisTab) {
                                    0 -> {
                                        // Summary View
                                        MarkdownText(
                                            text = result.summary
                                        )
                                    }

                                    1 -> {
                                        // Key points view
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            result.keyPoints.forEach { point ->
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                    MarkdownText(
                                                        text = point,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    2 -> {
                                        // Connections view
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            result.connections.forEach { conn ->
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text("🔗", fontSize = 14.sp)
                                                    MarkdownText(
                                                        text = conn,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    3 -> {
                                        // Discussion questions view
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            result.questions.forEachIndexed { idx, q ->
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(12.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .background(
                                                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                                                    CircleShape
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                (idx + 1).toString(),
                                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                                color = MaterialTheme.colorScheme.secondary
                                                            )
                                                        }
                                                        MarkdownText(
                                                            text = q,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (chunks.isEmpty() && !viewModel.isAnalyzing) {
                    // Tutorial onboarding first load guide inside empty state
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = "Start",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                            )
                            Text(
                                "Your Active Companion is Ready",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            )
                            Text(
                                "To begin reading Sapiens or any study guide, tap the Microphone button, read a paragraph out loud, then tap Process Segment! EchoReader will start assembling notes.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LabelledBadge(label: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val annotatedString = remember(text, primaryColor) {
        buildAnnotatedString {
            val lines = text.split("\n")
            for (line in lines) {
                when {
                    line.startsWith("## ") || line.startsWith("### ") -> {
                        val content = line.removePrefix("### ").removePrefix("## ")
                        withStyle(style = SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = primaryColor)) {
                            appendMarkdownLine(content)
                            append("\n\n")
                        }
                    }
                    line.startsWith("- ") || line.startsWith("* ") -> {
                        val content = line.substring(2)
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = primaryColor)) {
                            append("•  ")
                        }
                        appendMarkdownLine(content)
                        append("\n\n")
                    }
                    line.trim().isEmpty() -> {
                        // avoid multiple empty lines
                    }
                    else -> {
                        appendMarkdownLine(line)
                        append("\n\n")
                    }
                }
            }
        }
    }
    
    val trimmedString = remember(annotatedString) {
        var trimIndex = annotatedString.length
        while (trimIndex > 0 && annotatedString[trimIndex - 1].isWhitespace()) {
            trimIndex--
        }
        annotatedString.subSequence(0, trimIndex)
    }

    Text(
        text = trimmedString,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
        fontFamily = FontFamily.Serif
    )
}

fun AnnotatedString.Builder.appendMarkdownLine(text: String) {
    val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
    var lastIndex = 0
    val matches = boldRegex.findAll(text)
    for (match in matches) {
        append(text.substring(lastIndex, match.range.first))
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(match.groupValues[1])
        }
        lastIndex = match.range.last + 1
    }
    if (lastIndex < text.length) {
        append(text.substring(lastIndex))
    }
}

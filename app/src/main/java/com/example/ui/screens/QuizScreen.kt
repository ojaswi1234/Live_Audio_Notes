package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StudyCard
import com.example.viewmodel.AppScreen
import com.example.viewmodel.EchoReaderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: EchoReaderViewModel,
    modifier: Modifier = Modifier
) {
    val cards = viewModel.activeCards.value
    var currentIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Reset flipped state when moving to another card
    LaunchedEffect(currentIndex) {
        isFlipped = false
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Study & Flashcards",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.SESSION_DASHBOARD) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (cards.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f)
                        .padding(top = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Quiz,
                        contentDescription = "Empty Cards",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "No flashcards generated",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "When you analyze reading segments, EchoReader automatically extracts key study facts and designs flashcard questions here.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.navigateTo(AppScreen.SESSION_DASHBOARD) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Resume Reading Segment")
                    }
                }
            } else {
                val activeIndex = currentIndex.coerceIn(0, cards.size - 1)
                val card = cards[activeIndex]

                // Progress Indicator
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Card ${activeIndex + 1} of ${cards.size}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val masteredCount = cards.count { it.isMastered }
                            Text(
                                text = "🏆 $masteredCount Mastered",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // 3D Card-Flip Animation Layout
                val rotationY by animateFloatAsState(
                    targetValue = if (isFlipped) 180f else 0f,
                    animationSpec = tween(durationMillis = 400),
                    label = "cardFlip"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .graphicsLayer {
                            this.rotationY = rotationY
                            cameraDistance = 8 * density
                        }
                        .clickable { isFlipped = !isFlipped }
                        .testTag("flashcard_box")
                ) {
                    if (rotationY <= 90f) {
                        // FRONT: Question Side
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(
                                        Icons.Default.Quiz,
                                        contentDescription = "Question",
                                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                    )
                                    if (card.isMastered) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Mastered",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Text(
                                    text = card.front,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        lineHeight = 28.sp,
                                        fontFamily = FontFamily.Serif
                                    ),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )

                                Text(
                                    text = "Tap Card to Reveal Answer",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        // BACK: Answer Side (Render with 180-deg flip offset)
                        Card(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { this.rotationY = 180f },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Answer",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = {
                                            viewModel.speak(card.back)
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.VolumeUp,
                                            contentDescription = "TTS Answer",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Text(
                                    text = card.back,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        lineHeight = 24.sp,
                                        fontFamily = FontFamily.Serif
                                    ),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )

                                Text(
                                    text = "Tap Card to Return to Question",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // Card Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.toggleCardMastered(card)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (card.isMastered) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (card.isMastered) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = if (card.isMastered) Icons.Default.Close else Icons.Default.CheckCircle,
                            contentDescription = "Master"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (card.isMastered) "Practicing" else "Mastered")
                    }

                    IconButton(
                        onClick = {
                            viewModel.deleteCard(card.id)
                            Toast.makeText(context, "Card deleted", Toast.LENGTH_SHORT).show()
                            if (currentIndex >= cards.size - 1 && currentIndex > 0) {
                                currentIndex--
                            }
                        },
                        modifier = Modifier.background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f), CircleShape)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }

                // Navigation Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { if (activeIndex > 0) currentIndex-- },
                        enabled = activeIndex > 0
                    ) {
                        Icon(Icons.Default.ArrowBackIos, contentDescription = "Prev")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Previous")
                    }

                    TextButton(
                        onClick = { if (activeIndex < cards.size - 1) currentIndex++ },
                        enabled = activeIndex < cards.size - 1
                    ) {
                        Text("Next")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next")
                    }
                }
            }
        }
    }
}

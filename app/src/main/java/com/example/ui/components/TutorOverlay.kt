package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.AppScreen
import com.example.viewmodel.EchoReaderViewModel
import kotlinx.coroutines.delay

enum class TutorStep(val title: String, val message: String, val screen: AppScreen, val nextText: String = "Next") {
    WELCOME(
        "Welcome to EchoReader",
        "EchoReader uses the 'Production Effect' to help you remember what you read. Let me show you around!",
        AppScreen.HISTORY_LIST,
        "Start Tour"
    ),
    START_SESSION(
        "Start a Session",
        "Tap the '+' button to start a new reading session.",
        AppScreen.HISTORY_LIST
    ),
    GOALS_SETUP(
        "Set Your Intentions",
        "Before you start reading, set your focus. Tell EchoReader what you want to learn.",
        AppScreen.GOALS_SETUP
    ),
    SESSION_DASHBOARD(
        "Your Reading Dashboard",
        "This is where you'll spend most of your time reading and understanding.",
        AppScreen.SESSION_DASHBOARD
    ),
    VOICE_READER(
        "Read Aloud (Production Effect)",
        "Tap the microphone, read your book aloud, and EchoReader will summarize what you just read to help you remember it longer.",
        AppScreen.SESSION_DASHBOARD
    ),
    FINISHED(
        "You're All Set!",
        "That's it! You're ready to start retaining more from your books.",
        AppScreen.HISTORY_LIST,
        "Finish"
    )
}

object TutorState {
    var activeStep by mutableStateOf<TutorStep?>(null)
    val targetCoordinates = mutableStateMapOf<TutorStep, Rect>()

    fun registerTarget(step: TutorStep, coordinates: LayoutCoordinates) {
        targetCoordinates[step] = coordinates.boundsInWindow()
    }
}

fun Modifier.tutorTarget(step: TutorStep): Modifier = this.onGloballyPositioned { coordinates ->
    TutorState.registerTarget(step, coordinates)
}

@Composable
fun TutorOverlay(viewModel: EchoReaderViewModel) {
    val currentStep = TutorState.activeStep ?: return
    
    // Animate target rect changes
    var currentRect by remember { mutableStateOf<Rect?>(null) }
    
    LaunchedEffect(currentStep, TutorState.targetCoordinates[currentStep]) {
        if (currentStep == TutorStep.START_SESSION || currentStep == TutorStep.VOICE_READER) {
            currentRect = TutorState.targetCoordinates[currentStep]
        } else {
            currentRect = null // center popup
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(alpha = 0.99f)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Color.Black.copy(alpha = 0.7f))
            
            currentRect?.let { rect ->
                val margin = 8.dp.toPx()
                val path = Path().apply {
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            left = rect.left - margin,
                            top = rect.top - margin,
                            right = rect.right + margin,
                            bottom = rect.bottom + margin,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx())
                        )
                    )
                }
                drawPath(
                    path = path,
                    color = Color.Transparent,
                    blendMode = BlendMode.Clear
                )
            }
        }
        
        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            val alignModifier = if (currentRect != null) {
                if (currentRect!!.top > 1000f || currentRect!!.top > 500f) {
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 150.dp)
                } else if (currentRect!!.top < 300f) {
                    Modifier.align(Alignment.TopCenter).padding(top = 150.dp)
                } else {
                    Modifier.align(Alignment.Center)
                }
            } else {
                Modifier.align(Alignment.Center)
            }
            
            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 350.dp)
                    .then(alignModifier),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentStep.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { TutorState.activeStep = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close Tour")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = currentStep.message,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${currentStep.ordinal + 1} of ${TutorStep.values().size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Button(
                            onClick = {
                                val nextOrdinal = currentStep.ordinal + 1
                                if (nextOrdinal < TutorStep.values().size) {
                                    val nextStep = TutorStep.values()[nextOrdinal]
                                    TutorState.activeStep = nextStep
                                    viewModel.navigateTo(nextStep.screen)
                                } else {
                                    TutorState.activeStep = null
                                }
                            }
                        ) {
                            Text(currentStep.nextText)
                        }
                    }
                }
            }
        }
    }
}

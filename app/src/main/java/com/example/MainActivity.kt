package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.EchoReaderViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: EchoReaderViewModel = viewModel()
                val currentScreen by viewModel.currentScreen.collectAsState()

                // Audio permission launcher
                val requestPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        viewModel.toggleVoiceListening()
                    } else {
                        Toast.makeText(
                            this,
                            "Microphone permission is required to read aloud in real-time.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    Crossfade(
                        targetState = currentScreen,
                        label = "screen_routing_fade",
                        modifier = Modifier.padding(innerPadding)
                    ) { screen ->
                        when (screen) {
                            AppScreen.MODEL_DOWNLOAD -> {
                                ModelDownloadScreen(viewModel = viewModel)
                            }

                            AppScreen.ONBOARDING -> {
                                OnboardingScreen(
                                    onGetStarted = { viewModel.navigateTo(AppScreen.HISTORY_LIST) }
                                )
                            }

                            AppScreen.GOALS_SETUP -> {
                                GoalsSetupScreen(
                                    onStartSession = { title, author, purpose, depth, focus ->
                                        viewModel.startNewSession(title, author, purpose, depth, focus)
                                    },
                                    onBack = { viewModel.navigateTo(AppScreen.HISTORY_LIST) }
                                )
                            }

                            AppScreen.SESSION_DASHBOARD -> {
                                SessionScreen(viewModel = viewModel)
                            }

                            AppScreen.HISTORY_LIST -> {
                                HistoryScreen(viewModel = viewModel)
                            }

                            AppScreen.STUDY_QUIZ -> {
                                QuizScreen(viewModel = viewModel)
                            }
                        }
                    }
                }

                // Auto request audio permissions hook for voice triggers
                LaunchedEffect(viewModel.isListening) {
                    if (viewModel.isListening) {
                        val recordPermission = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.RECORD_AUDIO
                        )
                        if (recordPermission != PackageManager.PERMISSION_GRANTED) {
                            viewModel.stopListening()
                            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure all zombie and orphan C++ processes are strictly killed when app is actually closed (not on rotate)
        if (isFinishing) {
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}

package dev.joetul.tao

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.joetul.tao.model.TimerState
import dev.joetul.tao.ui.screens.MeditationScreen
import dev.joetul.tao.ui.screens.SettingsScreen
import dev.joetul.tao.ui.screens.JournalScreen
import dev.joetul.tao.ui.theme.TaoTheme
import dev.joetul.tao.viewmodel.TimerViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: TimerViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermission()

        setContent {
            TaoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val context = LocalContext.current

                    // Collect timer state as state properly
                    val timerState by viewModel.timerState.collectAsState()

                    // Keep screen on during meditation if the setting is enabled
                    DisposableEffect(timerState) {
                        if (timerState == TimerState.RUNNING) {
                            val sharedPrefs = context.getSharedPreferences("meditation_settings", MODE_PRIVATE)
                            val keepScreenOn = sharedPrefs.getBoolean("keep_screen_on", true)

                            if (keepScreenOn) {
                                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            }
                        } else {
                            // Clear the flag when timer is not running
                            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }

                        onDispose {
                            // Always clear the flag when the effect is disposed
                            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }

                    // Updated NavHost with Journal screen
                    NavHost(navController, startDestination = "meditation") {
                        composable("meditation") {
                            MeditationScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                },
                                onNavigateToJournal = {
                                    navController.navigate("journal")
                                }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onBackPressed = {
                                    navController.navigateUp()
                                }
                            )
                        }
                        composable("journal") {
                            JournalScreen(
                                onBackPressed = {
                                    navController.navigateUp()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {  // TIRAMISU is API 33
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
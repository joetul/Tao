package dev.joetul.tao.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.joetul.tao.model.TimerState
import dev.joetul.tao.ui.components.CircularTimePickerDialog
import dev.joetul.tao.ui.components.TimerButton
import dev.joetul.tao.ui.components.TimerDisplay
import dev.joetul.tao.viewmodel.TimerViewModel
import dev.joetul.tao.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeditationScreen(
    viewModel: TimerViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToJournal: () -> Unit
) {
    val timerState by viewModel.timerState.collectAsState()
    val timerData by viewModel.timerData.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState()
    val batteryOptimizationRequired by viewModel.batteryOptimizationRequired.collectAsState()

    val context = LocalContext.current
    var showTimerPicker by remember { mutableStateOf(false) }
    var showBatteryDialog by remember { mutableStateOf(false) }

    // Check for pending actions when the screen becomes active again
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPendingActions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = context.getString(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToJournal) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = stringResource(id = R.string.cd_journal)
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(id = R.string.cd_settings)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            // Use Box with centered content alignment
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f) // Use 90% of screen width
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    TimerDisplay(
                        timerData = if (timerState == TimerState.IDLE) timerData else currentTime,
                        onTimerClick = {
                            if (timerState == TimerState.IDLE) {
                                showTimerPicker = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    TimerButton(
                        timerState = timerState,
                        onClick = {
                            // Check if we need battery optimization before starting
                            if (timerState == TimerState.IDLE && batteryOptimizationRequired) {
                                showBatteryDialog = true
                            } else {
                                viewModel.toggleTimer()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.7f) // Button takes 70% of column width
                    )
                }
            }

            // Show circular time picker dialog when needed
            if (showTimerPicker && timerState == TimerState.IDLE) {
                val totalMinutes = timerData.hours * 60 + timerData.minutes

                CircularTimePickerDialog(
                    initialMinutes = totalMinutes,
                    onDismiss = { showTimerPicker = false },
                    onConfirm = { newTimerData ->
                        viewModel.updateTimerData(
                            newTimerData.hours,
                            newTimerData.minutes,
                            newTimerData.seconds
                        )
                        showTimerPicker = false
                    }
                )
            }

            // Show battery optimization dialog
            if (showBatteryDialog) {
                AlertDialog(
                    onDismissRequest = { /* Dialog cannot be dismissed */ },
                    title = { Text(stringResource(id = R.string.title_battery_optimization)) },
                    text = {
                        Text(stringResource(id = R.string.message_battery_optimization))
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showBatteryDialog = false
                                viewModel.requestBatteryOptimizationExemption()
                            }
                        ) {
                            Text(stringResource(id = R.string.action_continue))
                        }
                    }
                    // No dismiss button - user must continue
                )
            }
        }
    }
}
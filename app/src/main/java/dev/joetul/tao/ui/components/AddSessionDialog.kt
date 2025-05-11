package dev.joetul.tao.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.joetul.tao.data.MeditationSession
import dev.joetul.tao.viewmodel.SessionPreferencesViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import dev.joetul.tao.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSessionDialog(
    onDismiss: () -> Unit,
    onAddSession: (MeditationSession) -> Unit,
    viewModel: SessionPreferencesViewModel = viewModel()
) {
    // Get the last session duration from preferences
    val lastSessionDuration by viewModel.lastSessionDurationMinutes.collectAsState()

    // State variables
    var selectedDurationMinutes by remember { mutableIntStateOf(lastSessionDuration) }
    var selectedDateTime by remember { mutableStateOf(LocalDateTime.now()) }

    // Dialog visibility states
    var showDurationPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Get the current context to check system time format
    val context = LocalContext.current

    // Check if the system is using 24-hour format
    val is24HourFormat = remember(context) {
        DateFormat.is24HourFormat(context)
    }

    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

    // Time formatter based on system settings
    val timeFormatter = remember(is24HourFormat) {
        if (is24HourFormat) {
            DateTimeFormatter.ofPattern("HH:mm")  // 24-hour format (16:30)
        } else {
            DateTimeFormatter.ofPattern("h:mm a") // 12-hour format (4:30 PM)
        }
    }

    // Main Dialog
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.add_meditation_dialog_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Default session info (date, time, duration)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.session_details_title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$selectedDurationMinutes ${stringResource(id = R.string.unit_minutes)}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                selectedDateTime.format(dateFormatter),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                selectedDateTime.format(timeFormatter),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                // Action buttons for pickers
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedButton(
                        onClick = { showDurationPicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = stringResource(id = R.string.cd_set_duration)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(id = R.string.cd_set_duration))
                    }

                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = stringResource(id = R.string.cd_set_date)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(id = R.string.cd_set_date))
                    }

                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = stringResource(id = R.string.cd_set_time)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(id = R.string.cd_set_time))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val durationSeconds = selectedDurationMinutes.toLong() * 60

                    // Save the selected duration for future use
                    viewModel.saveSessionDuration(selectedDurationMinutes)

                    val session = MeditationSession(
                        startTime = selectedDateTime,
                        duration = durationSeconds,
                        plannedDuration = durationSeconds
                    )

                    onAddSession(session)
                    onDismiss()
                }
            ) {
                Text(text = stringResource(id = R.string.action_add_session))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        }
    )

    // Duration Picker Dialog - Using your CircularTimePickerDialog directly
    if (showDurationPicker) {
        CircularTimePickerDialog(
            initialMinutes = selectedDurationMinutes,
            onDismiss = { showDurationPicker = false },
            onConfirm = { newTimerData ->
                // Update duration from the picker (converting hours and minutes to total minutes)
                val newDurationMinutes = newTimerData.hours * 60 + newTimerData.minutes
                selectedDurationMinutes = newDurationMinutes

                // We don't need to save here as we'll save when user confirms the entire dialog
                showDurationPicker = false
            }
        )
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val newDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()

                        // Update the date part of selectedDateTime
                        selectedDateTime = LocalDateTime.of(
                            newDate,
                            LocalTime.of(selectedDateTime.hour, selectedDateTime.minute)
                        )
                    }
                    showDatePicker = false
                }) {
                    Text(text = stringResource(id = R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedDateTime.hour,
            initialMinute = selectedDateTime.minute
        )

        // Using a custom TimePickerDialog similar to the one in your JournalScreen
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.title_select_time),
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TimePicker(state = timePickerState)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(text = stringResource(id = R.string.action_cancel))
                        }

                        TextButton(onClick = {
                            // Update the time part of selectedDateTime
                            selectedDateTime = selectedDateTime
                                .withHour(timePickerState.hour)
                                .withMinute(timePickerState.minute)
                            showTimePicker = false
                        }) {
                            Text(text = stringResource(id = R.string.action_ok))
                        }
                    }
                }
            }
        }
    }
}
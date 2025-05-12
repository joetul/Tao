package dev.joetul.tao.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.joetul.tao.data.MeditationSession
import java.time.format.DateTimeFormatter
import java.util.Locale
import dev.joetul.tao.R
import androidx.compose.ui.res.stringResource

@Composable
fun SessionItem(
    session: MeditationSession,
    onNoteUpdate: (String) -> Unit,
    onDeleteSession: (MeditationSession) -> Unit,
    modifier: Modifier = Modifier
) {
    var showNoteDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf(session.note) }

    // Get the current context to check system time format
    val context = LocalContext.current

    // Check if the system is using 24-hour format
    val is24HourFormat = remember(context) {
        DateFormat.is24HourFormat(context)
    }

    // Create a date formatter based on system time format
    val dateFormatter = remember(is24HourFormat) {
        if (is24HourFormat) {
            DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")  // 24-hour format (16:30)
        } else {
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a") // 12-hour format (4:30 PM)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { showNoteDialog = true },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with date and action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Date and time
                Text(
                    text = session.startTime.format(dateFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                // Action buttons in a row
                Row {
                    // Note button - icon only
                    IconButton(
                        onClick = { showNoteDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(id = R.string.cd_edit_note),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Delete button - icon only
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.cd_delete_session),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Duration information - rounded to nearest minute
            Text(
                text = formatDurationRounded(session.duration),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Show note if it exists
            if (session.note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = session.note,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }

    // Note editing dialog
    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text(stringResource(id = R.string.title_session_notes)) },
            text = {
                Column {
                    Text(
                        text = session.startTime.format(dateFormatter),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        label = { Text(stringResource(id = R.string.label_notes)) },
                        placeholder = { Text(stringResource(id = R.string.placeholder_meditation_notes)) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onNoteUpdate(noteText)
                        showNoteDialog = false
                    }
                ) {
                    Text(stringResource(id = R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        noteText = session.note
                        showNoteDialog = false
                    }
                ) {
                    Text(stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(id = R.string.title_delete_session)) },
            text = {
                Text(stringResource(id = R.string.message_delete_confirmation))
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSession(session)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(id = R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text(stringResource(id = R.string.action_cancel))
                }
            }
        )
    }
}

// Format duration rounded to the nearest minute
fun formatDurationRounded(seconds: Long): String {
    // Round to the nearest minute (30 seconds or more rounds up)
    val totalMinutes = (seconds + 30) / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return when {
        hours > 0 -> String.format(Locale.US, "%d h %d min", hours, minutes)
        minutes > 0 -> String.format(Locale.US, "%d min", minutes)
        else -> "Less than 1 min" // For sessions less than 30 seconds
    }
}
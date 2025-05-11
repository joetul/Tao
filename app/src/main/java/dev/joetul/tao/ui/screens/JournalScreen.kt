package dev.joetul.tao.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.joetul.tao.ui.components.AddSessionDialog
import dev.joetul.tao.ui.components.SessionItem
import dev.joetul.tao.ui.components.StatsCard
import dev.joetul.tao.viewmodel.JournalViewModel
import dev.joetul.tao.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    onBackPressed: () -> Unit,
    viewModel: JournalViewModel = viewModel()
) {
    // We need to get the session list properly
    val sessionsState = viewModel.allSessions.collectAsState(initial = emptyList())
    val sessions = sessionsState.value

    val sessionCount = viewModel.sessionCount.collectAsState(initial = 0).value
    val totalMeditationTime = viewModel.totalMeditationTime.collectAsState(initial = 0L).value ?: 0L
    val streakData = viewModel.streakData.collectAsState().value

    var showAddSessionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.screen_journal)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.cd_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (sessions.isEmpty()) {
// Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.message_no_sessions),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.message_complete_first_session),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showAddSessionDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.action_add_session_manually))
                    }
                }
            }
        } else {
            // Journal content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Stats card
                    StatsCard(
                        sessionCount = sessionCount,
                        totalMeditationTime = totalMeditationTime,
                        currentStreak = streakData.currentStreak,
                        maxStreak = streakData.maxStreak,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.title_session_history),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Manual add button
                        Button(
                            onClick = { showAddSessionDialog = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(id = R.string.action_add_session))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Session list
                items(sessions) { session ->
                    SessionItem(
                        session = session,
                        onNoteUpdate = { note ->
                            viewModel.updateNote(session.id, note)
                        },
                        onDeleteSession = { sessionToDelete ->
                            viewModel.deleteSession(sessionToDelete)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Show add session dialog when requested
        if (showAddSessionDialog) {
            AddSessionDialog(
                onDismiss = { showAddSessionDialog = false },
                onAddSession = { session ->
                    viewModel.recordSession(
                        durationSeconds = session.duration,
                        plannedDurationSeconds = session.plannedDuration,
                        startTime = session.startTime
                    )
                }
            )
        }
    }
}
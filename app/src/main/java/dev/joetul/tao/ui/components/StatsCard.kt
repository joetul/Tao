package dev.joetul.tao.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Locale
import dev.joetul.tao.R
import androidx.compose.ui.res.stringResource

@Composable
fun StatsCard(
    sessionCount: Int,
    totalMeditationTime: Long,
    currentStreak: Int,
    maxStreak: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.title_meditation_stats),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = sessionCount.toString(),
                    label = stringResource(id = R.string.stat_label_sessions)
                )

                StatItem(
                    value = formatTotalMeditationTime(totalMeditationTime),
                    label = stringResource(id = R.string.stat_label_total_time)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    // Format streak with "d" suffix
                    value = formatStreakValue(currentStreak),
                    label = stringResource(id = R.string.stat_label_current_streak)
                )

                StatItem(
                    // Format streak with "d" suffix
                    value = formatStreakValue(maxStreak),
                    label = stringResource(id = R.string.stat_label_best_streak)
                )
            }
        }
    }
}

// Helper function to format streak values with "d" for days
fun formatStreakValue(days: Int): String {
    return when (days) {
        0 -> "0d"
        1 -> "1d"
        else -> "${days}d"
    }
}

@Composable
fun StatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun formatTotalMeditationTime(seconds: Long): String {
    val hours = seconds / 3600
    val days = hours / 24

    return when {
        days > 0 -> String.format(Locale.US, "%dd %dh", days, hours % 24)
        hours > 0 -> String.format(Locale.US, "%dh %dm", hours, (seconds % 3600) / 60)
        else -> String.format(Locale.US, "%dm", (seconds % 3600) / 60)
    }
}
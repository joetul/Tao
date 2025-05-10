package dev.joetul.tao.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Locale

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
                text = "Meditation Stats",
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
                    label = "Sessions"
                )

                StatItem(
                    value = formatTotalMeditationTime(totalMeditationTime),
                    label = "Total Time"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = currentStreak.toString(),
                    label = "Current Streak"
                )

                StatItem(
                    value = maxStreak.toString(),
                    label = "Best Streak"
                )
            }
        }
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
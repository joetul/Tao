package dev.joetul.tao.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.joetul.tao.model.TimerData
import java.util.Locale

@Composable
fun TimerDisplay(
    timerData: TimerData,
    onTimerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onTimerClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Simple time display in HH:MM:SS format
        Text(
            text = String.format(Locale.US, "%02d:%02d:%02d", timerData.hours, timerData.minutes, timerData.seconds),
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
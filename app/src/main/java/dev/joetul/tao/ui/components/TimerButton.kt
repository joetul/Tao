package dev.joetul.tao.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.joetul.tao.model.TimerState

@Composable
fun TimerButton(
    timerState: TimerState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (buttonText, buttonColors) = when (timerState) {
        TimerState.IDLE -> Pair(
            "Start",
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        )
        TimerState.RUNNING -> Pair(
            "Stop",
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        )
        TimerState.PAUSED -> Pair(
            "Resume",
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            )
        )
    }

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp), // Increased from 8.dp to 24.dp for a more rounded look
        colors = buttonColors,
        modifier = modifier.padding(8.dp)
    ) {
        Text(
            text = buttonText,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
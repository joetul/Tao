package dev.joetul.tao.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*
import dev.joetul.tao.model.TimerData

@Composable
fun CircularTimePickerDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (TimerData) -> Unit
) {
    var selectedTotalMinutes by remember { mutableIntStateOf(initialMinutes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Set Meditation Duration",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                MinimalCircularTimePicker(
                    minutes = selectedTotalMinutes,
                    onMinutesChanged = { selectedTotalMinutes = it },
                    modifier = Modifier
                        .size(280.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalMinutes = maxOf(1, selectedTotalMinutes)
                    val hours = finalMinutes / 60
                    val minutes = finalMinutes % 60
                    onConfirm(TimerData(hours, minutes, 0))
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MinimalCircularTimePicker(
    minutes: Int,
    onMinutesChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val surface = MaterialTheme.colorScheme.surface
        val track = MaterialTheme.colorScheme.surfaceVariant

        var dragInProgress by remember { mutableStateOf(false) }

        // Define maximum limit: 9 hours and 59 minutes = 599 minutes
        val maxMinutes = 599

        // We'll use total accumulated angle instead of limiting to 0-360
        var accumulatedAngle by remember { mutableFloatStateOf(totalMinutesToAngle(minutes)) }
        var lastAngle by remember { mutableFloatStateOf(accumulatedAngle % 360f) }
        var revolutions by remember { mutableIntStateOf(minutes / 60) }

        val currentTotalMinutes by remember(accumulatedAngle) {
            derivedStateOf {
                (accumulatedAngle / 6f).roundToInt().coerceIn(0, maxMinutes)
            }
        }

        LaunchedEffect(minutes) {
            if (!dragInProgress) {
                accumulatedAngle = totalMinutesToAngle(minutes.coerceIn(0, maxMinutes))
                revolutions = minutes.coerceIn(0, maxMinutes) / 60
                lastAngle = accumulatedAngle % 360f
            }
        }

        LaunchedEffect(currentTotalMinutes) {
            // Make sure we stay between 0 and maxMinutes
            val validMinutes = currentTotalMinutes.coerceIn(0, maxMinutes)
            onMinutesChanged(validMinutes)

            // If we're trying to go below 0, reset the accumulated angle and revolutions
            if (currentTotalMinutes <= 0 && accumulatedAngle < 0) {
                accumulatedAngle = 0f
                revolutions = 0
                lastAngle = 0f
            }

            // If we're trying to go above maxMinutes, cap the accumulated angle and revolutions
            if (currentTotalMinutes >= maxMinutes && accumulatedAngle > totalMinutesToAngle(maxMinutes)) {
                accumulatedAngle = totalMinutesToAngle(maxMinutes)
                revolutions = maxMinutes / 60
                lastAngle = accumulatedAngle % 360f
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { dragInProgress = true },
                        onDragEnd = { dragInProgress = false },
                        onDragCancel = { dragInProgress = false },
                        onDrag = { change, _ ->
                            val centerX = size.width / 2
                            val centerY = size.height / 2
                            val angleRadians = atan2(
                                change.position.y - centerY,
                                change.position.x - centerX
                            )
                            val angleDegrees = Math.toDegrees(angleRadians.toDouble()).toFloat() + 90f
                            val normalizedAngle = (angleDegrees + 360f) % 360f

                            // Detect crossing the 0/360 boundary
                            val crossingForward = normalizedAngle < 90f && lastAngle > 270f
                            val crossingBackward = normalizedAngle > 270f && lastAngle < 90f

                            // Check if we would go below 0 or above maxMinutes
                            val wouldGoNegative = revolutions == 0 && crossingBackward
                            val wouldExceedMax = (revolutions * 60) + (normalizedAngle / 6f).roundToInt() > maxMinutes &&
                                    ((crossingForward && revolutions >= maxMinutes / 60) ||
                                            (!crossingForward && !crossingBackward && revolutions >= maxMinutes / 60))

                            if (!wouldGoNegative && !wouldExceedMax) {
                                // Update revolutions if crossing the boundary
                                if (crossingForward) {
                                    revolutions++
                                } else if (crossingBackward && revolutions > 0) {
                                    revolutions--
                                }

                                lastAngle = normalizedAngle
                                accumulatedAngle = normalizedAngle + (revolutions * 360f)

                                // Additional check to ensure we don't exceed maxMinutes
                                val newTotalMinutes = (accumulatedAngle / 6f).roundToInt()
                                if (newTotalMinutes > maxMinutes) {
                                    accumulatedAngle = totalMinutesToAngle(maxMinutes)
                                    revolutions = maxMinutes / 60
                                    lastAngle = accumulatedAngle % 360f
                                }
                            }
                            // If it would go negative or exceed max, do not update anything
                        }
                    )
                }
        ) {
            val strokeWidthPx = 20.dp.toPx()
            val radius = (size.minDimension - strokeWidthPx) / 2

            // Calculate track alpha based on revolutions
            // For first revolution (revolutions = 0), keep it light at 0.3f
            // For subsequent revolutions, make it darker (50% of the primary color)
            val trackColor = if (revolutions > 0) {
                // Make track 50% of the primary color when past first revolution
                primary.copy(alpha = 0.5f)
            } else {
                // Original light track for first revolution
                track.copy(alpha = 0.3f)
            }

            // Draw track with dynamic color
            drawCircle(
                color = trackColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidthPx)
            )

            // Draw progress - only showing current position in the circle (0-360)
            val sweepAngle = lastAngle
            drawArc(
                color = primary,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(
                    (size.width - radius * 2) / 2,
                    (size.height - radius * 2) / 2
                ),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // Draw handle
            val angleRadians = Math.toRadians((sweepAngle - 90).toDouble())
            val handleRadius = 13.dp.toPx()
            val handleX = center.x + radius * cos(angleRadians).toFloat()
            val handleY = center.y + radius * sin(angleRadians).toFloat()

            drawCircle(
                color = surface,
                radius = handleRadius,
                center = Offset(handleX, handleY)
            )
            drawCircle(
                color = primary,
                radius = handleRadius,
                center = Offset(handleX, handleY),
                style = Stroke(width = 2.5f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val isNegative = currentTotalMinutes < 0
            // Use absolute values for display
            val absTotal = abs(currentTotalMinutes)
            val displayHours = absTotal / 60
            val displayMinutes = absTotal % 60

            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                // Add minus sign for negative values
                if (isNegative) {
                    Text(
                        text = "-",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = displayHours.toString(),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "h",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp, end = 8.dp)
                )
                Text(
                    text = "%02d".format(displayMinutes),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "m",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }
}

private fun totalMinutesToAngle(totalMinutes: Int): Float {
    // For calculating the initial angle based on total minutes
    val minutesPart = totalMinutes % 60
    val hoursPart = totalMinutes / 60
    return (minutesPart * 6f) + (hoursPart * 360f)
}
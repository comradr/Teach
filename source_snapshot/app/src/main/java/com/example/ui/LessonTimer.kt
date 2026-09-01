package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop

@Composable
fun LessonTimer(durationMinutes: Int = 45, modifier: Modifier = Modifier) {
    var isRunning by remember { mutableStateOf(false) }
    
    // We use total seconds for the duration.
    val totalSeconds = durationMinutes * 60
    
    // When timer is running, this holds the timestamp (in ms) when the timer should finish.
    // When paused, we just keep track of how many seconds are left.
    var timeLeftSeconds by remember { mutableIntStateOf(totalSeconds) }
    var targetEndTimeMs by remember { mutableLongStateOf(0L) }

    // Reset if duration changes and not running (optional, but good for UX)
    LaunchedEffect(durationMinutes) {
        if (!isRunning && timeLeftSeconds > totalSeconds) {
            timeLeftSeconds = totalSeconds
        }
    }

    LaunchedEffect(isRunning, targetEndTimeMs) {
        if (isRunning) {
            // Recalculate target end time if we just started
            if (targetEndTimeMs == 0L) {
                targetEndTimeMs = System.currentTimeMillis() + (timeLeftSeconds * 1000L)
            }
            
            while (isRunning) {
                val current = System.currentTimeMillis()
                val remainingMs = targetEndTimeMs - current
                if (remainingMs <= 0) {
                    timeLeftSeconds = 0
                    isRunning = false
                    targetEndTimeMs = 0L
                    break
                } else {
                    timeLeftSeconds = (remainingMs / 1000L).toInt()
                }
                delay(200L) // check frequently to update UI smoothly when returning from background
            }
        } else {
            targetEndTimeMs = 0L
        }
    }
    
    val minutes = timeLeftSeconds / 60
    val seconds = timeLeftSeconds % 60
    val progress = 1f - (timeLeftSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Таймер урока", 
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = progress,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000, easing = androidx.compose.animation.core.LinearEasing)
                )
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            IconButton(
                onClick = { 
                    if (timeLeftSeconds <= 0) {
                        timeLeftSeconds = totalSeconds
                        targetEndTimeMs = 0L
                    }
                    if (!isRunning) {
                        // Starting
                        targetEndTimeMs = System.currentTimeMillis() + (timeLeftSeconds * 1000L)
                    } else {
                        // Pausing
                        targetEndTimeMs = 0L
                    }
                    isRunning = !isRunning 
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Остановить" else "Старт",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

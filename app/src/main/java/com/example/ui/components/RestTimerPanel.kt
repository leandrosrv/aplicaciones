package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.RestTimerState
import com.example.ui.TimerMode
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.util.Locale

@Composable
fun RestTimerPanel(
    state: RestTimerState,
    onModeChange: (TimerMode) -> Unit,
    onSelectPreset: (Int) -> Unit,
    onAdjustTimer: (Int) -> Unit,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onToggleStopwatch: () -> Unit,
    onLapStopwatch: () -> Unit,
    onResetStopwatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val activeText = when {
        state.mode == TimerMode.TIMER || state.isTimerRunning -> formatMmSs(state.timerRemainingMillis)
        else -> formatMmSsTenths(state.stopwatchElapsedMillis)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (state.isTimerCompleted) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .testTag("rest_timer_header"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = if (state.mode == TimerMode.TIMER) {
                            Icons.Filled.HourglassBottom
                        } else {
                            Icons.Filled.Timer
                        },
                        contentDescription = null,
                        tint = if (state.isTimerCompleted) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                    Column {
                        Text(
                            text = if (state.isTimerCompleted) {
                                stringResource(R.string.timer_finished)
                            } else {
                                stringResource(R.string.timer_section_title)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (state.mode == TimerMode.TIMER) "Temporizador" else "Cronómetro",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    ) {
                        Text(
                            text = activeText,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                    IconButton(
                        onClick = {
                            if (state.mode == TimerMode.TIMER) onToggleTimer() else onToggleStopwatch()
                        },
                        modifier = Modifier.testTag("quick_timer_toggle"),
                    ) {
                        val running = if (state.mode == TimerMode.TIMER) {
                            state.isTimerRunning
                        } else {
                            state.isStopwatchRunning
                        }
                        Icon(
                            imageVector = if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (running) stringResource(R.string.btn_pause) else stringResource(R.string.btn_start),
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = state.mode == TimerMode.TIMER,
                            onClick = { onModeChange(TimerMode.TIMER) },
                            label = { Text(stringResource(R.string.tab_timer)) },
                            leadingIcon = {
                                Icon(Icons.Filled.HourglassBottom, null, Modifier.size(16.dp))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_timer_chip"),
                        )
                        FilterChip(
                            selected = state.mode == TimerMode.STOPWATCH,
                            onClick = { onModeChange(TimerMode.STOPWATCH) },
                            label = { Text(stringResource(R.string.tab_stopwatch)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Timer, null, Modifier.size(16.dp))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_stopwatch_chip"),
                        )
                    }

                    if (state.mode == TimerMode.TIMER) {
                        val totalMs = (state.presetSeconds * 1000L).coerceAtLeast(1L)
                        val progress = (state.timerRemainingMillis.toFloat() / totalMs.toFloat())
                            .coerceIn(0f, 1f)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = { onAdjustTimer(-15) },
                                modifier = Modifier.testTag("timer_minus_15"),
                            ) {
                                Icon(Icons.Filled.Remove, stringResource(R.string.cd_decrement))
                            }
                            Text(
                                text = formatMmSs(state.timerRemainingMillis),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            IconButton(
                                onClick = { onAdjustTimer(15) },
                                modifier = Modifier.testTag("timer_plus_15"),
                            ) {
                                Icon(Icons.Filled.Add, stringResource(R.string.cd_increment))
                            }
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(30, 60, 90, 120, 180).forEach { secs ->
                                AssistChip(
                                    onClick = { onSelectPreset(secs) },
                                    label = {
                                        Text(if (secs < 60) "${secs}s" else "${secs / 60}m${if (secs % 60 == 0) "" else " ${secs % 60}s"}")
                                    },
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = onToggleTimer,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("timer_start_pause_button"),
                            ) {
                                Icon(
                                    imageVector = if (state.isTimerRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (state.isTimerRunning) stringResource(R.string.btn_pause)
                                    else stringResource(R.string.btn_start)
                                )
                            }
                            OutlinedButton(
                                onClick = onResetTimer,
                                modifier = Modifier.testTag("timer_reset_button"),
                            ) {
                                Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.btn_reset))
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = formatMmSsTenths(state.stopwatchElapsedMillis),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                            state.lastLapMillis?.let { lap ->
                                Text(
                                    text = "Última vuelta: ${formatMmSsTenths(lap)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = onToggleStopwatch,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("stopwatch_start_pause_button"),
                            ) {
                                Icon(
                                    imageVector = if (state.isStopwatchRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (state.isStopwatchRunning) stringResource(R.string.btn_pause)
                                    else stringResource(R.string.btn_start)
                                )
                            }
                            OutlinedButton(
                                onClick = onLapStopwatch,
                                enabled = state.stopwatchElapsedMillis > 0L,
                                modifier = Modifier.testTag("stopwatch_lap_button"),
                            ) {
                                Icon(Icons.Filled.Flag, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.btn_lap))
                            }
                            OutlinedButton(
                                onClick = onResetStopwatch,
                                modifier = Modifier.testTag("stopwatch_reset_button"),
                            ) {
                                Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatMmSs(millis: Long): String {
    val totalSeconds = ((millis + 999L) / 1000L).coerceAtLeast(0L)
    val mins = totalSeconds / 60L
    val secs = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}

fun formatMmSsTenths(millis: Long): String {
    val safe = millis.coerceAtLeast(0L)
    val totalSeconds = safe / 1000L
    val mins = totalSeconds / 60L
    val secs = totalSeconds % 60L
    val tenths = (safe % 1000L) / 100L
    return String.format(Locale.US, "%02d:%02d.%d", mins, secs, tenths)
}

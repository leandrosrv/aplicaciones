package com.example.gamification.minigames

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gamification.data.MiniGameRewardOutcome
import com.example.gamification.engine.AthleteAppearance
import com.example.ui.RestTimerState
import com.example.ui.components.formatMmSs
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

enum class MiniGameKind(
    val title: String,
    val subtitle: String,
    val durationSeconds: Int,
    val icon: ImageVector,
) {
    LIFT_TIMING(
        title = "Ritmo de Fuerza",
        subtitle = "Detén la barra en la zona verde para sumar repeticiones limpias.",
        durationSeconds = 45,
        icon = Icons.Filled.FitnessCenter,
    ),
    JUMP_ROPE(
        title = "Salto a la Soga",
        subtitle = "Salta justo cuando la cuerda pasa abajo antes de que suene el silbato.",
        durationSeconds = 45,
        icon = Icons.Filled.SportsEsports,
    ),
    HYDRATION_CATCH(
        title = "Atrapa Hidratación",
        subtitle = "Mueve a tu atleta entre carriles para juntar agua y evitar distracciones.",
        durationSeconds = 45,
        icon = Icons.Filled.WaterDrop,
    ),
}

@Composable
fun MiniGamesDialog(
    appearance: AthleteAppearance,
    restTimerState: RestTimerState,
    dailyXpUsed: Int,
    dailyXpCap: Int,
    lastOutcome: MiniGameRewardOutcome?,
    initialGame: MiniGameKind? = null,
    onGameFinished: (won: Boolean) -> Unit,
    onClearOutcome: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedGame by remember(initialGame) { mutableStateOf(initialGame) }

    Dialog(
        onDismissRequest = {
            onClearOutcome()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (selectedGame != null) {
                            IconButton(
                                onClick = {
                                    onClearOutcome()
                                    selectedGame = null
                                },
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a lista")
                            }
                        }
                        Column {
                            Text(
                                text = selectedGame?.title ?: "Minijuegos de pausa",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Máximo 45 s · Límite diario $dailyXpUsed/$dailyXpCap XP",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            onClearOutcome()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("minigame_close_button"),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar minijuegos")
                    }
                }

                // Live sync with real rest timer
                if (restTimerState.isTimerRunning || restTimerState.isTimerCompleted) {
                    Surface(
                        color = if (restTimerState.isTimerCompleted) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Filled.HourglassBottom, null, Modifier.size(18.dp))
                                Text(
                                    text = if (restTimerState.isTimerCompleted) {
                                        "¡Descanso terminado! Listo para tu serie"
                                    } else {
                                        "Descanso entre series"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                            Text(
                                text = formatMmSs(restTimerState.timerRemainingMillis),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                }

                val activeGame = selectedGame
                if (activeGame == null) {
                    Text(
                        text = "Juega partidas rápidas entre series sin perder ritmo. El entrenamiento real siempre da más progreso.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    MiniGameKind.entries.forEach { kind ->
                        Card(
                            onClick = {
                                onClearOutcome()
                                selectedGame = kind
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("select_minigame_${kind.name.lowercase()}"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(44.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = kind.icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(kind.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(kind.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                ) {
                                    Text(
                                        text = "${kind.durationSeconds}s",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                } else {
                    when (activeGame) {
                        MiniGameKind.LIFT_TIMING -> LiftTimingMiniGame(
                            primaryColor = appearance.primaryColor,
                            secondaryColor = appearance.secondaryColor,
                            maxSeconds = activeGame.durationSeconds,
                            lastOutcome = lastOutcome,
                            onFinished = onGameFinished,
                            onReplay = onClearOutcome,
                            onBack = {
                                onClearOutcome()
                                selectedGame = null
                            },
                        )
                        MiniGameKind.JUMP_ROPE -> JumpRopeMiniGame(
                            primaryColor = appearance.primaryColor,
                            secondaryColor = appearance.secondaryColor,
                            maxSeconds = activeGame.durationSeconds,
                            lastOutcome = lastOutcome,
                            onFinished = onGameFinished,
                            onReplay = onClearOutcome,
                            onBack = {
                                onClearOutcome()
                                selectedGame = null
                            },
                        )
                        MiniGameKind.HYDRATION_CATCH -> HydrationCatchMiniGame(
                            primaryColor = appearance.primaryColor,
                            secondaryColor = appearance.secondaryColor,
                            maxSeconds = activeGame.durationSeconds,
                            lastOutcome = lastOutcome,
                            onFinished = onGameFinished,
                            onReplay = onClearOutcome,
                            onBack = {
                                onClearOutcome()
                                selectedGame = null
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiftTimingMiniGame(
    primaryColor: Color,
    secondaryColor: Color,
    maxSeconds: Int,
    lastOutcome: MiniGameRewardOutcome?,
    onFinished: (won: Boolean) -> Unit,
    onReplay: () -> Unit,
    onBack: () -> Unit,
) {
    val targetReps = 8
    var roundKey by remember { mutableIntStateOf(0) }
    var timeLeft by remember(roundKey) { mutableIntStateOf(maxSeconds) }
    var score by remember(roundKey) { mutableIntStateOf(0) }
    var combo by remember(roundKey) { mutableIntStateOf(0) }
    var cursor by remember(roundKey) { mutableFloatStateOf(0.1f) }
    var direction by remember(roundKey) { mutableFloatStateOf(1f) }
    var feedback by remember(roundKey) { mutableStateOf("Toca cuando la barra entre en la zona verde") }
    var finished by remember(roundKey) { mutableStateOf(false) }
    var won by remember(roundKey) { mutableStateOf(false) }

    LaunchedEffect(roundKey, finished) {
        if (finished) return@LaunchedEffect
        var tickMs = 0
        while (isActive && !finished) {
            delay(20L)
            tickMs += 20
            val speed = 0.015f + (score * 0.0014f)
            cursor += direction * speed
            if (cursor >= 0.96f) {
                cursor = 0.96f
                direction = -1f
            } else if (cursor <= 0.04f) {
                cursor = 0.04f
                direction = 1f
            }
            if (tickMs >= 1000) {
                tickMs = 0
                timeLeft = (timeLeft - 1).coerceAtLeast(0)
                if (timeLeft == 0) {
                    finished = true
                    won = score >= targetReps
                    onFinished(won)
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MiniGameHeaderRow(timeLeft = timeLeft, score = score, target = targetReps, combo = combo)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp),
        ) {
            val barH = 34f
            val top = (size.height - barH) * 0.5f
            drawRoundRect(
                color = Color(0xFF334155).copy(alpha = 0.25f),
                topLeft = Offset(0f, top),
                size = Size(size.width, barH),
                cornerRadius = CornerRadius(18f, 18f),
            )
            // Sweet spot center zone [0.38 .. 0.62]
            val greenLeft = size.width * 0.38f
            val greenW = size.width * 0.24f
            drawRoundRect(
                color = Color(0xFF10B981).copy(alpha = 0.78f),
                topLeft = Offset(greenLeft, top),
                size = Size(greenW, barH),
                cornerRadius = CornerRadius(12f, 12f),
            )
            // Moving bar needle
            val needleX = size.width * cursor
            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(needleX - 8f, top - 14f),
                size = Size(16f, barH + 28f),
                cornerRadius = CornerRadius(8f, 8f),
            )
        }

        Text(
            text = feedback,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!finished) {
            Button(
                onClick = {
                    if (cursor in 0.36f..0.64f) {
                        score += 1
                        combo += 1
                        feedback = "¡Repetición limpia! ($score/$targetReps)"
                        if (score >= targetReps) {
                            finished = true
                            won = true
                            onFinished(true)
                        }
                    } else {
                        combo = 0
                        feedback = "Casi... espera la zona verde."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("minigame_lift_tap_button"),
            ) {
                Text("¡Empujar ahora!", fontWeight = FontWeight.Bold)
            }
        } else {
            MiniGameResultCard(
                won = won,
                scoreText = "Lograste $score/$targetReps repeticiones limpias",
                outcome = lastOutcome,
                onReplay = {
                    onReplay()
                    roundKey += 1
                },
                onBack = onBack,
            )
        }
    }
}

@Composable
private fun JumpRopeMiniGame(
    primaryColor: Color,
    secondaryColor: Color,
    maxSeconds: Int,
    lastOutcome: MiniGameRewardOutcome?,
    onFinished: (won: Boolean) -> Unit,
    onReplay: () -> Unit,
    onBack: () -> Unit,
) {
    val targetJumps = 12
    var roundKey by remember { mutableIntStateOf(0) }
    var timeLeft by remember(roundKey) { mutableIntStateOf(maxSeconds) }
    var score by remember(roundKey) { mutableIntStateOf(0) }
    var combo by remember(roundKey) { mutableIntStateOf(0) }
    var ropePhase by remember(roundKey) { mutableFloatStateOf(0f) }
    var jumpHeight by remember(roundKey) { mutableFloatStateOf(0f) }
    var feedback by remember(roundKey) { mutableStateOf("Toca ¡Saltar! cuando la cuerda pase abajo") }
    var finished by remember(roundKey) { mutableStateOf(false) }
    var won by remember(roundKey) { mutableStateOf(false) }

    LaunchedEffect(roundKey, finished) {
        if (finished) return@LaunchedEffect
        var tickMs = 0
        while (isActive && !finished) {
            delay(20L)
            tickMs += 20
            ropePhase = (ropePhase + 0.017f + score * 0.0005f) % 1f
            if (jumpHeight > 0f) {
                jumpHeight = (jumpHeight - 0.06f).coerceAtLeast(0f)
            }
            if (tickMs >= 1000) {
                tickMs = 0
                timeLeft = (timeLeft - 1).coerceAtLeast(0)
                if (timeLeft == 0) {
                    finished = true
                    won = score >= targetJumps
                    onFinished(won)
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MiniGameHeaderRow(timeLeft = timeLeft, score = score, target = targetJumps, combo = combo)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp),
        ) {
            val cx = size.width * 0.5f
            val floorY = size.height * 0.85f
            val swing = sin(ropePhase * 2f * PI.toFloat())
            val ropeY = floorY - size.height * 0.35f + swing * size.height * 0.36f
            val inJumpZone = swing > 0.45f

            // Floor marker
            drawLine(
                color = if (inJumpZone) Color(0xFF10B981) else Color.Gray.copy(alpha = 0.4f),
                start = Offset(size.width * 0.2f, floorY),
                end = Offset(size.width * 0.8f, floorY),
                strokeWidth = 6f,
                cap = StrokeCap.Round,
            )

            // Athlete stick figure jumping
            val athleteBaseY = floorY - jumpHeight * size.height * 0.28f
            drawCircle(primaryColor, radius = 18f, center = Offset(cx, athleteBaseY - 68f))
            drawLine(primaryColor, Offset(cx, athleteBaseY - 50f), Offset(cx, athleteBaseY - 18f), strokeWidth = 10f, cap = StrokeCap.Round)
            drawLine(primaryColor, Offset(cx, athleteBaseY - 18f), Offset(cx - 14f, athleteBaseY), strokeWidth = 8f, cap = StrokeCap.Round)
            drawLine(primaryColor, Offset(cx, athleteBaseY - 18f), Offset(cx + 14f, athleteBaseY), strokeWidth = 8f, cap = StrokeCap.Round)

            // Rope curve
            drawLine(
                color = if (inJumpZone) Color(0xFF10B981) else secondaryColor,
                start = Offset(size.width * 0.22f, ropeY),
                end = Offset(size.width * 0.78f, ropeY),
                strokeWidth = 7f,
                cap = StrokeCap.Round,
            )
        }

        Text(feedback, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (!finished) {
            Button(
                onClick = {
                    jumpHeight = 1f
                    val swing = sin(ropePhase * 2f * PI.toFloat())
                    if (swing > 0.35f) {
                        score += 1
                        combo += 1
                        feedback = "¡Buen salto! ($score/$targetJumps)"
                        if (score >= targetJumps) {
                            finished = true
                            won = true
                            onFinished(true)
                        }
                    } else {
                        combo = 0
                        feedback = "Salto a destiempo — espera la línea verde abajo."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("minigame_jump_tap_button"),
            ) {
                Text("¡Saltar!", fontWeight = FontWeight.Bold)
            }
        } else {
            MiniGameResultCard(
                won = won,
                scoreText = "Saltos completados: $score/$targetJumps",
                outcome = lastOutcome,
                onReplay = {
                    onReplay()
                    roundKey += 1
                },
                onBack = onBack,
            )
        }
    }
}

private data class FallingItem(
    val id: Int,
    val lane: Int,
    val yFraction: Float,
    val isGood: Boolean,
)

@Composable
private fun HydrationCatchMiniGame(
    primaryColor: Color,
    secondaryColor: Color,
    maxSeconds: Int,
    lastOutcome: MiniGameRewardOutcome?,
    onFinished: (won: Boolean) -> Unit,
    onReplay: () -> Unit,
    onBack: () -> Unit,
) {
    val targetPoints = 10
    var roundKey by remember { mutableIntStateOf(0) }
    var timeLeft by remember(roundKey) { mutableIntStateOf(maxSeconds) }
    var score by remember(roundKey) { mutableIntStateOf(0) }
    var combo by remember(roundKey) { mutableIntStateOf(0) }
    var playerLane by remember(roundKey) { mutableIntStateOf(1) }
    val items = remember(roundKey) { mutableStateListOf<FallingItem>() }
    var nextId by remember(roundKey) { mutableIntStateOf(1) }
    var finished by remember(roundKey) { mutableStateOf(false) }
    var won by remember(roundKey) { mutableStateOf(false) }

    LaunchedEffect(roundKey, finished) {
        if (finished) return@LaunchedEffect
        var tickMs = 0
        var spawnMs = 0
        while (isActive && !finished) {
            delay(35L)
            tickMs += 35
            spawnMs += 35

            if (spawnMs >= 680) {
                spawnMs = 0
                items += FallingItem(
                    id = nextId++,
                    lane = Random.nextInt(0, 3),
                    yFraction = 0.05f,
                    isGood = Random.nextFloat() < 0.72f,
                )
            }

            val updated = mutableListOf<FallingItem>()
            for (item in items) {
                val ny = item.yFraction + 0.025f
                if (ny in 0.76f..0.92f && item.lane == playerLane) {
                    if (item.isGood) {
                        score += 1
                        combo += 1
                        if (score >= targetPoints) {
                            finished = true
                            won = true
                            onFinished(true)
                        }
                    } else {
                        combo = 0
                        score = (score - 1).coerceAtLeast(0)
                    }
                } else if (ny < 0.96f) {
                    updated += item.copy(yFraction = ny)
                }
            }
            items.clear()
            items.addAll(updated)

            if (tickMs >= 1000 && !finished) {
                tickMs = 0
                timeLeft = (timeLeft - 1).coerceAtLeast(0)
                if (timeLeft == 0) {
                    finished = true
                    won = score >= targetPoints
                    onFinished(won)
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MiniGameHeaderRow(timeLeft = timeLeft, score = score, target = targetPoints, combo = combo)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    MaterialTheme.shapes.large,
                ),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val laneW = size.width / 3f
                // Lane dividers
                for (i in 1..2) {
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.25f),
                        start = Offset(laneW * i, 0f),
                        end = Offset(laneW * i, size.height),
                        strokeWidth = 2f,
                    )
                }
                // Falling items (Blue circle = hydration, Red triangle/square = distraction)
                items.forEach { item ->
                    val cx = laneW * item.lane + laneW * 0.5f
                    val cy = size.height * item.yFraction
                    if (item.isGood) {
                        drawCircle(Color(0xFF0EA5E9), radius = 16f, center = Offset(cx, cy))
                    } else {
                        drawRect(
                            color = Color(0xFFEF4444),
                            topLeft = Offset(cx - 14f, cy - 14f),
                            size = Size(28f, 28f),
                        )
                    }
                }
                // Player basket / athlete marker
                val px = laneW * playerLane + laneW * 0.5f
                val py = size.height * 0.85f
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(px - 28f, py - 14f),
                    size = Size(56f, 24f),
                    cornerRadius = CornerRadius(12f, 12f),
                )
                drawCircle(secondaryColor, radius = 9f, center = Offset(px, py - 24f))
            }

            // Invisible lane tap targets for easy 1-hand tapping
            Row(modifier = Modifier.fillMaxSize()) {
                for (lane in 0..2) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable(enabled = !finished) { playerLane = lane },
                    )
                }
            }
        }

        if (!finished) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { playerLane = (playerLane - 1).coerceAtLeast(0) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("◀ Izquierda")
                }
                OutlinedButton(
                    onClick = { playerLane = (playerLane + 1).coerceAtMost(2) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Derecha ▶")
                }
            }
        } else {
            MiniGameResultCard(
                won = won,
                scoreText = "Hidratación capturada: $score/$targetPoints",
                outcome = lastOutcome,
                onReplay = {
                    onReplay()
                    roundKey += 1
                },
                onBack = onBack,
            )
        }
    }
}

@Composable
private fun MiniGameHeaderRow(
    timeLeft: Int,
    score: Int,
    target: Int,
    combo: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Puntos: $score / $target",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (combo >= 2) {
                Text(
                    text = "Racha x$combo",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "${timeLeft}s",
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (timeLeft <= 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }
        LinearProgressIndicator(
            progress = { (score.toFloat() / target.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
        )
    }
}

@Composable
private fun MiniGameResultCard(
    won: Boolean,
    scoreText: String,
    outcome: MiniGameRewardOutcome?,
    onReplay: () -> Unit,
    onBack: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (won) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.EmojiEvents, null)
                Text(
                    text = if (won) "¡Pausa completada!" else "Tiempo terminado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(scoreText, style = MaterialTheme.typography.bodyMedium)
            if (outcome != null) {
                Text(
                    text = if (outcome.capReached) {
                        "+${outcome.coinsGranted} monedas · Límite diario de XP de minijuegos (${outcome.dailyXpCap} XP) alcanzado."
                    } else {
                        "+${outcome.xpGranted} XP · +${outcome.coinsGranted} monedas (${outcome.dailyXpUsed}/${outcome.dailyXpCap} XP hoy)"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Text("Otros juegos")
                }
                Button(onClick = onReplay, modifier = Modifier.weight(1f)) {
                    Text("Jugar otra vez")
                }
            }
        }
    }
}

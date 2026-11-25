package com.example.gamification.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gamification.engine.AvatarActionAnimation
import com.example.gamification.engine.GamificationSnapshot
import com.example.gamification.minigames.MiniGameKind
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AthleteScreen(
    snapshot: GamificationSnapshot,
    manualAction: AvatarActionAnimation?,
    onToggleGamification: (Boolean) -> Unit,
    onToggleReduceMotion: (Boolean) -> Unit,
    onOpenAnimalSelection: () -> Unit,
    onOpenWizard: () -> Unit,
    onOpenSettings: () -> Unit,
    onTriggerAction: (AvatarActionAnimation) -> Unit,
    onRefreshClock: () -> Unit,
    onOpenMiniGame: (MiniGameKind?) -> Unit,
    onNavigateToLog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Header card with optional Enable / Disable switch
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mi Atleta",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (snapshot.enabled) {
                                    "Capa de juego activa · Tus datos reales siguen intactos"
                                } else {
                                    "Capa de juego pausada · Solo registro clásico activo"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier.testTag("athlete_settings_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Ajustes de Mi Atleta",
                                )
                            }
                            Switch(
                                checked = snapshot.enabled,
                                onCheckedChange = onToggleGamification,
                                modifier = Modifier.testTag("gamification_toggle_switch"),
                            )
                        }
                    }
                }

                if (!snapshot.enabled) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "Modo clásico de entrenamiento",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Desactivaste el avatar y los minijuegos. Tus entrenamientos, series, volumen, 1RM y gráficos continúan funcionando exactamente igual. Puedes volver a activarlo cuando quieras.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = { onToggleGamification(true) },
                                modifier = Modifier.testTag("reactivate_gamification_button"),
                            ) {
                                Text("Activar Mi Atleta")
                            }
                        }
                    }
                    return@Column
                }

                // 1. Animated Avatar Stage + Level + Pet Interactions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = snapshot.appearance.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "${snapshot.appearance.animalSpecies.label} · ${snapshot.appearance.gender.label} · ${snapshot.appearance.sportStyle.label} · ${snapshot.appearance.sportStyle.badgeTitle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = onOpenAnimalSelection,
                                    modifier = Modifier.testTag("open_animal_selection_button"),
                                ) {
                                    Icon(Icons.Filled.Pets, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Animal")
                                }
                                OutlinedButton(
                                    onClick = onOpenWizard,
                                    modifier = Modifier.testTag("edit_athlete_button"),
                                ) {
                                    Icon(Icons.Filled.Edit, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Estilo")
                                }
                            }
                        }

                        // Avatar stage
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                        ) {
                            AthleteAvatarCanvas(
                                appearance = snapshot.appearance,
                                moodState = snapshot.moodState,
                                animation = manualAction ?: snapshot.recommendedAnimation,
                                level = snapshot.levelProgress.level,
                                backgroundTier = snapshot.unlockedBackgroundTier,
                                reduceMotion = snapshot.reduceMotion,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("athlete_avatar_canvas"),
                                onAvatarTap = { onTriggerAction(AvatarActionAnimation.CELEBRATE) },
                            )

                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp),
                            ) {
                                Text(
                                    text = "Nv. ${snapshot.levelProgress.level}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }

                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp),
                            ) {
                                Text(
                                    text = snapshot.moodState.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }

                        Text(
                            text = snapshot.moodState.friendlyMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        // Virtual-pet style simple interactions
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AssistChip(
                                onClick = { onTriggerAction(AvatarActionAnimation.STRETCH) },
                                label = { Text("Estirar") },
                                leadingIcon = { Icon(Icons.Filled.SelfImprovement, null, Modifier.size(16.dp)) },
                            )
                            AssistChip(
                                onClick = { onTriggerAction(AvatarActionAnimation.DRINK_WATER) },
                                label = { Text("Hidratar") },
                                leadingIcon = { Icon(Icons.Filled.LocalDrink, null, Modifier.size(16.dp)) },
                            )
                            AssistChip(
                                onClick = { onTriggerAction(AvatarActionAnimation.DUMBBELL_LIFT) },
                                label = { Text("Mancuerna") },
                                leadingIcon = { Icon(Icons.Filled.FitnessCenter, null, Modifier.size(16.dp)) },
                            )
                            AssistChip(
                                onClick = { onTriggerAction(AvatarActionAnimation.TOWEL_WIPE) },
                                label = { Text("Toalla") },
                                leadingIcon = { Icon(Icons.Filled.AutoAwesome, null, Modifier.size(16.dp)) },
                            )
                        }

                        // Level & XP Progress bar
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Experiencia: ${snapshot.levelProgress.currentLevelXp} / ${snapshot.levelProgress.xpForNextLevel} XP",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Monedas: ${snapshot.virtualCoins}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            AvatarXpProgressBar(
                                levelProgress = snapshot.levelProgress,
                                reduceMotion = snapshot.reduceMotion,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Faltan ${snapshot.levelProgress.xpRemainingToNextLevel} XP para Nv. ${snapshot.levelProgress.level + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = snapshot.levelProgress.nextRewardLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // 2. Reloj Biológico 24h & Estado real derivado
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (snapshot.biologicalClock.needsTrainingToday) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
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
                                Icon(Icons.Filled.AccessTime, null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Reloj biológico (ciclo 24 h)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            AssistChip(
                                onClick = onRefreshClock,
                                label = { Text("Actualizar") },
                                leadingIcon = { Icon(Icons.Filled.Refresh, null, Modifier.size(16.dp)) },
                            )
                        }

                        Text(
                            text = snapshot.biologicalClock.statusTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = snapshot.biologicalClock.statusSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        // Derived real metrics grid (shows "Sin datos suficientes" if none)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            StatPill(
                                icon = Icons.Filled.BatteryChargingFull,
                                label = "Energía / Recup.",
                                value = if (snapshot.recovery.energyPercent != null && snapshot.recovery.recoveryPercent != null) {
                                    "${snapshot.recovery.energyPercent}% · ${snapshot.recovery.recoveryPercent}%"
                                } else {
                                    "Sin datos suficientes"
                                },
                                modifier = Modifier.weight(1f),
                            )
                            StatPill(
                                icon = Icons.Filled.Whatshot,
                                label = "Racha / Constancia",
                                value = if (snapshot.metrics.hasData && snapshot.streak.consistencyPercent != null) {
                                    "${snapshot.streak.currentStreakDays} días · ${snapshot.streak.consistencyPercent}%"
                                } else {
                                    "Sin datos suficientes"
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        if (snapshot.biologicalClock.lastTrainingAtMillis > 0L) {
                            val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            val lastTxt = fmt.format(Date(snapshot.biologicalClock.lastTrainingAtMillis))
                            val nextTxt = fmt.format(Date(snapshot.biologicalClock.nextRecommendedActivityAtMillis))
                            Text(
                                text = "Última sesión: $lastTxt · Próximo ciclo: $nextTxt",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (snapshot.biologicalClock.needsTrainingToday || !snapshot.metrics.hasData) {
                            Button(
                                onClick = onNavigateToLog,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Filled.FitnessCenter, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Ir a registrar entrenamiento")
                            }
                        }
                    }
                }

                // 3. Daily Mission & Post-Set Rest Mini-Games
                snapshot.dailyMission?.let { mission ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = if (mission.completed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                                )
                                Text(
                                    text = "Misión del día: ${mission.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Text(
                                text = mission.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
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
                                Icon(Icons.Filled.SportsEsports, null, tint = MaterialTheme.colorScheme.secondary)
                                Text(
                                    text = "Minijuegos entre series (máx. 1 min)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Text(
                            text = "Para pausas entre series (${snapshot.minigameXpEarnedToday}/${snapshot.minigameDailyXpCap} XP diarios). El progreso principal viene de tus entrenamientos reales.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MiniGameKind.entries.forEach { kind ->
                                OutlinedButton(
                                    onClick = { onOpenMiniGame(kind) },
                                    modifier = Modifier.testTag("athlete_open_${kind.name.lowercase()}"),
                                ) {
                                    Icon(kind.icon, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(kind.title)
                                }
                            }
                        }
                    }
                }

                // 4. Badges & Accessibility option
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Filled.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Insignias reales (${snapshot.badges.count { it.unlocked }}/${snapshot.badges.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            snapshot.badges.forEach { badge ->
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = if (badge.unlocked) {
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            imageVector = if (badge.unlocked) Icons.Filled.EmojiEvents else Icons.Filled.Lock,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Text(badge.title, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Reducir movimiento del avatar",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Switch(
                                checked = snapshot.reduceMotion,
                                onCheckedChange = onToggleReduceMotion,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(icon, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

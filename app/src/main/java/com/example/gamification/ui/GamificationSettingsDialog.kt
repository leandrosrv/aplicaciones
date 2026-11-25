package com.example.gamification.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gamification.engine.AccessoryType
import com.example.gamification.engine.AnimalSpecies
import com.example.gamification.engine.AthleteAppearance
import com.example.gamification.engine.AvatarActionAnimation
import com.example.gamification.engine.AvatarGender
import com.example.gamification.engine.AvatarMoodState
import com.example.gamification.engine.AvatarPalettes
import com.example.gamification.engine.BodyBuild
import com.example.gamification.engine.BottomStyle
import com.example.gamification.engine.GamificationSnapshot
import com.example.gamification.engine.SportStyle
import com.example.gamification.engine.TopStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GamificationSettingsDialog(
    snapshot: GamificationSnapshot,
    isSaving: Boolean,
    levelUpSoundEnabled: Boolean = true,
    onToggleLevelUpSound: (Boolean) -> Unit = {},
    onTestLevelUpSound: () -> Unit = {},
    onSaveSettings: (enabled: Boolean, reduceMotion: Boolean, appearance: AthleteAppearance) -> Unit,
    onOpenAnimalSelection: () -> Unit,
    onOpenFullWizard: () -> Unit,
    onResetGameProgressOnly: () -> Unit,
    onDismiss: () -> Unit,
) {
    var draftEnabled by remember(snapshot.enabled) { mutableStateOf(snapshot.enabled) }
    var draftReduceMotion by remember(snapshot.reduceMotion) { mutableStateOf(snapshot.reduceMotion) }
    var draftAppearance by remember(snapshot.appearance) { mutableStateOf(snapshot.appearance) }
    var confirmReset by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column {
                            Text(
                                text = "Ajustes de Mi Atleta",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Configuración modular y segura",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSaving,
                        modifier = Modifier.testTag("settings_close_button"),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar ajustes")
                    }
                }

                // Data safety notice
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Tus series, peso, volumen, 1RM e historial se guardan aparte y nunca se alteran desde estos ajustes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }

                // Gamification Module On/Off Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Activar módulo de gamificación",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (draftEnabled) {
                                    "Muestra avatar, reloj biológico de 24 h y minijuegos de pausa."
                                } else {
                                    "Solo registro clásico activo. Puedes reactivarlo cuando quieras."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = draftEnabled,
                            onCheckedChange = { draftEnabled = it },
                            modifier = Modifier.testTag("settings_gamification_switch"),
                        )
                    }
                }

                AnimatedVisibility(visible = draftEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Preferencias del avatar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                TextButton(
                                    onClick = onOpenAnimalSelection,
                                    modifier = Modifier.testTag("settings_open_animal_selection_button"),
                                ) {
                                    Icon(Icons.Filled.Pets, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Animal")
                                }
                                TextButton(
                                    onClick = onOpenFullWizard,
                                    modifier = Modifier.testTag("settings_open_wizard_button"),
                                ) {
                                    Icon(Icons.Filled.Palette, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Asistente")
                                }
                            }
                        }

                        // Live preview
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            ),
                        ) {
                            AthleteAvatarCanvas(
                                appearance = draftAppearance,
                                moodState = snapshot.moodState,
                                animation = AvatarActionAnimation.IDLE_BREATHE,
                                level = snapshot.levelProgress.level,
                                backgroundTier = snapshot.unlockedBackgroundTier,
                                reduceMotion = draftReduceMotion,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(165.dp)
                                    .padding(6.dp),
                            )
                        }

                        OutlinedTextField(
                            value = draftAppearance.name,
                            onValueChange = { draftAppearance = draftAppearance.copy(name = it.take(24)) },
                            label = { Text("Nombre del atleta") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_athlete_name_input"),
                        )

                        Text("Género del personaje", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AvatarGender.entries.forEach { gender ->
                                FilterChip(
                                    selected = draftAppearance.gender == gender,
                                    onClick = { draftAppearance = draftAppearance.copy(gender = gender) },
                                    label = { Text(gender.label) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_gender_${gender.id.lowercase()}"),
                                )
                            }
                        }

                        Text("Animal atleta", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = draftAppearance.animalSpecies.stereotypeHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AnimalSpecies.entries.forEach { species ->
                                FilterChip(
                                    selected = draftAppearance.animalSpecies == species,
                                    onClick = { draftAppearance = draftAppearance.copy(animalSpecies = species) },
                                    label = { Text(species.label) },
                                    modifier = Modifier.testTag("settings_species_${species.id.lowercase()}"),
                                )
                            }
                        }

                        Text("Estilo deportivo (solo visual)", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SportStyle.entries.forEach { style ->
                                FilterChip(
                                    selected = draftAppearance.sportStyle == style,
                                    onClick = { draftAppearance = draftAppearance.copy(sportStyle = style) },
                                    label = { Text(style.label) },
                                )
                            }
                        }

                        Text("Complexión", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BodyBuild.entries.forEach { build ->
                                FilterChip(
                                    selected = draftAppearance.bodyBuild == build,
                                    onClick = { draftAppearance = draftAppearance.copy(bodyBuild = build) },
                                    label = { Text(build.label) },
                                )
                            }
                        }

                        Text("Ropa de entrenamiento", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TopStyle.entries.forEach { top ->
                                FilterChip(
                                    selected = draftAppearance.topStyle == top,
                                    onClick = { draftAppearance = draftAppearance.copy(topStyle = top) },
                                    label = { Text(top.label) },
                                )
                            }
                            BottomStyle.entries.forEach { bottom ->
                                FilterChip(
                                    selected = draftAppearance.bottomStyle == bottom,
                                    onClick = { draftAppearance = draftAppearance.copy(bottomStyle = bottom) },
                                    label = { Text(bottom.label) },
                                )
                            }
                        }

                        SettingsColorRow(
                            title = "Color principal",
                            items = AvatarPalettes.outfitColors,
                            selectedIndex = draftAppearance.primaryColorIndex,
                            onSelect = { draftAppearance = draftAppearance.copy(primaryColorIndex = it) },
                        )

                        SettingsColorRow(
                            title = "Color secundario",
                            items = AvatarPalettes.outfitColors,
                            selectedIndex = draftAppearance.secondaryColorIndex,
                            onSelect = { draftAppearance = draftAppearance.copy(secondaryColorIndex = it) },
                        )

                        Text("Accesorios deportivos", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AccessoryType.entries.forEach { acc ->
                                val equipped = acc in draftAppearance.accessories
                                FilterChip(
                                    selected = equipped,
                                    onClick = {
                                        val next = draftAppearance.accessories.toMutableSet()
                                        if (equipped) next.remove(acc) else next.add(acc)
                                        draftAppearance = draftAppearance.copy(accessories = next)
                                    },
                                    label = { Text(acc.label) },
                                    leadingIcon = if (equipped) {
                                        { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }
                                    } else null,
                                )
                            }
                        }

                        // Accessibility reduce motion
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Reducir movimiento del avatar", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "Mantiene poses estáticas y desactiva bucles de animación.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = draftReduceMotion,
                                onCheckedChange = { draftReduceMotion = it },
                                modifier = Modifier.testTag("settings_reduce_motion_switch"),
                            )
                        }

                        // Level-up sound effect preference + preview button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sonido al subir de nivel", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "Reproduce un efecto festivo cuando tu atleta sube de nivel.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onTestLevelUpSound,
                                    enabled = levelUpSoundEnabled,
                                    modifier = Modifier.testTag("settings_test_levelup_sound_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.VolumeUp,
                                        contentDescription = "Probar sonido de subida de nivel",
                                    )
                                }
                                Switch(
                                    checked = levelUpSoundEnabled,
                                    onCheckedChange = onToggleLevelUpSound,
                                    modifier = Modifier.testTag("settings_levelup_sound_switch"),
                                )
                            }
                        }

                        HorizontalDivider()

                        // Isolated game progress reset (never touches workout_sets)
                        if (!confirmReset) {
                            OutlinedButton(
                                onClick = { confirmReset = true },
                                enabled = !isSaving,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_reset_progress_button"),
                            ) {
                                Icon(Icons.Filled.RestartAlt, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Reiniciar solo progreso del juego (XP/nivel)")
                            }
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = "¿Reiniciar XP, nivel y monedas del minijuego? Tus entrenamientos reales no se borrarán.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { confirmReset = false },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text("Cancelar")
                                        }
                                        Button(
                                            onClick = {
                                                confirmReset = false
                                                onResetGameProgressOnly()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError,
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("settings_confirm_reset_button"),
                                        ) {
                                            Text("Confirmar")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Save / Cancel actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            onSaveSettings(draftEnabled, draftReduceMotion, draftAppearance)
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_save_preferences_button"),
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Filled.Save, null, Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Guardar ajustes")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsColorRow(
    title: String,
    items: List<Pair<String, Color>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val currentName = items.getOrNull(selectedIndex)?.first.orEmpty()
        Text(text = "$title: $currentName", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items.forEachIndexed { idx, (label, color) ->
                val isSelected = idx == selectedIndex
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = CircleShape,
                        )
                        .clickable { onSelect(idx) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = label,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

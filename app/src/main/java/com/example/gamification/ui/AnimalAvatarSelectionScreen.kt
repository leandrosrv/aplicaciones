package com.example.gamification.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gamification.engine.AnimalSpecies
import com.example.gamification.engine.AthleteAppearance
import com.example.gamification.engine.AvatarActionAnimation
import com.example.gamification.engine.AvatarGender
import com.example.gamification.engine.AvatarMoodState
import com.example.gamification.engine.SportStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimalAvatarSelectionScreen(
    currentAppearance: AthleteAppearance,
    level: Int,
    backgroundTier: Int,
    reduceMotion: Boolean,
    isSaving: Boolean,
    onSaveSelection: (animal: AnimalSpecies, gender: AvatarGender, sportStyle: SportStyle) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = !isSaving, onBack = onBack)

    var selectedAnimal by remember(currentAppearance.animalSpecies) {
        mutableStateOf(currentAppearance.animalSpecies)
    }
    var selectedGender by remember(currentAppearance.gender) {
        mutableStateOf(currentAppearance.gender)
    }
    var selectedSport by remember(currentAppearance.sportStyle) {
        mutableStateOf(currentAppearance.sportStyle)
    }
    var previewAction by remember { mutableStateOf(AvatarActionAnimation.IDLE_BREATHE) }

    val previewAppearance = remember(currentAppearance, selectedAnimal, selectedGender, selectedSport) {
        currentAppearance.copy(
            animalSpecies = selectedAnimal,
            gender = selectedGender,
            sportStyle = selectedSport,
            skinToneIndex = selectedAnimal.recommendedCoatIndex,
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columnCount = if (maxWidth >= 600.dp) 3 else 2

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
                    // Header with Back action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconButton(
                            onClick = onBack,
                            enabled = !isSaving,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("selection_back_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver a Mi Atleta",
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Elige tu Atleta Animal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Selecciona especie animada, género y deporte",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Live Hero Animated Stage
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(225.dp),
                            ) {
                                AthleteAvatarCanvas(
                                    appearance = previewAppearance,
                                    moodState = AvatarMoodState.PREPARADO,
                                    animation = previewAction,
                                    level = level,
                                    backgroundTier = backgroundTier,
                                    reduceMotion = reduceMotion,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("selection_hero_canvas"),
                                    onAvatarTap = { previewAction = AvatarActionAnimation.CELEBRATE },
                                )

                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp),
                                ) {
                                    Text(
                                        text = "${selectedAnimal.label} · ${selectedGender.label}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }

                            Text(
                                text = "${selectedAnimal.stereotypeHint} · Estereotipo ${selectedSport.label}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                AssistChip(
                                    onClick = { previewAction = AvatarActionAnimation.DUMBBELL_LIFT },
                                    label = { Text("Pesas") },
                                    leadingIcon = { Icon(Icons.Filled.FitnessCenter, null, Modifier.size(16.dp)) },
                                )
                                AssistChip(
                                    onClick = { previewAction = AvatarActionAnimation.JOG_IN_PLACE },
                                    label = { Text("Trote") },
                                    leadingIcon = { Icon(Icons.Filled.DirectionsRun, null, Modifier.size(16.dp)) },
                                )
                                AssistChip(
                                    onClick = { previewAction = AvatarActionAnimation.STRETCH },
                                    label = { Text("Estirar") },
                                    leadingIcon = { Icon(Icons.Filled.SelfImprovement, null, Modifier.size(16.dp)) },
                                )
                                AssistChip(
                                    onClick = { previewAction = AvatarActionAnimation.CELEBRATE },
                                    label = { Text("Celebrar") },
                                    leadingIcon = { Icon(Icons.Filled.EmojiEvents, null, Modifier.size(16.dp)) },
                                )
                            }
                        }
                    }

                    // 1. Gender Configuration Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "1. Configurar género",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                AvatarGender.entries.forEach { gender ->
                                    val isSelected = selectedGender == gender
                                    Surface(
                                        onClick = { selectedGender = gender },
                                        shape = MaterialTheme.shapes.large,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        },
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            },
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .minimumInteractiveComponentSize()
                                            .testTag("selection_gender_${gender.id.lowercase()}"),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                        ) {
                                            Icon(
                                                imageVector = if (gender == AvatarGender.MALE) {
                                                    Icons.Filled.Male
                                                } else {
                                                    Icons.Filled.Female
                                                },
                                                contentDescription = null,
                                                tint = if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = gender.label,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                            )
                                            if (isSelected) {
                                                Spacer(Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = "Seleccionado",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Animated Animal Grid Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "2. Elige tu animal atleta",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Todos entrenan con estética deportiva y cambian según el género elegido.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            val rows = AnimalSpecies.entries.chunked(columnCount)
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                rows.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        rowItems.forEach { species ->
                                            val isSelected = selectedAnimal == species
                                            AnimalChoiceCard(
                                                species = species,
                                                baseAppearance = currentAppearance,
                                                selectedGender = selectedGender,
                                                selectedSport = selectedSport,
                                                isSelected = isSelected,
                                                onClick = { selectedAnimal = species },
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                        repeat(columnCount - rowItems.size) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Quick Sport Stereotype Selector
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "3. Estereotipo de deporte",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                SportStyle.entries.forEach { sport ->
                                    val selected = selectedSport == sport
                                    FilterChip(
                                        selected = selected,
                                        onClick = { selectedSport = sport },
                                        label = { Text(sport.label) },
                                        leadingIcon = if (selected) {
                                            { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }
                                        } else null,
                                    )
                                }
                            }
                        }
                    }

                    // Save / Cancel Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            enabled = !isSaving,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                        ) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = { onSaveSelection(selectedAnimal, selectedGender, selectedSport) },
                            enabled = !isSaving,
                            modifier = Modifier
                                .weight(1.4f)
                                .height(50.dp)
                                .testTag("selection_save_button"),
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(Icons.Filled.Save, null, Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("Guardar selección", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimalChoiceCard(
    species: AnimalSpecies,
    baseAppearance: AthleteAppearance,
    selectedGender: AvatarGender,
    selectedSport: SportStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbAppearance = remember(baseAppearance, species, selectedGender, selectedSport) {
        baseAppearance.copy(
            animalSpecies = species,
            gender = selectedGender,
            sportStyle = selectedSport,
            skinToneIndex = species.recommendedCoatIndex,
        )
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("selection_animal_${species.id.lowercase()}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
            },
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AthleteAvatarCanvas(
                appearance = thumbAppearance,
                moodState = AvatarMoodState.PREPARADO,
                animation = AvatarActionAnimation.IDLE_BREATHE,
                level = 1,
                backgroundTier = 1,
                reduceMotion = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp),
            )
            Text(
                text = species.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = species.stereotypeHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

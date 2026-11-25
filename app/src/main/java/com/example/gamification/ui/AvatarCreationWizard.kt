package com.example.gamification.ui

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.gamification.engine.FacialHairStyle
import com.example.gamification.engine.HairStyle
import com.example.gamification.engine.SportStyle
import com.example.gamification.engine.TopStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AvatarCreationWizardDialog(
    initialAppearance: AthleteAppearance,
    level: Int,
    reduceMotion: Boolean,
    onDismiss: (() -> Unit)?,
    onSave: (AthleteAppearance) -> Unit,
) {
    var draft by remember(initialAppearance) { mutableStateOf(initialAppearance) }
    var step by remember { mutableIntStateOf(0) }
    val stepTitles = listOf(
        "1. Nombre, animal y estilo",
        "2. Pelaje, rasgos y pelo",
        "3. Ropa y colores",
        "4. Accesorios",
    )

    Dialog(
        onDismissRequest = { onDismiss?.invoke() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Personaliza tu Atleta",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stepTitles[step],
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (onDismiss != null) {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("wizard_close_button")) {
                            Icon(Icons.Filled.Close, contentDescription = "Cerrar asistente")
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { (step + 1) / 4f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                )

                // Live animated preview of the customized character
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                ) {
                    AthleteAvatarCanvas(
                        appearance = draft,
                        moodState = AvatarMoodState.PREPARADO,
                        animation = AvatarActionAnimation.IDLE_BREATHE,
                        level = level,
                        backgroundTier = 2,
                        reduceMotion = reduceMotion,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .padding(8.dp),
                    )
                }

                when (step) {
                    0 -> {
                        OutlinedTextField(
                            value = draft.name,
                            onValueChange = { draft = draft.copy(name = it.take(24)) },
                            label = { Text("Nombre del atleta") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wizard_athlete_name"),
                        )

                        Text("Género del personaje", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AvatarGender.entries.forEach { gender ->
                                FilterChip(
                                    selected = draft.gender == gender,
                                    onClick = { draft = draft.copy(gender = gender) },
                                    label = { Text(gender.label) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("wizard_gender_${gender.id.lowercase()}"),
                                )
                            }
                        }

                        Text("Animal atleta", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = draft.animalSpecies.stereotypeHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AnimalSpecies.entries.forEach { species ->
                                FilterChip(
                                    selected = draft.animalSpecies == species,
                                    onClick = { draft = draft.copy(animalSpecies = species) },
                                    label = { Text(species.label) },
                                    modifier = Modifier.testTag("wizard_species_${species.id.lowercase()}"),
                                )
                            }
                        }

                        Text("Deporte / entrenamiento (estereotipo visual)", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SportStyle.entries.forEach { style ->
                                FilterChip(
                                    selected = draft.sportStyle == style,
                                    onClick = { draft = draft.copy(sportStyle = style) },
                                    label = { Text(style.label) },
                                )
                            }
                        }

                        Text("Complexión base", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BodyBuild.entries.forEach { build ->
                                FilterChip(
                                    selected = draft.bodyBuild == build,
                                    onClick = { draft = draft.copy(bodyBuild = build) },
                                    label = { Text(build.label) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    1 -> {
                        ColorSwatchRow(
                            title = "Pelaje / plumaje",
                            items = AvatarPalettes.skinTones,
                            selectedIndex = draft.skinToneIndex,
                            onSelect = { draft = draft.copy(skinToneIndex = it) },
                        )

                        ColorSwatchRow(
                            title = "Color de ojos",
                            items = AvatarPalettes.eyeColors,
                            selectedIndex = draft.eyeColorIndex,
                            onSelect = { draft = draft.copy(eyeColorIndex = it) },
                        )

                        Text("Estilo de pelo / cresta", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            HairStyle.entries.forEach { hair ->
                                FilterChip(
                                    selected = draft.hairStyle == hair,
                                    onClick = { draft = draft.copy(hairStyle = hair) },
                                    label = { Text(hair.label) },
                                )
                            }
                        }

                        ColorSwatchRow(
                            title = "Color de melena y barba",
                            items = AvatarPalettes.hairColors,
                            selectedIndex = draft.hairColorIndex,
                            onSelect = { draft = draft.copy(hairColorIndex = it) },
                        )

                        Text("Barba o bigote", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FacialHairStyle.entries.forEach { fh ->
                                FilterChip(
                                    selected = draft.facialHair == fh,
                                    onClick = { draft = draft.copy(facialHair = fh) },
                                    label = { Text(fh.label) },
                                )
                            }
                        }
                    }

                    2 -> {
                        Text("Parte superior", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TopStyle.entries.forEach { top ->
                                FilterChip(
                                    selected = draft.topStyle == top,
                                    onClick = { draft = draft.copy(topStyle = top) },
                                    label = { Text(top.label) },
                                )
                            }
                        }

                        Text("Parte inferior", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            BottomStyle.entries.forEach { bottom ->
                                FilterChip(
                                    selected = draft.bottomStyle == bottom,
                                    onClick = { draft = draft.copy(bottomStyle = bottom) },
                                    label = { Text(bottom.label) },
                                )
                            }
                        }

                        ColorSwatchRow(
                            title = "Color principal",
                            items = AvatarPalettes.outfitColors,
                            selectedIndex = draft.primaryColorIndex,
                            onSelect = { draft = draft.copy(primaryColorIndex = it) },
                        )

                        ColorSwatchRow(
                            title = "Color secundario",
                            items = AvatarPalettes.outfitColors,
                            selectedIndex = draft.secondaryColorIndex,
                            onSelect = { draft = draft.copy(secondaryColorIndex = it) },
                        )

                        ColorSwatchRow(
                            title = "Zapatillas",
                            items = AvatarPalettes.shoeColors,
                            selectedIndex = draft.shoeColorIndex,
                            onSelect = { draft = draft.copy(shoeColorIndex = it) },
                        )
                    }

                    3 -> {
                        Text(
                            text = "Combina libremente accesorios deportivos",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AccessoryType.entries.forEach { acc ->
                                val equipped = acc in draft.accessories
                                FilterChip(
                                    selected = equipped,
                                    onClick = {
                                        val next = draft.accessories.toMutableSet()
                                        if (equipped) next.remove(acc) else next.add(acc)
                                        draft = draft.copy(accessories = next)
                                    },
                                    label = { Text(acc.label) },
                                    leadingIcon = if (equipped) {
                                        { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }
                                    } else null,
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (step > 0) {
                        OutlinedButton(
                            onClick = { step -= 1 },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("wizard_prev_button"),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Anterior")
                        }
                    }

                    if (step < 3) {
                        Button(
                            onClick = { step += 1 },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("wizard_next_button"),
                        ) {
                            Text("Siguiente")
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
                        }
                    } else {
                        Button(
                            onClick = { onSave(draft) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("wizard_save_button"),
                        ) {
                            Icon(Icons.Filled.Check, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Guardar atleta")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatchRow(
    title: String,
    items: List<Pair<String, Color>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val currentName = items.getOrNull(selectedIndex)?.first.orEmpty()
        Text(
            text = "$title: $currentName",
            style = MaterialTheme.typography.labelLarge,
        )
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
                            tint = if (
                                color == Color(0xFFE2E8F0) ||
                                color == Color(0xFFEBD9C3) ||
                                color == Color(0xFFE3A857)
                            ) {
                                Color.Black
                            } else {
                                Color.White
                            },
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

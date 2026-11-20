package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.WorkoutSet
import com.example.ui.UiState
import java.util.Locale

@Composable
fun HistoryScreen(
    setsState: UiState<List<WorkoutSet>>,
    muscleGroups: List<String>,
    selectedGroupFilter: String?,
    searchQuery: String,
    onGroupFilterChange: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onDeleteSet: (Long) -> Unit,
    onUpdateSet: (
        setId: Long,
        createdAtMillis: Long,
        fecha: String,
        grupo: String,
        ejercicio: String,
        serie: Long,
        peso: Double,
        reps: Long,
        rir: Long,
        pesoCorporal: Double?,
        obs: String?,
        onDone: () -> Unit,
    ) -> Unit,
    onSaveImage: () -> Unit,
    onOpenPdfReport: () -> Unit,
    onExportCsv: (List<WorkoutSet>) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearConfirm by remember { mutableStateOf(false) }
    var editingSet by remember { mutableStateOf<WorkoutSet?>(null) }

    when (setsState) {
        UiState.Loading -> {
            Column(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        is UiState.Error -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = setsState.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }

        is UiState.Success -> {
            val allSets = setsState.data
            val filtered = remember(allSets, selectedGroupFilter, searchQuery) {
                val query = searchQuery.trim().lowercase(Locale.getDefault())
                allSets.filter { item ->
                    val matchesGroup = selectedGroupFilter == null ||
                        item.grupo.equals(selectedGroupFilter, ignoreCase = true)
                    val matchesQuery = query.isEmpty() ||
                        item.ejercicio.lowercase(Locale.getDefault()).contains(query) ||
                        item.grupo.lowercase(Locale.getDefault()).contains(query) ||
                        item.obs.orEmpty().lowercase(Locale.getDefault()).contains(query)
                    matchesGroup && matchesQuery
                }
            }

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 840.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${stringResource(R.string.section_logged_sets)} (${filtered.size})",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text(stringResource(R.string.search_exercise_hint)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = stringResource(R.string.btn_cancel),
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("history_search_input"),
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = selectedGroupFilter == null,
                                onClick = { onGroupFilterChange(null) },
                                label = { Text(stringResource(R.string.filter_all_groups)) },
                                modifier = Modifier.testTag("history_filter_all"),
                            )
                            muscleGroups.forEach { group ->
                                FilterChip(
                                    selected = selectedGroupFilter.equals(group, ignoreCase = true),
                                    onClick = { onGroupFilterChange(group) },
                                    label = { Text(group) },
                                )
                            }
                        }

                        // Save Image, PDF Report, Export CSV & Clear row
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Button(
                                    onClick = onSaveImage,
                                    enabled = allSets.isNotEmpty(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("history_save_image_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.btn_image_short))
                                }
                                Button(
                                    onClick = onOpenPdfReport,
                                    enabled = allSets.isNotEmpty(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("history_pdf_report_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PictureAsPdf,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.btn_pdf_report))
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { onExportCsv(allSets) },
                                    enabled = allSets.isNotEmpty(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("export_csv_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.btn_export_csv))
                                }
                                OutlinedButton(
                                    onClick = { showClearConfirm = true },
                                    enabled = allSets.isNotEmpty(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("clear_log_button"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.btn_clear_log))
                                }
                            }
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .widthIn(max = 840.dp)
                                .fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp),
                                )
                                Text(
                                    text = stringResource(R.string.empty_history_title),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = stringResource(R.string.empty_history_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { item ->
                        WorkoutSetCard(
                            item = item,
                            onEdit = { editingSet = item },
                            onDelete = { onDeleteSet(item.id) },
                            modifier = Modifier
                                .widthIn(max = 840.dp)
                                .fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.dialog_clear_title)) },
            text = { Text(stringResource(R.string.dialog_clear_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirm = false
                        onClearAll()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    modifier = Modifier.testTag("confirm_clear_button"),
                ) {
                    Text(stringResource(R.string.btn_delete_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        )
    }

    editingSet?.let { target ->
        EditSetDialog(
            item = target,
            muscleGroups = muscleGroups,
            onDismiss = { editingSet = null },
            onSave = { fecha, grupo, ejercicio, serie, peso, reps, rir, pesoCorporal, obs ->
                onUpdateSet(
                    target.id,
                    target.createdAtMillis,
                    fecha,
                    grupo,
                    ejercicio,
                    serie,
                    peso,
                    reps,
                    rir,
                    pesoCorporal,
                    obs,
                ) {
                    editingSet = null
                }
            },
        )
    }
}

@Composable
private fun WorkoutSetCard(
    item: WorkoutSet,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = item.grupo,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    Text(
                        text = item.fecha,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_set_${item.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.cd_edit_set),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_set_${item.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = stringResource(R.string.cd_delete_set),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${item.ejercicio} · Serie ${item.serie}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${formatCleanNum(item.peso)} kg × ${item.reps} reps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "RIR ${item.rir}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Vol: ${String.format(Locale.US, "%.1f", item.volumeKg)} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "1RM: ${String.format(Locale.US, "%.1f", item.estimated1RmKg)} kg",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                if (item.pesoCorporal != null) {
                    Text(
                        text = "PC: ${formatCleanNum(item.pesoCorporal)} kg",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!item.obs.isNullOrBlank()) {
                Text(
                    text = item.obs,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EditSetDialog(
    item: WorkoutSet,
    muscleGroups: List<String>,
    onDismiss: () -> Unit,
    onSave: (
        fecha: String,
        grupo: String,
        ejercicio: String,
        serie: Long,
        peso: Double,
        reps: Long,
        rir: Long,
        pesoCorporal: Double?,
        obs: String?,
    ) -> Unit,
) {
    var fecha by remember { mutableStateOf(item.fecha) }
    var grupo by remember { mutableStateOf(item.grupo) }
    var ejercicio by remember { mutableStateOf(item.ejercicio) }
    var serieText by remember { mutableStateOf(item.serie.toString()) }
    var pesoText by remember { mutableStateOf(formatCleanNum(item.peso)) }
    var repsText by remember { mutableStateOf(item.reps.toString()) }
    var rir by remember { mutableIntStateOf(item.rir.toInt().coerceIn(0, 5)) }
    var pesoCorporalText by remember { mutableStateOf(item.pesoCorporal?.let { formatCleanNum(it) } ?: "") }
    var obs by remember { mutableStateOf(item.obs.orEmpty()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.section_edit_set)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    muscleGroups.forEach { g ->
                        FilterChip(
                            selected = grupo.equals(g, ignoreCase = true),
                            onClick = { grupo = g },
                            label = { Text(g) },
                        )
                    }
                }
                OutlinedTextField(
                    value = ejercicio,
                    onValueChange = { ejercicio = it },
                    label = { Text(stringResource(R.string.label_exercise)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fecha,
                        onValueChange = { fecha = it },
                        label = { Text(stringResource(R.string.label_date)) },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f),
                    )
                    OutlinedTextField(
                        value = serieText,
                        onValueChange = { serieText = it },
                        label = { Text("Serie") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.7f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pesoText,
                        onValueChange = { pesoText = it },
                        label = { Text(stringResource(R.string.label_weight_kg)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = { repsText = it },
                        label = { Text(stringResource(R.string.label_reps)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    (0..5).forEach { value ->
                        FilterChip(
                            selected = rir == value,
                            onClick = { rir = value },
                            label = { Text("$value") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                OutlinedTextField(
                    value = pesoCorporalText,
                    onValueChange = { pesoCorporalText = it },
                    label = { Text(stringResource(R.string.label_body_weight)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = obs,
                    onValueChange = { obs = it },
                    label = { Text(stringResource(R.string.label_observation)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (errorText != null) {
                    Text(
                        text = errorText.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ex = ejercicio.trim()
                    val ser = serieText.toLongOrNull()
                    val wt = pesoText.replace(',', '.').toDoubleOrNull()
                    val rp = repsText.toLongOrNull()
                    val bw = pesoCorporalText.trim().takeIf { it.isNotEmpty() }?.replace(',', '.')?.toDoubleOrNull()
                    when {
                        !Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(fecha.trim()) ->
                            errorText = "Fecha inválida (usa AAAA-MM-DD)."
                        ex.isEmpty() ->
                            errorText = "El ejercicio es obligatorio."
                        ser == null || ser !in 1L..999L ->
                            errorText = "Serie inválida (1 a 999)."
                        wt == null || wt < 0.0 || wt > 2000.0 ->
                            errorText = "Peso inválido (0 a 2000 kg)."
                        rp == null || rp !in 1L..100L ->
                            errorText = "Repeticiones inválidas (1 a 100)."
                        pesoCorporalText.trim().isNotEmpty() && (bw == null || bw < 0.0 || bw > 500.0) ->
                            errorText = "Peso corporal inválido (0 a 500 kg)."
                        else -> onSave(
                            fecha.trim(),
                            grupo.trim(),
                            ex,
                            ser,
                            wt,
                            rp,
                            rir.toLong(),
                            bw,
                            obs.trim().takeIf { it.isNotEmpty() },
                        )
                    }
                },
            ) {
                Text(stringResource(R.string.btn_save_changes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        },
    )
}

private fun formatCleanNum(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
}

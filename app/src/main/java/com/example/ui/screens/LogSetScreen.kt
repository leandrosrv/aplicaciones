package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PlanillaRowInput
import com.example.data.WorkoutSet
import com.example.data.WorkoutSummary
import com.example.gamification.engine.GamificationSnapshot
import com.example.gamification.ui.HomeAvatarXpBannerCard
import com.example.ui.EntryMode
import com.example.ui.RestTimerState
import com.example.ui.SummaryScope
import com.example.ui.TimerMode
import com.example.ui.WorkoutFormState
import com.example.ui.components.RestTimerPanel
import com.example.ui.components.SummaryMetricsCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSetScreen(
    entryMode: EntryMode,
    formState: WorkoutFormState,
    planillaRows: List<PlanillaRowInput>,
    isSavingPlanilla: Boolean,
    allSets: List<WorkoutSet>,
    muscleGroups: List<String>,
    recentExercisesForGroup: List<String>,
    summary: WorkoutSummary,
    summaryScope: SummaryScope,
    restTimerState: RestTimerState,
    canSaveImage: Boolean,
    onEntryModeChange: (EntryMode) -> Unit,
    onOpenSaveImage: () -> Unit,
    onOpenPdfReport: () -> Unit,
    onSummaryScopeChange: (SummaryScope) -> Unit,
    onFechaChange: (String) -> Unit,
    onGrupoChange: (String) -> Unit,
    onEjercicioChange: (String) -> Unit,
    onSerieChange: (Int) -> Unit,
    onPesoChange: (String) -> Unit,
    onNudgePeso: (Double) -> Unit,
    onRepsChange: (String) -> Unit,
    onNudgeReps: (Int) -> Unit,
    onRirChange: (Int) -> Unit,
    onPesoCorporalChange: (String) -> Unit,
    onObsChange: (String) -> Unit,
    onSubmitSet: () -> Unit,
    onAddCustomGroup: (String) -> Unit,
    onAddPlanillaRow: () -> Unit,
    onDuplicatePlanillaRow: (Long) -> Unit,
    onRemovePlanillaRow: (Long) -> Unit,
    onUpdatePlanillaRow: (Long, (PlanillaRowInput) -> PlanillaRowInput) -> Unit,
    onLoadPreviousSession: () -> Unit,
    onSubmitPlanilla: () -> Unit,
    onTimerModeChange: (TimerMode) -> Unit,
    onSelectTimerPreset: (Int) -> Unit,
    onAdjustTimer: (Int) -> Unit,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onToggleStopwatch: () -> Unit,
    onLapStopwatch: () -> Unit,
    onResetStopwatch: () -> Unit,
    onOpenMiniGames: (() -> Unit)? = null,
    gamificationSnapshot: GamificationSnapshot? = null,
    onOpenAthleteTab: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddGroupDialog by remember { mutableStateOf(false) }

    val previousReferences = remember(allSets, formState.grupo, formState.fecha) {
        allSets
            .filter { it.grupo.equals(formState.grupo, ignoreCase = true) && it.fecha < formState.fecha }
            .ifEmpty { allSets.filter { it.grupo.equals(formState.grupo, ignoreCase = true) } }
    }

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
                if (gamificationSnapshot != null && gamificationSnapshot.enabled) {
                    HomeAvatarXpBannerCard(
                        snapshot = gamificationSnapshot,
                        onOpenAthlete = onOpenAthleteTab,
                    )
                }

                SummaryMetricsCard(
                    summary = summary,
                    scope = summaryScope,
                    onScopeChange = onSummaryScopeChange,
                    onSaveImageClick = onOpenSaveImage,
                    onOpenPdfReportClick = onOpenPdfReport,
                    canSaveImage = canSaveImage,
                )

                RestTimerPanel(
                    state = restTimerState,
                    onModeChange = onTimerModeChange,
                    onSelectPreset = onSelectTimerPreset,
                    onAdjustTimer = onAdjustTimer,
                    onToggleTimer = onToggleTimer,
                    onResetTimer = onResetTimer,
                    onToggleStopwatch = onToggleStopwatch,
                    onLapStopwatch = onLapStopwatch,
                    onResetStopwatch = onResetStopwatch,
                    onOpenMiniGames = onOpenMiniGames,
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = entryMode == EntryMode.SHEET,
                        onClick = { onEntryModeChange(EntryMode.SHEET) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("mode_sheet_button"),
                    ) {
                        Text(stringResource(R.string.mode_sheet))
                    }
                    SegmentedButton(
                        selected = entryMode == EntryMode.SINGLE,
                        onClick = { onEntryModeChange(EntryMode.SINGLE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("mode_single_button"),
                    ) {
                        Text(stringResource(R.string.mode_single))
                    }
                }

                // Shared header card: Date, Muscle Group, Body Weight
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                            Text(
                                text = if (entryMode == EntryMode.SHEET) {
                                    stringResource(R.string.sheet_title)
                                } else {
                                    stringResource(R.string.section_new_set)
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            OutlinedButton(
                                onClick = { showDatePicker = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("date_picker_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarMonth,
                                    contentDescription = stringResource(R.string.cd_pick_date),
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(formState.fecha)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.label_muscle_group),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                muscleGroups.forEach { group ->
                                    val selected = formState.grupo.equals(group, ignoreCase = true)
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onGrupoChange(group) },
                                        label = { Text(group) },
                                        leadingIcon = if (selected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        } else null,
                                        modifier = Modifier.testTag("group_chip_$group"),
                                    )
                                }
                                AssistChip(
                                    onClick = { showAddGroupDialog = true },
                                    label = { Text(stringResource(R.string.btn_add_group)) },
                                    modifier = Modifier.testTag("add_group_chip"),
                                )
                            }
                        }

                        OutlinedTextField(
                            value = formState.pesoCorporalText,
                            onValueChange = onPesoCorporalChange,
                            label = { Text(stringResource(R.string.label_body_weight)) },
                            placeholder = { Text(stringResource(R.string.placeholder_body_weight)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("body_weight_input"),
                        )

                        if (entryMode == EntryMode.SHEET) {
                            OutlinedButton(
                                onClick = onLoadPreviousSession,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("load_previous_session_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("${stringResource(R.string.sheet_load_previous)} (${formState.grupo})")
                            }
                        }
                    }
                }
            }
        }

        if (entryMode == EntryMode.SHEET) {
            // Goal progress overview banner for the planilla
            item {
                val doneRows = planillaRows.filter { it.isDone }
                val currentVol = planillaRows.filter { it.isDone }.sumOf { row ->
                    val w = row.pesoText.replace(',', '.').toDoubleOrNull() ?: 0.0
                    val r = row.repsText.toLongOrNull() ?: 0L
                    w * r
                }
                val targetVol = planillaRows.sumOf { row ->
                    val tw = row.targetPeso
                    val tr = row.targetReps
                    if (tw != null && tr != null) tw * tr else {
                        val ref = findMatchingTarget(row, previousReferences)
                        if (ref != null) ref.peso * ref.reps else 0.0
                    }
                }
                val progressFraction = if (planillaRows.isNotEmpty()) {
                    doneRows.size.toFloat() / planillaRows.size.toFloat()
                } else 0f

                Card(
                    modifier = Modifier
                        .widthIn(max = 840.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
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
                            Icon(
                                imageVector = Icons.Filled.TrackChanges,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(
                                    R.string.sheet_goal_progress,
                                    doneRows.size,
                                    planillaRows.size,
                                    currentVol,
                                    targetVol,
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progressFraction.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                        )
                        Text(
                            text = stringResource(R.string.sheet_goal_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            itemsIndexed(planillaRows, key = { _, r -> r.rowId }) { index, row ->
                val matchedTarget = remember(row, previousReferences) {
                    if (row.targetPeso != null && row.targetReps != null) {
                        Triple(row.targetPeso, row.targetReps, row.targetRir ?: 2L)
                    } else {
                        findMatchingTarget(row, previousReferences)?.let {
                            Triple(it.peso, it.reps, it.rir)
                        }
                    }
                }
                PlanillaRowCard(
                    index = index,
                    row = row,
                    recentExercises = recentExercisesForGroup,
                    target = matchedTarget,
                    onUpdate = { transform -> onUpdatePlanillaRow(row.rowId, transform) },
                    onDuplicate = { onDuplicatePlanillaRow(row.rowId) },
                    onRemove = { onRemovePlanillaRow(row.rowId) },
                    modifier = Modifier
                        .widthIn(max = 840.dp)
                        .fillMaxWidth(),
                )
            }

            item {
                val doneCount = planillaRows.count { it.isDone }
                Column(
                    modifier = Modifier
                        .widthIn(max = 840.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onAddPlanillaRow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("planilla_add_row_button"),
                    ) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.sheet_add_row))
                    }

                    Button(
                        onClick = onSubmitPlanilla,
                        enabled = !isSavingPlanilla && doneCount > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("planilla_save_all_button"),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        if (isSavingPlanilla) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Filled.Save, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.sheet_save_all, doneCount),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        } else {
            item {
                SingleSetFormCard(
                    formState = formState,
                    recentExercisesForGroup = recentExercisesForGroup,
                    onEjercicioChange = onEjercicioChange,
                    onSerieChange = onSerieChange,
                    onPesoChange = onPesoChange,
                    onNudgePeso = onNudgePeso,
                    onRepsChange = onRepsChange,
                    onNudgeReps = onNudgeReps,
                    onRirChange = onRirChange,
                    onObsChange = onObsChange,
                    onSubmitSet = onSubmitSet,
                    modifier = Modifier
                        .widthIn(max = 840.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = remember(formState.fecha) {
            runCatching {
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                fmt.parse(formState.fecha)?.time
            }.getOrNull() ?: System.currentTimeMillis()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            onFechaChange(fmt.format(Date(millis)))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showAddGroupDialog) {
        var newGroupName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddGroupDialog = false },
            title = { Text(stringResource(R.string.dialog_add_group_title)) },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text(stringResource(R.string.label_new_group_name)) },
                    placeholder = { Text(stringResource(R.string.placeholder_new_group)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_group_input"),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddCustomGroup(newGroupName)
                        showAddGroupDialog = false
                    },
                    modifier = Modifier.testTag("save_group_button"),
                ) {
                    Text(stringResource(R.string.btn_save_group))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGroupDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        )
    }
}

@Composable
private fun PlanillaRowCard(
    index: Int,
    row: PlanillaRowInput,
    recentExercises: List<String>,
    target: Triple<Double, Long, Long>?,
    onUpdate: ((PlanillaRowInput) -> PlanillaRowInput) -> Unit,
    onDuplicate: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val curPeso = row.pesoText.replace(',', '.').toDoubleOrNull()
    val curReps = row.repsText.toLongOrNull()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (row.isDone) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (row.isDone) 2.dp else 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Checkbox(
                        checked = row.isDone,
                        onCheckedChange = { checked ->
                            onUpdate { it.copy(isDone = checked) }
                        },
                        modifier = Modifier.testTag("planilla_done_$index"),
                    )
                    Text(
                        text = "Serie #${row.serie}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    IconButton(
                        onClick = { onUpdate { it.copy(serie = (it.serie - 1).coerceAtLeast(1)) } },
                        enabled = row.serie > 1,
                    ) {
                        Icon(Icons.Filled.Remove, stringResource(R.string.cd_decrement), Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { onUpdate { it.copy(serie = (it.serie + 1).coerceAtMost(999)) } },
                    ) {
                        Icon(Icons.Filled.Add, stringResource(R.string.cd_increment), Modifier.size(16.dp))
                    }
                }

                Row {
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.testTag("planilla_dup_$index"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Duplicar serie",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.testTag("planilla_remove_$index"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_delete_set),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            OutlinedTextField(
                value = row.ejercicio,
                onValueChange = { text -> onUpdate { it.copy(ejercicio = text) } },
                label = { Text(stringResource(R.string.label_exercise)) },
                placeholder = { Text(stringResource(R.string.placeholder_exercise)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("planilla_ex_$index"),
            )

            if (recentExercises.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    recentExercises.forEach { suggestion ->
                        AssistChip(
                            onClick = { onUpdate { it.copy(ejercicio = suggestion) } },
                            label = { Text(suggestion) },
                        )
                    }
                }
            }

            if (target != null) {
                val (tPeso, tReps, tRir) = target
                val target1Rm = tPeso * (1.0 + tReps / 30.0)
                val current1Rm = if (curPeso != null && curReps != null) curPeso * (1.0 + curReps / 30.0) else null
                val delta1Rm = if (current1Rm != null) current1Rm - target1Rm else null
                val deltaLabel = when {
                    delta1Rm == null -> null
                    delta1Rm > 0.05 -> "+${String.format(Locale.US, "%.1f", delta1Rm)} kg 1RM"
                    delta1Rm < -0.05 -> "${String.format(Locale.US, "%.1f", delta1Rm)} kg 1RM"
                    else -> "= Objetivo"
                }
                val badgeColor = when {
                    delta1Rm != null && delta1Rm > 0.05 -> MaterialTheme.colorScheme.tertiaryContainer
                    delta1Rm != null && delta1Rm < -0.05 -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val badgeTextColor = when {
                    delta1Rm != null && delta1Rm > 0.05 -> MaterialTheme.colorScheme.onTertiaryContainer
                    delta1Rm != null && delta1Rm < -0.05 -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = badgeColor,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Obj: ${formatNum(tPeso)} kg × $tReps (RIR $tRir)",
                            style = MaterialTheme.typography.labelMedium,
                            color = badgeTextColor,
                        )
                        if (deltaLabel != null) {
                            Text(
                                text = deltaLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = badgeTextColor,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = row.pesoText,
                    onValueChange = { text -> onUpdate { it.copy(pesoText = text) } },
                    label = { Text("kg") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("planilla_peso_$index"),
                )
                OutlinedTextField(
                    value = row.repsText,
                    onValueChange = { text -> onUpdate { it.copy(repsText = text) } },
                    label = { Text("Reps") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("planilla_reps_$index"),
                )
                OutlinedTextField(
                    value = row.obs,
                    onValueChange = { text -> onUpdate { it.copy(obs = text) } },
                    label = { Text("Nota") },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "RIR:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp),
                )
                (0..5).forEach { value ->
                    FilterChip(
                        selected = row.rir == value,
                        onClick = { onUpdate { it.copy(rir = value) } },
                        label = { Text(value.toString()) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SingleSetFormCard(
    formState: WorkoutFormState,
    recentExercisesForGroup: List<String>,
    onEjercicioChange: (String) -> Unit,
    onSerieChange: (Int) -> Unit,
    onPesoChange: (String) -> Unit,
    onNudgePeso: (Double) -> Unit,
    onRepsChange: (String) -> Unit,
    onNudgeReps: (Int) -> Unit,
    onRirChange: (Int) -> Unit,
    onObsChange: (String) -> Unit,
    onSubmitSet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val parsedWeight = formState.pesoText.replace(',', '.').toDoubleOrNull()
    val parsedReps = formState.repsText.toLongOrNull()
    val liveVolume = if (parsedWeight != null && parsedReps != null && parsedWeight >= 0 && parsedReps >= 1) {
        parsedWeight * parsedReps
    } else null
    val live1Rm = if (parsedWeight != null && parsedReps != null && parsedWeight >= 0 && parsedReps >= 1) {
        parsedWeight * (1.0 + parsedReps / 30.0)
    } else null

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = formState.ejercicio,
                    onValueChange = onEjercicioChange,
                    label = { Text(stringResource(R.string.label_exercise)) },
                    placeholder = { Text(stringResource(R.string.placeholder_exercise)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exercise_input"),
                )
                if (recentExercisesForGroup.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        recentExercisesForGroup.forEach { suggestion ->
                            AssistChip(
                                onClick = { onEjercicioChange(suggestion) },
                                label = { Text(suggestion) },
                            )
                        }
                    }
                }
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.label_set_number),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconButton(
                            onClick = { onSerieChange(formState.serie - 1) },
                            enabled = formState.serie > 1,
                            modifier = Modifier.testTag("serie_minus_button"),
                        ) {
                            Icon(Icons.Filled.Remove, stringResource(R.string.cd_decrement))
                        }
                        Text(
                            text = "#${formState.serie}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        IconButton(
                            onClick = { onSerieChange(formState.serie + 1) },
                            enabled = formState.serie < 999,
                            modifier = Modifier.testTag("serie_plus_button"),
                        ) {
                            Icon(Icons.Filled.Add, stringResource(R.string.cd_increment))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    OutlinedTextField(
                        value = formState.pesoText,
                        onValueChange = onPesoChange,
                        label = { Text(stringResource(R.string.label_weight_kg)) },
                        placeholder = { Text("80") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("weight_input"),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(
                            onClick = { onNudgePeso(-2.5) },
                            label = { Text("-2.5") },
                            modifier = Modifier.weight(1f),
                        )
                        AssistChip(
                            onClick = { onNudgePeso(2.5) },
                            label = { Text("+2.5") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    OutlinedTextField(
                        value = formState.repsText,
                        onValueChange = onRepsChange,
                        label = { Text(stringResource(R.string.label_reps)) },
                        placeholder = { Text("10") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reps_input"),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(
                            onClick = { onNudgeReps(-1) },
                            label = { Text("-1") },
                            modifier = Modifier.weight(1f),
                        )
                        AssistChip(
                            onClick = { onNudgeReps(1) },
                            label = { Text("+1") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.label_rir),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    (0..5).forEach { value ->
                        FilterChip(
                            selected = formState.rir == value,
                            onClick = { onRirChange(value) },
                            label = { Text(value.toString(), fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rir_chip_$value"),
                        )
                    }
                }
            }

            OutlinedTextField(
                value = formState.obs,
                onValueChange = onObsChange,
                label = { Text(stringResource(R.string.label_observation)) },
                placeholder = { Text(stringResource(R.string.placeholder_observation)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("observation_input"),
            )

            if (liveVolume != null && live1Rm != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Volumen: ${String.format(Locale.US, "%.1f", liveVolume)} kg",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "1RM est.: ${String.format(Locale.US, "%.1f", live1Rm)} kg",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            Button(
                onClick = onSubmitSet,
                enabled = !formState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("add_set_button"),
                shape = MaterialTheme.shapes.medium,
            ) {
                if (formState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Filled.Add, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_add_set),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

private fun findMatchingTarget(
    row: PlanillaRowInput,
    references: List<WorkoutSet>,
): WorkoutSet? {
    val ex = row.ejercicio.trim()
    if (ex.isEmpty()) return null
    val sameEx = references.filter { it.ejercicio.equals(ex, ignoreCase = true) }
    if (sameEx.isEmpty()) return null
    val latestDate = sameEx.maxOf { it.fecha }
    val latestDay = sameEx.filter { it.fecha == latestDate }
    return latestDay.firstOrNull { it.serie.toInt() == row.serie } ?: latestDay.firstOrNull()
}

private fun formatNum(v: Double): String {
    return if (v % 1.0 == 0.0) v.toInt().toString()
    else String.format(Locale.US, "%.2f", v).trimEnd('0').trimEnd('.')
}

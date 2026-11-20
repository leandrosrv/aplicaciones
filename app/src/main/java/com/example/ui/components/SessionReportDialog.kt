package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ProgressMetric
import com.example.data.TimeGranularity
import com.example.data.WorkoutSet
import com.example.data.calculateSummary
import com.example.ui.ALL_GROUPS_CHART
import com.example.ui.buildChartModel
import com.example.ui.currentTodayIsoDate
import java.util.Locale

@Composable
fun SessionReportDialog(
    allSets: List<WorkoutSet>,
    muscleGroups: List<String>,
    initialScope: SessionImageScope = SessionImageScope.LATEST,
    initialChartGroup: String = ALL_GROUPS_CHART,
    initialMetric: ProgressMetric = ProgressMetric.ONE_RM,
    initialGranularity: TimeGranularity = TimeGranularity.DAY,
    isProcessing: Boolean = false,
    onDismiss: () -> Unit,
    onSharePdf: (PdfReportSpec, String) -> Unit,
    onSaveToDownloads: (PdfReportSpec, String) -> Unit,
    onSaveWithPicker: (PdfReportSpec, String) -> Unit,
) {
    val today = remember { currentTodayIsoDate() }
    var selectedScope by remember { mutableStateOf(initialScope) }
    var selectedGroup by remember { mutableStateOf(initialChartGroup) }
    var selectedGranularity by remember { mutableStateOf(initialGranularity) }
    var selectedMetrics by remember {
        mutableStateOf(
            linkedSetOf(ProgressMetric.ONE_RM, ProgressMetric.VOLUME, initialMetric).toSet()
        )
    }

    val (scopeSubtitle, scopedSets) = remember(allSets, selectedScope, today) {
        filterSetsForImageScope(allSets, selectedScope, today)
    }

    val orderedMetrics = remember(selectedMetrics) {
        ProgressMetric.entries.filter { it in selectedMetrics }
    }

    val previewChartModel = remember(allSets, selectedGroup, orderedMetrics, selectedGranularity) {
        val metric = orderedMetrics.firstOrNull() ?: ProgressMetric.ONE_RM
        buildChartModel(
            allSets = allSets,
            group = selectedGroup,
            metric = metric,
            granularity = selectedGranularity,
        )
    }

    val summary = remember(scopedSets) { calculateSummary(scopedSets) }
    val groupCount = remember(scopedSets) { scopedSets.map { it.grupo }.distinct().size }

    val spec = remember(
        selectedScope,
        scopeSubtitle,
        scopedSets,
        allSets,
        selectedGroup,
        selectedGranularity,
        orderedMetrics,
    ) {
        PdfReportSpec(
            scope = selectedScope,
            scopeSubtitle = scopeSubtitle,
            scopedSets = scopedSets,
            allSetsForCharts = allSets,
            chartGroup = selectedGroup,
            granularity = selectedGranularity,
            chartMetrics = orderedMetrics,
        )
    }

    val suggestedFileName = remember(selectedScope, today) {
        when (selectedScope) {
            SessionImageScope.TODAY -> "informe_gym_$today.pdf"
            SessionImageScope.LATEST -> "informe_gym_ultima_sesion.pdf"
            SessionImageScope.ALL -> "informe_gym_completo.pdf"
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Filled.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text(stringResource(R.string.dialog_pdf_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Scope selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.label_pdf_scope),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SessionImageScope.entries.forEach { scope ->
                            FilterChip(
                                selected = selectedScope == scope,
                                onClick = { selectedScope = scope },
                                label = { Text(scope.label) },
                                modifier = Modifier.testTag("pdf_scope_${scope.name.lowercase()}"),
                            )
                        }
                    }
                }

                // Muscle group for charts
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.label_muscle_group),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilterChip(
                            selected = selectedGroup.equals(ALL_GROUPS_CHART, ignoreCase = true),
                            onClick = { selectedGroup = ALL_GROUPS_CHART },
                            label = { Text(stringResource(R.string.filter_all_groups_chart)) },
                        )
                        muscleGroups.forEach { group ->
                            FilterChip(
                                selected = selectedGroup.equals(group, ignoreCase = true),
                                onClick = { selectedGroup = group },
                                label = { Text(group) },
                            )
                        }
                    }
                }

                // Granularity
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.label_time_granularity),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        TimeGranularity.entries.forEach { granularity ->
                            FilterChip(
                                selected = selectedGranularity == granularity,
                                onClick = { selectedGranularity = granularity },
                                label = { Text(granularity.label) },
                            )
                        }
                    }
                }

                // Included chart metrics (multi-select)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.label_pdf_charts),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ProgressMetric.entries.forEach { metric ->
                            val isSelected = metric in selectedMetrics
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedMetrics = if (isSelected) {
                                        selectedMetrics - metric
                                    } else {
                                        selectedMetrics + metric
                                    }
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                } else null,
                                label = { Text(metric.label) },
                                modifier = Modifier.testTag("pdf_metric_${metric.id}"),
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Live report preview card
                Text(
                    text = stringResource(R.string.label_pdf_preview),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )

                if (scopedSets.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = stringResource(R.string.msg_pdf_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = scopeSubtitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${summary.totalSets} series · ${summary.totalReps} reps · " +
                                    "${String.format(Locale.US, "%.1f", summary.totalVolumeKg)} kg · " +
                                    "1RM ${String.format(Locale.US, "%.1f", summary.bestEstimated1RmKg)} kg",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "$groupCount grupos · ${orderedMetrics.size} gráficos PDF · ${selectedGranularity.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            if (orderedMetrics.isNotEmpty() &&
                                previewChartModel.buckets.isNotEmpty() &&
                                previewChartModel.series.isNotEmpty()
                            ) {
                                ProgressLineChart(model = previewChartModel)
                            }
                        }
                    }
                }

                // Extra save actions inside dialog body so Share & Save are all clear and accessible
                if (scopedSets.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            OutlinedButton(
                                onClick = { onSaveToDownloads(spec, suggestedFileName) },
                                enabled = !isProcessing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_pdf_downloads_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.btn_save_pdf_downloads))
                            }
                        }
                        OutlinedButton(
                            onClick = { onSaveWithPicker(spec, suggestedFileName) },
                            enabled = !isProcessing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_pdf_picker_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    stringResource(R.string.btn_save_pdf_as)
                                } else {
                                    stringResource(R.string.btn_save_pdf)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSharePdf(spec, suggestedFileName) },
                enabled = scopedSets.isNotEmpty() && !isProcessing,
                modifier = Modifier.testTag("share_pdf_confirm_button"),
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_share_pdf))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProcessing,
            ) {
                Text(stringResource(R.string.btn_cancel))
            }
        },
    )
}

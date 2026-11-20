package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.ChartModel
import com.example.ui.theme.ChartSeriesColors
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressLineChart(
    model: ChartModel,
    modifier: Modifier = Modifier,
) {
    var selectedBucketIndex by remember(model) {
        mutableStateOf<Int?>(model.buckets.lastIndex.takeIf { it >= 0 })
    }

    val allValues = remember(model) {
        model.series.flatMap { s -> s.values.filterNotNull() }
    }

    if (model.buckets.isEmpty() || model.series.isEmpty() || allValues.isEmpty()) {
        return
    }

    val rawMin = allValues.minOrNull() ?: 0.0
    val rawMax = allValues.maxOrNull() ?: 10.0
    val range = (rawMax - rawMin).let { if (it < 1e-6) maxOf(1.0, rawMax * 0.2) else it }
    val minY = floor((rawMin - range * 0.12).coerceAtLeast(0.0))
    val maxY = ceil(rawMax + range * 0.12)
    val ySpan = (maxY - minY).coerceAtLeast(1.0)

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            model.series.forEachIndexed { idx, series ->
                val color = ChartSeriesColors[idx % ChartSeriesColors.size]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Text(
                        text = series.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .height(210.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                val ticks = 4
                for (i in ticks downTo 0) {
                    val value = minY + (ySpan * i / ticks)
                    Text(
                        text = formatAxisValue(value),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(210.dp)
                    .pointerInput(model.buckets) {
                        detectTapGestures { offset ->
                            if (model.buckets.size == 1) {
                                selectedBucketIndex = 0
                            } else {
                                val padH = 12.dp.toPx()
                                val usableW = (size.width - padH * 2).coerceAtLeast(1f)
                                val fraction = ((offset.x - padH) / usableW).coerceIn(0f, 1f)
                                val index = (fraction * (model.buckets.size - 1)).roundToInt()
                                selectedBucketIndex = index.coerceIn(0, model.buckets.lastIndex)
                            }
                        }
                    }
            ) {
                val padH = 12.dp.toPx()
                val padV = 10.dp.toPx()
                val chartW = (size.width - padH * 2).coerceAtLeast(1f)
                val chartH = (size.height - padV * 2).coerceAtLeast(1f)

                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                val ticks = 4
                for (i in 0..ticks) {
                    val y = padV + chartH * (1f - i.toFloat() / ticks)
                    drawLine(
                        color = gridColor,
                        start = Offset(padH, y),
                        end = Offset(padH + chartW, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = if (i == 0) null else dashEffect,
                    )
                }

                fun xForIndex(index: Int): Float {
                    return if (model.buckets.size <= 1) {
                        padH + chartW / 2f
                    } else {
                        padH + chartW * (index.toFloat() / (model.buckets.size - 1))
                    }
                }

                fun yForValue(v: Double): Float {
                    val norm = ((v - minY) / ySpan).toFloat().coerceIn(0f, 1f)
                    return padV + chartH * (1f - norm)
                }

                selectedBucketIndex?.let { selIdx ->
                    if (selIdx in model.buckets.indices) {
                        val sx = xForIndex(selIdx)
                        drawLine(
                            color = labelColor.copy(alpha = 0.35f),
                            start = Offset(sx, padV),
                            end = Offset(sx, padV + chartH),
                            strokeWidth = 1.5.dp.toPx(),
                        )
                    }
                }

                model.series.forEachIndexed { sIdx, series ->
                    val color = ChartSeriesColors[sIdx % ChartSeriesColors.size]
                    val path = Path()
                    var segmentStarted = false

                    series.values.forEachIndexed { dIdx, value ->
                        if (value == null) {
                            segmentStarted = false
                        } else {
                            val pt = Offset(xForIndex(dIdx), yForValue(value))
                            if (!segmentStarted) {
                                path.moveTo(pt.x, pt.y)
                                segmentStarted = true
                            } else {
                                path.lineTo(pt.x, pt.y)
                            }
                        }
                    }

                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(
                            width = 2.8.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )

                    series.values.forEachIndexed { dIdx, value ->
                        if (value != null) {
                            val pt = Offset(xForIndex(dIdx), yForValue(value))
                            val isSelected = selectedBucketIndex == dIdx
                            val radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx()
                            drawCircle(
                                color = surfaceColor,
                                radius = radius + 1.5.dp.toPx(),
                                center = pt,
                            )
                            drawCircle(
                                color = color,
                                radius = radius,
                                center = pt,
                            )
                        }
                    }
                }
            }
        }

        // X Axis bucket labels (first, middle, last)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 36.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (model.buckets.size == 1) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = model.buckets.first().axisLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                )
                Spacer(Modifier.weight(1f))
            } else {
                Text(
                    text = model.buckets.first().axisLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                )
                if (model.buckets.size >= 3) {
                    Text(
                        text = model.buckets[model.buckets.size / 2].axisLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                    )
                }
                Text(
                    text = model.buckets.last().axisLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                )
            }
        }

        // Interactive Tooltip Inspector
        selectedBucketIndex?.let { selIdx ->
            if (selIdx in model.buckets.indices) {
                val bucket = model.buckets[selIdx]
                val entries = model.series.mapIndexedNotNull { idx, series ->
                    series.values.getOrNull(selIdx)?.let { valNum ->
                        Triple(
                            series.name,
                            valNum,
                            ChartSeriesColors[idx % ChartSeriesColors.size],
                        )
                    }
                }
                if (entries.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "${bucket.tooltipLabel} · ${model.metric.label} (${model.granularity.label})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                            )
                            entries.forEach { (name, value, color) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                        )
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                    Text(
                                        text = formatMetricValue(value, model.metric.unitSuffix),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatAxisValue(v: Double): String {
    return if (v >= 1000.0) {
        String.format(Locale.US, "%.1fk", v / 1000.0)
    } else if (v % 1.0 == 0.0) {
        v.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", v)
    }
}

private fun formatMetricValue(v: Double, unit: String): String {
    val num = if (v % 1.0 == 0.0) v.toInt().toString() else String.format(Locale.US, "%.1f", v)
    return if (unit.isNotEmpty()) "$num $unit" else num
}

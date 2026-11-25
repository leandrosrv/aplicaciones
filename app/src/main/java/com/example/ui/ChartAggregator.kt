package com.example.ui

import com.example.data.ExercisePrSummary
import com.example.data.ProgressMetric
import com.example.data.TimeGranularity
import com.example.data.WorkoutSet
import java.text.Collator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val ALL_GROUPS_CHART = "Todos"
const val ALL_EXERCISES_CHART = "Todos"

fun availableExercisesForGroup(
    allSets: List<WorkoutSet>,
    group: String,
): List<String> {
    val subset = if (group.equals(ALL_GROUPS_CHART, ignoreCase = true)) {
        allSets
    } else {
        allSets.filter { it.grupo.equals(group, ignoreCase = true) }
    }
    val collator = Collator.getInstance(Locale("es", "ES")).apply {
        strength = Collator.PRIMARY
    }
    val seen = LinkedHashMap<String, String>()
    for (item in subset) {
        val trimmed = item.ejercicio.trim()
        if (trimmed.isNotEmpty()) {
            val key = trimmed.lowercase(Locale.getDefault())
            if (!seen.containsKey(key)) {
                seen[key] = trimmed
            }
        }
    }
    return seen.values.sortedWith { a, b -> collator.compare(a, b) }
}

data class ChartSeries(
    val name: String,
    val values: List<Double?>,
)

data class ChartBucket(
    val sortKey: String,
    val axisLabel: String,
    val tooltipLabel: String,
)

data class ChartModel(
    val buckets: List<ChartBucket>,
    val series: List<ChartSeries>,
    val metric: ProgressMetric,
    val granularity: TimeGranularity,
)

fun buildChartModel(
    allSets: List<WorkoutSet>,
    group: String,
    metric: ProgressMetric,
    granularity: TimeGranularity,
    exercise: String = ALL_EXERCISES_CHART,
): ChartModel {
    val groupSubset = if (group.equals(ALL_GROUPS_CHART, ignoreCase = true)) {
        allSets
    } else {
        allSets.filter { it.grupo.equals(group, ignoreCase = true) }
    }
    val subset = if (exercise.equals(ALL_EXERCISES_CHART, ignoreCase = true)) {
        groupSubset
    } else {
        val target = exercise.trim()
        groupSubset.filter { it.ejercicio.trim().equals(target, ignoreCase = true) }
    }
    if (subset.isEmpty()) {
        return ChartModel(emptyList(), emptyList(), metric, granularity)
    }

    val chronological = subset.sortedWith(
        compareBy<WorkoutSet> { it.fecha }
            .thenBy { it.createdAtMillis }
            .thenBy { it.serie }
    )

    val bucketMap = linkedMapOf<String, ChartBucket>()
    fun resolveBucket(item: WorkoutSet, seqIndex: Int): ChartBucket {
        val bucket = bucketForItem(item, seqIndex, granularity)
        return bucketMap.getOrPut(bucket.sortKey) { bucket }
    }

    if (metric == ProgressMetric.BODY_WEIGHT) {
        val byBucket = mutableMapOf<String, Double>()
        chronological.forEachIndexed { idx, item ->
            val bucket = resolveBucket(item, idx)
            val bw = item.pesoCorporal
            if (bw != null) {
                byBucket[bucket.sortKey] = bw
            }
        }
        val buckets = bucketMap.values.sortedBy { it.sortKey }
        val values = buckets.map { b -> byBucket[b.sortKey] }
        val series = if (values.any { it != null }) {
            listOf(ChartSeries("Peso corporal", values))
        } else {
            emptyList()
        }
        return ChartModel(buckets, series, metric, granularity)
    }

    data class Agg(
        var sumVol: Double = 0.0,
        var sumReps: Long = 0L,
        var sets: Int = 0,
        var maxWeight: Double = 0.0,
        var best1Rm: Double = 0.0,
    )

    val byExercise = sortedMapOf<String, MutableMap<String, Agg>>()
    chronological.forEachIndexed { idx, item ->
        val bucket = resolveBucket(item, idx)
        val exMap = byExercise.getOrPut(item.ejercicio) { mutableMapOf() }
        val agg = exMap.getOrPut(bucket.sortKey) { Agg() }
        agg.sumVol += item.volumeKg
        agg.sumReps += item.reps
        agg.sets += 1
        agg.maxWeight = maxOf(agg.maxWeight, item.peso)
        agg.best1Rm = maxOf(agg.best1Rm, item.estimated1RmKg)
    }

    val buckets = bucketMap.values.sortedBy { it.sortKey }
    val seriesList = byExercise.entries.map { (exercise, mapByBucket) ->
        val values = buckets.map { b ->
            val agg = mapByBucket[b.sortKey] ?: return@map null
            when (metric) {
                ProgressMetric.ONE_RM -> agg.best1Rm
                ProgressMetric.WEIGHT -> agg.maxWeight
                ProgressMetric.VOLUME -> agg.sumVol
                ProgressMetric.REPS -> agg.sumReps.toDouble()
                ProgressMetric.SETS -> agg.sets.toDouble()
                ProgressMetric.BODY_WEIGHT -> null
            }
        }
        ChartSeries(exercise, values)
    }
    return ChartModel(buckets, seriesList, metric, granularity)
}

private fun bucketForItem(
    item: WorkoutSet,
    seqIndex: Int,
    granularity: TimeGranularity,
): ChartBucket {
    return when (granularity) {
        TimeGranularity.SESSION -> {
            val timeStr = if (item.createdAtMillis > 0L) {
                SimpleDateFormat("HH:mm", Locale.US).format(Date(item.createdAtMillis))
            } else {
                "S${item.serie}"
            }
            val shortDate = if (item.fecha.length >= 10) item.fecha.substring(5) else item.fecha
            val seqPadded = seqIndex.toString().padStart(5, '0')
            ChartBucket(
                sortKey = "${item.fecha}_${seqPadded}",
                axisLabel = "$shortDate S${item.serie}",
                tooltipLabel = "${item.fecha} $timeStr (S${item.serie})",
            )
        }

        TimeGranularity.DAY -> {
            val shortDate = if (item.fecha.length >= 10) item.fecha.substring(5) else item.fecha
            ChartBucket(
                sortKey = item.fecha,
                axisLabel = shortDate,
                tooltipLabel = item.fecha,
            )
        }

        TimeGranularity.WEEK -> {
            val parsed = runCatching {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(item.fecha)
            }.getOrNull()
            if (parsed != null) {
                val cal = Calendar.getInstance(Locale("es", "ES")).apply {
                    firstDayOfWeek = Calendar.MONDAY
                    minimalDaysInFirstWeek = 4
                    time = parsed
                }
                val weekYear = cal.weekYear
                val weekOfYear = cal.get(Calendar.WEEK_OF_YEAR)
                val weekPadded = weekOfYear.toString().padStart(2, '0')
                ChartBucket(
                    sortKey = "$weekYear-W$weekPadded",
                    axisLabel = "S$weekPadded",
                    tooltipLabel = "Semana $weekPadded · $weekYear",
                )
            } else {
                ChartBucket(item.fecha, item.fecha, item.fecha)
            }
        }

        TimeGranularity.MONTH -> {
            val ym = if (item.fecha.length >= 7) item.fecha.substring(0, 7) else item.fecha
            val label = runCatching {
                val dt = SimpleDateFormat("yyyy-MM", Locale.US).parse(ym)
                if (dt != null) SimpleDateFormat("MMM yy", Locale("es", "ES")).format(dt) else ym
            }.getOrDefault(ym)
            ChartBucket(
                sortKey = ym,
                axisLabel = label,
                tooltipLabel = ym,
            )
        }
    }
}

fun buildExercisePrs(
    allSets: List<WorkoutSet>,
    group: String,
    exercise: String = ALL_EXERCISES_CHART,
): List<ExercisePrSummary> {
    val groupSubset = if (group.equals(ALL_GROUPS_CHART, ignoreCase = true)) {
        allSets
    } else {
        allSets.filter { it.grupo.equals(group, ignoreCase = true) }
    }
    val subset = if (exercise.equals(ALL_EXERCISES_CHART, ignoreCase = true)) {
        groupSubset
    } else {
        val target = exercise.trim()
        groupSubset.filter { it.ejercicio.trim().equals(target, ignoreCase = true) }
    }
    return subset.groupBy { it.ejercicio }
        .map { (exercise, items) ->
            ExercisePrSummary(
                exerciseName = exercise,
                best1RmKg = items.maxOf { it.estimated1RmKg },
                maxWeightKg = items.maxOf { it.peso },
                totalSets = items.size,
                lastDate = items.maxOf { it.fecha },
            )
        }
        .sortedByDescending { it.best1RmKg }
}

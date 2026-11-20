package com.example.data

import java.text.Collator
import java.util.Locale

val DEFAULT_MUSCLE_GROUPS: List<String> = listOf(
    "Pecho",
    "Espalda",
    "Hombros",
    "Bíceps",
    "Tríceps",
    "Piernas",
    "Glúteos",
    "Abdominales",
    "Gemelos",
)

data class WorkoutSet(
    val id: Long = 0L,
    val fecha: String = "",
    val grupo: String = "",
    val ejercicio: String = "",
    val serie: Long = 1L,
    val peso: Double = 0.0,
    val reps: Long = 1L,
    val rir: Long = 2L,
    val pesoCorporal: Double? = null,
    val obs: String? = null,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
) {
    val volumeKg: Double
        get() = peso * reps.toDouble()

    val estimated1RmKg: Double
        get() = peso * (1.0 + reps.toDouble() / 30.0)
}

enum class ProgressMetric(
    val id: String,
    val label: String,
    val yAxisLabel: String,
    val unitSuffix: String,
) {
    ONE_RM("1rm", "1RM estimado", "1RM estimado (kg)", "kg"),
    WEIGHT("peso", "Peso utilizado", "Peso utilizado (kg)", "kg"),
    VOLUME("volumen", "Volumen", "Volumen (kg × reps)", "kg"),
    REPS("reps", "Repeticiones", "Repeticiones", "reps"),
    SETS("series", "Series", "Series", "series"),
    BODY_WEIGHT("corporal", "Peso corporal", "Peso corporal (kg)", "kg"),
}

enum class TimeGranularity(
    val id: String,
    val label: String,
) {
    SESSION("sesion", "Por serie / sesión"),
    DAY("dia", "Por día"),
    WEEK("semana", "Por semana"),
    MONTH("mes", "Por mes"),
}

data class WorkoutSummary(
    val totalSets: Int = 0,
    val totalReps: Long = 0L,
    val totalVolumeKg: Double = 0.0,
    val bestEstimated1RmKg: Double = 0.0,
)

data class ExercisePrSummary(
    val exerciseName: String,
    val best1RmKg: Double,
    val maxWeightKg: Double,
    val totalSets: Int,
    val lastDate: String,
)

data class PlanillaRowInput(
    val rowId: Long,
    val ejercicio: String = "",
    val serie: Int = 1,
    val pesoText: String = "",
    val repsText: String = "",
    val rir: Int = 2,
    val obs: String = "",
    val targetPeso: Double? = null,
    val targetReps: Long? = null,
    val targetRir: Long? = null,
    val isDone: Boolean = true,
)

data class NewSetInput(
    val fecha: String,
    val grupo: String,
    val ejercicio: String,
    val serie: Long,
    val peso: Double,
    val reps: Long,
    val rir: Long,
    val pesoCorporal: Double?,
    val obs: String?,
)

fun mergeMuscleGroups(customNames: List<String>): List<String> {
    val collator = Collator.getInstance(Locale("es", "ES")).apply {
        strength = Collator.PRIMARY
    }
    val combined = LinkedHashMap<String, String>()
    DEFAULT_MUSCLE_GROUPS.forEach { combined[it.lowercase(Locale.getDefault())] = it }
    customNames.forEach { name ->
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            val key = trimmed.lowercase(Locale.getDefault())
            if (!combined.containsKey(key)) {
                combined[key] = trimmed
            }
        }
    }
    return combined.values.sortedWith { a, b -> collator.compare(a, b) }
}

fun calculateSummary(sets: List<WorkoutSet>): WorkoutSummary {
    if (sets.isEmpty()) return WorkoutSummary()
    var repsSum = 0L
    var volumeSum = 0.0
    var best1Rm = 0.0
    for (set in sets) {
        repsSum += set.reps
        volumeSum += set.volumeKg
        best1Rm = maxOf(best1Rm, set.estimated1RmKg)
    }
    return WorkoutSummary(
        totalSets = sets.size,
        totalReps = repsSum,
        totalVolumeKg = volumeSum,
        bestEstimated1RmKg = best1Rm,
    )
}

fun buildWorkoutCsv(sets: List<WorkoutSet>): String {
    val header = listOf(
        "Fecha",
        "Grupo",
        "Ejercicio",
        "Serie",
        "Peso_kg",
        "Peso_corporal_kg",
        "Reps",
        "RIR",
        "Volumen_kg",
        "1RM_Epley_kg",
        "Observacion",
    )
    fun escapeCsv(value: String): String {
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    val sb = StringBuilder()
    sb.append('\uFEFF')
    sb.append(header.joinToString(";") { escapeCsv(it) })
    sb.append('\n')
    for (item in sets) {
        val row = listOf(
            item.fecha,
            item.grupo,
            item.ejercicio,
            item.serie.toString(),
            formatNumberForCsv(item.peso),
            item.pesoCorporal?.let { formatNumberForCsv(it) } ?: "",
            item.reps.toString(),
            item.rir.toString(),
            String.format(Locale.US, "%.2f", item.volumeKg),
            String.format(Locale.US, "%.2f", item.estimated1RmKg),
            item.obs.orEmpty(),
        )
        sb.append(row.joinToString(";") { escapeCsv(it) })
        sb.append('\n')
    }
    return sb.toString()
}

private fun formatNumberForCsv(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        value.toString()
    }
}

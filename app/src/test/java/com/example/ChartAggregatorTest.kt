package com.example

import com.example.data.ProgressMetric
import com.example.data.TimeGranularity
import com.example.data.WorkoutSet
import com.example.ui.ALL_EXERCISES_CHART
import com.example.ui.ALL_GROUPS_CHART
import com.example.ui.availableExercisesForGroup
import com.example.ui.buildChartModel
import com.example.ui.buildExercisePrs
import org.junit.Assert.assertEquals
import org.junit.Test

class ChartAggregatorTest {

    private val sampleSets = listOf(
        WorkoutSet(1L, "2026-11-20", "Pecho", "Press banca", 1, 80.0, 8, 2, createdAtMillis = 1000L),
        WorkoutSet(2L, "2026-11-22", "Pecho", "Press banca", 1, 85.0, 8, 2, createdAtMillis = 2000L),
        WorkoutSet(3L, "2026-11-20", "Pecho", "Aperturas", 1, 20.0, 12, 2, createdAtMillis = 3000L),
        WorkoutSet(4L, "2026-11-21", "Espalda", "Remo con barra", 1, 70.0, 10, 2, createdAtMillis = 4000L),
    )

    @Test
    fun `availableExercisesForGroup lists unique exercises filtered by muscle group`() {
        val pechoExercises = availableExercisesForGroup(sampleSets, "Pecho")
        assertEquals(listOf("Aperturas", "Press banca"), pechoExercises)

        val allExercises = availableExercisesForGroup(sampleSets, ALL_GROUPS_CHART)
        assertEquals(listOf("Aperturas", "Press banca", "Remo con barra"), allExercises)
    }

    @Test
    fun `buildChartModel isolates single exercise curve when exercise filter is active`() {
        val allInPecho = buildChartModel(
            allSets = sampleSets,
            group = "Pecho",
            metric = ProgressMetric.WEIGHT,
            granularity = TimeGranularity.DAY,
            exercise = ALL_EXERCISES_CHART,
        )
        assertEquals(listOf("Aperturas", "Press banca"), allInPecho.series.map { it.name })

        val onlyPressBanca = buildChartModel(
            allSets = sampleSets,
            group = "Pecho",
            metric = ProgressMetric.WEIGHT,
            granularity = TimeGranularity.DAY,
            exercise = "Press banca",
        )
        assertEquals(listOf("Press banca"), onlyPressBanca.series.map { it.name })
        assertEquals(listOf(80.0, 85.0), onlyPressBanca.series.first().values)

        val prsFiltered = buildExercisePrs(sampleSets, "Pecho", "Aperturas")
        assertEquals(listOf("Aperturas"), prsFiltered.map { it.exerciseName })
    }
}

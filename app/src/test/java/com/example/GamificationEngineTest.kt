package com.example

import com.example.data.GamificationData
import com.example.data.WorkoutSet
import com.example.gamification.data.AthleteProfileEntity
import com.example.gamification.engine.AnimalSpecies
import com.example.gamification.engine.AvatarGender
import com.example.gamification.engine.AvatarMoodState
import com.example.gamification.engine.GamificationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class GamificationEngineTest {

    @Test
    fun `empty workout history reports sin datos suficientes without inventing metrics`() {
        val snapshot = GamificationEngine.buildGamificationSnapshot(
            profile = AthleteProfileEntity(),
            sets = emptyList(),
            isRestTimerActive = false,
            systemReduceMotion = false,
            nowMillis = 1795520000000L,
        )

        assertFalse(snapshot.metrics.hasData)
        assertNull(snapshot.recovery.energyPercent)
        assertNull(snapshot.recovery.recoveryPercent)
        assertEquals("Sin datos suficientes", snapshot.recovery.statusLabel)
        assertEquals("Sin datos suficientes", snapshot.biologicalClock.statusTitle)
        assertNull(snapshot.streak.consistencyPercent)
    }

    @Test
    fun `biological clock updates automatically after 24h and 96h without background timer`() {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val tSession = sdf.parse("2026-11-20 18:00")!!.time
        val sets = listOf(
            WorkoutSet(
                id = 1L,
                fecha = "2026-11-20",
                grupo = "Pecho",
                ejercicio = "Press banca",
                serie = 1,
                peso = 80.0,
                reps = 8,
                rir = 2,
                createdAtMillis = tSession,
                updatedAtMillis = tSession,
            )
        )

        // 2 hours later -> same day, training/recovery
        val snap2h = GamificationEngine.buildGamificationSnapshot(
            profile = AthleteProfileEntity(),
            sets = sets,
            isRestTimerActive = false,
            systemReduceMotion = false,
            nowMillis = tSession + 2 * 60 * 60 * 1000L,
        )
        assertFalse(snap2h.biologicalClock.isCycleDue24h)

        // 25 hours later -> 24h cycle completed, needs training today
        val snap25h = GamificationEngine.buildGamificationSnapshot(
            profile = AthleteProfileEntity(),
            sets = sets,
            isRestTimerActive = false,
            systemReduceMotion = false,
            nowMillis = tSession + 25 * 60 * 60 * 1000L,
        )
        assertTrue(snap25h.biologicalClock.isCycleDue24h)
        assertTrue(snap25h.biologicalClock.needsTrainingToday)
        assertEquals(AvatarMoodState.PREPARADO, snap25h.moodState)

        // 100 hours later -> sleeping gently waiting for activity
        val snap100h = GamificationEngine.buildGamificationSnapshot(
            profile = AthleteProfileEntity(),
            sets = sets,
            isRestTimerActive = false,
            systemReduceMotion = false,
            nowMillis = tSession + 100 * 60 * 60 * 1000L,
        )
        assertEquals(AvatarMoodState.DURMIENDO, snap100h.moodState)
    }

    @Test
    fun `personal record and rest pause timer states are reflected accurately`() {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val day1 = sdf.parse("2026-11-20 18:00")!!.time
        val day2 = sdf.parse("2026-11-24 10:00")!!.time
        val sets = listOf(
            WorkoutSet(1L, "2026-11-20", "Pecho", "Press banca", 1, 80.0, 8, 2, createdAtMillis = day1, updatedAtMillis = day1),
            WorkoutSet(2L, "2026-11-24", "Pecho", "Press banca", 1, 90.0, 8, 2, createdAtMillis = day2, updatedAtMillis = day2),
        )

        val prSnap = GamificationEngine.buildGamificationSnapshot(
            profile = AthleteProfileEntity(),
            sets = sets,
            isRestTimerActive = false,
            systemReduceMotion = false,
            nowMillis = day2 + 10 * 60 * 1000L,
        )
        assertEquals(1, prSnap.personalRecords.size)
        assertEquals(AvatarMoodState.CELEBRANDO_PR, prSnap.moodState)
        assertTrue(prSnap.levelProgress.totalXp >= 250)

        val pauseSnap = GamificationEngine.buildGamificationSnapshot(
            profile = AthleteProfileEntity(),
            sets = sets,
            isRestTimerActive = true,
            systemReduceMotion = false,
            nowMillis = day2 + 10 * 60 * 1000L,
        )
        assertEquals(AvatarMoodState.PAUSA_ENTRE_SERIES, pauseSnap.moodState)
    }

    @Test
    fun `athlete profile entity maps animal species and gender safely with defaults`() {
        val femaleFox = AthleteProfileEntity(
            name = "Vicky",
            gender = AvatarGender.FEMALE.id,
            animalSpecies = AnimalSpecies.FOX.id,
        ).toAppearance()

        assertEquals("Vicky", femaleFox.name)
        assertEquals(AvatarGender.FEMALE, femaleFox.gender)
        assertEquals(AnimalSpecies.FOX, femaleFox.animalSpecies)

        val fallback = AthleteProfileEntity(
            gender = "UNKNOWN",
            animalSpecies = "UNKNOWN",
        ).toAppearance()

        assertEquals(AvatarGender.MALE, fallback.gender)
        assertEquals(AnimalSpecies.LION, fallback.animalSpecies)
        assertEquals(2, AvatarGender.entries.size)
        assertEquals(9, AnimalSpecies.entries.size)
        assertTrue(AnimalSpecies.TIGER in AnimalSpecies.entries)
        assertTrue(AnimalSpecies.BEAR in AnimalSpecies.entries)
        assertTrue(AnimalSpecies.EAGLE in AnimalSpecies.entries)
    }

    @Test
    fun `snapshot applies selected animal and gender stored in GamificationData`() {
        val data = GamificationData(
            xp = 120,
            level = 2,
            lastSessionTimestamp = 1795520000000L,
            streak = 3,
            selectedAnimal = AnimalSpecies.TIGER.id,
            avatarGender = AvatarGender.FEMALE.id,
        )
        val snap = GamificationEngine.buildGamificationSnapshot(
            profile = AthleteProfileEntity(),
            sets = emptyList(),
            isRestTimerActive = false,
            systemReduceMotion = false,
            nowMillis = 1795520000000L,
            gamificationData = data,
        )
        assertEquals(AnimalSpecies.TIGER, snap.appearance.animalSpecies)
        assertEquals(AvatarGender.FEMALE, snap.appearance.gender)

        val level1 = GamificationEngine.computeLevelProgress(totalXp = 40)
        assertEquals(1, level1.level)
        assertEquals(40, level1.currentLevelXp)
        assertEquals(100, level1.xpForNextLevel)
        assertEquals(60, level1.xpRemainingToNextLevel)

        assertTrue(GamificationEngine.didLevelUp(previousLevel = 1, currentLevel = 2, gamificationEnabled = true))
        assertTrue(GamificationEngine.didLevelUp(previousLevel = 2, currentLevel = 4, gamificationEnabled = true))
        assertFalse(GamificationEngine.didLevelUp(previousLevel = 2, currentLevel = 2, gamificationEnabled = true))
        assertFalse(GamificationEngine.didLevelUp(previousLevel = 3, currentLevel = 1, gamificationEnabled = true))
        assertFalse(GamificationEngine.didLevelUp(previousLevel = 1, currentLevel = 2, gamificationEnabled = false))
    }
}

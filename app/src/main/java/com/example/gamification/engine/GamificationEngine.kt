package com.example.gamification.engine

import com.example.data.GamificationData
import com.example.data.WorkoutSet
import com.example.gamification.data.AthleteProfileEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

object GamificationEngine {

    const val HOURS_24_MILLIS = 24L * 60L * 60L * 1000L
    const val HOURS_12_MILLIS = 12L * 60L * 60L * 1000L
    const val HOURS_48_MILLIS = 48L * 60L * 60L * 1000L
    const val HOURS_96_MILLIS = 96L * 60L * 60L * 1000L

    const val XP_PER_SESSION_DAY = 100
    const val XP_COMPLETE_SESSION_BONUS = 20
    const val XP_PER_PR = 50
    const val XP_WEEKLY_CONSISTENCY_BONUS = 100
    const val MINIGAME_PLAY_XP = 3
    const val MINIGAME_WIN_XP = 5
    const val MINIGAME_DAILY_XP_CAP = 15

    fun xpRequiredForNextLevel(level: Int): Int {
        val safeLevel = level.coerceAtLeast(1)
        return (100.0 * safeLevel.toDouble().pow(1.35)).roundToInt().coerceAtLeast(100)
    }

    fun computeLevelProgress(totalXp: Int): LevelProgress {
        var remaining = totalXp.coerceAtLeast(0)
        var level = 1
        var needed = xpRequiredForNextLevel(level)
        while (remaining >= needed && level < 99) {
            remaining -= needed
            level += 1
            needed = xpRequiredForNextLevel(level)
        }
        val fraction = if (needed > 0) (remaining.toFloat() / needed.toFloat()).coerceIn(0f, 1f) else 0f
        return LevelProgress(
            level = level,
            currentLevelXp = remaining,
            xpForNextLevel = needed,
            totalXp = totalXp.coerceAtLeast(0),
            progressFraction = fraction,
            nextRewardLabel = rewardLabelForLevel(level + 1),
        )
    }

    fun didLevelUp(
        previousLevel: Int,
        currentLevel: Int,
        gamificationEnabled: Boolean,
    ): Boolean {
        return gamificationEnabled && previousLevel >= 1 && currentLevel > previousLevel
    }

    private fun rewardLabelForLevel(nextLevel: Int): String {
        return when {
            nextLevel == 2 -> "Insignia Despegue y postura segura"
            nextLevel == 3 -> "Fondo Gimnasio Pro"
            nextLevel == 5 -> "Aura Energía Dorada"
            nextLevel == 8 -> "Fondo Estadio Atlético"
            nextLevel % 5 == 0 -> "Medalla de Constancia Nv.$nextLevel"
            else -> "Evolución deportiva Nv.$nextLevel"
        }
    }

    fun extractRealWorkoutMetrics(
        sets: List<WorkoutSet>,
        nowMillis: Long = System.currentTimeMillis(),
    ): RealWorkoutMetrics {
        if (sets.isEmpty()) return RealWorkoutMetrics()

        val sortedByDateDesc = sets.sortedWith(
            compareByDescending<WorkoutSet> { it.fecha }
                .thenByDescending { it.createdAtMillis }
        )
        val latestDate = sortedByDateDesc.first().fecha
        val distinctDates = sets.map { it.fecha }.filter { it.isNotBlank() }.distinct().sortedDescending()
        val latestSets = sets.filter { it.fecha == latestDate }

        val todayIso = formatIsoDate(nowMillis)
        val sevenDaysAgoIso = shiftIsoDate(todayIso, -6)
        val setsLast7 = sets.filter { it.fecha in sevenDaysAgoIso..todayIso }
        val daysLast7 = setsLast7.map { it.fecha }.distinct().size

        val latestTimestamp = resolveLatestSessionTimestamp(
            latestDate = latestDate,
            latestSets = latestSets,
            todayIso = todayIso,
            nowMillis = nowMillis,
        )

        val avgRir = if (latestSets.isNotEmpty()) {
            latestSets.map { it.rir.toDouble() }.average()
        } else {
            null
        }

        val hasNotesOrBody = latestSets.any {
            !it.obs.isNullOrBlank() || it.pesoCorporal != null
        }

        return RealWorkoutMetrics(
            hasData = true,
            totalSets = sets.size,
            totalVolumeKg = sets.sumOf { it.volumeKg },
            distinctTrainingDays = distinctDates.size,
            trainingDatesDesc = distinctDates,
            latestSessionDate = latestDate,
            latestSessionSets = latestSets.size,
            latestSessionVolumeKg = latestSets.sumOf { it.volumeKg },
            latestSessionExercises = latestSets.map { it.ejercicio.trim().lowercase(Locale.getDefault()) }
                .filter { it.isNotEmpty() }
                .distinct()
                .size,
            latestSessionMuscleGroups = latestSets.map { it.grupo.trim() }
                .filter { it.isNotEmpty() }
                .distinct(),
            latestSessionAvgRir = avgRir,
            latestSessionHasNotesOrBodyWeight = hasNotesOrBody,
            latestSessionTimestampMillis = latestTimestamp,
            setsInLast7Days = setsLast7.size,
            daysInLast7Days = daysLast7,
        )
    }

    fun detectPersonalRecords(sets: List<WorkoutSet>): List<PersonalRecordBadge> {
        if (sets.isEmpty()) return emptyList()
        val chronological = sets.sortedWith(
            compareBy<WorkoutSet> { it.fecha }
                .thenBy { it.createdAtMillis }
        )
        val best1RmByExercise = HashMap<String, Double>()
        val bestWeightByExercise = HashMap<String, Double>()
        val prByExercise = LinkedHashMap<String, PersonalRecordBadge>()

        for (set in chronological) {
            val key = set.ejercicio.trim().lowercase(Locale.getDefault())
            if (key.isEmpty()) continue
            val prev1Rm = best1RmByExercise[key]
            val prevWeight = bestWeightByExercise[key]
            val current1Rm = set.estimated1RmKg
            val currentWeight = set.peso

            val isRecordAfterBaseline = prev1Rm != null && prevWeight != null &&
                (current1Rm > prev1Rm + 0.25 || currentWeight > prevWeight + 0.1)

            if (prev1Rm == null || current1Rm > prev1Rm) {
                best1RmByExercise[key] = current1Rm
            }
            if (prevWeight == null || currentWeight > prevWeight) {
                bestWeightByExercise[key] = currentWeight
            }

            if (isRecordAfterBaseline) {
                prByExercise[key] = PersonalRecordBadge(
                    exerciseName = set.ejercicio.trim(),
                    best1RmKg = best1RmByExercise[key] ?: current1Rm,
                    maxWeightKg = bestWeightByExercise[key] ?: currentWeight,
                    achievedDate = set.fecha,
                )
            }
        }
        return prByExercise.values.toList()
    }

    fun calculateStreak(
        sets: List<WorkoutSet>,
        todayIso: String = formatIsoDate(System.currentTimeMillis()),
    ): StreakResult {
        if (sets.isEmpty()) return StreakResult()

        val activeDays = sets.map { it.fecha }.filter { it.isNotBlank() }.distinct().sortedDescending()
        if (activeDays.isEmpty()) return StreakResult()

        val activeSet = activeDays.toHashSet()
        // Current streak: consecutive days ending today or yesterday (allows 1 rest day gap? No, strict daily or 48h gym cadence)
        // Use 48h-tolerant gym streak: consecutive active days where gap <= 2 days so rest days don't unfairly break a gym routine
        var currentStreak = 0
        val latest = activeDays.first()
        val daysSinceLatest = daysBetweenIso(latest, todayIso)
        if (daysSinceLatest <= 2) {
            currentStreak = 1
            var cursor = latest
            for (i in 1 until activeDays.size) {
                val prev = activeDays[i]
                val gap = daysBetweenIso(prev, cursor)
                if (gap in 1..2) {
                    currentStreak += 1
                    cursor = prev
                } else {
                    break
                }
            }
        }

        // Best streak across history (with <= 2 day gym-rest cadence)
        val asc = activeDays.sorted()
        var bestStreak = 1
        var run = 1
        for (i in 1 until asc.size) {
            val gap = daysBetweenIso(asc[i - 1], asc[i])
            if (gap in 1..2) {
                run += 1
                if (run > bestStreak) bestStreak = run
            } else {
                run = 1
            }
        }

        val weekStart = shiftIsoDate(todayIso, -6)
        val activeThisWeek = activeSet.count { it in weekStart..todayIso }
        val consistencyPct = ((activeThisWeek.toFloat() / 4f) * 100f).roundToInt().coerceIn(0, 100)

        return StreakResult(
            currentStreakDays = currentStreak,
            bestStreakDays = bestStreak,
            activeThisWeekDays = activeThisWeek,
            consistencyPercent = consistencyPct,
        )
    }

    fun calculateExperience(
        sets: List<WorkoutSet>,
        metrics: RealWorkoutMetrics,
        prs: List<PersonalRecordBadge>,
        streak: StreakResult,
        minigameTotalXp: Int,
    ): LevelProgress {
        if (!metrics.hasData && minigameTotalXp <= 0) {
            return LevelProgress()
        }

        val sessionsByDate = sets.groupBy { it.fecha }
        var workoutXp = 0
        for ((_, daySets) in sessionsByDate) {
            workoutXp += XP_PER_SESSION_DAY
            val hasFullMeta = daySets.any { !it.obs.isNullOrBlank() || it.pesoCorporal != null }
            if (hasFullMeta || daySets.size >= 6) {
                workoutXp += XP_COMPLETE_SESSION_BONUS
            }
        }

        val prXp = prs.size * XP_PER_PR
        val consistencyWeeks = metrics.distinctTrainingDays / 3
        val consistencyXp = consistencyWeeks * XP_WEEKLY_CONSISTENCY_BONUS
        val streakBonusXp = (streak.currentStreakDays / 7) * 50

        val totalXp = workoutXp + prXp + consistencyXp + streakBonusXp + minigameTotalXp.coerceAtLeast(0)
        return computeLevelProgress(totalXp)
    }

    fun calculateRecovery(
        metrics: RealWorkoutMetrics,
        nowMillis: Long = System.currentTimeMillis(),
    ): RecoveryResult {
        if (!metrics.hasData || metrics.latestSessionTimestampMillis <= 0L) {
            return RecoveryResult()
        }

        val elapsedHours = ((nowMillis - metrics.latestSessionTimestampMillis).coerceAtLeast(0L) / (60f * 60f * 1000f))
        val volumeFactor = (metrics.latestSessionVolumeKg / 8000.0).coerceIn(0.2, 1.3)
        val fatigueLoad = (25.0 * volumeFactor).roundToInt().coerceIn(10, 45)

        val recoveryPct = when {
            elapsedHours < 1f -> (100 - fatigueLoad).coerceIn(45, 90)
            elapsedHours < 24f -> {
                val ratio = (elapsedHours / 24f).coerceIn(0f, 1f)
                ((100 - fatigueLoad) + (fatigueLoad * ratio)).roundToInt().coerceIn(50, 100)
            }
            else -> 100
        }

        val energyPct = when {
            elapsedHours < 2f -> (recoveryPct - 8).coerceIn(40, 95)
            elapsedHours <= 48f -> recoveryPct
            elapsedHours <= 96f -> (recoveryPct - 15).coerceIn(60, 90)
            else -> 65
        }

        val label = when {
            recoveryPct < 70 -> "Recuperando musculatura"
            recoveryPct < 95 -> "En recuperación progresiva"
            elapsedHours <= 48f -> "Energía completa"
            else -> "Listo para activarse"
        }

        return RecoveryResult(
            energyPercent = energyPct,
            recoveryPercent = recoveryPct,
            statusLabel = label,
        )
    }

    fun updateBiologicalClock(
        metrics: RealWorkoutMetrics,
        previousLastUpdateMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
    ): BiologicalClockState {
        if (!metrics.hasData || metrics.latestSessionTimestampMillis <= 0L) {
            return BiologicalClockState(
                lastTrainingAtMillis = 0L,
                lastAvatarUpdateAtMillis = nowMillis,
                nextRecommendedActivityAtMillis = 0L,
                nextRecoveryCheckAtMillis = 0L,
                hoursSinceLastTraining = null,
                needsTrainingToday = false,
                isCycleDue24h = false,
                statusTitle = "Sin datos suficientes",
                statusSubtitle = "Registra tu primer entrenamiento para iniciar el reloj biológico de 24 h.",
            )
        }

        val lastTrain = metrics.latestSessionTimestampMillis
        val nextRecommended = lastTrain + HOURS_24_MILLIS
        val nextRecoveryCheck = lastTrain + HOURS_12_MILLIS
        val elapsedMillis = (nowMillis - lastTrain).coerceAtLeast(0L)
        val elapsedHours = (elapsedMillis / (60L * 60L * 1000L)).toInt()

        val todayIso = formatIsoDate(nowMillis)
        val trainedToday = metrics.latestSessionDate == todayIso
        val is24hPassed = nowMillis >= nextRecommended
        val needsTrainingToday = !trainedToday && (is24hPassed || (metrics.latestSessionDate != null && metrics.latestSessionDate < todayIso))

        val dayChangedSincePrevUpdate = previousLastUpdateMillis > 0L &&
            formatIsoDate(previousLastUpdateMillis) != todayIso
        val isCycleDue = is24hPassed || dayChangedSincePrevUpdate

        val (title, subtitle) = when {
            trainedToday && elapsedHours < 6 -> {
                "Sesión registrada hoy · Recuperación activa" to
                    "Próxima actualización del reloj biológico en ${24 - elapsedHours.coerceAtMost(23)} h."
            }
            trainedToday -> {
                "Entrenaste hoy · Asimilando carga" to
                    "El próximo ciclo de 24 h sugerirá tu siguiente sesión mañana."
            }
            elapsedHours in 24..47 -> {
                "¡Reloj biológico listo (24 h cumplidas)!" to
                    "Pasaron $elapsedHours h desde tu última sesión. Buen momento para entrenar hoy."
            }
            elapsedHours >= 48 -> {
                "Tu atleta te espera hace ${elapsedHours / 24} días" to
                    "Sin presión: una sesión corta hoy reactiva su energía y racha."
            }
            else -> {
                val hoursRemaining = (24 - elapsedHours).coerceAtLeast(1)
                "Descanso programado ($elapsedHours h)" to
                    "Faltan ~$hoursRemaining h para el próximo aviso de entrenamiento."
            }
        }

        return BiologicalClockState(
            lastTrainingAtMillis = lastTrain,
            lastAvatarUpdateAtMillis = nowMillis,
            nextRecommendedActivityAtMillis = nextRecommended,
            nextRecoveryCheckAtMillis = nextRecoveryCheck,
            hoursSinceLastTraining = elapsedHours,
            needsTrainingToday = needsTrainingToday,
            isCycleDue24h = isCycleDue,
            statusTitle = title,
            statusSubtitle = subtitle,
        )
    }

    fun calculateAvatarState(
        clock: BiologicalClockState,
        recovery: RecoveryResult,
        metrics: RealWorkoutMetrics,
        prs: List<PersonalRecordBadge>,
        isRestTimerActive: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
    ): AvatarMoodState {
        if (isRestTimerActive) return AvatarMoodState.PAUSA_ENTRE_SERIES
        if (!metrics.hasData) return AvatarMoodState.PREPARADO

        val todayIso = formatIsoDate(nowMillis)
        val hasPrToday = prs.any { it.achievedDate == todayIso }
        if (hasPrToday) return AvatarMoodState.CELEBRANDO_PR

        val elapsedHours = clock.hoursSinceLastTraining ?: 0
        val trainedToday = metrics.latestSessionDate == todayIso

        return when {
            trainedToday && elapsedHours < 2 && metrics.latestSessionVolumeKg >= 5000.0 -> AvatarMoodState.CANSADO
            trainedToday && elapsedHours < 2 -> AvatarMoodState.ENTRENANDO
            trainedToday && (recovery.recoveryPercent ?: 100) < 85 -> AvatarMoodState.RECUPERANDOSE
            trainedToday -> AvatarMoodState.CONTENTO_MEJORA
            elapsedHours >= 96 -> AvatarMoodState.DURMIENDO
            elapsedHours >= 48 -> AvatarMoodState.INVITANDO_ACTIVIDAD
            elapsedHours >= 24 -> AvatarMoodState.PREPARADO
            metrics.daysInLast7Days >= 3 -> AvatarMoodState.MOTIVADO
            else -> AvatarMoodState.DESCANSANDO
        }
    }

    fun getAvatarAnimation(
        moodState: AvatarMoodState,
        sportStyle: SportStyle,
        manualAction: AvatarActionAnimation? = null,
    ): AvatarActionAnimation {
        if (manualAction != null) return manualAction
        return when (moodState) {
            AvatarMoodState.ENTRENANDO -> when (sportStyle) {
                SportStyle.RUNNING, SportStyle.SOCCER -> AvatarActionAnimation.JOG_IN_PLACE
                SportStyle.CROSS_TRAINING, SportStyle.CALISTHENICS -> AvatarActionAnimation.JUMP_ROPE
                else -> AvatarActionAnimation.DUMBBELL_LIFT
            }
            AvatarMoodState.CELEBRANDO_PR, AvatarMoodState.CONTENTO_MEJORA -> AvatarActionAnimation.CELEBRATE
            AvatarMoodState.CANSADO -> AvatarActionAnimation.TOWEL_WIPE
            AvatarMoodState.RECUPERANDOSE -> AvatarActionAnimation.DRINK_WATER
            AvatarMoodState.DURMIENDO -> AvatarActionAnimation.SLEEP
            AvatarMoodState.PAUSA_ENTRE_SERIES, AvatarMoodState.DESCANSANDO -> AvatarActionAnimation.SIT_REST
            AvatarMoodState.PREPARADO, AvatarMoodState.MOTIVADO -> AvatarActionAnimation.STRETCH
            AvatarMoodState.INVITANDO_ACTIVIDAD -> AvatarActionAnimation.IDLE_BREATHE
        }
    }

    fun generateDailyMission(
        metrics: RealWorkoutMetrics,
        todayIso: String = formatIsoDate(System.currentTimeMillis()),
    ): DailyMission {
        val trainedToday = metrics.latestSessionDate == todayIso
        return if (!metrics.hasData) {
            DailyMission(
                title = "Primera sesión real",
                description = "Registra al menos 1 serie en la pestaña Registrar para ganar +100 XP.",
                completed = false,
                rewardXp = XP_PER_SESSION_DAY,
            )
        } else if (trainedToday) {
            DailyMission(
                title = "Sesión del día completada",
                description = "Registraste ${metrics.latestSessionSets} series hoy. ¡Hidrata y descansa bien!",
                completed = true,
                rewardXp = XP_PER_SESSION_DAY,
            )
        } else {
            DailyMission(
                title = "Activa tu día de gimnasio",
                description = "Registra una sesión hoy para mantener tu racha y actualizar el reloj de 24 h.",
                completed = false,
                rewardXp = XP_PER_SESSION_DAY,
            )
        }
    }

    fun buildBadges(
        metrics: RealWorkoutMetrics,
        prs: List<PersonalRecordBadge>,
        streak: StreakResult,
        level: Int,
    ): List<GamificationBadge> {
        return listOf(
            GamificationBadge(
                id = "first_set",
                title = "Primer Registro",
                description = "Registra tu primera serie real de entrenamiento.",
                unlocked = metrics.totalSets >= 1,
            ),
            GamificationBadge(
                id = "ten_sets",
                title = "Ritmo Constante",
                description = "Acumula 10 o más series registradas.",
                unlocked = metrics.totalSets >= 10,
            ),
            GamificationBadge(
                id = "first_pr",
                title = "Superación",
                description = "Mejora tu marca personal en algún ejercicio.",
                unlocked = prs.isNotEmpty(),
            ),
            GamificationBadge(
                id = "three_day_week",
                title = "Semana Activa",
                description = "Entrena 3 o más días en los últimos 7 días.",
                unlocked = metrics.daysInLast7Days >= 3,
            ),
            GamificationBadge(
                id = "streak_3",
                title = "Racha x3",
                description = "Mantén una racha de 3 días de entrenamiento.",
                unlocked = streak.bestStreakDays >= 3,
            ),
            GamificationBadge(
                id = "level_5",
                title = "Nivel 5",
                description = "Alcanza el nivel 5 de atleta.",
                unlocked = level >= 5,
            ),
        )
    }

    fun buildGamificationSnapshot(
        profile: AthleteProfileEntity,
        sets: List<WorkoutSet>,
        isRestTimerActive: Boolean,
        systemReduceMotion: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
        gamificationData: GamificationData? = null,
    ): GamificationSnapshot {
        val todayIso = formatIsoDate(nowMillis)
        val baseAppearance = profile.toAppearance()
        val appearance = if (gamificationData != null) {
            baseAppearance.copy(
                animalSpecies = AnimalSpecies.fromId(gamificationData.selectedAnimal),
                gender = AvatarGender.fromId(gamificationData.avatarGender),
            )
        } else {
            baseAppearance
        }
        val metrics = extractRealWorkoutMetrics(sets, nowMillis)
        val prs = detectPersonalRecords(sets)
        val streak = calculateStreak(sets, todayIso)
        val levelProgress = calculateExperience(
            sets = sets,
            metrics = metrics,
            prs = prs,
            streak = streak,
            minigameTotalXp = profile.minigameTotalXpEarned,
        )
        val recovery = calculateRecovery(metrics, nowMillis)
        val clock = updateBiologicalClock(metrics, profile.lastAvatarUpdateAtMillis, nowMillis)
        val mood = calculateAvatarState(clock, recovery, metrics, prs, isRestTimerActive, nowMillis)
        val animation = getAvatarAnimation(mood, appearance.sportStyle)
        val dailyMinigameUsed = if (profile.minigameXpDate == todayIso) profile.minigameXpEarnedToday else 0
        val unlockedBgTier = when {
            levelProgress.level >= 8 -> 3
            levelProgress.level >= 3 -> 2
            else -> 1
        }

        return GamificationSnapshot(
            enabled = profile.gamificationEnabled,
            onboardingCompleted = profile.onboardingCompleted,
            appearance = appearance,
            metrics = metrics,
            levelProgress = levelProgress,
            streak = streak,
            recovery = recovery,
            biologicalClock = clock,
            moodState = mood,
            recommendedAnimation = animation,
            personalRecords = prs,
            dailyMission = generateDailyMission(metrics, todayIso),
            badges = buildBadges(metrics, prs, streak, levelProgress.level),
            virtualCoins = profile.virtualCoins,
            minigameXpEarnedToday = dailyMinigameUsed,
            minigameDailyXpCap = MINIGAME_DAILY_XP_CAP,
            reduceMotion = systemReduceMotion || profile.reduceMotionOverride,
            unlockedBackgroundTier = unlockedBgTier,
        )
    }

    fun formatIsoDate(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(millis))
    }

    fun shiftIsoDate(isoDate: String, deltaDays: Int): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsed = runCatching { sdf.parse(isoDate) }.getOrNull() ?: Date()
        val cal = Calendar.getInstance(Locale.US).apply {
            time = parsed
            add(Calendar.DAY_OF_YEAR, deltaDays)
        }
        return sdf.format(cal.time)
    }

    fun daysBetweenIso(earlierIso: String, laterIso: String): Int {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val d1 = runCatching { sdf.parse(earlierIso) }.getOrNull() ?: return 0
        val d2 = runCatching { sdf.parse(laterIso) }.getOrNull() ?: return 0
        val diffMillis = d2.time - d1.time
        return (diffMillis / HOURS_24_MILLIS).toInt()
    }

    private fun resolveLatestSessionTimestamp(
        latestDate: String,
        latestSets: List<WorkoutSet>,
        todayIso: String,
        nowMillis: Long,
    ): Long {
        if (latestDate == todayIso) {
            val maxCreated = latestSets.maxOfOrNull { it.createdAtMillis } ?: 0L
            return if (maxCreated > 0L) maxCreated.coerceAtMost(nowMillis) else nowMillis
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsed = runCatching { sdf.parse(latestDate) }.getOrNull()
        if (parsed != null) {
            val cal = Calendar.getInstance(Locale.US).apply {
                time = parsed
                set(Calendar.HOUR_OF_DAY, 18)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            return cal.timeInMillis.coerceAtMost(nowMillis)
        }
        return latestSets.maxOfOrNull { it.createdAtMillis } ?: nowMillis
    }
}

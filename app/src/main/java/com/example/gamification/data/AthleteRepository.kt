package com.example.gamification.data

import android.util.Log
import com.example.gamification.engine.AccessoryType
import com.example.gamification.engine.AthleteAppearance
import com.example.gamification.engine.AvatarPalettes
import com.example.gamification.engine.BiologicalClockState
import com.example.gamification.engine.GamificationEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AthleteRepository(
    private val dao: AthleteProfileDao,
) {
    private val writeMutex = Mutex()

    fun observeProfile(): Flow<AthleteProfileEntity> {
        return dao.observeProfile().map { it ?: AthleteProfileEntity() }
    }

    suspend fun getOrCreateProfile(): AthleteProfileEntity {
        return try {
            withContext(Dispatchers.IO) {
                writeMutex.withLock {
                    dao.initializeIfMissing()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("AthleteRepository", "Falling back to default AthleteProfileEntity", e)
            AthleteProfileEntity()
        }
    }

    suspend fun saveAppearance(
        appearance: AthleteAppearance,
        markOnboardingComplete: Boolean = true,
    ): Result<Unit> {
        return safeDbWrite {
            val current = dao.initializeIfMissing()
            dao.upsertProfile(current.withSanitizedAppearance(appearance, markOnboardingComplete))
        }
    }

    suspend fun saveSettings(
        gamificationEnabled: Boolean,
        reduceMotionOverride: Boolean,
        appearance: AthleteAppearance,
    ): Result<Unit> {
        return safeDbWrite {
            val current = dao.initializeIfMissing()
            dao.upsertProfile(
                current
                    .withSanitizedAppearance(appearance, markOnboardingComplete = current.onboardingCompleted)
                    .copy(
                        gamificationEnabled = gamificationEnabled,
                        reduceMotionOverride = reduceMotionOverride,
                    )
            )
        }
    }

    suspend fun setGamificationEnabled(enabled: Boolean): Result<Unit> {
        return safeDbWrite {
            val current = dao.initializeIfMissing()
            dao.upsertProfile(current.copy(gamificationEnabled = enabled))
        }
    }

    suspend fun setReduceMotionOverride(reduceMotion: Boolean): Result<Unit> {
        return safeDbWrite {
            val current = dao.initializeIfMissing()
            dao.upsertProfile(current.copy(reduceMotionOverride = reduceMotion))
        }
    }

    suspend fun syncBiologicalClockTimestamps(clock: BiologicalClockState): Result<Unit> {
        return safeDbWrite {
            val current = dao.initializeIfMissing()
            if (current.lastTrainingAtMillis == clock.lastTrainingAtMillis &&
                current.lastAvatarUpdateAtMillis == clock.lastAvatarUpdateAtMillis &&
                current.nextRecommendedActivityAtMillis == clock.nextRecommendedActivityAtMillis &&
                current.nextRecoveryCheckAtMillis == clock.nextRecoveryCheckAtMillis
            ) {
                return@safeDbWrite
            }
            dao.upsertProfile(
                current.copy(
                    lastTrainingAtMillis = clock.lastTrainingAtMillis.coerceAtLeast(0L),
                    lastAvatarUpdateAtMillis = clock.lastAvatarUpdateAtMillis.coerceAtLeast(0L),
                    nextRecommendedActivityAtMillis = clock.nextRecommendedActivityAtMillis.coerceAtLeast(0L),
                    nextRecoveryCheckAtMillis = clock.nextRecoveryCheckAtMillis.coerceAtLeast(0L),
                )
            )
        }
    }

    /**
     * Awards mini-game XP & coins while strictly enforcing the daily mini-game XP cap
     * so mini-games can never replace real gym workouts as the primary progression source.
     */
    suspend fun recordMiniGameResult(
        todayIso: String,
        won: Boolean,
    ): Result<MiniGameRewardOutcome> {
        return try {
            withContext(Dispatchers.IO) {
                writeMutex.withLock {
                    val current = dao.initializeIfMissing()
                    val earnedBeforeToday = if (current.minigameXpDate == todayIso) {
                        current.minigameXpEarnedToday.coerceAtLeast(0)
                    } else {
                        0
                    }
                    val remainingCap = (GamificationEngine.MINIGAME_DAILY_XP_CAP - earnedBeforeToday)
                        .coerceAtLeast(0)
                    val rawXp = if (won) GamificationEngine.MINIGAME_WIN_XP else GamificationEngine.MINIGAME_PLAY_XP
                    val grantedXp = rawXp.coerceAtMost(remainingCap)
                    val grantedCoins = if (won) 5 else 2

                    dao.upsertProfile(
                        current.copy(
                            minigameXpDate = todayIso,
                            minigameXpEarnedToday = earnedBeforeToday + grantedXp,
                            minigameTotalXpEarned = (current.minigameTotalXpEarned + grantedXp).coerceAtLeast(0),
                            virtualCoins = (current.virtualCoins + grantedCoins).coerceAtLeast(0),
                        )
                    )
                    Result.success(
                        MiniGameRewardOutcome(
                            xpGranted = grantedXp,
                            coinsGranted = grantedCoins,
                            dailyXpUsed = earnedBeforeToday + grantedXp,
                            dailyXpCap = GamificationEngine.MINIGAME_DAILY_XP_CAP,
                            capReached = remainingCap == 0,
                        )
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetMiniGameCounters(): Result<Unit> {
        return safeDbWrite {
            val current = dao.initializeIfMissing()
            dao.upsertProfile(
                current.copy(
                    minigameXpDate = "",
                    minigameXpEarnedToday = 0,
                    minigameTotalXpEarned = 0,
                    virtualCoins = 0,
                )
            )
        }
    }

    private suspend inline fun safeDbWrite(crossinline block: suspend () -> Unit): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                writeMutex.withLock {
                    block()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun AthleteProfileEntity.withSanitizedAppearance(
        appearance: AthleteAppearance,
        markOnboardingComplete: Boolean,
    ): AthleteProfileEntity {
        val safeName = appearance.name.trim().ifBlank { "Mi Atleta" }.take(24)
        return copy(
            onboardingCompleted = if (markOnboardingComplete) true else onboardingCompleted,
            name = safeName,
            gender = appearance.gender.id,
            animalSpecies = appearance.animalSpecies.id,
            sportStyle = appearance.sportStyle.id,
            bodyBuild = appearance.bodyBuild.id,
            skinToneIndex = appearance.skinToneIndex.coerceIn(0, AvatarPalettes.skinTones.lastIndex),
            eyeColorIndex = appearance.eyeColorIndex.coerceIn(0, AvatarPalettes.eyeColors.lastIndex),
            hairStyle = appearance.hairStyle.id,
            hairColorIndex = appearance.hairColorIndex.coerceIn(0, AvatarPalettes.hairColors.lastIndex),
            facialHair = appearance.facialHair.id,
            topStyle = appearance.topStyle.id,
            bottomStyle = appearance.bottomStyle.id,
            primaryColorIndex = appearance.primaryColorIndex.coerceIn(0, AvatarPalettes.outfitColors.lastIndex),
            secondaryColorIndex = appearance.secondaryColorIndex.coerceIn(0, AvatarPalettes.outfitColors.lastIndex),
            shoeColorIndex = appearance.shoeColorIndex.coerceIn(0, AvatarPalettes.shoeColors.lastIndex),
            accessoriesCsv = AccessoryType.toCsv(appearance.accessories),
        )
    }
}

data class MiniGameRewardOutcome(
    val xpGranted: Int,
    val coinsGranted: Int,
    val dailyXpUsed: Int,
    val dailyXpCap: Int,
    val capReached: Boolean,
)

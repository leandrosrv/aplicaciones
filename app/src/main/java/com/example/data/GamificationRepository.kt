package com.example.data

import com.example.gamification.engine.AnimalSpecies
import com.example.gamification.engine.AvatarGender
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class GamificationRepository(
    private val dao: GamificationDao,
) {
    private val writeMutex = Mutex()

    fun observeGamificationData(): Flow<GamificationData> {
        return dao.observeGamificationData().map { entity ->
            entity ?: GamificationData()
        }
    }

    suspend fun initialize(): Result<GamificationData> {
        return try {
            withContext(Dispatchers.IO) {
                writeMutex.withLock {
                    Result.success(dao.initializeIfMissing())
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGamificationData(): GamificationData {
        return try {
            withContext(Dispatchers.IO) {
                dao.getGamificationDataOnce() ?: GamificationData()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            GamificationData()
        }
    }

    suspend fun saveGamificationData(
        xp: Int,
        level: Int,
        lastSessionTimestamp: Long,
        streak: Int,
    ): Result<Unit> {
        return try {
            require(xp >= 0) { "La experiencia (XP) no puede ser negativa." }
            require(level >= 1) { "El nivel debe ser mayor o igual a 1." }
            require(lastSessionTimestamp >= 0L) { "La marca temporal no puede ser negativa." }
            require(streak >= 0) { "La racha no puede ser negativa." }
            withContext(Dispatchers.IO) {
                writeMutex.withLock {
                    val current = dao.initializeIfMissing()
                    dao.upsertGamificationData(
                        current.copy(
                            xp = xp,
                            level = level,
                            lastSessionTimestamp = lastSessionTimestamp,
                            streak = streak,
                        )
                    )
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveAvatarPreferences(
        animal: AnimalSpecies,
        gender: AvatarGender,
    ): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                writeMutex.withLock {
                    val current = dao.initializeIfMissing()
                    dao.upsertGamificationData(
                        current.copy(
                            selectedAnimal = animal.id,
                            avatarGender = gender.id,
                        )
                    )
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetGamificationData(): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                writeMutex.withLock {
                    val current = dao.initializeIfMissing()
                    dao.upsertGamificationData(
                        GamificationData(
                            id = GamificationData.SINGLETON_ID,
                            xp = 0,
                            level = 1,
                            lastSessionTimestamp = 0L,
                            streak = 0,
                            selectedAnimal = current.selectedAnimal,
                            avatarGender = current.avatarGender,
                        )
                    )
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

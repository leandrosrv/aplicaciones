package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.gamification.engine.AnimalSpecies
import com.example.gamification.engine.AvatarGender
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "gamification_data")
data class GamificationData(
    @PrimaryKey val id: Long = SINGLETON_ID,
    val xp: Int = 0,
    val level: Int = 1,
    val lastSessionTimestamp: Long = 0L,
    val streak: Int = 0,
    val selectedAnimal: String = AnimalSpecies.LION.id,
    val avatarGender: String = AvatarGender.MALE.id,
) {
    companion object {
        const val SINGLETON_ID = 1L
    }
}

@Dao
interface GamificationDao {
    @Query("SELECT * FROM gamification_data WHERE id = 1 LIMIT 1")
    fun observeGamificationData(): Flow<GamificationData?>

    @Query("SELECT * FROM gamification_data WHERE id = 1 LIMIT 1")
    suspend fun getGamificationDataOnce(): GamificationData?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitial(entity: GamificationData): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGamificationData(entity: GamificationData)

    @Update
    suspend fun updateGamificationData(entity: GamificationData)

    @Delete
    suspend fun deleteGamificationData(entity: GamificationData)

    @Query("UPDATE gamification_data SET selectedAnimal = :animal, avatarGender = :gender WHERE id = 1")
    suspend fun updateAvatarPreferences(animal: String, gender: String)

    @Transaction
    suspend fun initializeIfMissing(): GamificationData {
        insertInitial(GamificationData())
        return getGamificationDataOnce() ?: GamificationData()
    }

    @Query("DELETE FROM gamification_data")
    suspend fun clearGamificationData()
}

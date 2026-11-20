package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "workout_sets")
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val fecha: String,
    val grupo: String,
    val ejercicio: String,
    val serie: Long,
    val peso: Double,
    val reps: Long,
    val rir: Long,
    val pesoCorporal: Double? = null,
    val obs: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "muscle_groups",
    indices = [Index(value = ["name"], unique = true)],
)
data class MuscleGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
)

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_sets ORDER BY fecha DESC, createdAtMillis DESC, serie DESC")
    fun observeAllSets(): Flow<List<WorkoutSetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(entity: WorkoutSetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(entities: List<WorkoutSetEntity>): List<Long>

    @Update
    suspend fun updateSet(entity: WorkoutSetEntity)

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun deleteSetById(id: Long)

    @Query("DELETE FROM workout_sets")
    suspend fun deleteAllSets()

    @Query("SELECT * FROM muscle_groups ORDER BY name COLLATE NOCASE ASC")
    fun observeCustomGroups(): Flow<List<MuscleGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCustomGroup(entity: MuscleGroupEntity): Long
}

@Database(
    entities = [WorkoutSetEntity::class, MuscleGroupEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var instance: WorkoutDatabase? = null

        fun getInstance(context: Context): WorkoutDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "registro_gym_db",
                ).build().also { instance = it }
            }
        }
    }
}

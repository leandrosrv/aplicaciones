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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gamification.data.AthleteProfileDao
import com.example.gamification.data.AthleteProfileEntity
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
    entities = [
        WorkoutSetEntity::class,
        MuscleGroupEntity::class,
        GamificationData::class,
        AthleteProfileEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun gamificationDao(): GamificationDao
    abstract fun athleteProfileDao(): AthleteProfileDao

    companion object {
        private const val CREATE_GAMIFICATION_DATA_SQL = """
            CREATE TABLE IF NOT EXISTS `gamification_data` (
                `id` INTEGER NOT NULL,
                `xp` INTEGER NOT NULL,
                `level` INTEGER NOT NULL,
                `lastSessionTimestamp` INTEGER NOT NULL,
                `streak` INTEGER NOT NULL,
                `selectedAnimal` TEXT NOT NULL,
                `avatarGender` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """

        private const val CREATE_ATHLETE_PROFILE_SQL = """
            CREATE TABLE IF NOT EXISTS `athlete_profile` (
                `id` INTEGER NOT NULL,
                `gamificationEnabled` INTEGER NOT NULL,
                `onboardingCompleted` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `gender` TEXT NOT NULL,
                `animalSpecies` TEXT NOT NULL,
                `sportStyle` TEXT NOT NULL,
                `bodyBuild` TEXT NOT NULL,
                `skinToneIndex` INTEGER NOT NULL,
                `eyeColorIndex` INTEGER NOT NULL,
                `hairStyle` TEXT NOT NULL,
                `hairColorIndex` INTEGER NOT NULL,
                `facialHair` TEXT NOT NULL,
                `topStyle` TEXT NOT NULL,
                `bottomStyle` TEXT NOT NULL,
                `primaryColorIndex` INTEGER NOT NULL,
                `secondaryColorIndex` INTEGER NOT NULL,
                `shoeColorIndex` INTEGER NOT NULL,
                `accessoriesCsv` TEXT NOT NULL,
                `virtualCoins` INTEGER NOT NULL,
                `lastTrainingAtMillis` INTEGER NOT NULL,
                `lastAvatarUpdateAtMillis` INTEGER NOT NULL,
                `nextRecommendedActivityAtMillis` INTEGER NOT NULL,
                `nextRecoveryCheckAtMillis` INTEGER NOT NULL,
                `minigameXpDate` TEXT NOT NULL,
                `minigameXpEarnedToday` INTEGER NOT NULL,
                `minigameTotalXpEarned` INTEGER NOT NULL,
                `reduceMotionOverride` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """

        private fun tableExists(db: SupportSQLiteDatabase, tableName: String): Boolean {
            db.query(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
                arrayOf(tableName),
            ).use { cursor ->
                return cursor.moveToFirst()
            }
        }

        private fun hasColumn(
            db: SupportSQLiteDatabase,
            tableName: String,
            columnName: String,
        ): Boolean {
            if (!tableExists(db, tableName)) return false
            db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex < 0) return false
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex).equals(columnName, ignoreCase = true)) {
                        return true
                    }
                }
                return false
            }
        }

        private fun addColumnIfMissing(
            db: SupportSQLiteDatabase,
            tableName: String,
            columnName: String,
            columnDef: String,
        ) {
            if (!hasColumn(db, tableName, columnName)) {
                db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnDef")
            }
        }

        private fun ensureGamificationDataSchema(db: SupportSQLiteDatabase) {
            db.execSQL(CREATE_GAMIFICATION_DATA_SQL.trimIndent())
            addColumnIfMissing(db, "gamification_data", "xp", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "gamification_data", "level", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing(db, "gamification_data", "lastSessionTimestamp", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "gamification_data", "streak", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "gamification_data", "selectedAnimal", "TEXT NOT NULL DEFAULT 'LION'")
            addColumnIfMissing(db, "gamification_data", "avatarGender", "TEXT NOT NULL DEFAULT 'MALE'")
        }

        private fun ensureAthleteProfileSchema(db: SupportSQLiteDatabase) {
            db.execSQL(CREATE_ATHLETE_PROFILE_SQL.trimIndent())
            addColumnIfMissing(db, "athlete_profile", "gamificationEnabled", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing(db, "athlete_profile", "onboardingCompleted", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "athlete_profile", "name", "TEXT NOT NULL DEFAULT 'Mi Atleta'")
            addColumnIfMissing(db, "athlete_profile", "gender", "TEXT NOT NULL DEFAULT 'MALE'")
            addColumnIfMissing(db, "athlete_profile", "animalSpecies", "TEXT NOT NULL DEFAULT 'LION'")
            addColumnIfMissing(db, "athlete_profile", "sportStyle", "TEXT NOT NULL DEFAULT 'HYPERTROPHY'")
            addColumnIfMissing(db, "athlete_profile", "bodyBuild", "TEXT NOT NULL DEFAULT 'ATHLETIC'")
            addColumnIfMissing(db, "athlete_profile", "skinToneIndex", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "athlete_profile", "eyeColorIndex", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "athlete_profile", "hairStyle", "TEXT NOT NULL DEFAULT 'SHORT_SPIKY'")
            addColumnIfMissing(db, "athlete_profile", "hairColorIndex", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing(db, "athlete_profile", "facialHair", "TEXT NOT NULL DEFAULT 'NONE'")
            addColumnIfMissing(db, "athlete_profile", "topStyle", "TEXT NOT NULL DEFAULT 'TANK_TOP'")
            addColumnIfMissing(db, "athlete_profile", "bottomStyle", "TEXT NOT NULL DEFAULT 'SHORTS'")
            addColumnIfMissing(db, "athlete_profile", "primaryColorIndex", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "athlete_profile", "secondaryColorIndex", "INTEGER NOT NULL DEFAULT 2")
            addColumnIfMissing(db, "athlete_profile", "shoeColorIndex", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "athlete_profile", "accessoriesCsv", "TEXT NOT NULL DEFAULT 'WATCH,BOTTLE'")
            addColumnIfMissing(db, "athlete_profile", "virtualCoins", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "athlete_profile", "lastTrainingAtMillis", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "athlete_profile", "lastAvatarUpdateAtMillis", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "athlete_profile", "nextRecommendedActivityAtMillis", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "athlete_profile", "nextRecoveryCheckAtMillis", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "athlete_profile", "minigameXpDate", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "athlete_profile", "minigameXpEarnedToday", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "athlete_profile", "minigameTotalXpEarned", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "athlete_profile", "reduceMotionOverride", "INTEGER NOT NULL DEFAULT 0")
        }

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `GamificationData` (
                        `id` INTEGER NOT NULL,
                        `xp` INTEGER NOT NULL,
                        `currentLevel` INTEGER NOT NULL,
                        `lastSessionDate` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (tableExists(db, "GamificationData")) {
                    addColumnIfMissing(
                        db,
                        "GamificationData",
                        "lastUpdatedAtMillis",
                        "INTEGER NOT NULL DEFAULT 0",
                    )
                }
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureGamificationDataSchema(db)
                if (tableExists(db, "GamificationData")) {
                    val xpExpr = if (hasColumn(db, "GamificationData", "xp")) {
                        "CAST(`xp` AS INTEGER)"
                    } else {
                        "0"
                    }
                    val levelExpr = when {
                        hasColumn(db, "GamificationData", "currentLevel") -> "`currentLevel`"
                        hasColumn(db, "GamificationData", "level") -> "`level`"
                        else -> "1"
                    }
                    val tsExpr = when {
                        hasColumn(db, "GamificationData", "lastUpdatedAtMillis") -> "`lastUpdatedAtMillis`"
                        hasColumn(db, "GamificationData", "lastSessionTimestamp") -> "`lastSessionTimestamp`"
                        else -> "0"
                    }
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO `gamification_data` (`id`, `xp`, `level`, `lastSessionTimestamp`, `streak`, `selectedAnimal`, `avatarGender`)
                        SELECT `id`, $xpExpr, $levelExpr, $tsExpr, 0, 'LION', 'MALE'
                        FROM `GamificationData`
                        """.trimIndent()
                    )
                    db.execSQL("DROP TABLE IF EXISTS `GamificationData`")
                }
            }
        }

        val MIGRATION_1_4: Migration = object : Migration(1, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureGamificationDataSchema(db)
            }
        }

        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureGamificationDataSchema(db)
                ensureAthleteProfileSchema(db)
            }
        }

        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureGamificationDataSchema(db)
                ensureAthleteProfileSchema(db)
            }
        }

        val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureGamificationDataSchema(db)
                ensureAthleteProfileSchema(db)
                if (tableExists(db, "athlete_profile") &&
                    hasColumn(db, "athlete_profile", "animalSpecies") &&
                    hasColumn(db, "athlete_profile", "gender")
                ) {
                    db.execSQL(
                        """
                        UPDATE `gamification_data`
                        SET `selectedAnimal` = COALESCE((SELECT `animalSpecies` FROM `athlete_profile` WHERE `id` = 1), `selectedAnimal`),
                            `avatarGender` = COALESCE((SELECT `gender` FROM `athlete_profile` WHERE `id` = 1), `avatarGender`)
                        """.trimIndent()
                    )
                }
            }
        }

        @Volatile
        private var instance: WorkoutDatabase? = null

        fun getInstance(context: Context): WorkoutDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "registro_gym_db",
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_1_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                    )
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}

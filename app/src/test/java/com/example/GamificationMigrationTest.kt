package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.GamificationData
import com.example.data.GamificationRepository
import com.example.data.WorkoutDatabase
import com.example.data.WorkoutRepository
import com.example.gamification.data.AthleteRepository
import com.example.gamification.engine.AccessoryType
import com.example.gamification.engine.AnimalSpecies
import com.example.gamification.engine.AthleteAppearance
import com.example.gamification.engine.AvatarGender
import com.example.gamification.engine.SportStyle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GamificationMigrationTest {

    private lateinit var context: Context
    private val testDbName = "test_migration_gym.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(testDbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(testDbName)
    }

    @Test
    fun `migration from v1 to v7 creates gamification tables and preserves existing workout data`() = runBlocking {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(testDbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `workout_sets` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `fecha` TEXT NOT NULL,
                                `grupo` TEXT NOT NULL,
                                `ejercicio` TEXT NOT NULL,
                                `serie` INTEGER NOT NULL,
                                `peso` REAL NOT NULL,
                                `reps` INTEGER NOT NULL,
                                `rir` INTEGER NOT NULL,
                                `pesoCorporal` REAL,
                                `obs` TEXT,
                                `createdAtMillis` INTEGER NOT NULL,
                                `updatedAtMillis` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `muscle_groups` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `name` TEXT NOT NULL,
                                `createdAtMillis` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            "CREATE UNIQUE INDEX IF NOT EXISTS `index_muscle_groups_name` ON `muscle_groups` (`name`)"
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )

        helper.writableDatabase.use { v1Db ->
            v1Db.execSQL(
                """
                INSERT INTO workout_sets (fecha, grupo, ejercicio, serie, peso, reps, rir, pesoCorporal, obs, createdAtMillis, updatedAtMillis)
                VALUES ('2026-11-20', 'Pecho', 'Press banca', 1, 82.5, 8, 2, 74.2, 'Buena técnica', 1700000000000, 1700000000000)
                """.trimIndent()
            )
            v1Db.execSQL(
                "INSERT INTO muscle_groups (name, createdAtMillis) VALUES ('Antebrazos', 1700000000000)"
            )
        }
        helper.close()

        val migratedDb = Room.databaseBuilder(context, WorkoutDatabase::class.java, testDbName)
            .addMigrations(
                WorkoutDatabase.MIGRATION_1_2,
                WorkoutDatabase.MIGRATION_2_3,
                WorkoutDatabase.MIGRATION_3_4,
                WorkoutDatabase.MIGRATION_1_4,
                WorkoutDatabase.MIGRATION_4_5,
                WorkoutDatabase.MIGRATION_5_6,
                WorkoutDatabase.MIGRATION_6_7,
            )
            .allowMainThreadQueries()
            .build()

        try {
            val workoutRepo = WorkoutRepository(migratedDb.workoutDao())
            val gamificationDao = migratedDb.gamificationDao()
            val gamificationRepo = GamificationRepository(gamificationDao)
            val athleteRepo = AthleteRepository(migratedDb.athleteProfileDao())

            // Existing workout data & custom groups remain 100% preserved
            val sets = workoutRepo.observeWorkoutSets().first()
            assertEquals(1, sets.size)
            assertEquals("Press banca", sets.first().ejercicio)
            assertEquals(82.5, sets.first().peso, 0.001)
            assertEquals(74.2, sets.first().pesoCorporal!!, 0.001)
            assertEquals("Buena técnica", sets.first().obs)

            val customGroups = workoutRepo.observeCustomGroupNames().first()
            assertEquals(listOf("Antebrazos"), customGroups)

            // Before initializeIfMissing(), table is empty and repository returns safe defaults
            assertNull(gamificationDao.getGamificationDataOnce())
            assertEquals(GamificationData(), gamificationRepo.observeGamificationData().first())

            val initialized = gamificationDao.initializeIfMissing()
            assertEquals(GamificationData(id = 1L, xp = 0, level = 1, lastSessionTimestamp = 0L, streak = 0), initialized)

            val saveRes = gamificationRepo.saveGamificationData(
                xp = 250,
                level = 3,
                lastSessionTimestamp = 1795522000000L,
                streak = 5,
            )
            assertTrue(saveRes.isSuccess)

            val afterSecondInit = gamificationDao.initializeIfMissing()
            assertEquals(250, afterSecondInit.xp)
            assertEquals(3, afterSecondInit.level)
            assertEquals(1795522000000L, afterSecondInit.lastSessionTimestamp)
            assertEquals(5, afterSecondInit.streak)

            // Save animal avatar & gender into gamification_data and ensure XP updates preserve them
            assertTrue(gamificationRepo.saveAvatarPreferences(AnimalSpecies.TIGER, AvatarGender.FEMALE).isSuccess)
            assertTrue(gamificationRepo.saveGamificationData(310, 4, 1795523000000L, 6).isSuccess)
            val updatedData = gamificationRepo.getGamificationData()
            assertEquals(AnimalSpecies.TIGER.id, updatedData.selectedAnimal)
            assertEquals(AvatarGender.FEMALE.id, updatedData.avatarGender)
            assertEquals(310, updatedData.xp)

            // Verify AthleteProfile persistence & toggle
            val appearance = AthleteAppearance(
                name = "Leandro",
                gender = AvatarGender.FEMALE,
                animalSpecies = AnimalSpecies.FOX,
                sportStyle = SportStyle.POWERLIFTING,
                accessories = setOf(AccessoryType.CAP, AccessoryType.GLOVES),
            )
            assertTrue(athleteRepo.saveAppearance(appearance).isSuccess)
            val savedProfile = athleteRepo.getOrCreateProfile()
            assertTrue(savedProfile.onboardingCompleted)
            assertEquals("Leandro", savedProfile.name)
            assertEquals(AvatarGender.FEMALE.id, savedProfile.gender)
            assertEquals(AnimalSpecies.FOX.id, savedProfile.animalSpecies)
            assertEquals(SportStyle.POWERLIFTING.id, savedProfile.sportStyle)
            assertTrue(AccessoryType.CAP in savedProfile.toAppearance().accessories)

            assertTrue(athleteRepo.setGamificationEnabled(false).isSuccess)
            assertFalse(athleteRepo.getOrCreateProfile().gamificationEnabled)

            // Existing workout sets remain intact
            assertEquals(1, workoutRepo.observeWorkoutSets().first().size)
        } finally {
            migratedDb.close()
        }
    }

    @Test
    fun `minigame daily XP cap prevents grinding without training`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val athleteRepo = AthleteRepository(db.athleteProfileDao())
            val today = "2026-11-24"

            val r1 = athleteRepo.recordMiniGameResult(today, won = true).getOrThrow()
            val r2 = athleteRepo.recordMiniGameResult(today, won = true).getOrThrow()
            val r3 = athleteRepo.recordMiniGameResult(today, won = true).getOrThrow()
            val r4 = athleteRepo.recordMiniGameResult(today, won = true).getOrThrow()

            assertEquals(5, r1.xpGranted)
            assertEquals(5, r2.xpGranted)
            assertEquals(5, r3.xpGranted)
            assertEquals(0, r4.xpGranted)
            assertTrue(r4.capReached)
            assertEquals(15, r4.dailyXpUsed)
        } finally {
            db.close()
        }
    }

    @Test
    fun `gamification repository rejects invalid inputs safely`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = GamificationRepository(db.gamificationDao())
            assertTrue(repo.saveGamificationData(xp = -10, level = 1, lastSessionTimestamp = 0L, streak = 0).isFailure)
            assertTrue(repo.saveGamificationData(xp = 10, level = 0, lastSessionTimestamp = 0L, streak = 0).isFailure)
            assertTrue(repo.saveGamificationData(xp = 10, level = 1, lastSessionTimestamp = -1L, streak = 0).isFailure)
            assertTrue(repo.saveGamificationData(xp = 10, level = 1, lastSessionTimestamp = 0L, streak = -1).isFailure)
            assertEquals(GamificationData(), repo.getGamificationData())
        } finally {
            db.close()
        }
    }

    @Test
    fun `settings persistence sanitizes inputs and leaves workout data untouched`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val workoutRepo = WorkoutRepository(db.workoutDao())
            val gamificationRepo = GamificationRepository(db.gamificationDao())
            val athleteRepo = AthleteRepository(db.athleteProfileDao())

            // Record real workout data first
            assertTrue(
                workoutRepo.addWorkoutSet(
                    fecha = "2026-11-24",
                    grupo = "Espalda",
                    ejercicio = "Remo con barra",
                    serie = 1L,
                    peso = 75.0,
                    reps = 10L,
                    rir = 2L,
                    pesoCorporal = 74.0,
                    obs = "Controlado",
                ).isSuccess
            )
            assertTrue(workoutRepo.addCustomMuscleGroup("Antebrazos").isSuccess)

            // Save settings with out-of-bounds indices & toggle gamification OFF
            val res = athleteRepo.saveSettings(
                gamificationEnabled = false,
                reduceMotionOverride = true,
                appearance = AthleteAppearance(
                    name = "   Atleta Pro   ",
                    gender = AvatarGender.FEMALE,
                    animalSpecies = AnimalSpecies.PANTHER,
                    sportStyle = SportStyle.RUNNING,
                    skinToneIndex = -3,
                    primaryColorIndex = 999,
                    accessories = setOf(AccessoryType.WATCH, AccessoryType.TOWEL),
                ),
            )
            assertTrue(res.isSuccess)

            val profile = athleteRepo.getOrCreateProfile()
            assertFalse(profile.gamificationEnabled)
            assertTrue(profile.reduceMotionOverride)
            assertEquals("Atleta Pro", profile.name)
            assertEquals(AvatarGender.FEMALE, profile.toAppearance().gender)
            assertEquals(AnimalSpecies.PANTHER, profile.toAppearance().animalSpecies)
            assertEquals(SportStyle.RUNNING.id, profile.sportStyle)
            assertEquals(0, profile.skinToneIndex)
            assertTrue(profile.primaryColorIndex in 0..7)
            assertEquals(setOf(AccessoryType.WATCH, AccessoryType.TOWEL), profile.toAppearance().accessories)

            // Reset game progress only (keeps selectedAnimal & avatarGender in GamificationData)
            assertTrue(gamificationRepo.saveAvatarPreferences(AnimalSpecies.EAGLE, AvatarGender.FEMALE).isSuccess)
            assertTrue(gamificationRepo.saveGamificationData(300, 3, 1795520000000L, 4).isSuccess)
            assertTrue(gamificationRepo.resetGamificationData().isSuccess)
            val postResetData = gamificationRepo.getGamificationData()
            assertEquals(0, postResetData.xp)
            assertEquals(1, postResetData.level)
            assertEquals(AnimalSpecies.EAGLE.id, postResetData.selectedAnimal)
            assertEquals(AvatarGender.FEMALE.id, postResetData.avatarGender)
            assertTrue(athleteRepo.resetMiniGameCounters().isSuccess)

            // Workout tracking data & custom groups must remain 100% untouched
            val sets = workoutRepo.observeWorkoutSets().first()
            assertEquals(1, sets.size)
            assertEquals("Remo con barra", sets.first().ejercicio)
            assertEquals(75.0, sets.first().peso, 0.001)
            assertEquals(listOf("Antebrazos"), workoutRepo.observeCustomGroupNames().first())
        } finally {
            db.close()
        }
    }
}

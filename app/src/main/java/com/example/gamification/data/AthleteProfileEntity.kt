package com.example.gamification.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import com.example.gamification.engine.AccessoryType
import com.example.gamification.engine.AnimalSpecies
import com.example.gamification.engine.AthleteAppearance
import com.example.gamification.engine.AvatarGender
import com.example.gamification.engine.BodyBuild
import com.example.gamification.engine.BottomStyle
import com.example.gamification.engine.FacialHairStyle
import com.example.gamification.engine.HairStyle
import com.example.gamification.engine.SportStyle
import com.example.gamification.engine.TopStyle
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "athlete_profile")
data class AthleteProfileEntity(
    @PrimaryKey val id: Long = SINGLETON_ID,
    val gamificationEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val name: String = "Mi Atleta",
    val gender: String = AvatarGender.MALE.id,
    val animalSpecies: String = AnimalSpecies.LION.id,
    val sportStyle: String = SportStyle.HYPERTROPHY.id,
    val bodyBuild: String = BodyBuild.ATHLETIC.id,
    val skinToneIndex: Int = 0,
    val eyeColorIndex: Int = 0,
    val hairStyle: String = HairStyle.SHORT_SPIKY.id,
    val hairColorIndex: Int = 1,
    val facialHair: String = FacialHairStyle.NONE.id,
    val topStyle: String = TopStyle.TANK_TOP.id,
    val bottomStyle: String = BottomStyle.SHORTS.id,
    val primaryColorIndex: Int = 0,
    val secondaryColorIndex: Int = 2,
    val shoeColorIndex: Int = 0,
    val accessoriesCsv: String = "${AccessoryType.WATCH.id},${AccessoryType.BOTTLE.id}",
    val virtualCoins: Int = 0,
    val lastTrainingAtMillis: Long = 0L,
    val lastAvatarUpdateAtMillis: Long = 0L,
    val nextRecommendedActivityAtMillis: Long = 0L,
    val nextRecoveryCheckAtMillis: Long = 0L,
    val minigameXpDate: String = "",
    val minigameXpEarnedToday: Int = 0,
    val minigameTotalXpEarned: Int = 0,
    val reduceMotionOverride: Boolean = false,
) {
    companion object {
        const val SINGLETON_ID = 1L
    }

    fun toAppearance(): AthleteAppearance {
        return AthleteAppearance(
            name = name.ifBlank { "Mi Atleta" },
            gender = AvatarGender.fromId(gender),
            animalSpecies = AnimalSpecies.fromId(animalSpecies),
            sportStyle = SportStyle.fromId(sportStyle),
            bodyBuild = BodyBuild.fromId(bodyBuild),
            skinToneIndex = skinToneIndex,
            eyeColorIndex = eyeColorIndex,
            hairStyle = HairStyle.fromId(hairStyle),
            hairColorIndex = hairColorIndex,
            facialHair = FacialHairStyle.fromId(facialHair),
            topStyle = TopStyle.fromId(topStyle),
            bottomStyle = BottomStyle.fromId(bottomStyle),
            primaryColorIndex = primaryColorIndex,
            secondaryColorIndex = secondaryColorIndex,
            shoeColorIndex = shoeColorIndex,
            accessories = AccessoryType.parseCsv(accessoriesCsv),
        )
    }
}

@Dao
interface AthleteProfileDao {
    @Query("SELECT * FROM athlete_profile WHERE id = 1 LIMIT 1")
    fun observeProfile(): Flow<AthleteProfileEntity?>

    @Query("SELECT * FROM athlete_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileOnce(): AthleteProfileEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitial(entity: AthleteProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(entity: AthleteProfileEntity)

    @Transaction
    suspend fun initializeIfMissing(): AthleteProfileEntity {
        insertInitial(AthleteProfileEntity())
        return getProfileOnce() ?: AthleteProfileEntity()
    }
}

package com.example.gamification.engine

import androidx.compose.ui.graphics.Color
import com.example.data.WorkoutSet

enum class AvatarGender(
    val id: String,
    val label: String,
) {
    MALE("MALE", "Hombre"),
    FEMALE("FEMALE", "Mujer");

    companion object {
        fun fromId(id: String): AvatarGender = entries.firstOrNull { it.id == id } ?: MALE
    }
}

enum class AnimalSpecies(
    val id: String,
    val label: String,
    val stereotypeHint: String,
    val recommendedCoatIndex: Int,
) {
    TIGER("TIGER", "Tigre", "Explosividad, sprint y garra", 6),
    BEAR("BEAR", "Oso", "Fuerza pesada y potencia", 3),
    EAGLE("EAGLE", "Águila", "Visión, running y funcional", 7),
    LION("LION", "León", "Rey del gimnasio y disciplina", 0),
    BULL("BULL", "Toro", "Potencia pura y tarima", 7),
    WOLF("WOLF", "Lobo", "Resistencia, cardio y manada", 4),
    GORILLA("GORILLA", "Gorila", "Hierro clásico y calistenia", 5),
    PANTHER("PANTHER", "Pantera", "Agilidad, velocidad y cancha", 5),
    FOX("FOX", "Zorro", "Cross training y técnica rápida", 6);

    companion object {
        fun fromId(id: String): AnimalSpecies = entries.firstOrNull { it.id == id } ?: LION
    }
}

enum class SportStyle(
    val id: String,
    val label: String,
    val badgeTitle: String,
) {
    STRENGTH("STRENGTH", "Fuerza", "Levantador de Hierro"),
    HYPERTROPHY("HYPERTROPHY", "Hipertrofia", "Constructor Estético"),
    POWERLIFTING("POWERLIFTING", "Powerlifting", "Atleta de Tarima"),
    GENERAL_FITNESS("GENERAL_FITNESS", "Fitness general", "Atleta Integral"),
    RUNNING("RUNNING", "Running", "Corredor Constante"),
    CYCLING("CYCLING", "Ciclismo", "Ciclista de Ruta"),
    SOCCER("SOCCER", "Fútbol", "Jugador de Cancha"),
    CROSS_TRAINING("CROSS_TRAINING", "Cross training", "Atleta Híbrido"),
    CALISTHENICS("CALISTHENICS", "Calistenia", "Dominio Corporal"),
    FUNCTIONAL("FUNCTIONAL", "Entrenamiento funcional", "Motor Funcional");

    companion object {
        fun fromId(id: String): SportStyle = entries.firstOrNull { it.id == id } ?: HYPERTROPHY
    }
}

enum class BodyBuild(
    val id: String,
    val label: String,
) {
    AGILE("AGILE", "Ágil"),
    ATHLETIC("ATHLETIC", "Atlético"),
    ROBUST("ROBUST", "Fuerte");

    companion object {
        fun fromId(id: String): BodyBuild = entries.firstOrNull { it.id == id } ?: ATHLETIC
    }
}

enum class HairStyle(
    val id: String,
    val label: String,
) {
    BUZZ("BUZZ", "Rapado"),
    SHORT_SPIKY("SHORT_SPIKY", "Corto"),
    CURLY("CURLY", "Rulos"),
    SIDE_PART("SIDE_PART", "Clásico"),
    PONYTAIL("PONYTAIL", "Coleta"),
    LONG_FLOW("LONG_FLOW", "Largo"),
    BALD("BALD", "Natural");

    companion object {
        fun fromId(id: String): HairStyle = entries.firstOrNull { it.id == id } ?: SHORT_SPIKY
    }
}

enum class FacialHairStyle(
    val id: String,
    val label: String,
) {
    NONE("NONE", "Sin barba"),
    STUBBLE("STUBBLE", "Sombra"),
    MUSTACHE("MUSTACHE", "Bigote"),
    SHORT_BEARD("SHORT_BEARD", "Barba corta"),
    FULL_BEARD("FULL_BEARD", "Barba completa");

    companion object {
        fun fromId(id: String): FacialHairStyle = entries.firstOrNull { it.id == id } ?: NONE
    }
}

enum class TopStyle(
    val id: String,
    val label: String,
) {
    TSHIRT("TSHIRT", "Remera"),
    TANK_TOP("TANK_TOP", "Musculosa"),
    HOODIE("HOODIE", "Buzo"),
    JERSEY("JERSEY", "Camiseta deportiva");

    companion object {
        fun fromId(id: String): TopStyle = entries.firstOrNull { it.id == id } ?: TANK_TOP
    }
}

enum class BottomStyle(
    val id: String,
    val label: String,
) {
    SHORTS("SHORTS", "Pantalón corto"),
    LEGGINGS("LEGGINGS", "Calza"),
    JOGGERS("JOGGERS", "Pantalón deportivo");

    companion object {
        fun fromId(id: String): BottomStyle = entries.firstOrNull { it.id == id } ?: SHORTS
    }
}

enum class AccessoryType(
    val id: String,
    val label: String,
) {
    WRISTBANDS("WRISTBANDS", "Muñequeras"),
    HEADBAND("HEADBAND", "Vincha"),
    CAP("CAP", "Gorra"),
    GLOVES("GLOVES", "Guantes"),
    BOTTLE("BOTTLE", "Botella"),
    TOWEL("TOWEL", "Toalla"),
    WATCH("WATCH", "Reloj deportivo");

    companion object {
        fun parseCsv(csv: String): Set<AccessoryType> {
            if (csv.isBlank()) return emptySet()
            return csv.split(',')
                .mapNotNull { token ->
                    val trimmed = token.trim()
                    entries.firstOrNull { it.id == trimmed }
                }
                .toSet()
        }

        fun toCsv(items: Set<AccessoryType>): String =
            items.joinToString(",") { it.id }
    }
}

object AvatarPalettes {
    val skinTones: List<Pair<String, Color>> = listOf(
        "Dorado león" to Color(0xFFE3A857),
        "Crema polar" to Color(0xFFEBD9C3),
        "Canela" to Color(0xFFC68652),
        "Marrón oso" to Color(0xFF8B5A3C),
        "Gris lobo" to Color(0xFF7A8699),
        "Carbón pantera" to Color(0xFF333945),
        "Naranja zorro" to Color(0xFFD96B27),
        "Café oscuro" to Color(0xFF5A3928),
    )

    val eyeColors: List<Pair<String, Color>> = listOf(
        "Oscuros" to Color(0xFF231F20),
        "Castaños" to Color(0xFF6B4226),
        "Verdes" to Color(0xFF2E7D5B),
        "Azules" to Color(0xFF2768B3),
        "Ámbar" to Color(0xFFD98218),
    )

    val hairColors: List<Pair<String, Color>> = listOf(
        "Negro" to Color(0xFF1F2229),
        "Castaño" to Color(0xFF5A3825),
        "Dorado" to Color(0xFFD6A84E),
        "Cobrizo" to Color(0xFFA94426),
        "Gris plata" to Color(0xFF8A929E),
        "Azul deportivo" to Color(0xFF2563EB),
    )

    val outfitColors: List<Pair<String, Color>> = listOf(
        "Azul cobalto" to Color(0xFF2563EB),
        "Rojo coral" to Color(0xFFE11D48),
        "Esmeralda" to Color(0xFF059669),
        "Naranja energía" to Color(0xFFEA580C),
        "Violeta" to Color(0xFF7C3AED),
        "Grafito" to Color(0xFF334155),
        "Cian" to Color(0xFF0891B2),
        "Dorado" to Color(0xFFD97706),
    )

    val shoeColors: List<Pair<String, Color>> = listOf(
        "Blanco/Azul" to Color(0xFFE2E8F0),
        "Negro carbón" to Color(0xFF1E293B),
        "Rojo vivo" to Color(0xFFDC2626),
        "Neón lima" to Color(0xFF65A30D),
        "Naranja" to Color(0xFFF97316),
    )
}

data class AthleteAppearance(
    val name: String = "Mi Atleta",
    val gender: AvatarGender = AvatarGender.MALE,
    val animalSpecies: AnimalSpecies = AnimalSpecies.LION,
    val sportStyle: SportStyle = SportStyle.HYPERTROPHY,
    val bodyBuild: BodyBuild = BodyBuild.ATHLETIC,
    val skinToneIndex: Int = 0,
    val eyeColorIndex: Int = 0,
    val hairStyle: HairStyle = HairStyle.SHORT_SPIKY,
    val hairColorIndex: Int = 1,
    val facialHair: FacialHairStyle = FacialHairStyle.NONE,
    val topStyle: TopStyle = TopStyle.TANK_TOP,
    val bottomStyle: BottomStyle = BottomStyle.SHORTS,
    val primaryColorIndex: Int = 0,
    val secondaryColorIndex: Int = 2,
    val shoeColorIndex: Int = 0,
    val accessories: Set<AccessoryType> = setOf(AccessoryType.WATCH, AccessoryType.BOTTLE),
) {
    val skinColor: Color
        get() = AvatarPalettes.skinTones.getOrElse(skinToneIndex) { AvatarPalettes.skinTones[0] }.second

    val eyeColor: Color
        get() = AvatarPalettes.eyeColors.getOrElse(eyeColorIndex) { AvatarPalettes.eyeColors[0] }.second

    val hairColor: Color
        get() = AvatarPalettes.hairColors.getOrElse(hairColorIndex) { AvatarPalettes.hairColors[0] }.second

    val primaryColor: Color
        get() = AvatarPalettes.outfitColors.getOrElse(primaryColorIndex) { AvatarPalettes.outfitColors[0] }.second

    val secondaryColor: Color
        get() = AvatarPalettes.outfitColors.getOrElse(secondaryColorIndex) { AvatarPalettes.outfitColors[2] }.second

    val shoeColor: Color
        get() = AvatarPalettes.shoeColors.getOrElse(shoeColorIndex) { AvatarPalettes.shoeColors[0] }.second
}

enum class AvatarMoodState(
    val label: String,
    val friendlyMessage: String,
) {
    DESCANSANDO("Descansando", "Recuperando energías entre sesiones."),
    PREPARADO("Listo para entrenar", "Energía cargada y reloj biológico listo."),
    ENTRENANDO("Entrenando", "¡Gran ritmo! Sumando series reales."),
    CANSADO("Cansado", "Sesión exigente completada. Prioriza descanso e hidratación."),
    RECUPERANDOSE("Recuperándose", "Asimilando la última sesión de gimnasio."),
    MOTIVADO("Motivado", "Tu constancia impulsa tu nivel deportivo."),
    CONTENTO_MEJORA("Contento por mejora", "Subiste tu rendimiento frente a sesiones previas."),
    INVITANDO_ACTIVIDAD("Esperando sesión", "¡Buen momento para volver al gimnasio hoy!"),
    CELEBRANDO_PR("¡Nuevo Récord Personal!", "Superaste tu mejor marca registrada."),
    DURMIENDO("Durmiendo", "Pausa larga sin actividad. Toca entrenar para despertarlo."),
    PAUSA_ENTRE_SERIES("Pausa entre series", "Descansando con el temporizador activo."),
}

enum class AvatarActionAnimation(
    val label: String,
) {
    IDLE_BREATHE("Respiración"),
    STRETCH("Estiramiento"),
    DRINK_WATER("Tomar agua"),
    TOWEL_WIPE("Secarse"),
    DUMBBELL_LIFT("Mancuerna"),
    JUMP_ROPE("Soga"),
    JOG_IN_PLACE("Trote"),
    CELEBRATE("Celebración"),
    SIT_REST("Descanso"),
    SLEEP("Siesta"),
}

data class RealWorkoutMetrics(
    val hasData: Boolean = false,
    val totalSets: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val distinctTrainingDays: Int = 0,
    val trainingDatesDesc: List<String> = emptyList(),
    val latestSessionDate: String? = null,
    val latestSessionSets: Int = 0,
    val latestSessionVolumeKg: Double = 0.0,
    val latestSessionExercises: Int = 0,
    val latestSessionMuscleGroups: List<String> = emptyList(),
    val latestSessionAvgRir: Double? = null,
    val latestSessionHasNotesOrBodyWeight: Boolean = false,
    val latestSessionTimestampMillis: Long = 0L,
    val setsInLast7Days: Int = 0,
    val daysInLast7Days: Int = 0,
)

data class PersonalRecordBadge(
    val exerciseName: String,
    val best1RmKg: Double,
    val maxWeightKg: Double,
    val achievedDate: String,
)

data class StreakResult(
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val activeThisWeekDays: Int = 0,
    val consistencyPercent: Int? = null,
)

data class LevelProgress(
    val level: Int = 1,
    val currentLevelXp: Int = 0,
    val xpForNextLevel: Int = 100,
    val totalXp: Int = 0,
    val progressFraction: Float = 0f,
    val nextRewardLabel: String = "Vincha Pro y fondo Estadio",
) {
    val xpRemainingToNextLevel: Int
        get() = (xpForNextLevel - currentLevelXp).coerceAtLeast(0)
}

data class RecoveryResult(
    val energyPercent: Int? = null,
    val recoveryPercent: Int? = null,
    val statusLabel: String = "Sin datos suficientes",
)

data class BiologicalClockState(
    val lastTrainingAtMillis: Long = 0L,
    val lastAvatarUpdateAtMillis: Long = 0L,
    val nextRecommendedActivityAtMillis: Long = 0L,
    val nextRecoveryCheckAtMillis: Long = 0L,
    val hoursSinceLastTraining: Int? = null,
    val needsTrainingToday: Boolean = false,
    val isCycleDue24h: Boolean = false,
    val statusTitle: String = "Sin datos suficientes",
    val statusSubtitle: String = "Registra tu primer entrenamiento para activar el reloj biológico de 24 h.",
)

data class DailyMission(
    val title: String,
    val description: String,
    val completed: Boolean,
    val rewardXp: Int,
)

data class GamificationBadge(
    val id: String,
    val title: String,
    val description: String,
    val unlocked: Boolean,
)

data class GamificationSnapshot(
    val enabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val appearance: AthleteAppearance = AthleteAppearance(),
    val metrics: RealWorkoutMetrics = RealWorkoutMetrics(),
    val levelProgress: LevelProgress = LevelProgress(),
    val streak: StreakResult = StreakResult(),
    val recovery: RecoveryResult = RecoveryResult(),
    val biologicalClock: BiologicalClockState = BiologicalClockState(),
    val moodState: AvatarMoodState = AvatarMoodState.PREPARADO,
    val recommendedAnimation: AvatarActionAnimation = AvatarActionAnimation.IDLE_BREATHE,
    val personalRecords: List<PersonalRecordBadge> = emptyList(),
    val dailyMission: DailyMission? = null,
    val badges: List<GamificationBadge> = emptyList(),
    val virtualCoins: Int = 0,
    val minigameXpEarnedToday: Int = 0,
    val minigameDailyXpCap: Int = 15,
    val reduceMotion: Boolean = false,
    val unlockedBackgroundTier: Int = 1,
)

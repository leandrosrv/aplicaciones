package com.example.gamification.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GamificationData
import com.example.data.GamificationRepository
import com.example.data.WorkoutRepository
import com.example.data.WorkoutSet
import com.example.gamification.data.AthleteProfileEntity
import com.example.gamification.data.AthleteRepository
import com.example.gamification.data.MiniGameRewardOutcome
import com.example.gamification.engine.AnimalSpecies
import com.example.gamification.engine.AthleteAppearance
import com.example.gamification.engine.AvatarActionAnimation
import com.example.gamification.engine.AvatarGender
import com.example.gamification.engine.GamificationEngine
import com.example.gamification.engine.GamificationSnapshot
import com.example.gamification.engine.SportStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class LevelUpEvent(
    val previousLevel: Int,
    val newLevel: Int,
    val rewardLabel: String,
)

class GamificationViewModel(
    private val workoutRepository: WorkoutRepository,
    private val gamificationRepository: GamificationRepository,
    private val athleteRepository: AthleteRepository,
    private val systemReduceMotion: Boolean = false,
) : ViewModel() {

    private val _nowMillis = MutableStateFlow(System.currentTimeMillis())
    private val _isRestTimerActive = MutableStateFlow(false)
    private val _manualAction = MutableStateFlow<AvatarActionAnimation?>(null)
    val manualAction: StateFlow<AvatarActionAnimation?> = _manualAction.asStateFlow()

    private val _lastMiniGameOutcome = MutableStateFlow<MiniGameRewardOutcome?>(null)
    val lastMiniGameOutcome: StateFlow<MiniGameRewardOutcome?> = _lastMiniGameOutcome.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _levelUpEvents = MutableSharedFlow<LevelUpEvent>(extraBufferCapacity = 4)
    val levelUpEvents: SharedFlow<LevelUpEvent> = _levelUpEvents.asSharedFlow()

    private val setsFlow = workoutRepository.observeWorkoutSets()
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing sets in GamificationViewModel", error)
            emit(emptyList())
        }

    private val profileFlow = athleteRepository.observeProfile()
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing athlete profile", error)
            emit(AthleteProfileEntity())
        }

    private val gamificationDataFlow = gamificationRepository.observeGamificationData()
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing gamification_data", error)
            emit(GamificationData())
        }

    val snapshot: StateFlow<GamificationSnapshot> = combine(
        profileFlow,
        gamificationDataFlow,
        setsFlow,
        _isRestTimerActive,
        _nowMillis,
    ) { profile, gamificationData, sets, restActive, now ->
        GamificationEngine.buildGamificationSnapshot(
            profile = profile,
            sets = sets,
            isRestTimerActive = restActive,
            systemReduceMotion = systemReduceMotion,
            nowMillis = now,
            gamificationData = gamificationData,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = GamificationSnapshot(reduceMotion = systemReduceMotion),
    )

    init {
        // Hydrate baseline first so app startup never falsely triggers a level-up sound effect,
        // then observe XP/level/clock changes and emit LevelUpEvent on genuine level-ups.
        viewModelScope.launch {
            try {
                val initialProfile = athleteRepository.getOrCreateProfile()
                val initialData = gamificationRepository.initialize().getOrNull() ?: GamificationData()
                val initialSets = setsFlow.first()
                val baselineSnap = GamificationEngine.buildGamificationSnapshot(
                    profile = initialProfile,
                    sets = initialSets,
                    isRestTimerActive = false,
                    systemReduceMotion = systemReduceMotion,
                    nowMillis = System.currentTimeMillis(),
                    gamificationData = initialData,
                )
                var lastKnownLevel = baselineSnap.levelProgress.level

                snapshot
                    .drop(1)
                    .distinctUntilChanged { old, new ->
                        old.levelProgress.totalXp == new.levelProgress.totalXp &&
                            old.levelProgress.level == new.levelProgress.level &&
                            old.streak.currentStreakDays == new.streak.currentStreakDays &&
                            old.biologicalClock.lastTrainingAtMillis == new.biologicalClock.lastTrainingAtMillis &&
                            old.biologicalClock.nextRecommendedActivityAtMillis == new.biologicalClock.nextRecommendedActivityAtMillis
                    }
                    .collect { snap ->
                        val newLevel = snap.levelProgress.level
                        if (GamificationEngine.didLevelUp(lastKnownLevel, newLevel, snap.enabled)) {
                            _levelUpEvents.tryEmit(
                                LevelUpEvent(
                                    previousLevel = lastKnownLevel,
                                    newLevel = newLevel,
                                    rewardLabel = snap.levelProgress.nextRewardLabel,
                                )
                            )
                            triggerAvatarAction(AvatarActionAnimation.CELEBRATE)
                        }
                        lastKnownLevel = newLevel

                        gamificationRepository.saveGamificationData(
                            xp = snap.levelProgress.totalXp,
                            level = newLevel,
                            lastSessionTimestamp = snap.biologicalClock.lastTrainingAtMillis,
                            streak = snap.streak.currentStreakDays,
                        )
                        athleteRepository.syncBiologicalClockTimestamps(snap.biologicalClock)
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Gamification startup hydration skipped", e)
            }
        }

        // Periodic lightweight clock refresh every 60s while app is in foreground so 24h transitions happen automatically
        viewModelScope.launch {
            while (isActive) {
                delay(60_000L)
                _nowMillis.value = System.currentTimeMillis()
            }
        }
    }

    fun refreshBiologicalClockNow() {
        _nowMillis.value = System.currentTimeMillis()
    }

    fun setRestTimerActive(active: Boolean) {
        _isRestTimerActive.value = active
    }

    fun triggerAvatarAction(action: AvatarActionAnimation) {
        viewModelScope.launch {
            _manualAction.value = action
            delay(2_600L)
            if (_manualAction.value == action) {
                _manualAction.value = null
            }
        }
    }

    private val _isSavingSettings = MutableStateFlow(false)
    val isSavingSettings: StateFlow<Boolean> = _isSavingSettings.asStateFlow()

    fun saveAnimalAndGenderSelection(
        animal: AnimalSpecies,
        gender: AvatarGender,
        sportStyle: SportStyle,
        applySpeciesCoat: Boolean = true,
        onSaved: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isSavingSettings.value = true
            val currentAppearance = snapshot.value.appearance
            val updatedAppearance = currentAppearance.copy(
                animalSpecies = animal,
                gender = gender,
                sportStyle = sportStyle,
                skinToneIndex = if (applySpeciesCoat) animal.recommendedCoatIndex else currentAppearance.skinToneIndex,
            )
            val dataRes = gamificationRepository.saveAvatarPreferences(animal = animal, gender = gender)
            val profileRes = athleteRepository.saveAppearance(
                appearance = updatedAppearance,
                markOnboardingComplete = true,
            )
            _isSavingSettings.value = false
            if (dataRes.isSuccess && profileRes.isSuccess) {
                _events.emit("${animal.label} (${gender.label}) guardado en GamificationData.")
                onSaved()
            } else {
                _events.emit("No se pudieron guardar las preferencias del avatar.")
            }
        }
    }

    fun saveAppearance(
        appearance: AthleteAppearance,
        markOnboardingComplete: Boolean = true,
        onSaved: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isSavingSettings.value = true
            val dataRes = gamificationRepository.saveAvatarPreferences(
                animal = appearance.animalSpecies,
                gender = appearance.gender,
            )
            val res = athleteRepository.saveAppearance(appearance, markOnboardingComplete)
            _isSavingSettings.value = false
            if (dataRes.isSuccess && res.isSuccess) {
                _events.emit("Atleta \"${appearance.name.trim().ifBlank { "Mi Atleta" }}\" actualizado.")
                onSaved()
            } else {
                _events.emit("No se pudo guardar el perfil del atleta.")
            }
        }
    }

    fun saveSettings(
        gamificationEnabled: Boolean,
        reduceMotion: Boolean,
        appearance: AthleteAppearance,
        onSaved: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isSavingSettings.value = true
            val dataRes = gamificationRepository.saveAvatarPreferences(
                animal = appearance.animalSpecies,
                gender = appearance.gender,
            )
            val res = athleteRepository.saveSettings(
                gamificationEnabled = gamificationEnabled,
                reduceMotionOverride = reduceMotion,
                appearance = appearance,
            )
            _isSavingSettings.value = false
            if (dataRes.isSuccess && res.isSuccess) {
                _events.emit("Preferencias de Mi Atleta guardadas.")
                onSaved()
            } else {
                _events.emit("No se pudieron guardar las preferencias en la base de datos.")
            }
        }
    }

    fun setGamificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val res = athleteRepository.setGamificationEnabled(enabled)
            if (res.isSuccess) {
                _events.emit(
                    if (enabled) "Modo juego Mi Atleta activado."
                    else "Modo juego pausado. Tu registro de entrenamientos sigue igual."
                )
            } else {
                _events.emit("No se pudo actualizar el estado del módulo.")
            }
        }
    }

    fun setReduceMotionOverride(reduce: Boolean) {
        viewModelScope.launch {
            val res = athleteRepository.setReduceMotionOverride(reduce)
            if (res.isFailure) {
                _events.emit("No se pudo guardar la preferencia de movimiento.")
            }
        }
    }

    fun resetGamificationProgressOnly() {
        viewModelScope.launch {
            _isSavingSettings.value = true
            val resetData = gamificationRepository.resetGamificationData()
            val resetMini = athleteRepository.resetMiniGameCounters()
            _isSavingSettings.value = false
            if (resetData.isSuccess && resetMini.isSuccess) {
                _nowMillis.value = System.currentTimeMillis()
                _events.emit("Progreso del juego reiniciado. Tus entrenamientos reales permanecen intactos.")
            } else {
                _events.emit("Error al reiniciar el progreso del juego.")
            }
        }
    }

    fun recordMiniGameResult(won: Boolean) {
        viewModelScope.launch {
            val todayIso = GamificationEngine.formatIsoDate(System.currentTimeMillis())
            val res = athleteRepository.recordMiniGameResult(todayIso = todayIso, won = won)
            res.onSuccess { outcome ->
                _lastMiniGameOutcome.value = outcome
                _nowMillis.value = System.currentTimeMillis()
                val msg = if (outcome.capReached) {
                    "¡Buen descanso! +${outcome.coinsGranted} monedas (límite diario de ${outcome.dailyXpCap} XP por minijuegos alcanzado)."
                } else {
                    "+${outcome.xpGranted} XP y +${outcome.coinsGranted} monedas (hoy ${outcome.dailyXpUsed}/${outcome.dailyXpCap} XP de pausa)."
                }
                _events.emit(msg)
            }
        }
    }

    fun clearLastMiniGameOutcome() {
        _lastMiniGameOutcome.value = null
    }

    private companion object {
        const val TAG = "GamificationVM"
    }
}

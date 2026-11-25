package com.example.ui

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DEFAULT_MUSCLE_GROUPS
import com.example.data.ExercisePrSummary
import com.example.data.NewSetInput
import com.example.data.PlanillaRowInput
import com.example.data.ProgressMetric
import com.example.data.TimeGranularity
import com.example.data.WorkoutRepository
import com.example.data.WorkoutSet
import com.example.data.WorkoutSummary
import com.example.data.calculateSummary
import com.example.data.mergeMuscleGroups
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class SummaryScope(val label: String) {
    ALL("Total"),
    TODAY("Hoy"),
}

enum class EntryMode {
    SHEET,
    SINGLE,
}

enum class TimerMode {
    TIMER,
    STOPWATCH,
}

data class RestTimerState(
    val mode: TimerMode = TimerMode.TIMER,
    val presetSeconds: Int = 90,
    val timerRemainingMillis: Long = 90_000L,
    val isTimerRunning: Boolean = false,
    val isTimerCompleted: Boolean = false,
    val stopwatchElapsedMillis: Long = 0L,
    val isStopwatchRunning: Boolean = false,
    val lastLapMillis: Long? = null,
)

data class WorkoutFormState(
    val fecha: String = currentTodayIsoDate(),
    val grupo: String = DEFAULT_MUSCLE_GROUPS.first(),
    val ejercicio: String = "",
    val serie: Int = 1,
    val pesoText: String = "",
    val repsText: String = "",
    val rir: Int = 2,
    val pesoCorporalText: String = "",
    val obs: String = "",
    val isSaving: Boolean = false,
)

fun currentTodayIsoDate(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}

class WorkoutViewModel(
    private val repository: WorkoutRepository,
) : ViewModel() {

    val setsState: StateFlow<UiState<List<WorkoutSet>>> = repository.observeWorkoutSets()
        .map<List<WorkoutSet>, UiState<List<WorkoutSet>>> { list ->
            val sorted = list.sortedWith(
                compareByDescending<WorkoutSet> { it.fecha }
                    .thenByDescending { it.createdAtMillis }
                    .thenByDescending { it.serie }
            )
            UiState.Success(sorted)
        }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing workoutSets", error)
            emit(UiState.Error(error.localizedMessage ?: "No se pudieron cargar las series."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    private val customGroupsFlow: StateFlow<List<String>> =
        repository.observeCustomGroupNames()
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing muscleGroups", error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList(),
            )

    val muscleGroups: StateFlow<List<String>> = customGroupsFlow
        .map { custom -> mergeMuscleGroups(custom) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = mergeMuscleGroups(emptyList()),
        )

    private val _entryMode = MutableStateFlow(EntryMode.SHEET)
    val entryMode: StateFlow<EntryMode> = _entryMode.asStateFlow()

    private val _formState = MutableStateFlow(WorkoutFormState())
    val formState: StateFlow<WorkoutFormState> = _formState.asStateFlow()

    private var nextRowId = 1L
    private val _planillaRows = MutableStateFlow(
        listOf(
            PlanillaRowInput(rowId = nextRowId++, serie = 1),
            PlanillaRowInput(rowId = nextRowId++, serie = 2),
            PlanillaRowInput(rowId = nextRowId++, serie = 3),
        )
    )
    val planillaRows: StateFlow<List<PlanillaRowInput>> = _planillaRows.asStateFlow()

    private val _isSavingPlanilla = MutableStateFlow(false)
    val isSavingPlanilla: StateFlow<Boolean> = _isSavingPlanilla.asStateFlow()

    private val _summaryScope = MutableStateFlow(SummaryScope.ALL)
    val summaryScope: StateFlow<SummaryScope> = _summaryScope.asStateFlow()

    private val _chartGroup = MutableStateFlow(DEFAULT_MUSCLE_GROUPS.first())
    val chartGroup: StateFlow<String> = _chartGroup.asStateFlow()

    private val _chartExercise = MutableStateFlow(ALL_EXERCISES_CHART)
    val chartExercise: StateFlow<String> = _chartExercise.asStateFlow()

    private val _chartMetric = MutableStateFlow(ProgressMetric.ONE_RM)
    val chartMetric: StateFlow<ProgressMetric> = _chartMetric.asStateFlow()

    private val _chartGranularity = MutableStateFlow(TimeGranularity.DAY)
    val chartGranularity: StateFlow<TimeGranularity> = _chartGranularity.asStateFlow()

    private val _historyGroupFilter = MutableStateFlow<String?>(null)
    val historyGroupFilter: StateFlow<String?> = _historyGroupFilter.asStateFlow()

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    private val _restTimerState = MutableStateFlow(RestTimerState())
    val restTimerState: StateFlow<RestTimerState> = _restTimerState.asStateFlow()

    private val _timerCompletedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val timerCompletedEvents: SharedFlow<Unit> = _timerCompletedEvents.asSharedFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private var timerJob: Job? = null
    private var stopwatchJob: Job? = null

    val summaryState: StateFlow<WorkoutSummary> = combine(setsState, _summaryScope) { state, scope ->
        val list = (state as? UiState.Success)?.data.orEmpty()
        val filtered = when (scope) {
            SummaryScope.ALL -> list
            SummaryScope.TODAY -> {
                val today = currentTodayIsoDate()
                list.filter { it.fecha == today }
            }
        }
        calculateSummary(filtered)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = WorkoutSummary(),
    )

    val availableChartExercises: StateFlow<List<String>> = combine(
        setsState,
        _chartGroup,
    ) { state, group ->
        val allSets = (state as? UiState.Success)?.data.orEmpty()
        availableExercisesForGroup(allSets, group)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
    )

    val chartModel: StateFlow<ChartModel> = combine(
        setsState,
        _chartGroup,
        _chartExercise,
        _chartMetric,
        _chartGranularity,
    ) { state, group, exercise, metric, granularity ->
        val allSets = (state as? UiState.Success)?.data.orEmpty()
        buildChartModel(allSets, group, metric, granularity, exercise)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ChartModel(emptyList(), emptyList(), ProgressMetric.ONE_RM, TimeGranularity.DAY),
    )

    val exercisePrSummaries: StateFlow<List<ExercisePrSummary>> = combine(
        setsState,
        _chartGroup,
        _chartExercise,
    ) { state, group, exercise ->
        val allSets = (state as? UiState.Success)?.data.orEmpty()
        buildExercisePrs(allSets, group, exercise)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
    )

    fun setEntryMode(mode: EntryMode) {
        _entryMode.value = mode
    }

    fun updateFecha(fecha: String) {
        _formState.update { it.copy(fecha = fecha) }
    }

    fun updateGrupo(grupo: String) {
        _formState.update { it.copy(grupo = grupo) }
    }

    fun updateEjercicio(ejercicio: String) {
        _formState.update { it.copy(ejercicio = ejercicio) }
    }

    fun updateSerie(serie: Int) {
        _formState.update { it.copy(serie = serie.coerceIn(1, 999)) }
    }

    fun updatePeso(pesoText: String) {
        _formState.update { it.copy(pesoText = pesoText) }
    }

    fun nudgePeso(delta: Double) {
        _formState.update { current ->
            val parsed = current.pesoText.replace(',', '.').toDoubleOrNull() ?: 0.0
            val next = (parsed + delta).coerceIn(0.0, 2000.0)
            current.copy(pesoText = formatCompactNumber(next))
        }
    }

    fun updateReps(repsText: String) {
        _formState.update { it.copy(repsText = repsText) }
    }

    fun nudgeReps(delta: Int) {
        _formState.update { current ->
            val parsed = current.repsText.toIntOrNull() ?: 0
            val next = (parsed + delta).coerceIn(1, 100)
            current.copy(repsText = next.toString())
        }
    }

    fun updateRir(rir: Int) {
        _formState.update { it.copy(rir = rir.coerceIn(0, 5)) }
    }

    fun updatePesoCorporal(text: String) {
        _formState.update { it.copy(pesoCorporalText = text) }
    }

    fun updateObs(obs: String) {
        _formState.update { it.copy(obs = obs) }
    }

    fun setSummaryScope(scope: SummaryScope) {
        _summaryScope.value = scope
    }

    fun setChartGroup(group: String) {
        _chartGroup.value = group
        val allSets = (setsState.value as? UiState.Success)?.data.orEmpty()
        val validExercises = availableExercisesForGroup(allSets, group)
        val currentExercise = _chartExercise.value
        if (!currentExercise.equals(ALL_EXERCISES_CHART, ignoreCase = true) &&
            validExercises.none { it.equals(currentExercise, ignoreCase = true) }
        ) {
            _chartExercise.value = ALL_EXERCISES_CHART
        }
    }

    fun setChartExercise(exercise: String) {
        _chartExercise.value = exercise
    }

    fun setChartMetric(metric: ProgressMetric) {
        _chartMetric.value = metric
    }

    fun setChartGranularity(granularity: TimeGranularity) {
        _chartGranularity.value = granularity
    }

    fun setHistoryGroupFilter(group: String?) {
        _historyGroupFilter.value = group
    }

    fun setHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    // --- Planilla (batch entry & goal comparison) ---

    fun addPlanillaRow() {
        _planillaRows.update { rows ->
            val last = rows.lastOrNull()
            val nextSerie = (last?.serie ?: 0) + 1
            rows + PlanillaRowInput(
                rowId = nextRowId++,
                ejercicio = last?.ejercicio.orEmpty(),
                serie = nextSerie.coerceAtMost(999),
                pesoText = last?.pesoText.orEmpty(),
                repsText = last?.repsText.orEmpty(),
                rir = last?.rir ?: 2,
                isDone = true,
            )
        }
    }

    fun duplicatePlanillaRow(rowId: Long) {
        _planillaRows.update { rows ->
            val idx = rows.indexOfFirst { it.rowId == rowId }
            if (idx == -1) return@update rows
            val src = rows[idx]
            val copyRow = src.copy(
                rowId = nextRowId++,
                serie = (src.serie + 1).coerceAtMost(999),
            )
            rows.toMutableList().apply { add(idx + 1, copyRow) }
        }
    }

    fun removePlanillaRow(rowId: Long) {
        _planillaRows.update { rows ->
            if (rows.size <= 1) {
                listOf(PlanillaRowInput(rowId = nextRowId++, serie = 1))
            } else {
                rows.filterNot { it.rowId == rowId }
            }
        }
    }

    fun updatePlanillaRow(
        rowId: Long,
        transform: (PlanillaRowInput) -> PlanillaRowInput,
    ) {
        _planillaRows.update { rows ->
            rows.map { if (it.rowId == rowId) transform(it) else it }
        }
    }

    fun loadPreviousSessionIntoPlanilla() {
        val allSets = (setsState.value as? UiState.Success)?.data.orEmpty()
        val group = _formState.value.grupo
        val groupSets = allSets.filter { it.grupo.equals(group, ignoreCase = true) }
        if (groupSets.isEmpty()) {
            _snackbarEvents.tryEmit("No hay sesiones anteriores registradas para $group.")
            return
        }
        val latestDate = groupSets.maxOf { it.fecha }
        val sessionSets = groupSets
            .filter { it.fecha == latestDate }
            .sortedWith(compareBy<WorkoutSet> { it.createdAtMillis }.thenBy { it.serie })

        _planillaRows.value = sessionSets.map { prev ->
            PlanillaRowInput(
                rowId = nextRowId++,
                ejercicio = prev.ejercicio,
                serie = prev.serie.toInt().coerceIn(1, 999),
                pesoText = formatCompactNumber(prev.peso),
                repsText = prev.reps.toString(),
                rir = prev.rir.toInt().coerceIn(0, 5),
                obs = "",
                targetPeso = prev.peso,
                targetReps = prev.reps,
                targetRir = prev.rir,
                isDone = false,
            )
        }
        _snackbarEvents.tryEmit("Cargadas ${sessionSets.size} series objetivo del $latestDate.")
    }

    fun submitPlanillaSets() {
        val form = _formState.value
        val rows = _planillaRows.value
        val doneRows = rows.filter { it.isDone }
        if (doneRows.isEmpty()) {
            _snackbarEvents.tryEmit("Marca al menos una serie como hecha para guardar.")
            return
        }

        val pesoCorporal = form.pesoCorporalText.trim()
            .takeIf { it.isNotEmpty() }
            ?.replace(',', '.')
            ?.toDoubleOrNull()
        if (form.pesoCorporalText.trim().isNotEmpty() &&
            (pesoCorporal == null || pesoCorporal < 0.0 || pesoCorporal > 500.0)
        ) {
            _snackbarEvents.tryEmit("El peso corporal debe estar entre 0 y 500 kg.")
            return
        }

        val payloads = mutableListOf<NewSetInput>()
        for ((idx, row) in doneRows.withIndex()) {
            val ex = row.ejercicio.trim()
            val wt = row.pesoText.replace(',', '.').toDoubleOrNull()
            val rp = row.repsText.toLongOrNull()
            if (ex.isEmpty()) {
                _snackbarEvents.tryEmit("Completa el nombre del ejercicio en la serie #${row.serie}.")
                return
            }
            if (wt == null || wt < 0.0 || wt > 2000.0) {
                _snackbarEvents.tryEmit("Peso inválido en \"$ex\" (fila ${idx + 1}).")
                return
            }
            if (rp == null || rp !in 1L..100L) {
                _snackbarEvents.tryEmit("Repeticiones inválidas en \"$ex\" (fila ${idx + 1}).")
                return
            }
            payloads += NewSetInput(
                fecha = form.fecha,
                grupo = form.grupo,
                ejercicio = ex,
                serie = row.serie.toLong(),
                peso = wt,
                reps = rp,
                rir = row.rir.toLong(),
                pesoCorporal = pesoCorporal,
                obs = row.obs.trim().takeIf { it.isNotEmpty() },
            )
        }

        viewModelScope.launch {
            _isSavingPlanilla.value = true
            val result = repository.addWorkoutSetsBatch(payloads)
            _isSavingPlanilla.value = false
            if (result.isSuccess) {
                val count = result.getOrNull() ?: payloads.size
                _snackbarEvents.emit("Guardadas $count series de ${form.grupo}.")
            } else {
                _snackbarEvents.emit(
                    result.exceptionOrNull()?.localizedMessage ?: "No se pudieron guardar las series."
                )
            }
        }
    }

    // --- Single set submission ---

    fun submitNewSet() {
        val current = _formState.value
        val ejercicioTrim = current.ejercicio.trim()
        val peso = current.pesoText.replace(',', '.').toDoubleOrNull()
        val reps = current.repsText.toLongOrNull()
        val pesoCorporal = current.pesoCorporalText.trim()
            .takeIf { it.isNotEmpty() }
            ?.replace(',', '.')
            ?.toDoubleOrNull()

        if (ejercicioTrim.isEmpty()) {
            _snackbarEvents.tryEmit("Ingresa el nombre del ejercicio.")
            return
        }
        if (peso == null || peso < 0.0 || peso > 2000.0) {
            _snackbarEvents.tryEmit("Ingresa un peso válido (0 a 2000 kg).")
            return
        }
        if (reps == null || reps !in 1L..100L) {
            _snackbarEvents.tryEmit("Ingresa repeticiones válidas (1 a 100).")
            return
        }
        if (current.pesoCorporalText.trim().isNotEmpty() &&
            (pesoCorporal == null || pesoCorporal < 0.0 || pesoCorporal > 500.0)
        ) {
            _snackbarEvents.tryEmit("El peso corporal debe estar entre 0 y 500 kg.")
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true) }
            val result = repository.addWorkoutSet(
                fecha = current.fecha,
                grupo = current.grupo,
                ejercicio = ejercicioTrim,
                serie = current.serie.toLong(),
                peso = peso,
                reps = reps,
                rir = current.rir.toLong(),
                pesoCorporal = pesoCorporal,
                obs = current.obs,
            )
            _formState.update { state ->
                if (result.isSuccess) {
                    state.copy(
                        serie = (state.serie + 1).coerceAtMost(999),
                        isSaving = false,
                    )
                } else {
                    state.copy(isSaving = false)
                }
            }
            if (result.isSuccess) {
                _snackbarEvents.emit("Serie ${current.serie} de $ejercicioTrim agregada.")
            } else {
                _snackbarEvents.emit(
                    result.exceptionOrNull()?.localizedMessage ?: "No se pudo guardar la serie."
                )
            }
        }
    }

    fun updateExistingSet(
        setId: Long,
        createdAtMillis: Long,
        fecha: String,
        grupo: String,
        ejercicio: String,
        serie: Long,
        peso: Double,
        reps: Long,
        rir: Long,
        pesoCorporal: Double?,
        obs: String?,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            val result = repository.updateWorkoutSet(
                setId = setId,
                createdAtMillis = createdAtMillis,
                fecha = fecha,
                grupo = grupo,
                ejercicio = ejercicio,
                serie = serie,
                peso = peso,
                reps = reps,
                rir = rir,
                pesoCorporal = pesoCorporal,
                obs = obs,
            )
            if (result.isSuccess) {
                _snackbarEvents.emit("Serie actualizada.")
                onDone()
            } else {
                _snackbarEvents.emit(
                    result.exceptionOrNull()?.localizedMessage ?: "No se pudo actualizar la serie."
                )
            }
        }
    }

    fun deleteSet(setId: Long) {
        viewModelScope.launch {
            val result = repository.deleteWorkoutSet(setId)
            if (result.isSuccess) {
                _snackbarEvents.emit("Serie eliminada.")
            } else {
                _snackbarEvents.emit(
                    result.exceptionOrNull()?.localizedMessage ?: "Error al eliminar la serie."
                )
            }
        }
    }

    fun clearAllSets() {
        viewModelScope.launch {
            val result = repository.clearAllWorkoutSets()
            if (result.isSuccess) {
                _formState.update { it.copy(serie = 1) }
                _snackbarEvents.emit("Registro borrado por completo.")
            } else {
                _snackbarEvents.emit(
                    result.exceptionOrNull()?.localizedMessage ?: "Error al borrar el registro."
                )
            }
        }
    }

    fun addCustomGroup(rawName: String) {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) {
            _snackbarEvents.tryEmit("Escribe un nombre para el grupo muscular.")
            return
        }
        val existing = muscleGroups.value.firstOrNull {
            it.equals(trimmed, ignoreCase = true)
        }
        if (existing != null) {
            _formState.update { it.copy(grupo = existing) }
            _chartGroup.value = existing
            _snackbarEvents.tryEmit("Grupo \"$existing\" seleccionado.")
            return
        }

        viewModelScope.launch {
            val result = repository.addCustomMuscleGroup(trimmed)
            if (result.isSuccess) {
                _formState.update { it.copy(grupo = trimmed) }
                _chartGroup.value = trimmed
                _snackbarEvents.emit("Grupo \"$trimmed\" guardado.")
            } else {
                _snackbarEvents.emit(
                    result.exceptionOrNull()?.localizedMessage ?: "No se pudo guardar el grupo."
                )
            }
        }
    }

    // --- Rest Timer & Stopwatch ---

    fun setTimerMode(mode: TimerMode) {
        _restTimerState.update { it.copy(mode = mode) }
    }

    fun selectTimerPreset(seconds: Int) {
        val clamped = seconds.coerceIn(10, 3600)
        timerJob?.cancel()
        _restTimerState.update {
            it.copy(
                presetSeconds = clamped,
                timerRemainingMillis = clamped * 1000L,
                isTimerRunning = false,
                isTimerCompleted = false,
            )
        }
    }

    fun adjustTimerSeconds(deltaSeconds: Int) {
        val current = _restTimerState.value
        val newRemaining = (current.timerRemainingMillis + deltaSeconds * 1000L)
            .coerceIn(5_000L, 3_600_000L)
        val newPreset = if (!current.isTimerRunning) {
            (newRemaining / 1000L).toInt()
        } else {
            current.presetSeconds
        }
        _restTimerState.update {
            it.copy(
                presetSeconds = newPreset,
                timerRemainingMillis = newRemaining,
                isTimerCompleted = false,
            )
        }
        if (current.isTimerRunning) {
            startTimerLoop(newRemaining)
        }
    }

    fun toggleTimerRunning() {
        val current = _restTimerState.value
        if (current.isTimerRunning) {
            timerJob?.cancel()
            _restTimerState.update { it.copy(isTimerRunning = false) }
        } else {
            val startMillis = if (current.timerRemainingMillis <= 0L) {
                current.presetSeconds * 1000L
            } else {
                current.timerRemainingMillis
            }
            startTimerLoop(startMillis)
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _restTimerState.update {
            it.copy(
                timerRemainingMillis = it.presetSeconds * 1000L,
                isTimerRunning = false,
                isTimerCompleted = false,
            )
        }
    }

    private fun startTimerLoop(durationMillis: Long) {
        timerJob?.cancel()
        val endElapsed = SystemClock.elapsedRealtime() + durationMillis
        _restTimerState.update {
            it.copy(
                timerRemainingMillis = durationMillis,
                isTimerRunning = true,
                isTimerCompleted = false,
            )
        }
        timerJob = viewModelScope.launch {
            while (isActive) {
                val remaining = (endElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                if (remaining == 0L) {
                    _restTimerState.update {
                        it.copy(
                            timerRemainingMillis = 0L,
                            isTimerRunning = false,
                            isTimerCompleted = true,
                        )
                    }
                    _timerCompletedEvents.emit(Unit)
                    break
                } else {
                    _restTimerState.update { it.copy(timerRemainingMillis = remaining) }
                }
                delay(150L)
            }
        }
    }

    fun toggleStopwatchRunning() {
        val current = _restTimerState.value
        if (current.isStopwatchRunning) {
            stopwatchJob?.cancel()
            _restTimerState.update { it.copy(isStopwatchRunning = false) }
        } else {
            val baseOffset = current.stopwatchElapsedMillis
            val startElapsed = SystemClock.elapsedRealtime()
            _restTimerState.update { it.copy(isStopwatchRunning = true) }
            stopwatchJob = viewModelScope.launch {
                while (isActive) {
                    val elapsed = baseOffset + (SystemClock.elapsedRealtime() - startElapsed)
                    _restTimerState.update { it.copy(stopwatchElapsedMillis = elapsed) }
                    delay(100L)
                }
            }
        }
    }

    fun recordStopwatchLap() {
        _restTimerState.update { it.copy(lastLapMillis = it.stopwatchElapsedMillis) }
    }

    fun resetStopwatch() {
        stopwatchJob?.cancel()
        _restTimerState.update {
            it.copy(
                stopwatchElapsedMillis = 0L,
                isStopwatchRunning = false,
                lastLapMillis = null,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        stopwatchJob?.cancel()
    }

    private companion object {
        const val TAG = "WorkoutVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}

private fun formatCompactNumber(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }
}

package com.example.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutRepository(
    private val dao: WorkoutDao,
) {
    fun observeWorkoutSets(): Flow<List<WorkoutSet>> {
        return dao.observeAllSets().map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }

    fun observeCustomGroupNames(): Flow<List<String>> {
        return dao.observeCustomGroups().map { groups ->
            groups.map { it.name }
        }
    }

    suspend fun addWorkoutSet(
        fecha: String,
        grupo: String,
        ejercicio: String,
        serie: Long,
        peso: Double,
        reps: Long,
        rir: Long,
        pesoCorporal: Double?,
        obs: String?,
    ): Result<Unit> {
        return try {
            validateSetFields(fecha, grupo, ejercicio, serie, peso, reps, rir, pesoCorporal, obs)
            val now = System.currentTimeMillis()
            val cleanObs = obs?.trim()?.takeIf { it.isNotEmpty() }
            dao.insertSet(
                WorkoutSetEntity(
                    fecha = fecha,
                    grupo = grupo.trim(),
                    ejercicio = ejercicio.trim(),
                    serie = serie,
                    peso = peso,
                    reps = reps,
                    rir = rir,
                    pesoCorporal = pesoCorporal,
                    obs = cleanObs,
                    createdAtMillis = now,
                    updatedAtMillis = now,
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addWorkoutSetsBatch(items: List<NewSetInput>): Result<Int> {
        return try {
            require(items.isNotEmpty()) { "Agrega al menos una serie válida para guardar." }
            val baseMillis = System.currentTimeMillis()
            val entities = items.mapIndexed { index, input ->
                validateSetFields(
                    input.fecha,
                    input.grupo,
                    input.ejercicio,
                    input.serie,
                    input.peso,
                    input.reps,
                    input.rir,
                    input.pesoCorporal,
                    input.obs,
                )
                val ts = baseMillis + index
                WorkoutSetEntity(
                    fecha = input.fecha,
                    grupo = input.grupo.trim(),
                    ejercicio = input.ejercicio.trim(),
                    serie = input.serie,
                    peso = input.peso,
                    reps = input.reps,
                    rir = input.rir,
                    pesoCorporal = input.pesoCorporal,
                    obs = input.obs?.trim()?.takeIf { it.isNotEmpty() },
                    createdAtMillis = ts,
                    updatedAtMillis = ts,
                )
            }
            dao.insertSets(entities)
            Result.success(entities.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWorkoutSet(
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
    ): Result<Unit> {
        return try {
            require(setId > 0L) { "Serie inválida." }
            validateSetFields(fecha, grupo, ejercicio, serie, peso, reps, rir, pesoCorporal, obs)
            val cleanObs = obs?.trim()?.takeIf { it.isNotEmpty() }
            dao.updateSet(
                WorkoutSetEntity(
                    id = setId,
                    fecha = fecha,
                    grupo = grupo.trim(),
                    ejercicio = ejercicio.trim(),
                    serie = serie,
                    peso = peso,
                    reps = reps,
                    rir = rir,
                    pesoCorporal = pesoCorporal,
                    obs = cleanObs,
                    createdAtMillis = createdAtMillis,
                    updatedAtMillis = System.currentTimeMillis(),
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteWorkoutSet(setId: Long): Result<Unit> {
        return try {
            dao.deleteSetById(setId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAllWorkoutSets(): Result<Unit> {
        return try {
            dao.deleteAllSets()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addCustomMuscleGroup(name: String): Result<Unit> {
        return try {
            val trimmed = name.trim()
            require(trimmed.length in 1..60) { "El grupo muscular debe tener entre 1 y 60 caracteres." }
            dao.insertCustomGroup(MuscleGroupEntity(name = trimmed))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun validateSetFields(
        fecha: String,
        grupo: String,
        ejercicio: String,
        serie: Long,
        peso: Double,
        reps: Long,
        rir: Long,
        pesoCorporal: Double?,
        obs: String?,
    ) {
        require(DATE_REGEX.matches(fecha)) { "Fecha inválida (usa AAAA-MM-DD)." }
        require(grupo.trim().length in 1..60) { "El grupo muscular es obligatorio (máx. 60 caracteres)." }
        require(ejercicio.trim().length in 1..100) { "El ejercicio es obligatorio (máx. 100 caracteres)." }
        require(serie in 1..999) { "La serie debe estar entre 1 y 999." }
        require(peso in 0.0..2000.0) { "El peso debe estar entre 0 y 2000 kg." }
        require(reps in 1..100) { "Las repeticiones deben estar entre 1 y 100." }
        require(rir in 0..5) { "El RIR debe estar entre 0 y 5." }
        if (pesoCorporal != null) {
            require(pesoCorporal in 0.0..500.0) { "El peso corporal debe estar entre 0 y 500 kg." }
        }
        if (obs != null) {
            require(obs.trim().length <= 300) { "La observación no puede superar 300 caracteres." }
        }
    }

    private fun WorkoutSetEntity.toDomain(): WorkoutSet {
        return WorkoutSet(
            id = id,
            fecha = fecha,
            grupo = grupo,
            ejercicio = ejercicio,
            serie = serie,
            peso = peso,
            reps = reps,
            rir = rir,
            pesoCorporal = pesoCorporal,
            obs = obs,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis,
        )
    }

    private companion object {
        val DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    }
}

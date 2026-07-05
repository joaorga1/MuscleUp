package pt.ipt.dama.muscleup.data.sync

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.google.gson.Gson
import pt.ipt.dama.muscleup.data.local.ExerciseDao
import pt.ipt.dama.muscleup.data.local.ExerciseEntity
import pt.ipt.dama.muscleup.data.local.ExercisePhotoDao
import pt.ipt.dama.muscleup.data.local.ExercisePhotoEntity
import pt.ipt.dama.muscleup.data.local.ExerciseSessionDao
import pt.ipt.dama.muscleup.data.local.ExerciseSessionEntity
import pt.ipt.dama.muscleup.data.local.ExerciseSetDao
import pt.ipt.dama.muscleup.data.local.ExerciseSetEntity
import pt.ipt.dama.muscleup.data.local.MachineConfigDao
import pt.ipt.dama.muscleup.data.local.MachineConfigEntity
import pt.ipt.dama.muscleup.data.local.PendingSyncDao
import pt.ipt.dama.muscleup.data.local.PendingSyncEntity
import pt.ipt.dama.muscleup.data.local.SessionExerciseSetEntity
import pt.ipt.dama.muscleup.data.local.WorkoutDao
import pt.ipt.dama.muscleup.data.local.WorkoutEntity
import pt.ipt.dama.muscleup.data.remote.ApiService
import pt.ipt.dama.muscleup.data.remote.uriToMultipart
import pt.ipt.dama.muscleup.data.remote.dto.ExerciseRequest
import pt.ipt.dama.muscleup.data.remote.dto.ExerciseSetRequest
import pt.ipt.dama.muscleup.data.remote.dto.MachineConfigRequest
import pt.ipt.dama.muscleup.data.remote.dto.SessionSetRequest
import pt.ipt.dama.muscleup.data.remote.dto.WorkoutRequest
import retrofit2.Response
import java.io.IOException
import java.time.Instant
import java.util.UUID

private const val TAG = "SyncManager"
private const val TYPE_WORKOUT = "WORKOUT"
private const val TYPE_EXERCISE = "EXERCISE"
private const val TYPE_EXERCISE_SET = "EXERCISE_SET"
private const val TYPE_MACHINE_CONFIG = "MACHINE_CONFIG"
private const val TYPE_PHOTO = "PHOTO"
private const val TYPE_SESSION_SET = "SESSION_SET"
private const val TYPE_SESSION = "SESSION"

/** Indica que o servidor recusou permanentemente uma operação (4xx excluindo 404). A fila avança sem reenviar. */
private class PermanentSyncException(message: String) : Exception(message)

/** Payload JSON para operações de treino na fila. */
private data class WorkoutSyncPayload(
    val title: String = "",
    val description: String = "",
    val type: String = "",
    val remoteId: String? = null
)

/** Payload JSON para operações de exercício na fila. */
private data class ExerciseSyncPayload(
    val workoutId: String = "",
    val name: String = "",
    val description: String = "",
    val targetMuscle: String = "",
    val remoteId: String? = null
)

/** Payload JSON para operações de série de exercício na fila. */
private data class ExerciseSetSyncPayload(
    val exerciseId: String = "",
    val reps: Int = 0,
    val weightKg: Float = 0f,
    val durationSeconds: Int = 0,
    val remoteId: String? = null
)

/** Payload JSON para operações de configuração de máquina na fila. */
private data class MachineConfigSyncPayload(
    val exerciseId: String = "",
    val name: String = "",
    val description: String = "",
    val angleDegrees: Float? = null,
    val remoteId: String? = null
)

/** Payload JSON para operações de fotografia de exercício na fila. */
private data class PhotoSyncPayload(
    val exerciseId: String = "",
    val localUri: String = "",
    val remoteId: String? = null
)

/** Payload JSON para operações de série de sessão na fila. */
private data class SessionSetSyncPayload(
    val exerciseId: String = "",
    val sessionId: String = "",
    val reps: Int = 0,
    val weightKg: Float = 0f,
    val durationSeconds: Int = 0,
    val remoteId: String? = null
)

/** Payload JSON para eventos de ciclo de vida de sessão na fila. */
private data class SessionSyncPayload(
    val exerciseId: String = ""
)

/** Gere a sincronização offline-first entre o Room e a API remota, usando uma fila de operações pendentes. */
class SyncManager(
    private val apiService: ApiService,
    private val pendingSyncDao: PendingSyncDao,
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val exerciseSetDao: ExerciseSetDao,
    private val machineConfigDao: MachineConfigDao,
    private val exercisePhotoDao: ExercisePhotoDao,
    private val exerciseSessionDao: ExerciseSessionDao,
    private val appContext: Context
) {
    private val gson = Gson()

    /** Enfileira a criação de um treino. */
    suspend fun onWorkoutCreated(workout: WorkoutEntity) = enqueueWorkoutOp("CREATE", workout)

    /** Enfileira a atualização de um treino. */
    suspend fun onWorkoutUpdated(workout: WorkoutEntity) = enqueueWorkoutOp("UPDATE", workout)

    /** Enfileira a remoção de um treino. Deve ser chamado antes de apagar do Room. */
    suspend fun onWorkoutDeleted(workout: WorkoutEntity) = enqueueWorkoutOp("DELETE", workout)

    /** Enfileira a criação de um exercício. */
    suspend fun onExerciseCreated(exercise: ExerciseEntity) = enqueueExerciseOp("CREATE", exercise)

    /** Enfileira a atualização de um exercício. */
    suspend fun onExerciseUpdated(exercise: ExerciseEntity) = enqueueExerciseOp("UPDATE", exercise)

    /** Enfileira a remoção de um exercício. Deve ser chamado antes de apagar do Room. */
    suspend fun onExerciseDeleted(exercise: ExerciseEntity) = enqueueExerciseOp("DELETE", exercise)

    /** Enfileira a criação de uma série de exercício. */
    suspend fun onExerciseSetCreated(set: ExerciseSetEntity) = enqueueExerciseSetOp("CREATE", set)

    /** Enfileira a remoção de uma série de exercício. Deve ser chamado antes de apagar do Room. */
    suspend fun onExerciseSetDeleted(set: ExerciseSetEntity) = enqueueExerciseSetOp("DELETE", set)

    /** Enfileira a criação de uma configuração de máquina. */
    suspend fun onMachineConfigCreated(config: MachineConfigEntity) = enqueueMachineConfigOp("CREATE", config)

    /** Enfileira a remoção de uma configuração de máquina. Deve ser chamado antes de apagar do Room. */
    suspend fun onMachineConfigDeleted(config: MachineConfigEntity) = enqueueMachineConfigOp("DELETE", config)

    /** Enfileira a criação de uma fotografia de exercício. */
    suspend fun onPhotoCreated(photo: ExercisePhotoEntity) = enqueuePhotoOp("CREATE", photo)

    /** Enfileira a remoção de uma fotografia de exercício. Deve ser chamado antes de apagar do Room. */
    suspend fun onPhotoDeleted(photo: ExercisePhotoEntity) = enqueuePhotoOp("DELETE", photo)

    /** Enfileira o registo de uma série de sessão. */
    suspend fun onSessionSetCreated(exerciseId: String, set: SessionExerciseSetEntity) =
        enqueueSessionSetOp("CREATE", exerciseId, set)

    /** Enfileira a remoção de uma série de sessão. Deve ser chamado antes de apagar do Room. */
    suspend fun onSessionSetDeleted(exerciseId: String, set: SessionExerciseSetEntity) =
        enqueueSessionSetOp("DELETE", exerciseId, set)

    /** Enfileira a finalização de uma sessão. */
    suspend fun onSessionFinalized(exerciseId: String, sessionId: String) =
        enqueueSessionOp("FINALIZE", exerciseId, sessionId)

    /** Enfileira o descarte de uma sessão, cancelando primeiro as operações pendentes das suas séries. */
    suspend fun onSessionDiscarded(exerciseId: String, sessionId: String) {
        cancelPendingSessionSetOps(sessionId)
        enqueueSessionOp("DISCARD", exerciseId, sessionId)
    }

    /** Adiciona uma operação de treino à fila, substituindo operações anteriores pendentes para a mesma entidade. */
    private suspend fun enqueueWorkoutOp(operation: String, workout: WorkoutEntity) {
        val previous = pendingSyncDao.getAllFor(workout.id, TYPE_WORKOUT)
        val hadPendingCreateWithoutRemote = previous.any { it.operation == "CREATE" } && workout.remoteId == null
        pendingSyncDao.deleteAllFor(workout.id, TYPE_WORKOUT)
        if (operation == "DELETE" && hadPendingCreateWithoutRemote) return
        val payload = WorkoutSyncPayload(workout.title, workout.description, workout.type, workout.remoteId)
        pendingSyncDao.insert(
            PendingSyncEntity(
                entityType = TYPE_WORKOUT,
                operation = operation,
                localId = workout.id,
                payloadJson = gson.toJson(payload)
            )
        )
    }

    /** Adiciona uma operação de exercício à fila, substituindo operações anteriores pendentes para a mesma entidade. */
    private suspend fun enqueueExerciseOp(operation: String, exercise: ExerciseEntity) {
        val previous = pendingSyncDao.getAllFor(exercise.id, TYPE_EXERCISE)
        val hadPendingCreateWithoutRemote = previous.any { it.operation == "CREATE" } && exercise.remoteId == null
        pendingSyncDao.deleteAllFor(exercise.id, TYPE_EXERCISE)
        if (operation == "DELETE" && hadPendingCreateWithoutRemote) return
        val payload = ExerciseSyncPayload(
            workoutId = exercise.workoutId,
            name = exercise.name,
            description = exercise.description,
            targetMuscle = exercise.targetMuscle,
            remoteId = exercise.remoteId
        )
        pendingSyncDao.insert(
            PendingSyncEntity(
                entityType = TYPE_EXERCISE,
                operation = operation,
                localId = exercise.id,
                payloadJson = gson.toJson(payload)
            )
        )
    }

    /** Adiciona uma operação de série à fila. */
    private suspend fun enqueueExerciseSetOp(operation: String, set: ExerciseSetEntity) {
        val previous = pendingSyncDao.getAllFor(set.id, TYPE_EXERCISE_SET)
        val hadPendingCreateWithoutRemote = previous.any { it.operation == "CREATE" } && set.remoteId == null
        pendingSyncDao.deleteAllFor(set.id, TYPE_EXERCISE_SET)
        if (operation == "DELETE" && hadPendingCreateWithoutRemote) return
        val payload = ExerciseSetSyncPayload(set.exerciseId, set.reps, set.weightKg, set.durationSeconds, set.remoteId)
        pendingSyncDao.insert(
            PendingSyncEntity(entityType = TYPE_EXERCISE_SET, operation = operation, localId = set.id, payloadJson = gson.toJson(payload))
        )
    }

    /** Adiciona uma operação de configuração de máquina à fila. */
    private suspend fun enqueueMachineConfigOp(operation: String, config: MachineConfigEntity) {
        val previous = pendingSyncDao.getAllFor(config.id, TYPE_MACHINE_CONFIG)
        val hadPendingCreateWithoutRemote = previous.any { it.operation == "CREATE" } && config.remoteId == null
        pendingSyncDao.deleteAllFor(config.id, TYPE_MACHINE_CONFIG)
        if (operation == "DELETE" && hadPendingCreateWithoutRemote) return
        val payload = MachineConfigSyncPayload(config.exerciseId, config.name, config.description, config.angleDegrees, config.remoteId)
        pendingSyncDao.insert(
            PendingSyncEntity(entityType = TYPE_MACHINE_CONFIG, operation = operation, localId = config.id, payloadJson = gson.toJson(payload))
        )
    }

    /** Adiciona uma operação de fotografia à fila. */
    private suspend fun enqueuePhotoOp(operation: String, photo: ExercisePhotoEntity) {
        val previous = pendingSyncDao.getAllFor(photo.id, TYPE_PHOTO)
        val hadPendingCreateWithoutRemote = previous.any { it.operation == "CREATE" } && photo.remoteId == null
        pendingSyncDao.deleteAllFor(photo.id, TYPE_PHOTO)
        if (operation == "DELETE" && hadPendingCreateWithoutRemote) return
        val payload = PhotoSyncPayload(photo.exerciseId, photo.uri, photo.remoteId)
        pendingSyncDao.insert(
            PendingSyncEntity(entityType = TYPE_PHOTO, operation = operation, localId = photo.id, payloadJson = gson.toJson(payload))
        )
    }

    /** Adiciona uma operação de série de sessão à fila. */
    private suspend fun enqueueSessionSetOp(operation: String, exerciseId: String, set: SessionExerciseSetEntity) {
        val previous = pendingSyncDao.getAllFor(set.id, TYPE_SESSION_SET)
        val hadPendingCreateWithoutRemote = previous.any { it.operation == "CREATE" } && set.remoteId == null
        pendingSyncDao.deleteAllFor(set.id, TYPE_SESSION_SET)
        if (operation == "DELETE" && hadPendingCreateWithoutRemote) return
        val payload = SessionSetSyncPayload(exerciseId, set.sessionId, set.reps, set.weightKg, set.durationSeconds, set.remoteId)
        pendingSyncDao.insert(
            PendingSyncEntity(entityType = TYPE_SESSION_SET, operation = operation, localId = set.id, payloadJson = gson.toJson(payload))
        )
    }

    /** Adiciona um evento de ciclo de vida de sessão à fila. */
    private suspend fun enqueueSessionOp(operation: String, exerciseId: String, sessionId: String) {
        val payload = SessionSyncPayload(exerciseId)
        pendingSyncDao.insert(
            PendingSyncEntity(entityType = TYPE_SESSION, operation = operation, localId = sessionId, payloadJson = gson.toJson(payload))
        )
    }

    /** Remove da fila as operações de séries pertencentes à sessão indicada. */
    private suspend fun cancelPendingSessionSetOps(sessionId: String) {
        val all = pendingSyncDao.getAll()
        for (op in all) {
            if (op.entityType != TYPE_SESSION_SET) continue
            val payload = try {
                gson.fromJson(op.payloadJson, SessionSetSyncPayload::class.java)
            } catch (_: Exception) { null }
            if (payload?.sessionId == sessionId) {
                pendingSyncDao.deleteById(op.id)
            }
        }
    }

    /**
     * Processa a fila de operações pendentes por ordem de criação.
     * Devolve true se a fila ficou vazia. Erros transitórios param a fila (devolve false).
     * Erros permanentes 4xx descartam a operação e a fila avança.
     */
    suspend fun syncPending(): Boolean {
        if (!isApiReachable()) {
            Log.d(TAG, "API inacessível, sincronização adiada.")
            return false
        }
        val ops = pendingSyncDao.getAll()
        if (ops.isEmpty()) return true
        for (op in ops) {
            try {
                when (op.entityType) {
                    TYPE_WORKOUT        -> syncWorkoutOp(op)
                    TYPE_EXERCISE       -> syncExerciseOp(op)
                    TYPE_EXERCISE_SET   -> syncExerciseSetOp(op)
                    TYPE_MACHINE_CONFIG -> syncMachineConfigOp(op)
                    TYPE_PHOTO          -> syncPhotoOp(op)
                    TYPE_SESSION_SET    -> syncSessionSetOp(op)
                    TYPE_SESSION        -> syncSessionOp(op)
                    else -> Log.w(TAG, "Tipo desconhecido na fila: ${op.entityType}")
                }
                pendingSyncDao.deleteById(op.id)
            } catch (e: PermanentSyncException) {
                Log.w(TAG, "Operação rejeitada permanentemente, a descartar: ${op.entityType}/${op.operation}, ${e.message}")
                pendingSyncDao.deleteById(op.id)
            } catch (e: IOException) {
                Log.d(TAG, "Erro de rede em ${op.entityType}/${op.operation}: ${e.message}")
                pendingSyncDao.incrementAttempts(op.id)
                return false
            } catch (e: Exception) {
                Log.e(TAG, "Erro em ${op.entityType}/${op.operation} (tentativa ${op.attempts + 1}): ${e.message}")
                pendingSyncDao.incrementAttempts(op.id)
                return false
            }
        }
        return true
    }

    /**
     * Tenta esvaziar a fila imediatamente, executando [syncPending] várias vezes.
     * Devolve o número de operações que ficaram por sincronizar.
     */
    suspend fun forcePushNow(maxAttempts: Int = 10): Int {
        var remaining = pendingSyncDao.getAll().size
        var attempts = 0
        while (attempts < maxAttempts && remaining > 0) {
            attempts++
            val before = remaining
            val finished = try { syncPending() } catch (_: Exception) { false }
            remaining = pendingSyncDao.getAll().size
            if (finished || remaining >= before) break
        }
        return remaining
    }

    /** Verifica se a API está acessível. */
    private suspend fun isApiReachable(): Boolean = try {
        apiService.checkHealth().isSuccessful
    } catch (_: Exception) { false }

    /**
     * Lança exceção transitória se o pai ainda está pendente, ou [PermanentSyncException]
     * se o pai nunca existiu na API (para descartar a operação filha).
     */
    private suspend fun throwMissingParent(parentType: String, parentLocalId: String, parentLabel: String): Nothing {
        val parentStillPending = pendingSyncDao.getAllFor(parentLocalId, parentType).any { it.operation == "CREATE" }
        if (parentStillPending) {
            throw IOException("$parentLabel pai ainda sem remoteId, tenta novamente mais tarde.")
        }
        throw PermanentSyncException("$parentLabel pai não existe na API, a descartar operação filha.")
    }

    /** Sincroniza uma operação pendente de treino. */
    private suspend fun syncWorkoutOp(op: PendingSyncEntity) {
        val payload = gson.fromJson(op.payloadJson, WorkoutSyncPayload::class.java)
        when (op.operation) {
            "CREATE" -> {
                val response = apiService.createWorkout(
                    WorkoutRequest(payload.title, payload.description, payload.type, clientId = op.localId)
                )
                response.throwIfFailed("createWorkout")
                response.body()?.id?.let { remoteId -> workoutDao.updateRemoteId(op.localId, remoteId) }
            }
            "UPDATE" -> {
                val remoteId = payload.remoteId ?: workoutDao.getByIdOnce(op.localId)?.remoteId
                if (remoteId == null) return
                val response = apiService.updateWorkout(remoteId, WorkoutRequest(payload.title, payload.description, payload.type))
                response.throwIfFailed("updateWorkout")
            }
            "DELETE" -> {
                val remoteId = payload.remoteId ?: return
                val response = apiService.deleteWorkout(remoteId)
                if (!response.isSuccessful && response.code() != 404) response.throwIfFailed("deleteWorkout")
            }
        }
    }

    /** Sincroniza uma operação pendente de exercício. */
    private suspend fun syncExerciseOp(op: PendingSyncEntity) {
        val payload = gson.fromJson(op.payloadJson, ExerciseSyncPayload::class.java)
        when (op.operation) {
            "CREATE" -> {
                val remoteWorkoutId = workoutDao.getByIdOnce(payload.workoutId)?.remoteId
                    ?: throwMissingParent(TYPE_WORKOUT, payload.workoutId, "Workout")
                val response = apiService.createExercise(
                    remoteWorkoutId,
                    ExerciseRequest(payload.name, payload.description, payload.targetMuscle, clientId = op.localId)
                )
                response.throwIfFailed("createExercise")
                response.body()?.id?.let { remoteId -> exerciseDao.updateRemoteId(op.localId, remoteId) }
            }
            "UPDATE" -> {
                val remoteId = payload.remoteId ?: exerciseDao.getByIdOnce(op.localId)?.remoteId
                if (remoteId == null) return
                val response = apiService.updateExercise(
                    remoteId,
                    ExerciseRequest(payload.name, payload.description, payload.targetMuscle)
                )
                response.throwIfFailed("updateExercise")
            }
            "DELETE" -> {
                val remoteId = payload.remoteId ?: return
                val response = apiService.deleteExercise(remoteId)
                if (!response.isSuccessful && response.code() != 404) response.throwIfFailed("deleteExercise")
            }
        }
    }

    /** Sincroniza uma operação pendente de série de exercício. */
    private suspend fun syncExerciseSetOp(op: PendingSyncEntity) {
        val payload = gson.fromJson(op.payloadJson, ExerciseSetSyncPayload::class.java)
        when (op.operation) {
            "CREATE" -> {
                val remoteExerciseId = exerciseDao.getByIdOnce(payload.exerciseId)?.remoteId
                    ?: throwMissingParent(TYPE_EXERCISE, payload.exerciseId, "Exercise")
                val response = apiService.addExerciseSet(
                    remoteExerciseId,
                    ExerciseSetRequest(payload.reps, payload.weightKg.toDouble(), payload.durationSeconds, clientId = op.localId)
                )
                response.throwIfFailed("addExerciseSet")
                response.body()?.id?.let { remoteId -> exerciseSetDao.updateRemoteId(op.localId, remoteId) }
            }
            "DELETE" -> {
                val remoteId = payload.remoteId ?: return
                val response = apiService.deleteExerciseSet(remoteId)
                if (!response.isSuccessful && response.code() != 404) response.throwIfFailed("deleteExerciseSet")
            }
        }
    }

    /** Sincroniza uma operação pendente de configuração de máquina. */
    private suspend fun syncMachineConfigOp(op: PendingSyncEntity) {
        val payload = gson.fromJson(op.payloadJson, MachineConfigSyncPayload::class.java)
        when (op.operation) {
            "CREATE" -> {
                val remoteExerciseId = exerciseDao.getByIdOnce(payload.exerciseId)?.remoteId
                    ?: throwMissingParent(TYPE_EXERCISE, payload.exerciseId, "Exercise")
                val response = apiService.addMachineConfig(
                    remoteExerciseId,
                    MachineConfigRequest(payload.name, payload.description, payload.angleDegrees?.toDouble(), clientId = op.localId)
                )
                response.throwIfFailed("addMachineConfig")
                response.body()?.id?.let { remoteId -> machineConfigDao.updateRemoteId(op.localId, remoteId) }
            }
            "DELETE" -> {
                val remoteId = payload.remoteId ?: return
                val response = apiService.deleteMachineConfig(remoteId)
                if (!response.isSuccessful && response.code() != 404) response.throwIfFailed("deleteMachineConfig")
            }
        }
    }

    /** Sincroniza uma operação pendente de fotografia. */
    private suspend fun syncPhotoOp(op: PendingSyncEntity) {
        val payload = gson.fromJson(op.payloadJson, PhotoSyncPayload::class.java)
        when (op.operation) {
            "CREATE" -> {
                val remoteExerciseId = exerciseDao.getByIdOnce(payload.exerciseId)?.remoteId
                    ?: throwMissingParent(TYPE_EXERCISE, payload.exerciseId, "Exercise")
                val part = try {
                    uriToMultipart(appContext, payload.localUri.toUri(), "photo")
                } catch (e: Exception) {
                    Log.w(TAG, "Foto local ilegível, a desistir do upload: ${e.message}")
                    return
                }
                val response = apiService.uploadExercisePhoto(remoteExerciseId, part)
                response.throwIfFailed("uploadExercisePhoto")
                response.body()?.id?.let { remoteId -> exercisePhotoDao.updateRemoteId(op.localId, remoteId) }
            }
            "DELETE" -> {
                val remoteId = payload.remoteId ?: return
                val response = apiService.deleteExercisePhoto(remoteId)
                if (!response.isSuccessful && response.code() != 404) response.throwIfFailed("deleteExercisePhoto")
            }
        }
    }

    /** Sincroniza uma operação pendente de série de sessão. */
    private suspend fun syncSessionSetOp(op: PendingSyncEntity) {
        val payload = gson.fromJson(op.payloadJson, SessionSetSyncPayload::class.java)
        when (op.operation) {
            "CREATE" -> {
                val remoteExerciseId = exerciseDao.getByIdOnce(payload.exerciseId)?.remoteId
                    ?: throwMissingParent(TYPE_EXERCISE, payload.exerciseId, "Exercise")
                val response = apiService.addRecordedSet(
                    remoteExerciseId,
                    SessionSetRequest(payload.reps, payload.weightKg.toDouble(), payload.durationSeconds)
                )
                response.throwIfFailed("addRecordedSet")
                val body = response.body() ?: throw IOException("addRecordedSet devolveu corpo vazio")
                exerciseSessionDao.updateSessionRemoteId(payload.sessionId, body.session.id)
                exerciseSessionDao.updateSetRemoteId(op.localId, body.set.id)
            }
            "DELETE" -> {
                val remoteId = payload.remoteId ?: return
                val response = apiService.deleteRecordedSet(remoteId)
                if (!response.isSuccessful && response.code() != 404) response.throwIfFailed("deleteRecordedSet")
            }
        }
    }

    /** Sincroniza um evento de ciclo de vida de sessão (finalizar ou descartar). */
    private suspend fun syncSessionOp(op: PendingSyncEntity) {
        val payload = gson.fromJson(op.payloadJson, SessionSyncPayload::class.java)
        when (op.operation) {
            "FINALIZE" -> {
                val remoteExerciseId = exerciseDao.getByIdOnce(payload.exerciseId)?.remoteId
                    ?: throwMissingParent(TYPE_EXERCISE, payload.exerciseId, "Exercise")
                val response = apiService.finalizeSession(remoteExerciseId)
                if (!response.isSuccessful && response.code() != 404) response.throwIfFailed("finalizeSession")
            }
            "DISCARD" -> {
                val remoteExerciseId = exerciseDao.getByIdOnce(payload.exerciseId)?.remoteId ?: return
                val response = apiService.discardDraftSession(remoteExerciseId)
                if (!response.isSuccessful && response.code() != 404) response.throwIfFailed("discardDraftSession")
            }
        }
    }

    /** Lança [PermanentSyncException] para 4xx (excluindo 404) ou [IOException] para outros erros. */
    private fun Response<*>.throwIfFailed(opName: String) {
        if (isSuccessful) return
        val httpCode = code()
        if (httpCode in 400..499 && httpCode != 404) {
            throw PermanentSyncException("$opName rejeitado pelo servidor ($httpCode)")
        }
        throw IOException("$opName falhou: $httpCode")
    }

    /** Traz da API os treinos do utilizador que ainda não existem localmente. */
    suspend fun pullWorkouts(localUserId: String) {
        try {
            val response = apiService.getWorkouts()
            if (!response.isSuccessful) return
            val remoteWorkouts = response.body().orEmpty()
            val existingRemoteIds = workoutDao.getAllRemoteIds().toHashSet()
            for (remote in remoteWorkouts) {
                if (remote.id !in existingRemoteIds) {
                    workoutDao.insert(
                        WorkoutEntity(
                            id = UUID.randomUUID().toString(),
                            userId = localUserId,
                            title = remote.title,
                            description = remote.description,
                            type = remote.type,
                            remoteId = remote.id
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "pullWorkouts: sem rede ou erro (${e.message})")
        }
    }

    /** Traz da API os exercícios do treino indicado que ainda não existem localmente. */
    suspend fun pullExercises(localWorkoutId: String) {
        try {
            val remoteWorkoutId = workoutDao.getByIdOnce(localWorkoutId)?.remoteId ?: return
            val response = apiService.getExercises(remoteWorkoutId)
            if (!response.isSuccessful) return
            val remoteExercises = response.body().orEmpty()
            val existingRemoteIds = exerciseDao.getAllRemoteIdsForWorkout(localWorkoutId).toHashSet()
            for (remote in remoteExercises) {
                if (remote.id !in existingRemoteIds) {
                    exerciseDao.insert(
                        ExerciseEntity(
                            id = UUID.randomUUID().toString(),
                            workoutId = localWorkoutId,
                            name = remote.name,
                            description = remote.description,
                            targetMuscle = remote.targetMuscle,
                            remoteId = remote.id
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "pullExercises: sem rede ou erro (${e.message})")
        }
    }

    /** Traz da API as séries pré-definidas do exercício indicado que ainda não existem localmente. */
    suspend fun pullExerciseSets(localExerciseId: String) {
        try {
            val remoteExerciseId = exerciseDao.getByIdOnce(localExerciseId)?.remoteId ?: return
            val response = apiService.getExerciseSets(remoteExerciseId)
            if (!response.isSuccessful) return
            val remoteSets = response.body().orEmpty()
            val existingRemoteIds = exerciseSetDao.getAllRemoteIdsForExercise(localExerciseId).toHashSet()
            for (remote in remoteSets) {
                if (remote.id !in existingRemoteIds) {
                    exerciseSetDao.insert(
                        ExerciseSetEntity(
                            id = UUID.randomUUID().toString(),
                            exerciseId = localExerciseId,
                            createdAt = System.currentTimeMillis(),
                            seriesOrder = remote.seriesOrder,
                            reps = remote.reps,
                            durationSeconds = remote.durationSeconds,
                            weightKg = remote.weightKg.toFloat(),
                            remoteId = remote.id
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "pullExerciseSets: sem rede ou erro (${e.message})")
        }
    }

    /** Traz da API as configurações de máquina do exercício indicado que ainda não existem localmente. */
    suspend fun pullMachineConfigs(localExerciseId: String) {
        try {
            val remoteExerciseId = exerciseDao.getByIdOnce(localExerciseId)?.remoteId ?: return
            val response = apiService.getMachineConfigs(remoteExerciseId)
            if (!response.isSuccessful) return
            val remoteConfigs = response.body().orEmpty()
            val existingRemoteIds = machineConfigDao.getAllRemoteIdsForExercise(localExerciseId).toHashSet()
            for (remote in remoteConfigs) {
                if (remote.id !in existingRemoteIds) {
                    machineConfigDao.insert(
                        MachineConfigEntity(
                            id = UUID.randomUUID().toString(),
                            exerciseId = localExerciseId,
                            name = remote.name,
                            description = remote.description,
                            createdAt = System.currentTimeMillis(),
                            angleDegrees = remote.angleDegrees?.toFloat(),
                            remoteId = remote.id
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "pullMachineConfigs: sem rede ou erro (${e.message})")
        }
    }

    /** Traz da API as fotografias do exercício indicado que ainda não existem localmente. */
    suspend fun pullExercisePhotos(localExerciseId: String) {
        try {
            val remoteExerciseId = exerciseDao.getByIdOnce(localExerciseId)?.remoteId ?: return
            val response = apiService.getExercisePhotos(remoteExerciseId)
            if (!response.isSuccessful) return
            val remotePhotos = response.body().orEmpty()
            val existingRemoteIds = exercisePhotoDao.getAllRemoteIdsForExercise(localExerciseId).toHashSet()
            for (remote in remotePhotos) {
                if (remote.id !in existingRemoteIds) {
                    exercisePhotoDao.insert(
                        ExercisePhotoEntity(
                            id = UUID.randomUUID().toString(),
                            exerciseId = localExerciseId,
                            uri = remote.uri,
                            createdAt = parseIsoDateOrNow(remote.createdAt),
                            remoteId = remote.id
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "pullExercisePhotos: sem rede ou erro (${e.message})")
        }
    }

    /** Traz da API o histórico de sessões finalizadas do exercício indicado que ainda não existem localmente. */
    suspend fun pullSessionHistory(localExerciseId: String, localUserId: String) {
        try {
            val remoteExerciseId = exerciseDao.getByIdOnce(localExerciseId)?.remoteId ?: return
            val response = apiService.getSessionHistory(remoteExerciseId)
            if (!response.isSuccessful) return
            val remoteSessions = response.body().orEmpty()
            val existingSessionIds = exerciseSessionDao.getAllSessionRemoteIdsForExercise(localExerciseId).toHashSet()
            val existingSetIds = exerciseSessionDao.getAllSetRemoteIdsForExercise(localExerciseId).toHashSet()
            for (remote in remoteSessions) {
                val localSessionId = if (remote.id !in existingSessionIds) {
                    val newId = UUID.randomUUID().toString()
                    exerciseSessionDao.insertSession(
                        ExerciseSessionEntity(
                            id = newId,
                            exerciseId = localExerciseId,
                            userId = localUserId,
                            createdAt = parseIsoDateOrNow(remote.createdAt),
                            finishedAt = remote.finishedAt?.let { parseIsoDateOrNow(it) },
                            status = remote.status,
                            remoteId = remote.id
                        )
                    )
                    newId
                } else {
                    exerciseSessionDao.getSessionByRemoteId(remote.id)?.id ?: continue
                }
                remote.sets.orEmpty().forEach { remoteSet ->
                    if (remoteSet.id !in existingSetIds) {
                        exerciseSessionDao.insertSessionSet(
                            SessionExerciseSetEntity(
                                id = UUID.randomUUID().toString(),
                                sessionId = localSessionId,
                                reps = remoteSet.reps,
                                durationSeconds = remoteSet.durationSeconds,
                                weightKg = remoteSet.weightKg.toFloat(),
                                setOrder = remoteSet.setOrder,
                                createdAt = System.currentTimeMillis(),
                                remoteId = remoteSet.id
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "pullSessionHistory: sem rede ou erro (${e.message})")
        }
    }

    /** Converte uma data ISO 8601 para milissegundos; devolve o instante atual em caso de erro. */
    private fun parseIsoDateOrNow(iso: String): Long = try {
        Instant.parse(iso).toEpochMilli()
    } catch (_: Exception) {
        System.currentTimeMillis()
    }
}

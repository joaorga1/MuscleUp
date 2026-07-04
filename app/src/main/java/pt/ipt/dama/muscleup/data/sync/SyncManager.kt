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
private const val TYPE_SESSION = "SESSION" // ciclo de vida da sessão (FINALIZE / DISCARD)

/**
 * Exceção lançada quando o servidor recusa permanentemente uma operação (4xx, excluindo 404).
 * Ao contrário de [IOException], NÃO para a fila — a op é descartada e a fila avança,
 * porque re-enviar nunca vai resolver um erro permanente do servidor.
 */
private class PermanentSyncException(message: String) : Exception(message)

/** Payload serializado (JSON) guardado na fila — estado necessário para reenviar a operação à API. */
private data class WorkoutSyncPayload(
    val title: String = "",
    val description: String = "",
    val type: String = "",
    val remoteId: String? = null
)

/** Payload serializado (JSON) da fila para operações de Exercise. */
private data class ExerciseSyncPayload(
    val workoutId: String = "", // id LOCAL do workout pai — usado para resolver o remoteId no momento do sync
    val name: String = "",
    val description: String = "",
    val targetMuscle: String = "",
    val remoteId: String? = null
)

/** Payload da fila para Exercise Sets (séries pré-definidas / "Meta"). Só suporta CREATE/DELETE (API não tem PUT). */
private data class ExerciseSetSyncPayload(
    val exerciseId: String = "", // id LOCAL do exercise pai
    val reps: Int = 0,
    val weightKg: Float = 0f,
    val durationSeconds: Int = 0,
    val remoteId: String? = null
)

/** Payload da fila para Machine Configs. Só suporta CREATE/DELETE (API não tem PUT). */
private data class MachineConfigSyncPayload(
    val exerciseId: String = "", // id LOCAL do exercise pai
    val name: String = "",
    val description: String = "",
    val angleDegrees: Float? = null,
    val remoteId: String? = null
)

/** Payload da fila para Exercise Photos. Só suporta CREATE/DELETE (API não tem PUT). */
private data class PhotoSyncPayload(
    val exerciseId: String = "", // id LOCAL do exercise pai
    val localUri: String = "",   // uri local (content://... ou file://...) usada para ler os bytes a enviar
    val remoteId: String? = null
)

/** Payload da fila para séries registadas em tempo real (sessão de treino). */
private data class SessionSetSyncPayload(
    val exerciseId: String = "", // id LOCAL do exercise pai
    val sessionId: String = "",  // id LOCAL da sessão (para detetar sets órfãos ao descartar)
    val reps: Int = 0,
    val weightKg: Float = 0f,
    val durationSeconds: Int = 0,
    val remoteId: String? = null
)

/** Payload da fila para eventos de ciclo de vida da sessão (FINALIZE / DISCARD). */
private data class SessionSyncPayload(
    val exerciseId: String = "" // id LOCAL do exercise pai
)

/**
 * Passo 8.3 — Sincronização offline-first (padrão outbox), cobrindo todas as verticais
 * associadas a um exercício: Workouts, Exercises, Exercise Sets, Machine Configs,
 * Exercise Photos e Exercise Sessions (registo em tempo real).
 *
 * - Toda a escrita local (Room) enfileira aqui uma operação pendente.
 * - [syncPending] tenta esvaziar a fila assim que há rede (chamado após cada escrita e no arranque da app).
 * - Os métodos `pull*` trazem para o Room o que existe na API mas ainda não localmente
 *   (ex: criados noutro dispositivo).
 *
 * Nota: esta é uma sincronização "best effort" (tenta imediatamente, em memória) — não usa
 * WorkManager, por isso não sobrevive ao processo ser morto enquanto está sem rede. A fila em
 * Room garante que nada se perde: a próxima vez que a app abrir com rede, a fila é esvaziada.
 */
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

    // ---------------------------------------------------------------------
    // Enfileirar operações (chamado pelos ViewModels logo após escrever no Room)
    // ---------------------------------------------------------------------

    suspend fun onWorkoutCreated(workout: WorkoutEntity) = enqueueWorkoutOp("CREATE", workout)

    suspend fun onWorkoutUpdated(workout: WorkoutEntity) = enqueueWorkoutOp("UPDATE", workout)

    /** Chamar com a entidade tal como estava ANTES de a apagar do Room (para saber o remoteId). */
    suspend fun onWorkoutDeleted(workout: WorkoutEntity) = enqueueWorkoutOp("DELETE", workout)

    suspend fun onExerciseCreated(exercise: ExerciseEntity) = enqueueExerciseOp("CREATE", exercise)

    suspend fun onExerciseUpdated(exercise: ExerciseEntity) = enqueueExerciseOp("UPDATE", exercise)

    /** Chamar com a entidade tal como estava ANTES de a apagar do Room (para saber o remoteId). */
    suspend fun onExerciseDeleted(exercise: ExerciseEntity) = enqueueExerciseOp("DELETE", exercise)

    suspend fun onExerciseSetCreated(set: ExerciseSetEntity) = enqueueExerciseSetOp("CREATE", set)

    /** Chamar com a entidade tal como estava ANTES de a apagar do Room (para saber o remoteId). */
    suspend fun onExerciseSetDeleted(set: ExerciseSetEntity) = enqueueExerciseSetOp("DELETE", set)

    suspend fun onMachineConfigCreated(config: MachineConfigEntity) = enqueueMachineConfigOp("CREATE", config)

    /** Chamar com a entidade tal como estava ANTES de a apagar do Room (para saber o remoteId). */
    suspend fun onMachineConfigDeleted(config: MachineConfigEntity) = enqueueMachineConfigOp("DELETE", config)

    suspend fun onPhotoCreated(photo: ExercisePhotoEntity) = enqueuePhotoOp("CREATE", photo)

    /** Chamar com a entidade tal como estava ANTES de a apagar do Room (para saber o remoteId). */
    suspend fun onPhotoDeleted(photo: ExercisePhotoEntity) = enqueuePhotoOp("DELETE", photo)

    suspend fun onSessionSetCreated(exerciseId: String, set: SessionExerciseSetEntity) =
        enqueueSessionSetOp("CREATE", exerciseId, set)

    /** Chamar com a entidade tal como estava ANTES de a apagar do Room (para saber o remoteId). */
    suspend fun onSessionSetDeleted(exerciseId: String, set: SessionExerciseSetEntity) =
        enqueueSessionSetOp("DELETE", exerciseId, set)

    suspend fun onSessionFinalized(exerciseId: String, sessionId: String) =
        enqueueSessionOp("FINALIZE", exerciseId, sessionId)

    suspend fun onSessionDiscarded(exerciseId: String, sessionId: String) {
        // Cancela quaisquer CREATE/DELETE de sets desta sessão ainda pendentes — já não fazem
        // sentido, a sessão inteira vai ser descartada (localmente já foi apagada em cascata).
        cancelPendingSessionSetOps(sessionId)
        enqueueSessionOp("DISCARD", exerciseId, sessionId)
    }

    private suspend fun enqueueWorkoutOp(operation: String, workout: WorkoutEntity) {
        val previous = pendingSyncDao.getAllFor(workout.id, TYPE_WORKOUT)
        val hadPendingCreateWithoutRemote = previous.any { it.operation == "CREATE" } && workout.remoteId == null
        pendingSyncDao.deleteAllFor(workout.id, TYPE_WORKOUT) // colapsa operações anteriores para a mesma entidade

        if (operation == "DELETE" && hadPendingCreateWithoutRemote) {
            // Nunca chegou a existir na API (criado e apagado ainda offline) — nada a sincronizar.
            return
        }
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

    private suspend fun enqueueExerciseOp(operation: String, exercise: ExerciseEntity) {
        val previous = pendingSyncDao.getAllFor(exercise.id, TYPE_EXERCISE)
        val hadPendingCreateWithoutRemote = previous.any { it.operation == "CREATE" } && exercise.remoteId == null
        pendingSyncDao.deleteAllFor(exercise.id, TYPE_EXERCISE) // colapsa operações anteriores para a mesma entidade

        if (operation == "DELETE" && hadPendingCreateWithoutRemote) {
            // Nunca chegou a existir na API (criado e apagado ainda offline) — nada a sincronizar.
            return
        }
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

    private suspend fun enqueueSessionOp(operation: String, exerciseId: String, sessionId: String) {
        val payload = SessionSyncPayload(exerciseId)
        pendingSyncDao.insert(
            PendingSyncEntity(entityType = TYPE_SESSION, operation = operation, localId = sessionId, payloadJson = gson.toJson(payload))
        )
    }

    /** Remove da fila quaisquer CREATE/DELETE de session-sets pertencentes à sessão indicada. */
    private suspend fun cancelPendingSessionSetOps(sessionId: String) {
        val all = pendingSyncDao.getAll()
        for (op in all) {
            if (op.entityType != TYPE_SESSION_SET) continue
            val payload = try {
                gson.fromJson(op.payloadJson, SessionSetSyncPayload::class.java)
            } catch (_: Exception) {
                null
            }
            if (payload?.sessionId == sessionId) {
                pendingSyncDao.deleteById(op.id)
            }
        }
    }

    // ---------------------------------------------------------------------
    // Push — esvazia a fila de pendentes
    // ---------------------------------------------------------------------

    /**
     * Esvazia a fila de pendentes. Devolve `true` se a fila ficou totalmente vazia,
     * ou `false` para sinalizar ao [SyncWorker] que deve pedir retry ao WorkManager.
     *
     * ## Garantia de ordem / dependências
     * A fila é processada por ordem de criação (FIFO). **Qualquer erro transiente pára
     * imediatamente a fila e devolve `false`**, garantindo que operações dependentes
     * (ex: CREATE de exercises que precisam do remoteId do workout pai) nunca são
     * tentadas antes das operações que as precedem. O WorkManager retenta com backoff.
     *
     * ## Erros permanentes
     * Se o servidor recusar permanentemente uma operação (4xx ≠ 404), ela é descartada
     * via [PermanentSyncException] e a fila avança — re-enviar nunca iria resolver.
     * O dado local continua a existir no Room.
     *
     * ## Sem limite de tentativas
     * Operações nunca são descartadas por "demasiadas tentativas". Um workout criado
     * offline ficará na fila até ser sincronizado com sucesso, garantindo que o
     * estado local nunca fica permanentemente dessincronizado da API por timeout.
     */
    suspend fun syncPending(): Boolean {
        // Verifica conectividade antes de processar — evita tentar toda a fila quando
        // o servidor está claramente em baixo.
        if (!isApiReachable()) {
            Log.d(TAG, "API inacessível — sync adiada")
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
                // Servidor recusou permanentemente (4xx) — descarta esta op e avança.
                // O dado local continua no Room mas não será sincronizado.
                Log.w(TAG, "Op permanentemente rejeitada, a descartar: ${op.entityType}/${op.operation} — ${e.message}")
                pendingSyncDao.deleteById(op.id)

            } catch (e: IOException) {
                // Erro transiente (rede, 5xx) — pára a fila. WorkManager retenta com backoff.
                Log.d(TAG, "Erro de rede em ${op.entityType}/${op.operation}: ${e.message}")
                pendingSyncDao.incrementAttempts(op.id)
                return false

            } catch (e: Exception) {
                // Erro de dependência (ex: entidade pai ainda sem remoteId) — pára também
                // para garantir que a ordem das dependências é sempre respeitada.
                Log.e(TAG, "Erro em ${op.entityType}/${op.operation} (tentativa ${op.attempts + 1}): ${e.message}")
                pendingSyncDao.incrementAttempts(op.id)
                return false
            }
        }
        return true
    }

    /** Verifica se a API está acessível com um pedido leve ao endpoint de health. */
    private suspend fun isApiReachable(): Boolean = try {
        apiService.checkHealth().isSuccessful
    } catch (_: Exception) { false }

    private suspend fun syncWorkoutOp(op: PendingSyncEntity) {
        val payload = gson.fromJson(op.payloadJson, WorkoutSyncPayload::class.java)
        when (op.operation) {
            "CREATE" -> {
                val response = apiService.createWorkout(WorkoutRequest(payload.title, payload.description, payload.type))
                response.throwIfFailed("createWorkout")
                response.body()?.id?.let { remoteId -> workoutDao.updateRemoteId(op.localId, remoteId) }
            }
            "UPDATE" -> {
                val remoteId = payload.remoteId ?: workoutDao.getByIdOnce(op.localId)?.remoteId
                if (remoteId == null) return // ainda não foi criado remotamente; o CREATE pendente trata disto
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

    private suspend fun syncExerciseOp(op: PendingSyncEntity) {
        val payload = gson.fromJson(op.payloadJson, ExerciseSyncPayload::class.java)
        when (op.operation) {
            "CREATE" -> {
                val remoteWorkoutId = workoutDao.getByIdOnce(payload.workoutId)?.remoteId
                    ?: throw Exception("Workout pai ainda sem remoteId — tenta novamente depois")
                val response = apiService.createExercise(
                    remoteWorkoutId,
                    ExerciseRequest(payload.name, payload.description, payload.targetMuscle)
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

    private suspend fun syncExerciseSetOp(op: PendingSyncEntity) {
        val payload = gson.fromJson(op.payloadJson, ExerciseSetSyncPayload::class.java)
        when (op.operation) {
            "CREATE" -> {
                val remoteExerciseId = exerciseDao.getByIdOnce(payload.exerciseId)?.remoteId
                    ?: throw Exception("Exercise pai ainda sem remoteId — tenta novamente depois")
                val response = apiService.addExerciseSet(
                    remoteExerciseId,
                    ExerciseSetRequest(payload.reps, payload.weightKg.toDouble(), payload.durationSeconds)
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

    private suspend fun syncMachineConfigOp(op: PendingSyncEntity) {
        val payload = gson.fromJson(op.payloadJson, MachineConfigSyncPayload::class.java)
        when (op.operation) {
            "CREATE" -> {
                val remoteExerciseId = exerciseDao.getByIdOnce(payload.exerciseId)?.remoteId
                    ?: throw Exception("Exercise pai ainda sem remoteId — tenta novamente depois")
                val response = apiService.addMachineConfig(
                    remoteExerciseId,
                    MachineConfigRequest(payload.name, payload.description, payload.angleDegrees?.toDouble())
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

    private suspend fun syncPhotoOp(op: PendingSyncEntity) {
        val payload = gson.fromJson(op.payloadJson, PhotoSyncPayload::class.java)
        when (op.operation) {
            "CREATE" -> {
                val remoteExerciseId = exerciseDao.getByIdOnce(payload.exerciseId)?.remoteId
                    ?: throw Exception("Exercise pai ainda sem remoteId — tenta novamente depois")
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

    private suspend fun syncSessionSetOp(op: PendingSyncEntity) {
        val payload = gson.fromJson(op.payloadJson, SessionSetSyncPayload::class.java)
        when (op.operation) {
            "CREATE" -> {
                val remoteExerciseId = exerciseDao.getByIdOnce(payload.exerciseId)?.remoteId
                    ?: throw Exception("Exercise pai ainda sem remoteId — tenta novamente depois")
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

    private suspend fun syncSessionOp(op: PendingSyncEntity) {
        val payload = gson.fromJson(op.payloadJson, SessionSyncPayload::class.java)
        when (op.operation) {
            "FINALIZE" -> {
                val remoteExerciseId = exerciseDao.getByIdOnce(payload.exerciseId)?.remoteId
                    ?: throw Exception("Exercise pai ainda sem remoteId — tenta novamente depois")
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

    /**
     * Lança a excepção correcta consoante o código HTTP:
     * - 4xx (≠404): [PermanentSyncException] — erro permanente do servidor, a op deve ser descartada
     * - outros: [IOException] — erro transiente, a fila deve parar e o WorkManager retenta
     */
    private fun Response<*>.throwIfFailed(opName: String) {
        if (isSuccessful) return
        val httpCode = code()
        if (httpCode in 400..499 && httpCode != 404) {
            throw PermanentSyncException("$opName rejeitado pelo servidor ($httpCode)")
        }
        throw IOException("$opName falhou: $httpCode")
    }

    // ---------------------------------------------------------------------
    // Pull — traz dados que existem na API mas não localmente (ex: outro dispositivo)
    // ---------------------------------------------------------------------

    suspend fun pullWorkouts(localUserId: String) {
        try {
            val response = apiService.getWorkouts()
            if (!response.isSuccessful) return
            val remoteWorkouts = response.body().orEmpty()
            for (remote in remoteWorkouts) {
                val existing = workoutDao.getByRemoteId(remote.id)
                if (existing == null) {
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
            Log.d(TAG, "pullWorkouts: sem rede ou erro, mantém só os dados locais (${e.message})")
        }
    }

    /** Traz para o Room os exercises do workout indicado (id LOCAL) que existem na API mas ainda não localmente. */
    suspend fun pullExercises(localWorkoutId: String) {
        try {
            val remoteWorkoutId = workoutDao.getByIdOnce(localWorkoutId)?.remoteId ?: return
            val response = apiService.getExercises(remoteWorkoutId)
            if (!response.isSuccessful) return
            val remoteExercises = response.body().orEmpty()
            for (remote in remoteExercises) {
                val existing = exerciseDao.getByRemoteId(remote.id)
                if (existing == null) {
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
            Log.d(TAG, "pullExercises: sem rede ou erro, mantém só os dados locais (${e.message})")
        }
    }

    /** Traz para o Room as séries pré-definidas ("Meta") do exercise indicado que ainda não existem localmente. */
    suspend fun pullExerciseSets(localExerciseId: String) {
        try {
            val remoteExerciseId = exerciseDao.getByIdOnce(localExerciseId)?.remoteId ?: return
            val response = apiService.getExerciseSets(remoteExerciseId)
            if (!response.isSuccessful) return
            val remoteSets = response.body().orEmpty()
            for (remote in remoteSets) {
                val existing = exerciseSetDao.getByRemoteId(remote.id)
                if (existing == null) {
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
            Log.d(TAG, "pullExerciseSets: sem rede ou erro, mantém só os dados locais (${e.message})")
        }
    }

    /** Traz para o Room as configurações de máquina do exercise indicado que ainda não existem localmente. */
    suspend fun pullMachineConfigs(localExerciseId: String) {
        try {
            val remoteExerciseId = exerciseDao.getByIdOnce(localExerciseId)?.remoteId ?: return
            val response = apiService.getMachineConfigs(remoteExerciseId)
            if (!response.isSuccessful) return
            val remoteConfigs = response.body().orEmpty()
            for (remote in remoteConfigs) {
                val existing = machineConfigDao.getByRemoteId(remote.id)
                if (existing == null) {
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
            Log.d(TAG, "pullMachineConfigs: sem rede ou erro, mantém só os dados locais (${e.message})")
        }
    }

    /** Traz para o Room as fotos do exercise indicado que ainda não existem localmente. */
    suspend fun pullExercisePhotos(localExerciseId: String) {
        try {
            val remoteExerciseId = exerciseDao.getByIdOnce(localExerciseId)?.remoteId ?: return
            val response = apiService.getExercisePhotos(remoteExerciseId)
            if (!response.isSuccessful) return
            val remotePhotos = response.body().orEmpty()
            for (remote in remotePhotos) {
                val existing = exercisePhotoDao.getByRemoteId(remote.id)
                if (existing == null) {
                    exercisePhotoDao.insert(
                        ExercisePhotoEntity(
                            id = UUID.randomUUID().toString(),
                            exerciseId = localExerciseId,
                            uri = remote.uri, // URL absoluta hospedada — o Coil sabe carregar tanto http(s) como content://
                            createdAt = parseIsoDateOrNow(remote.createdAt),
                            remoteId = remote.id
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "pullExercisePhotos: sem rede ou erro, mantém só os dados locais (${e.message})")
        }
    }

    /** Traz para o Room o histórico de sessões FINALIZADAS do exercise indicado que ainda não existem localmente. */
    suspend fun pullSessionHistory(localExerciseId: String, localUserId: String) {
        try {
            val remoteExerciseId = exerciseDao.getByIdOnce(localExerciseId)?.remoteId ?: return
            val response = apiService.getSessionHistory(remoteExerciseId)
            if (!response.isSuccessful) return
            val remoteSessions = response.body().orEmpty()
            for (remote in remoteSessions) {
                val existingSession = exerciseSessionDao.getSessionByRemoteId(remote.id)
                val localSessionId = existingSession?.id ?: UUID.randomUUID().toString()
                if (existingSession == null) {
                    exerciseSessionDao.insertSession(
                        ExerciseSessionEntity(
                            id = localSessionId,
                            exerciseId = localExerciseId,
                            userId = localUserId,
                            createdAt = parseIsoDateOrNow(remote.createdAt),
                            finishedAt = remote.finishedAt?.let { parseIsoDateOrNow(it) },
                            status = remote.status,
                            remoteId = remote.id
                        )
                    )
                }
                remote.sets.orEmpty().forEach { remoteSet ->
                    val existingSet = exerciseSessionDao.getSetByRemoteId(remoteSet.id)
                    if (existingSet == null) {
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
            Log.d(TAG, "pullSessionHistory: sem rede ou erro, mantém só os dados locais (${e.message})")
        }
    }

    private fun parseIsoDateOrNow(iso: String): Long = try {
        Instant.parse(iso).toEpochMilli()
    } catch (_: Exception) {
        System.currentTimeMillis()
    }
}











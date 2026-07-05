package pt.ipt.dama.muscleup.ui.screens.exercise

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.ipt.dama.muscleup.MuscleUpApp
import pt.ipt.dama.muscleup.R
import pt.ipt.dama.muscleup.data.local.AppDatabase
import pt.ipt.dama.muscleup.data.local.ExercisePhotoEntity
import pt.ipt.dama.muscleup.data.local.ExerciseSetEntity
import pt.ipt.dama.muscleup.data.local.ExerciseSessionEntity
import pt.ipt.dama.muscleup.data.local.MachineConfigEntity
import pt.ipt.dama.muscleup.data.local.SessionExerciseSetEntity
import pt.ipt.dama.muscleup.data.local.toModel
import pt.ipt.dama.muscleup.data.session.UserSession
import pt.ipt.dama.muscleup.model.Exercise
import pt.ipt.dama.muscleup.model.ExercisePhoto
import pt.ipt.dama.muscleup.model.SessionExerciseSet
import java.io.File
import java.util.UUID

private const val TAG = "ExerciseViewModel"

/** Representa uma sessão de treino passada, com a lista de séries registadas. */
data class ExerciseHistorySession(
    val sessionId: String,
    val createdAt: Long,
    val finishedAt: Long?,
    val sets: List<SessionExerciseSet>
)

/** Recordes pessoais do utilizador para um determinado exercício. */
data class ExercisePersonalRecord(
    val maxWeightKg: Float? = null,
    val maxWeightReps: Int? = null,
    val maxReps: Int? = null,           // para exercícios só de peso corporal
    val maxDurationSeconds: Int? = null
)

/**
 * ViewModel do ecrã de detalhe de um exercício.
 *
 * Gere séries pré-definidas, configurações de máquina, fotos e sessões de treino (draft e histórico).
 * Sincroniza com a API no arranque e após cada operação de escrita.
 *
 * @param application Contexto da aplicação.
 * @param exerciseId Identificador do exercício a gerir.
 */
class ExerciseViewModel(
    application: Application,
    private val exerciseId: String
) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val exerciseDao = db.exerciseDao()
    private val exerciseSetDao = db.exerciseSetDao()
    private val sessionDao = db.exerciseSessionDao()
    private val machineConfigDao = db.machineConfigDao()
    private val userDao = db.userDao()
    private val exercisePhotoDao = db.exercisePhotoDao()

    private var currentSessionId: String? = null
    private val _currentSessionSets = MutableStateFlow<List<SessionExerciseSet>>(emptyList())
    val currentSessionSets: StateFlow<List<SessionExerciseSet>> = _currentSessionSets.asStateFlow()
    private val currentUserId = resolveCurrentUserId()

    init {
        restoreDraftSession()
        // (sets, machine configs, fotos, histórico de sessões) que ainda não existem localmente.
        viewModelScope.launch {
            val app = getApplication<MuscleUpApp>()
            try { app.syncManager.syncPending() } catch (_: Exception) { /* offline: ignorar */ }
            app.syncManager.pullExerciseSets(exerciseId)
            app.syncManager.pullMachineConfigs(exerciseId)
            app.syncManager.pullExercisePhotos(exerciseId)
            app.syncManager.pullSessionHistory(exerciseId, currentUserId)
        }
    }

    val userName: String get() = UserSession.currentUserName

    val profilePhotoUri: StateFlow<String?> = userDao
        .getUserByEmailFlow(UserSession.currentUserEmail)
        .map { it?.profilePhotoUri?.ifBlank { null } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** workoutId do exercício — necessário para navegar para o ecrã de edição. */
    val workoutId: StateFlow<String> = exerciseDao.getExerciseById(exerciseId)
        .map { it?.workoutId ?: "" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    // Carrega exercício, séries pré-definidas e configs da máquina de forma reativa
    val exercise: StateFlow<Exercise?> = combine(
        exerciseDao.getExerciseById(exerciseId),
        exerciseSetDao.getSetsForExercise(exerciseId),
        machineConfigDao.getConfigsForExercise(exerciseId)
    ) { exerciseEntity, setEntities, configEntities ->
        exerciseEntity?.toModel(
            sets = setEntities.map { it.toModel() },
            machineConfigs = configEntities.map { it.toModel() }
        )
    }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val historySessions: StateFlow<List<ExerciseHistorySession>> = combine(
        sessionDao.getFinishedSessionsForExercise(exerciseId, currentUserId),
        sessionDao.getFinishedSetsForExercise(exerciseId, currentUserId)
    ) { sessions, sets ->
        val setsBySession = sets
            .groupBy { it.sessionId }
            .mapValues { (_, groupedSets) -> groupedSets.sortedByDescending { it.setOrder } }

        sessions
            .sortedByDescending { it.finishedAt ?: it.createdAt }
            .map { session ->
            ExerciseHistorySession(
                sessionId = session.id,
                createdAt = session.createdAt,
                finishedAt = session.finishedAt,
                sets = setsBySession[session.id].orEmpty().map { it.toModel() }
            )
            }
    }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val personalRecord: StateFlow<ExercisePersonalRecord> = historySessions
        .map { sessions ->
            val allSets = sessions.flatMap { it.sets }
            val heaviestSet = allSets
                .filter { it.weightKg > 0f }
                .maxWithOrNull(compareBy<SessionExerciseSet> { it.weightKg }.thenBy { it.reps })
            val maxDuration = allSets.map { it.durationSeconds }.filter { it > 0 }.maxOrNull()
            val maxReps = allSets.map { it.reps }.filter { it > 0 }.maxOrNull()
            ExercisePersonalRecord(
                maxWeightKg = heaviestSet?.weightKg,
                maxWeightReps = heaviestSet?.reps,
                maxReps = maxReps,
                maxDurationSeconds = maxDuration
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ExercisePersonalRecord())

    val photos: StateFlow<List<ExercisePhoto>> = exercisePhotoDao
        .getPhotosForExercise(exerciseId)
        .map { entities -> entities.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    /** Adiciona uma série pré-definida ao modelo de treino do exercício. */
    fun addPredefinedSet(reps: Int, weightKg: Float?, durationSeconds: Int?) {
        if (reps <= 0) {
            viewModelScope.launch { _uiEvent.emit(getApplication<Application>().getString(R.string.exercise_error_reps_positive)) }
            return
        }
        if (weightKg != null && weightKg < 0f) {
            viewModelScope.launch { _uiEvent.emit(getApplication<Application>().getString(R.string.exercise_error_weight_negative)) }
            return
        }
        val app = getApplication<MuscleUpApp>()
        viewModelScope.launch {
            try {
                val nextSeriesOrder = exerciseSetDao.getNextSeriesOrder(exerciseId)
                val entity = ExerciseSetEntity(
                    id = UUID.randomUUID().toString(),
                    exerciseId = exerciseId,
                    createdAt = System.currentTimeMillis(),
                    seriesOrder = nextSeriesOrder,
                    reps = reps,
                    durationSeconds = durationSeconds ?: 0,
                    weightKg = weightKg ?: 0f
                )
                exerciseSetDao.insert(entity)
                app.syncManager.onExerciseSetCreated(entity)
                app.triggerSync()
            } catch (_: Exception) {
                _uiEvent.emit(app.getString(R.string.exercise_error_save_set))
            }
        }
    }

    /** Remove uma série pré-definida pelo seu identificador. */
    fun removePredefinedSet(setId: String) {
        val app = getApplication<MuscleUpApp>()
        viewModelScope.launch {
            try {
                val existing = exerciseSetDao.getByIdOnce(setId)
                exerciseSetDao.deleteById(setId)
                if (existing != null) {
                    app.syncManager.onExerciseSetDeleted(existing)
                    app.triggerSync()
                }
            } catch (_: Exception) {
                _uiEvent.emit(app.getString(R.string.exercise_error_remove_set))
            }
        }
    }

    /** Guarda uma nova configuração de máquina (nome, descrição e ângulo opcional) para este exercício. */
    fun addMachineConfig(name: String, description: String, angleDegrees: Float? = null) {
        if (name.isBlank()) {
            viewModelScope.launch { _uiEvent.emit(getApplication<Application>().getString(R.string.exercise_error_config_name_empty)) }
            return
        }
        val app = getApplication<MuscleUpApp>()
        viewModelScope.launch {
            try {
                val entity = MachineConfigEntity(
                    id = UUID.randomUUID().toString(),
                    exerciseId = exerciseId,
                    name = name.trim(),
                    description = description.trim(),
                    createdAt = System.currentTimeMillis(),
                    angleDegrees = angleDegrees
                )
                machineConfigDao.insert(entity)
                app.syncManager.onMachineConfigCreated(entity)
                app.triggerSync()
            } catch (_: Exception) {
                _uiEvent.emit(app.getString(R.string.exercise_error_save_config))
            }
        }
    }

    /** Remove uma configuração de máquina pelo seu identificador. */
    fun removeMachineConfig(configId: String) {
        val app = getApplication<MuscleUpApp>()
        viewModelScope.launch {
            try {
                val existing = machineConfigDao.getByIdOnce(configId)
                machineConfigDao.deleteById(configId)
                if (existing != null) {
                    app.syncManager.onMachineConfigDeleted(existing)
                    app.triggerSync()
                }
            } catch (_: Exception) {
                _uiEvent.emit(app.getString(R.string.exercise_error_remove_config))
            }
        }
    }

    /** Cria um ficheiro temporário no FileProvider e devolve o [Uri] para a câmara escrever. */
    fun createPhotoUri(context: Context): Uri {
        val dir = File(context.filesDir, "exercise_photos").apply { if (!exists()) mkdirs() }
        val file = File(dir, "${exerciseId}_${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** Guarda uma foto do exercício a partir do [uri] fornecido. Rejeita ficheiros superiores a 10 MB. */
    fun addPhoto(uri: String) {
        val app = getApplication<MuscleUpApp>()
        viewModelScope.launch {
            try {
                // Verifica o tamanho antes de guardar — a API não aceita ficheiros > 10 MB
                val fileSizeBytes = app.contentResolver
                    .openFileDescriptor(uri.toUri(), "r")?.use { it.statSize } ?: 0L
                val maxBytes = 10L * 1024 * 1024 // 10 MB
                if (fileSizeBytes > maxBytes) {
                    val sizeMb = "%.1f".format(fileSizeBytes / (1024.0 * 1024.0))
                    _uiEvent.emit(app.getString(R.string.exercise_error_photo_too_large, sizeMb))
                    return@launch
                }

                val entity = ExercisePhotoEntity(
                    id = UUID.randomUUID().toString(),
                    exerciseId = exerciseId,
                    uri = uri,
                    createdAt = System.currentTimeMillis()
                )
                exercisePhotoDao.insert(entity)
                app.syncManager.onPhotoCreated(entity)
                app.triggerSync()
            } catch (_: Exception) {
                _uiEvent.emit(app.getString(R.string.exercise_error_save_photo))
            }
        }
    }

    /** Remove uma foto pelo seu identificador e apaga o ficheiro local se for do FileProvider. */
    fun removePhoto(photoId: String) {
        val app = getApplication<MuscleUpApp>()
        viewModelScope.launch {
            try {
                val existing = exercisePhotoDao.getByIdOnce(photoId)
                exercisePhotoDao.deleteById(photoId)
                existing?.uri?.let { deleteLocalPhotoFile(it) }
                if (existing != null) {
                    app.syncManager.onPhotoDeleted(existing)
                    app.triggerSync()
                }
            } catch (_: Exception) {
                _uiEvent.emit(app.getString(R.string.exercise_error_remove_photo))
            }
        }
    }

    private fun deleteLocalPhotoFile(uriString: String) {
        try {
            val context = getApplication<Application>()
            val uri = uriString.toUri()
            // Só apaga o ficheiro se pertencer ao nosso FileProvider (fotos tiradas com a câmara).
            // Fotos escolhidas da galeria (content:// do MediaStore) não são tocadas.
            if (uri.authority == "${context.packageName}.fileprovider") {
                context.contentResolver.delete(uri, null, null)
            }
        } catch (_: Exception) {
            // Falha na limpeza do ficheiro não é crítica — o registo já foi removido da BD.
        }
    }

    /** Regista uma série na sessão de treino em curso (cria um draft se ainda não existir). */
    fun addRecordedSet(reps: Int, weightKg: Float?, durationSeconds: Int?) {
        if (reps <= 0) {
            viewModelScope.launch { _uiEvent.emit(getApplication<Application>().getString(R.string.exercise_error_reps_positive)) }
            return
        }
        if (weightKg != null && weightKg < 0f) {
            viewModelScope.launch { _uiEvent.emit(getApplication<Application>().getString(R.string.exercise_error_weight_negative)) }
            return
        }
        val app = getApplication<MuscleUpApp>()
        viewModelScope.launch {
            try {
                val sessionId = ensureDraftSessionId()
                val setOrder = sessionDao.getNextSetOrder(sessionId)
                val sessionSet = SessionExerciseSetEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    reps = reps,
                    durationSeconds = durationSeconds ?: 0,
                    weightKg = weightKg ?: 0f,
                    setOrder = setOrder,
                    createdAt = System.currentTimeMillis()
                )
                sessionDao.insertSessionSet(sessionSet)
                reloadCurrentSessionSets(sessionId)
                app.syncManager.onSessionSetCreated(exerciseId, sessionSet)
                app.triggerSync()
                Log.d(TAG, "Set added: reps=$reps, weight=${weightKg ?: 0f}kg, time=${durationSeconds ?: 0}s, order=$setOrder")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding recorded set: ${e.message}", e)
                _uiEvent.emit(app.getString(R.string.exercise_error_record_set))
            }
        }
    }

    /** Remove uma série registada da sessão em curso. */
    fun removeRecordedSet(setId: String) {
        val app = getApplication<MuscleUpApp>()
        viewModelScope.launch {
            try {
                val existing = sessionDao.getSetByIdOnce(setId)
                sessionDao.deleteSessionSetById(setId)
                currentSessionId?.let { reloadCurrentSessionSets(it) }
                if (existing != null) {
                    app.syncManager.onSessionSetDeleted(exerciseId, existing)
                    app.triggerSync()
                }
            } catch (_: Exception) {
                _uiEvent.emit(app.getString(R.string.exercise_error_remove_set))
            }
        }
    }

    /** Termina e guarda a sessão de treino atual. Requer pelo menos uma série registada. */
    fun finalizeSession() {
        val app = getApplication<MuscleUpApp>()
        viewModelScope.launch {
            if (currentSessionId != null && _currentSessionSets.value.isNotEmpty()) {
                try {
                    val sessionId = currentSessionId!!
                    val finishTime = System.currentTimeMillis()
                    sessionDao.finalizeSession(sessionId, finishTime)
                    resetCurrentSessionState()
                    app.syncManager.onSessionFinalized(exerciseId, sessionId)
                    app.triggerSync()
                    _uiEvent.emit(app.getString(R.string.exercise_session_saved))
                } catch (e: Exception) {
                    Log.e(TAG, "Error finalizing session: ${e.message}", e)
                    _uiEvent.emit(app.getString(R.string.exercise_error_finalize_session))
                }
            } else {
                _uiEvent.emit(app.getString(R.string.exercise_error_need_one_set))
            }
        }
    }

    /** Descarta a sessão de treino em curso, apagando todas as séries registadas. */
    fun clearSession() {
        val app = getApplication<MuscleUpApp>()
        viewModelScope.launch {
            try {
                val sessionId = currentSessionId
                sessionId?.let { sessionDao.deleteSessionById(it) }
                resetCurrentSessionState()
                if (sessionId != null) {
                    app.syncManager.onSessionDiscarded(exerciseId, sessionId)
                    app.triggerSync()
                }
            } catch (_: Exception) {
                _uiEvent.emit(app.getString(R.string.exercise_error_clear_session))
            }
        }
    }

    private fun resolveCurrentUserId(): String {
        return UserSession.currentUserId.ifBlank { UserSession.currentUserEmail }
    }

    private suspend fun ensureDraftSessionId(): String {
        currentSessionId?.let { return it }

        val userId = resolveCurrentUserId()
        val existingDraft = sessionDao.getLatestDraftSession(exerciseId, userId)
        if (existingDraft != null) {
            currentSessionId = existingDraft.id
            return existingDraft.id
        }

        val newSessionId = UUID.randomUUID().toString()
        currentSessionId = newSessionId
        Log.d(TAG, "Creating new draft session: $newSessionId for exercise: $exerciseId, user: $userId")
        sessionDao.insertSession(
            ExerciseSessionEntity(
                id = newSessionId,
                exerciseId = exerciseId,
                userId = userId,
                createdAt = System.currentTimeMillis(),
                status = "DRAFT"
            )
        )
        return newSessionId
    }

    private fun restoreDraftSession() {
        viewModelScope.launch {
            val userId = resolveCurrentUserId()
            if (userId.isBlank()) {
                Log.w(TAG, "Não é possível restaurar a sessão em rascunho sem um identificador de utilizador.")
                return@launch
            }

            val draftSession = sessionDao.getLatestDraftSession(exerciseId, userId)
            if (draftSession == null) {
                resetCurrentSessionState()
                return@launch
            }

            currentSessionId = draftSession.id
            reloadCurrentSessionSets(draftSession.id)
            Log.d(TAG, "Sessão em rascunho ${draftSession.id} restaurada com ${_currentSessionSets.value.size} séries.")
        }
    }

    /** Recarrega as séries da sessão indicada para [currentSessionSets]. */
    private suspend fun reloadCurrentSessionSets(sessionId: String) {
        _currentSessionSets.value = sessionDao
            .getSetsForSessionOnce(sessionId)
            .map { it.toModel() }
    }

    /** Repõe o estado da sessão em curso, indicando que não existe nenhuma sessão ativa. */
    private fun resetCurrentSessionState() {
        currentSessionId = null
        _currentSessionSets.value = emptyList()
    }

    companion object {
    /** Cria a fábrica para instanciar o ExerciseViewModel com o id do exercício. */
        fun factory(exerciseId: String): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return ExerciseViewModel(app, exerciseId) as T
            }
        }
    }
}

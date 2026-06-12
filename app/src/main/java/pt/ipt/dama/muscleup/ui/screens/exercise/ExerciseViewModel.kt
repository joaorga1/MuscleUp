package pt.ipt.dama.muscleup.ui.screens.exercise

import android.app.Application
import android.util.Log
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
import pt.ipt.dama.muscleup.data.local.AppDatabase
import pt.ipt.dama.muscleup.data.local.ExerciseSetEntity
import pt.ipt.dama.muscleup.data.local.ExerciseSessionEntity
import pt.ipt.dama.muscleup.data.local.MachineConfigEntity
import pt.ipt.dama.muscleup.data.local.SessionExerciseSetEntity
import pt.ipt.dama.muscleup.data.local.toModel
import pt.ipt.dama.muscleup.data.session.UserSession
import pt.ipt.dama.muscleup.model.Exercise
import pt.ipt.dama.muscleup.model.SessionExerciseSet
import java.util.UUID

private const val TAG = "ExerciseViewModel"

data class ExerciseHistorySession(
    val sessionId: String,
    val createdAt: Long,
    val finishedAt: Long?,
    val sets: List<SessionExerciseSet>
)

data class ExercisePersonalRecord(
    val maxWeightKg: Float? = null,
    val maxWeightReps: Int? = null,
    val maxDurationSeconds: Int? = null
)

class ExerciseViewModel(
    application: Application,
    private val exerciseId: String
) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val exerciseDao = db.exerciseDao()
    private val exerciseSetDao = db.exerciseSetDao()
    private val sessionDao = db.exerciseSessionDao()
    private val machineConfigDao = db.machineConfigDao()

    private var currentSessionId: String? = null
    private val _currentSessionSets = MutableStateFlow<List<SessionExerciseSet>>(emptyList())
    val currentSessionSets: StateFlow<List<SessionExerciseSet>> = _currentSessionSets.asStateFlow()
    private val currentUserId = resolveCurrentUserId()

    init {
        restoreDraftSession()
    }

    val userName: String get() = UserSession.currentUserName

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
            ExercisePersonalRecord(
                maxWeightKg = heaviestSet?.weightKg,
                maxWeightReps = heaviestSet?.reps,
                maxDurationSeconds = maxDuration
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ExercisePersonalRecord())

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    fun addPredefinedSet(reps: Int, weightKg: Float?, durationSeconds: Int?) {
        if (reps <= 0) {
            viewModelScope.launch { _uiEvent.emit("As repetições têm de ser maiores que 0") }
            return
        }
        if (weightKg != null && weightKg < 0f) {
            viewModelScope.launch { _uiEvent.emit("O peso não pode ser negativo") }
            return
        }
        viewModelScope.launch {
            try {
                val nextSeriesOrder = exerciseSetDao.getNextSeriesOrder(exerciseId)
                exerciseSetDao.insert(
                    ExerciseSetEntity(
                        id = UUID.randomUUID().toString(),
                        exerciseId = exerciseId,
                        createdAt = System.currentTimeMillis(),
                        seriesOrder = nextSeriesOrder,
                        reps = reps,
                        durationSeconds = durationSeconds ?: 0,
                        weightKg = weightKg ?: 0f
                    )
                )
            } catch (_: Exception) {
                _uiEvent.emit("Erro ao guardar série. Tenta novamente.")
            }
        }
    }

    fun removePredefinedSet(setId: String) {
        viewModelScope.launch {
            try {
                exerciseSetDao.deleteById(setId)
            } catch (_: Exception) {
                _uiEvent.emit("Erro ao remover série. Tenta novamente.")
            }
        }
    }

    fun addMachineConfig(name: String, description: String) {
        if (name.isBlank()) {
            viewModelScope.launch { _uiEvent.emit("O nome da configuração não pode estar vazio") }
            return
        }
        viewModelScope.launch {
            try {
                machineConfigDao.insert(
                    MachineConfigEntity(
                        id = UUID.randomUUID().toString(),
                        exerciseId = exerciseId,
                        name = name.trim(),
                        description = description.trim(),
                        createdAt = System.currentTimeMillis()
                    )
                )
            } catch (_: Exception) {
                _uiEvent.emit("Erro ao guardar configuração. Tenta novamente.")
            }
        }
    }

    fun removeMachineConfig(configId: String) {
        viewModelScope.launch {
            try {
                machineConfigDao.deleteById(configId)
            } catch (_: Exception) {
                _uiEvent.emit("Erro ao remover configuração. Tenta novamente.")
            }
        }
    }

    fun addRecordedSet(reps: Int, weightKg: Float?, durationSeconds: Int?) {
        if (reps <= 0) {
            viewModelScope.launch { _uiEvent.emit("As repetições têm de ser maiores que 0") }
            return
        }
        if (weightKg != null && weightKg < 0f) {
            viewModelScope.launch { _uiEvent.emit("O peso não pode ser negativo") }
            return
        }
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
                Log.d(TAG, "Set added: reps=$reps, weight=${weightKg ?: 0f}kg, time=${durationSeconds ?: 0}s, order=$setOrder")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding recorded set: ${e.message}", e)
                _uiEvent.emit("Erro ao registar série. Tenta novamente.")
            }
        }
    }

    fun removeRecordedSet(setId: String) {
        viewModelScope.launch {
            try {
                sessionDao.deleteSessionSetById(setId)
                currentSessionId?.let { reloadCurrentSessionSets(it) }
            } catch (_: Exception) {
                _uiEvent.emit("Erro ao remover série. Tenta novamente.")
            }
        }
    }

    fun finalizeSession() {
        viewModelScope.launch {
            if (currentSessionId != null && _currentSessionSets.value.isNotEmpty()) {
                try {
                    val finishTime = System.currentTimeMillis()
                    sessionDao.finalizeSession(currentSessionId!!, finishTime)
                    resetCurrentSessionState()
                    _uiEvent.emit("Sessão guardada com sucesso!")
                } catch (e: Exception) {
                    Log.e(TAG, "Error finalizing session: ${e.message}", e)
                    _uiEvent.emit("Erro ao finalizar sessão. Tenta novamente.")
                }
            } else {
                _uiEvent.emit("Adiciona pelo menos uma série antes de finalizar")
            }
        }
    }

    fun clearSession() {
        viewModelScope.launch {
            try {
                currentSessionId?.let { sessionDao.deleteSessionById(it) }
                resetCurrentSessionState()
            } catch (_: Exception) {
                _uiEvent.emit("Erro ao limpar sessão. Tenta novamente.")
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
                Log.w(TAG, "Cannot restore draft session without user id")
                return@launch
            }

            val draftSession = sessionDao.getLatestDraftSession(exerciseId, userId)
            if (draftSession == null) {
                resetCurrentSessionState()
                return@launch
            }

            currentSessionId = draftSession.id
            reloadCurrentSessionSets(draftSession.id)
            Log.d(TAG, "Restored draft session ${draftSession.id} with ${_currentSessionSets.value.size} sets")
        }
    }

    private suspend fun reloadCurrentSessionSets(sessionId: String) {
        _currentSessionSets.value = sessionDao
            .getSetsForSessionOnce(sessionId)
            .map { it.toModel() }
    }

    private fun resetCurrentSessionState() {
        currentSessionId = null
        _currentSessionSets.value = emptyList()
    }

    companion object {
        fun factory(exerciseId: String): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return ExerciseViewModel(app, exerciseId) as T
            }
        }
    }
}

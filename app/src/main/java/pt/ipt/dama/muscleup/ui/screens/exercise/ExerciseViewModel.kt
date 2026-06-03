package pt.ipt.dama.muscleup.ui.screens.exercise

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.ipt.dama.muscleup.data.local.AppDatabase
import pt.ipt.dama.muscleup.data.local.ExerciseSetEntity
import pt.ipt.dama.muscleup.data.local.ExerciseSessionEntity
import pt.ipt.dama.muscleup.data.local.SessionExerciseSetEntity
import pt.ipt.dama.muscleup.data.local.toModel
import pt.ipt.dama.muscleup.data.session.UserSession
import pt.ipt.dama.muscleup.model.Exercise
import pt.ipt.dama.muscleup.model.SessionExerciseSet
import java.util.UUID

private const val TAG = "ExerciseViewModel"

class ExerciseViewModel(
    application: Application,
    private val exerciseId: String
) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val exerciseDao = db.exerciseDao()
    private val exerciseSetDao = db.exerciseSetDao()
    private val sessionDao = db.exerciseSessionDao()

    private var currentSessionId: String? = null
    private val _currentSessionSets = MutableStateFlow<List<SessionExerciseSet>>(emptyList())
    val currentSessionSets: StateFlow<List<SessionExerciseSet>> = _currentSessionSets.asStateFlow()

    init {
        restoreDraftSession()
    }

    val userName: String get() = UserSession.currentUserName

    // Carrega exercício e séries pré-definidas de forma reativa
    val exercise: StateFlow<Exercise?> = combine(
        exerciseDao.getExerciseById(exerciseId),
        exerciseSetDao.getSetsForExercise(exerciseId)
    ) { exerciseEntity, setEntities ->
        exerciseEntity?.toModel(sets = setEntities.map { it.toModel() })
    }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun addPredefinedSet(reps: Int, weightKg: Float?, durationSeconds: Int?) {
        viewModelScope.launch {
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
        }
    }

    fun removePredefinedSet(setId: String) {
        viewModelScope.launch {
            exerciseSetDao.deleteById(setId)
        }
    }

    fun addRecordedSet(reps: Int, weightKg: Float?, durationSeconds: Int?) {
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
                Log.d(TAG, "Set added to memory: reps=$reps, weight=${weightKg ?: 0f}kg, time=${durationSeconds ?: 0}s, order=$setOrder")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding recorded set: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }

    fun removeRecordedSet(setId: String) {
        viewModelScope.launch {
            sessionDao.deleteSessionSetById(setId)
            currentSessionId?.let { reloadCurrentSessionSets(it) }
        }
    }

    fun finalizeSession() {
        viewModelScope.launch {
            if (currentSessionId != null && _currentSessionSets.value.isNotEmpty()) {
                try {
                    Log.d(TAG, "Starting session finalization. SessionId: $currentSessionId, Sets count: ${_currentSessionSets.value.size}")

                    // Mark session as finished
                    val finishTime = System.currentTimeMillis()
                    sessionDao.finalizeSession(currentSessionId!!, finishTime)
                    Log.d(TAG, "Session finalized with finishedAt: $finishTime")

                    // Clear only in-memory state; session remains persisted as FINISHED.
                    resetCurrentSessionState()
                    Log.d(TAG, "Session cleared from memory")
                } catch (e: Exception) {
                    Log.e(TAG, "Error finalizing session: ${e.message}", e)
                    e.printStackTrace()
                }
            } else {
                Log.w(TAG, "Cannot finalize session. SessionId: $currentSessionId, Sets: ${_currentSessionSets.value.size}")
            }
        }
    }

    fun clearSession() {
        viewModelScope.launch {
            currentSessionId?.let { sessionDao.deleteSessionById(it) }
            resetCurrentSessionState()
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





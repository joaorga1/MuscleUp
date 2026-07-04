package pt.ipt.dama.muscleup.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.ipt.dama.muscleup.MuscleUpApp
import pt.ipt.dama.muscleup.data.local.AppDatabase
import pt.ipt.dama.muscleup.data.local.WorkoutEntity
import pt.ipt.dama.muscleup.data.local.toModel
import pt.ipt.dama.muscleup.data.session.UserSession
import pt.ipt.dama.muscleup.model.Workout
import pt.ipt.dama.muscleup.model.WorkoutType
import java.util.UUID

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val workoutDao = db.workoutDao()
    private val userDao = db.userDao()
    private val sessionPreferences = (application as MuscleUpApp).sessionPreferences
    private val _currentUserId = MutableStateFlow(resolveCurrentUserId())

    val userName: String get() = UserSession.currentUserName

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val profilePhotoUri: StateFlow<String?> = _currentUserId
        .flatMapLatest { email ->
            if (email.isBlank()) flowOf(null)
            else userDao.getUserByEmailFlow(email).map { it?.profilePhotoUri?.ifBlank { null } }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _workouts = MutableStateFlow<List<Workout>>(emptyList())
    val workouts: StateFlow<List<Workout>> = _workouts.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    init {
        observeWorkoutsForCurrentUser()
        viewModelScope.launch {
            syncBeforePull(_currentUserId.value)
        }
    }

    private fun observeWorkoutsForCurrentUser() {
        viewModelScope.launch {
            _currentUserId.collectLatest { userId ->
                if (userId.isBlank()) {
                    _workouts.value = emptyList()
                } else {
                    workoutDao.getWorkoutsForUser(userId).collectLatest { list ->
                        _workouts.value = list.map { it.toModel() }
                    }
                }
            }
        }
    }

    fun refreshSessionUser() {
        val resolvedUserId = resolveCurrentUserId()
        if (resolvedUserId == _currentUserId.value) return
        _currentUserId.value = resolvedUserId
        viewModelScope.launch {
            syncBeforePull(resolvedUserId)
        }
    }

    fun getWorkoutById(id: String): Workout? = _workouts.value.find { it.id == id }

    fun addWorkout(title: String, description: String, type: WorkoutType) {
        val userId = _currentUserId.value
        val app = getApplication<MuscleUpApp>()
        if (userId.isNotBlank()) {
            viewModelScope.launch {
                try {
                    val entity = WorkoutEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        title = title.trim(),
                        description = description.trim(),
                        type = type.name
                    )
                    workoutDao.insert(entity)
                    // Passo 8.3 — grava local primeiro (instantâneo), sincroniza com a API a seguir.
                    app.syncManager.onWorkoutCreated(entity)
                    app.triggerSync()
                } catch (_: Exception) {
                    _uiEvent.emit("Erro ao guardar treino. Tenta novamente.")
                }
            }
        }
    }

    fun editWorkout(id: String, title: String, description: String, type: WorkoutType) {
        val userId = _currentUserId.value
        val app = getApplication<MuscleUpApp>()
        if (userId.isNotBlank()) {
            viewModelScope.launch {
                try {
                    // Preserva o remoteId já atribuído (senão o próximo sync trataria isto como um novo workout).
                    val existingRemoteId = workoutDao.getByIdOnce(id)?.remoteId
                    val entity = WorkoutEntity(
                        id = id,
                        userId = userId,
                        title = title.trim(),
                        description = description.trim(),
                        type = type.name,
                        remoteId = existingRemoteId
                    )
                    workoutDao.update(entity)
                    app.syncManager.onWorkoutUpdated(entity)
                    app.triggerSync()
                } catch (_: Exception) {
                    _uiEvent.emit("Erro ao atualizar treino. Tenta novamente.")
                }
            }
        }
    }

    fun deleteWorkout(id: String) {
        val app = getApplication<MuscleUpApp>()
        viewModelScope.launch {
            try {
                val existing = workoutDao.getByIdOnce(id)
                workoutDao.deleteById(id)
                if (existing != null) {
                    app.syncManager.onWorkoutDeleted(existing)
                    app.triggerSync()
                }
            } catch (_: Exception) {
                _uiEvent.emit("Erro ao apagar treino. Tenta novamente.")
            }
        }
    }

    fun logout() {
        val app = getApplication<MuscleUpApp>()
        // Tenta esvaziar a fila pendente ENQUANTO o token ainda é válido, antes de limpar a sessão.
        viewModelScope.launch {
            try { app.syncManager.syncPending() } catch (_: Exception) { /* offline: ignorar */ }
            try { app.apiService.logout() } catch (_: Exception) { /* stateless: ignorar erro */ }
            app.tokenManager.clear()
            sessionPreferences.clear()
            UserSession.clear()
            _currentUserId.value = ""
        }
    }

    // Esvazia primeiro a fila de operações pendentes e SÓ DEPOIS puxa o estado remoto.
    private suspend fun syncBeforePull(userId: String) {
        if (userId.isBlank()) return
        val app = getApplication<MuscleUpApp>()
        try { app.syncManager.syncPending() } catch (_: Exception) { /* offline: ignorar */ }
        app.syncManager.pullWorkouts(userId)
    }

    private fun resolveCurrentUserId(): String {
        return sessionPreferences.getSavedEmail().orEmpty().ifBlank { UserSession.currentUserEmail }
    }
}

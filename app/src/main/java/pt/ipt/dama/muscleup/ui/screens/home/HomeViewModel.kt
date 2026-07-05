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
import pt.ipt.dama.muscleup.R
import pt.ipt.dama.muscleup.data.local.AppDatabase
import pt.ipt.dama.muscleup.data.local.WorkoutEntity
import pt.ipt.dama.muscleup.data.local.toModel
import pt.ipt.dama.muscleup.data.session.UserSession
import pt.ipt.dama.muscleup.model.Workout
import pt.ipt.dama.muscleup.model.WorkoutType
import java.util.UUID

/** ViewModel do ecrã inicial: gere a lista de treinos e o logout. */
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

    private val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack: SharedFlow<Unit> = _navigateBack.asSharedFlow()

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

    /** Atualiza o utilizador atual após login e sincroniza. */
    fun refreshSessionUser() {
        val resolvedUserId = resolveCurrentUserId()
        if (resolvedUserId == _currentUserId.value) return
        _currentUserId.value = resolvedUserId
        viewModelScope.launch {
            syncBeforePull(resolvedUserId)
        }
    }

    /** Devolve o treino com o id indicado, se estiver em memória. */
    fun getWorkoutById(id: String): Workout? = _workouts.value.find { it.id == id }

    /** Cria um novo treino, verificando duplicados. */
    fun addWorkout(title: String, description: String, type: WorkoutType) {
        val userId = _currentUserId.value
        val app = getApplication<MuscleUpApp>()
        if (userId.isNotBlank()) {
            viewModelScope.launch {
                try {
                    if (workoutDao.isDuplicate(userId, null, title.trim(), description.trim(), type.name)) {
                        _uiEvent.emit(app.getString(R.string.home_error_duplicate_workout))
                        return@launch
                    }
                    val entity = WorkoutEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        title = title.trim(),
                        description = description.trim(),
                        type = type.name
                    )
                    workoutDao.insert(entity)
                    app.syncManager.onWorkoutCreated(entity)
                    app.triggerSync()
                    _navigateBack.emit(Unit)
                } catch (_: Exception) {
                    _uiEvent.emit(app.getString(R.string.home_error_save_workout))
                }
            }
        }
    }

    /** Atualiza um treino existente, verificando duplicados. */
    fun editWorkout(id: String, title: String, description: String, type: WorkoutType) {
        val userId = _currentUserId.value
        val app = getApplication<MuscleUpApp>()
        if (userId.isNotBlank()) {
            viewModelScope.launch {
                try {
                    if (workoutDao.isDuplicate(userId, id, title.trim(), description.trim(), type.name)) {
                        _uiEvent.emit(app.getString(R.string.home_error_duplicate_workout))
                        return@launch
                    }
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
                    _navigateBack.emit(Unit)
                } catch (_: Exception) {
                    _uiEvent.emit(app.getString(R.string.home_error_update_workout))
                }
            }
        }
    }

    /** Remove um treino. */
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
                _uiEvent.emit(app.getString(R.string.home_error_delete_workout))
            }
        }
    }

    /** Termina a sessão, tentando sincronizar a fila pendente antes de limpar os dados. */
    fun logout() {
        val app = getApplication<MuscleUpApp>()
        viewModelScope.launch {
            try { app.syncManager.syncPending() } catch (_: Exception) { }
            try { app.apiService.logout() } catch (_: Exception) { }
            app.tokenManager.clear()
            sessionPreferences.clear()
            UserSession.clear()
            _currentUserId.value = ""
        }
    }

    /** Esvazia a fila pendente e traz os treinos remotos. */
    private suspend fun syncBeforePull(userId: String) {
        if (userId.isBlank()) return
        val app = getApplication<MuscleUpApp>()
        try { app.syncManager.syncPending() } catch (_: Exception) { }
        app.syncManager.pullWorkouts(userId)
    }

    /** Devolve o id do utilizador a partir das preferências ou da sessão em memória. */
    private fun resolveCurrentUserId(): String {
        return sessionPreferences.getSavedEmail().orEmpty().ifBlank { UserSession.currentUserEmail }
    }
}

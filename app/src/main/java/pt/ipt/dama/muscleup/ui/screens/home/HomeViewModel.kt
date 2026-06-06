package pt.ipt.dama.muscleup.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import pt.ipt.dama.muscleup.MuscleUpApp
import pt.ipt.dama.muscleup.data.local.AppDatabase
import pt.ipt.dama.muscleup.data.local.DatabaseSeeder
import pt.ipt.dama.muscleup.data.local.WorkoutEntity
import pt.ipt.dama.muscleup.data.local.toModel
import pt.ipt.dama.muscleup.data.session.UserSession
import pt.ipt.dama.muscleup.model.Workout
import pt.ipt.dama.muscleup.model.WorkoutType
import java.util.UUID

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val workoutDao = db.workoutDao()
    private val sessionPreferences = (application as MuscleUpApp).sessionPreferences
    private val _currentUserId = MutableStateFlow(resolveCurrentUserId())

    val userName: String get() = UserSession.currentUserName

    private val _workouts = MutableStateFlow<List<Workout>>(emptyList())
    val workouts: StateFlow<List<Workout>> = _workouts.asStateFlow()

    init {
        observeWorkoutsForCurrentUser()
        viewModelScope.launch { seedIfNeeded(_currentUserId.value) }
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
        viewModelScope.launch { seedIfNeeded(resolvedUserId) }
    }

    fun getWorkoutById(id: String): Workout? = _workouts.value.find { it.id == id }

    fun addWorkout(title: String, description: String, type: WorkoutType) {
        val userId = _currentUserId.value
        if (userId.isNotBlank()) {
            viewModelScope.launch {
                workoutDao.insert(
                    WorkoutEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        title = title,
                        description = description,
                        type = type.name
                    )
                )
            }
        }
    }

    fun editWorkout(id: String, title: String, description: String, type: WorkoutType) {
        val userId = _currentUserId.value
        if (userId.isNotBlank()) {
            viewModelScope.launch {
                workoutDao.update(
                    WorkoutEntity(
                        id = id,
                        userId = userId,
                        title = title,
                        description = description,
                        type = type.name
                    )
                )
            }
        }
    }

    fun deleteWorkout(id: String) {
        viewModelScope.launch { workoutDao.deleteById(id) }
    }

    fun logout() {
        sessionPreferences.clear()
        UserSession.clear()
        _currentUserId.value = ""
    }

    private suspend fun seedIfNeeded(userId: String) {
        if (userId.isNotBlank()) DatabaseSeeder.seed(db, userId)
    }

    private fun resolveCurrentUserId(): String {
        return sessionPreferences.getSavedEmail().orEmpty().ifBlank { UserSession.currentUserEmail }
    }
}

package pt.ipt.dama.muscleup.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    val userName: String get() = UserSession.currentUserName

    // Lê o userId das SharedPreferences — garantido mesmo antes do UserSession ser populado
    private val userId = sessionPreferences.getSavedEmail().orEmpty()

    val workouts: StateFlow<List<Workout>> = workoutDao
        .getWorkoutsForUser(userId)
        .map { list -> list.map { it.toModel() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            if (userId.isNotBlank()) DatabaseSeeder.seed(db, userId)
        }
    }

    fun getWorkoutById(id: String): Workout? = workouts.value.find { it.id == id }

    fun addWorkout(title: String, description: String, type: WorkoutType) {
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

    fun editWorkout(id: String, title: String, description: String, type: WorkoutType) {
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

    fun deleteWorkout(id: String) {
        viewModelScope.launch {
            workoutDao.deleteById(id)
        }
    }

    fun logout() {
        sessionPreferences.clear()
        UserSession.clear()
    }
}

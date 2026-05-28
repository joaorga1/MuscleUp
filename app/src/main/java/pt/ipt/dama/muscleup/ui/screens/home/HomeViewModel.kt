package pt.ipt.dama.muscleup.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pt.ipt.dama.muscleup.MuscleUpApp
import pt.ipt.dama.muscleup.data.session.UserSession
import pt.ipt.dama.muscleup.model.Exercise
import pt.ipt.dama.muscleup.model.Workout
import pt.ipt.dama.muscleup.model.WorkoutType
import java.util.UUID

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionPreferences = (application as MuscleUpApp).sessionPreferences

    val userName: String get() = UserSession.currentUserName
    private val currentUserId: String get() = UserSession.currentUserEmail

    // guarda todos os treinos internamente
    private val _allWorkouts = MutableStateFlow<List<Workout>>(emptyList())

    // expõe apenas os treinos do utilizador atual, de forma reativa
    val workouts: StateFlow<List<Workout>> = _allWorkouts
        .map { list -> list.filter { it.userId == currentUserId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // lê o email das SharedPreferences — disponível mesmo antes do UserSession ser populado
        val userId = sessionPreferences.getSavedEmail().orEmpty()
        if (userId.isNotBlank()) {
            _allWorkouts.value = listOf(
                Workout(
                    id = "w1",
                    userId = userId,
                    title = "Push Day",
                    description = "Peito, ombros e tríceps",
                    type = WorkoutType.FORCA,
                    exercises = listOf(
                        Exercise(id = "e1", name = "Supino Plano", description = "Exercício composto para peito", targetMuscle = "Peito"),
                        Exercise(id = "e2", name = "Press Militar", description = "Exercício composto para ombros", targetMuscle = "Ombros")
                    )
                ),
                Workout(
                    id = "w2",
                    userId = userId,
                    title = "Pull Day",
                    description = "Costas e bíceps",
                    type = WorkoutType.FORCA,
                    exercises = listOf(
                        Exercise(id = "e3", name = "Remada Curvada", description = "Exercício composto para costas", targetMuscle = "Costas")
                    )
                ),
                Workout(
                    id = "w3",
                    userId = userId,
                    title = "Core & Cardio",
                    description = "Abdominais e resistência",
                    type = WorkoutType.CARDIO,
                    exercises = listOf(
                        Exercise(id = "e4", name = "Prancha", description = "Isométrico para core", targetMuscle = "Core")
                    )
                )
            )
        }
    }

    fun getWorkoutById(id: String): Workout? = _allWorkouts.value.find { it.id == id }

    fun addWorkout(title: String, description: String, type: WorkoutType) {
        _allWorkouts.value += Workout(
            id = UUID.randomUUID().toString(),
            userId = currentUserId,
            title = title,
            description = description,
            type = type
        )
    }

    fun editWorkout(id: String, title: String, description: String, type: WorkoutType) {
        _allWorkouts.value = _allWorkouts.value.map { workout ->
            if (workout.id == id) workout.copy(title = title, description = description, type = type)
            else workout
        }
    }

    fun deleteWorkout(id: String) {
        _allWorkouts.value = _allWorkouts.value.filter { it.id != id }
    }

    fun logout() {
        sessionPreferences.clear()
        UserSession.clear()
    }
}



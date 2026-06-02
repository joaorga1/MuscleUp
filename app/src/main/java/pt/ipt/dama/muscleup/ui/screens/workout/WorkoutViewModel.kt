package pt.ipt.dama.muscleup.ui.screens.workout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.ipt.dama.muscleup.data.local.AppDatabase
import pt.ipt.dama.muscleup.data.local.ExerciseEntity
import pt.ipt.dama.muscleup.data.local.toModel
import pt.ipt.dama.muscleup.data.session.UserSession
import pt.ipt.dama.muscleup.model.Workout
import java.util.UUID

class WorkoutViewModel(
    application: Application,
    val workoutId: String
) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val workoutDao = db.workoutDao()
    private val exerciseDao = db.exerciseDao()

    val userName: String get() = UserSession.currentUserName

    // Combina o treino com os seus exercícios de forma reativa
    val workout: StateFlow<Workout?> = combine(
        workoutDao.getWorkoutById(workoutId),
        exerciseDao.getExercisesForWorkout(workoutId)
    ) { workoutEntity, exercises ->
        workoutEntity?.toModel(exercises.map { it.toModel() })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun addExercise(name: String, description: String, targetMuscle: String) {
        viewModelScope.launch {
            exerciseDao.insert(
                ExerciseEntity(
                    id = UUID.randomUUID().toString(),
                    workoutId = workoutId,
                    name = name,
                    description = description,
                    targetMuscle = targetMuscle
                )
            )
        }
    }

    fun editExercise(exerciseId: String, name: String, description: String, targetMuscle: String) {
        viewModelScope.launch {
            exerciseDao.update(
                ExerciseEntity(
                    id = exerciseId,
                    workoutId = workoutId,
                    name = name,
                    description = description,
                    targetMuscle = targetMuscle
                )
            )
        }
    }

    fun deleteExercise(exerciseId: String) {
        viewModelScope.launch {
            exerciseDao.deleteById(exerciseId)
        }
    }


    companion object {
        fun factory(workoutId: String): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return WorkoutViewModel(app, workoutId) as T
            }
        }
    }
}

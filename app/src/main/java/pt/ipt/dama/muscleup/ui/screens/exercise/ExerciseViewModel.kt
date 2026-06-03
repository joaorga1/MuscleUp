package pt.ipt.dama.muscleup.ui.screens.exercise

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
import pt.ipt.dama.muscleup.data.local.ExerciseSetEntity
import pt.ipt.dama.muscleup.data.local.toModel
import pt.ipt.dama.muscleup.data.session.UserSession
import pt.ipt.dama.muscleup.model.Exercise
import java.util.UUID

class ExerciseViewModel(
    application: Application,
    private val exerciseId: String
) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val exerciseDao = db.exerciseDao()
    private val exerciseSetDao = db.exerciseSetDao()

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





package pt.ipt.dama.muscleup.ui.screens.workout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import pt.ipt.dama.muscleup.data.MockWorkoutRepository
import pt.ipt.dama.muscleup.model.Workout

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MockWorkoutRepository(application.applicationContext)

    fun getWorkout(id: String): Workout? = repository.getWorkoutById(id)
}


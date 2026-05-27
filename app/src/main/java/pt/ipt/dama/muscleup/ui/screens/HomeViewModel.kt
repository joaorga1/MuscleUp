package pt.ipt.dama.muscleup.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pt.ipt.dama.muscleup.MuscleUpApp
import pt.ipt.dama.muscleup.data.MockWorkoutRepository
import pt.ipt.dama.muscleup.data.session.UserSession
import pt.ipt.dama.muscleup.model.Workout

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MockWorkoutRepository(application.applicationContext)
    private val sessionPreferences = (application as MuscleUpApp).sessionPreferences

    private val _workouts = MutableStateFlow<List<Workout>>(emptyList())
    val workouts: StateFlow<List<Workout>> = _workouts

    val userName: String get() = UserSession.currentUserName

    init {
        _workouts.value = repository.getWorkouts()
    }

    fun logout() {
        sessionPreferences.clear()
        UserSession.clear()
    }
}


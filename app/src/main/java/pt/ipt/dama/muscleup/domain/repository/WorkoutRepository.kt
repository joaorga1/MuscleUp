package pt.ipt.dama.muscleup.domain.repository

import pt.ipt.dama.muscleup.domain.model.Workout

interface WorkoutRepository {
    fun getWorkouts(): List<Workout>
    fun getWorkoutById(id: String): Workout?
}


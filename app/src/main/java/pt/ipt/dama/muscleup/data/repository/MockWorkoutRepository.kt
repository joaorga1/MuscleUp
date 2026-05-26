package pt.ipt.dama.muscleup.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import pt.ipt.dama.muscleup.domain.model.Workout
import pt.ipt.dama.muscleup.domain.repository.WorkoutRepository

class MockWorkoutRepository(private val context: Context) : WorkoutRepository {

    private val allWorkouts: List<Workout> by lazy {
        val json = context.assets.open("workouts.json").bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<Workout>>() {}.type
        Gson().fromJson(json, type)
    }

    override fun getWorkouts(): List<Workout> = allWorkouts

    override fun getWorkoutById(id: String): Workout? = allWorkouts.find { it.id == id }
}


package pt.ipt.dama.muscleup.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import pt.ipt.dama.muscleup.model.Workout

class MockWorkoutRepository(private val context: Context) {

    private val allWorkouts: List<Workout> by lazy {
        val json = context.assets.open("workouts.json").bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<Workout>>() {}.type
        Gson().fromJson(json, type)
    }

    fun getWorkouts(): List<Workout> = allWorkouts

    fun getWorkoutById(id: String): Workout? = allWorkouts.find { it.id == id }
}


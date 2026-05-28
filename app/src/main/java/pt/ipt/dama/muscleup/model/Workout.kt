package pt.ipt.dama.muscleup.model

data class Workout(
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val type: WorkoutType,
    val exercises: List<Exercise> = emptyList()
)


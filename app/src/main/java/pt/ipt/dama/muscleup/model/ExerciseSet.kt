package pt.ipt.dama.muscleup.model

data class ExerciseSet(
    val id: String,
    val reps: Int = 0,
    val durationSeconds: Int = 0,
    val weightKg: Float = 0f
)



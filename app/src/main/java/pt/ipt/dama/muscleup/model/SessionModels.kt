package pt.ipt.dama.muscleup.model


data class SessionExerciseSet(
    val id: String,
    val sessionId: String,
    val reps: Int = 0,
    val durationSeconds: Int = 0,
    val weightKg: Float = 0f,
    val setOrder: Int = 0
)


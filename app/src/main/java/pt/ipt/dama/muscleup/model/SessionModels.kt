package pt.ipt.dama.muscleup.model


/** Modelo de domínio que representa uma série realizada numa sessão de treino de um exercício. */
data class SessionExerciseSet(

    val id: String,
    val sessionId: String,
    val reps: Int = 0,
    val durationSeconds: Int = 0,
    val weightKg: Float = 0f,
    val setOrder: Int = 0
)


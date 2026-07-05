package pt.ipt.dama.muscleup.model

/** Modelo de domínio que representa uma série pré-definida (meta) de um exercício. */
data class ExerciseSet(

    val id: String,
    val reps: Int = 0,
    val durationSeconds: Int = 0,
    val weightKg: Float = 0f
)



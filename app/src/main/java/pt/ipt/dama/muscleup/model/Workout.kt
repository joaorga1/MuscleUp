package pt.ipt.dama.muscleup.model

/** Modelo de domínio que representa um treino, composto por vários exercícios. */
data class Workout(

    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val type: WorkoutType,
    val exercises: List<Exercise> = emptyList()
)


package pt.ipt.dama.muscleup.domain.model

data class Workout(
    val id: String,
    val title: String,
    val description: String,
    val type: String,
    val exercises: List<Exercise> = emptyList()
)


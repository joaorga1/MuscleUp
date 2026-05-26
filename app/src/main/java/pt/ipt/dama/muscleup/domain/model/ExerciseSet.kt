package pt.ipt.dama.muscleup.domain.model

data class ExerciseSet(
    val id: String,
    val targetType: TargetType,
    val reps: Int = 0,
    val durationSeconds: Int = 0,
    val weightKg: Float = 0f
)

enum class TargetType {
    WEIGHT, TIME
}

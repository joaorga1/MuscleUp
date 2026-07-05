package pt.ipt.dama.muscleup.data.remote.dto

// ---------------------------------------------------------------------------------
// Passo 8.1 — DTOs de Exercise Sets / Machine Configs (secções 9 e 10 do API_SPEC.md)
// ---------------------------------------------------------------------------------

data class ExerciseSetDto(
    val id: String,
    val exerciseId: String,
    val seriesOrder: Int,
    val reps: Int,
    val durationSeconds: Int,
    val weightKg: Double
)

data class ExerciseSetRequest(
    val reps: Int,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null,
    /** Ver comentário em [WorkoutRequest.clientId]. */
    val clientId: String? = null
)

data class MachineConfigDto(
    val id: String,
    val exerciseId: String,
    val name: String,
    val description: String,
    val angleDegrees: Double?
)

data class MachineConfigRequest(
    val name: String,
    val description: String,
    val angleDegrees: Double? = null,
    /** Ver comentário em [WorkoutRequest.clientId]. */
    val clientId: String? = null
)


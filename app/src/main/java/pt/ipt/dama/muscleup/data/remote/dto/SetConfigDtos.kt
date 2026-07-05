package pt.ipt.dama.muscleup.data.remote.dto

// Modelos de dados de transferência (DTOs) relativos a séries de exercício e a configurações de máquina.

/** Dados de uma série pré-definida de exercício devolvidos pela API. */
data class ExerciseSetDto(
    val id: String,
    val exerciseId: String,
    val seriesOrder: Int,
    val reps: Int,
    val durationSeconds: Int,
    val weightKg: Double
)

/** Dados enviados para criar uma nova série pré-definida de exercício. */
data class ExerciseSetRequest(
    val reps: Int,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null,
    /** Ver comentário em [WorkoutRequest.clientId]. */
    val clientId: String? = null
)

/** Dados de uma configuração de máquina devolvidos pela API. */
data class MachineConfigDto(
    val id: String,
    val exerciseId: String,
    val name: String,
    val description: String,
    val angleDegrees: Double?
)

/** Dados enviados para criar uma nova configuração de máquina. */
data class MachineConfigRequest(
    val name: String,
    val description: String,
    val angleDegrees: Double? = null,
    /** Ver comentário em [WorkoutRequest.clientId]. */
    val clientId: String? = null
)


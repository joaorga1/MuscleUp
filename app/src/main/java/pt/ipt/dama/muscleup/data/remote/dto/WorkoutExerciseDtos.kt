package pt.ipt.dama.muscleup.data.remote.dto

// Modelos de dados de transferência (DTOs) relativos a treinos e a exercícios.

/** Dados de um treino devolvidos pela API. */
data class WorkoutDto(
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val type: String, // Tipo de treino: FORCA, HIPERTROFIA, CARDIO, MOBILIDADE, HIIT ou FULL_BODY.
    val exerciseCount: Int? = null,
    val exercises: List<ExerciseSummaryDto>? = null
)

/** Dados enviados para criar ou atualizar um treino. */
data class WorkoutRequest(
    val title: String,
    val description: String,
    val type: String,
    val clientId: String? = null
)

/** Resumo de um exercício, usado dentro de um [WorkoutDto] com detalhe. */
data class ExerciseSummaryDto(
    val id: String,
    val name: String,
    val targetMuscle: String,
    val description: String
)

/** Dados completos de um exercício devolvidos pela API. */
data class ExerciseDto(
    val id: String,
    val workoutId: String,
    val name: String,
    val description: String,
    val targetMuscle: String,
    val sets: List<ExerciseSetDto>? = null,
    val machineConfigs: List<MachineConfigDto>? = null
)

/** Dados enviados para criar ou atualizar um exercício. */
data class ExerciseRequest(
    val name: String,
    val description: String,
    val targetMuscle: String,
    /** Ver comentário em [WorkoutRequest.clientId]. */
    val clientId: String? = null
)



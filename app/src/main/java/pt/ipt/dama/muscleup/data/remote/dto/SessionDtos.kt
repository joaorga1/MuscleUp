package pt.ipt.dama.muscleup.data.remote.dto

// Modelos de dados de transferência (DTOs) relativos a sessões de exercício e aos recordes pessoais.

/** Dados de uma sessão de exercício devolvidos pela API. */
data class ExerciseSessionDto(
    val id: String,
    val exerciseId: String,
    val userId: String,
    val status: String, // Estado da sessão: DRAFT ou FINISHED.
    val createdAt: String,
    val finishedAt: String?,
    val sets: List<SessionExerciseSetDto>? = null
)

/** Dados de uma série realizada numa sessão de exercício devolvidos pela API. */
data class SessionExerciseSetDto(
    val id: String,
    val sessionId: String,
    val setOrder: Int,
    val reps: Int,
    val durationSeconds: Int,
    val weightKg: Double
)

/** Dados enviados para registar uma nova série numa sessão em curso. */
data class SessionSetRequest(
    val reps: Int,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null
)

/** Resposta ao finalizar uma sessão de exercício. */
data class FinalizeSessionResponse(
    val message: String,
    val session: ExerciseSessionDto
)

/** Resposta ao registar uma nova série, contendo a sessão atualizada e a série criada. */
data class AddRecordedSetResponse(
    val session: ExerciseSessionDto,
    val set: SessionExerciseSetDto
)


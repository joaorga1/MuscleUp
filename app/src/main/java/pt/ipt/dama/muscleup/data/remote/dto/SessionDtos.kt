package pt.ipt.dama.muscleup.data.remote.dto

// ---------------------------------------------------------------------------------
// Passo 8.1 — DTOs de Exercise Sessions / Personal Record (secções 11 e 13 do API_SPEC.md)
// ---------------------------------------------------------------------------------

data class ExerciseSessionDto(
    val id: String,
    val exerciseId: String,
    val userId: String,
    val status: String, // DRAFT | FINISHED
    val createdAt: String,
    val finishedAt: String?,
    val sets: List<SessionExerciseSetDto>? = null
)

data class SessionExerciseSetDto(
    val id: String,
    val sessionId: String,
    val setOrder: Int,
    val reps: Int,
    val durationSeconds: Int,
    val weightKg: Double
)

data class SessionSetRequest(
    val reps: Int,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null
)

data class FinalizeSessionResponse(
    val message: String,
    val session: ExerciseSessionDto
)

/** Resposta de POST /api/exercises/{id}/sessions/draft/sets — devolve sessão + série criada */
data class AddRecordedSetResponse(
    val session: ExerciseSessionDto,
    val set: SessionExerciseSetDto
)




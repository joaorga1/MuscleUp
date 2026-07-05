package pt.ipt.dama.muscleup.data.remote.dto

// ---------------------------------------------------------------------------------
// Passo 8.1 — DTOs de Workout / Exercise (secções 7 e 8 do API_SPEC.md)
// ---------------------------------------------------------------------------------

data class WorkoutDto(
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val type: String, // FORCA | HIPERTROFIA | CARDIO | MOBILIDADE | HIIT | FULL_BODY
    // GET /api/workouts (lista) devolve `exerciseCount`; GET /api/workouts/:id (detalhe) devolve `exercises` populado.
    val exerciseCount: Int? = null,
    val exercises: List<ExerciseSummaryDto>? = null
)

data class WorkoutRequest(
    val title: String,
    val description: String,
    val type: String,
    // Passo 8.3 (proposta) — UUID gerado localmente (Room) no momento da criação, enviado à API
    // para servir de chave de idempotência (evita duplicados se o pedido for reenviado após
    // um "sucesso" que nunca chegou ao telemóvel) e, no futuro, para a API resolver o "pai"
    // de uma entidade filha sem depender do remoteId já ter sido sincronizado. Campo adicional
    // e opcional — não quebra a API atual, que o vai simplesmente ignorar até ser adotado.
    val clientId: String? = null
)

data class ExerciseSummaryDto(
    val id: String,
    val name: String,
    val targetMuscle: String,
    val description: String
)

data class ExerciseDto(
    val id: String,
    val workoutId: String,
    val name: String,
    val description: String,
    val targetMuscle: String,
    val sets: List<ExerciseSetDto>? = null,
    val machineConfigs: List<MachineConfigDto>? = null
)

data class ExerciseRequest(
    val name: String,
    val description: String,
    val targetMuscle: String,
    /** Ver comentário em [WorkoutRequest.clientId]. */
    val clientId: String? = null
)



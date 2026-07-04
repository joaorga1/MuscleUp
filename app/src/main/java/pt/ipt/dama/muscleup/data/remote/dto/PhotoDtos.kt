package pt.ipt.dama.muscleup.data.remote.dto

// ---------------------------------------------------------------------------------
// Passo 8.1 — DTO de Exercise Photos (secção 12 do API_SPEC.md)
// ---------------------------------------------------------------------------------

data class ExercisePhotoDto(
    val id: String,
    val exerciseId: String,
    val uri: String,
    val createdAt: String
)


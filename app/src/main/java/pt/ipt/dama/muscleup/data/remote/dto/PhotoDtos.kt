package pt.ipt.dama.muscleup.data.remote.dto

/** Dados de uma fotografia de exercício devolvidos pela API. */
data class ExercisePhotoDto(
    val id: String,
    val exerciseId: String,
    val uri: String,
    val createdAt: String
)


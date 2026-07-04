package pt.ipt.dama.muscleup.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import pt.ipt.dama.muscleup.model.ExercisePhoto

@Entity(
    tableName = "exercise_photos",
    foreignKeys = [ForeignKey(
        entity = ExerciseEntity::class,
        parentColumns = ["id"],
        childColumns = ["exerciseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("exerciseId")]
)
data class ExercisePhotoEntity(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val uri: String,
    val createdAt: Long,
    // Passo 8.3 — id do documento correspondente na API (Mongo _id). Null enquanto ainda
    // não foi sincronizado (criado só localmente / offline).
    val remoteId: String? = null
)

fun ExercisePhotoEntity.toModel() = ExercisePhoto(
    id = id,
    uri = uri,
    createdAt = createdAt
)



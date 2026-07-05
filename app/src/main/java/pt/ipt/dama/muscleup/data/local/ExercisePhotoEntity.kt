package pt.ipt.dama.muscleup.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import pt.ipt.dama.muscleup.model.ExercisePhoto

/** Entidade Room que representa uma fotografia associada a um exercício, guardada localmente. */
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
    val remoteId: String? = null
)

/** Converte a entidade da base de dados no modelo de domínio [ExercisePhoto]. */
fun ExercisePhotoEntity.toModel() = ExercisePhoto(
    id = id,
    uri = uri,
    createdAt = createdAt
)



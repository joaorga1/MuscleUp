package pt.ipt.dama.muscleup.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import pt.ipt.dama.muscleup.model.ExerciseSet

/** Entidade Room que representa uma série de um exercício, guardada localmente. */
@Entity(
    tableName = "exercise_sets",
    foreignKeys = [ForeignKey(
        entity = ExerciseEntity::class,
        parentColumns = ["id"],
        childColumns = ["exerciseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("exerciseId")]
)
data class ExerciseSetEntity(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val createdAt: Long,
    val seriesOrder: Int,
    val reps: Int,
    val durationSeconds: Int,
    val weightKg: Float,
    val remoteId: String? = null
)

/** Converte a entidade da base de dados no modelo de domínio [ExerciseSet]. */
fun ExerciseSetEntity.toModel() = ExerciseSet(
    id = id,
    reps = reps,
    durationSeconds = durationSeconds,
    weightKg = weightKg
)




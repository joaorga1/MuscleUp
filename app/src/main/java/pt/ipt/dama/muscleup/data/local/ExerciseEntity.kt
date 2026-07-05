package pt.ipt.dama.muscleup.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import pt.ipt.dama.muscleup.model.Exercise
import pt.ipt.dama.muscleup.model.ExerciseSet
import pt.ipt.dama.muscleup.model.MachineConfig

/** Entidade Room que representa um exercício de um treino, guardada localmente. */
@Entity(
    tableName = "exercises",
    foreignKeys = [ForeignKey(
        entity = WorkoutEntity::class,
        parentColumns = ["id"],
        childColumns = ["workoutId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("workoutId")]
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val name: String,
    val description: String,
    val targetMuscle: String,
    val remoteId: String? = null
)

/** Converte a entidade da base de dados no modelo de domínio [Exercise]. */
fun ExerciseEntity.toModel(
    sets: List<ExerciseSet> = emptyList(),
    machineConfigs: List<MachineConfig> = emptyList()
) = Exercise(
    id = id,
    name = name,
    description = description,
    targetMuscle = targetMuscle,
    sets = sets,
    machineConfigs = machineConfigs
)



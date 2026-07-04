package pt.ipt.dama.muscleup.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import pt.ipt.dama.muscleup.model.Exercise
import pt.ipt.dama.muscleup.model.ExerciseSet
import pt.ipt.dama.muscleup.model.MachineConfig

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
    // Passo 8.3 — id do documento correspondente na API (Mongo _id). Null enquanto ainda
    // não foi sincronizado (criado só localmente / offline).
    val remoteId: String? = null
)

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



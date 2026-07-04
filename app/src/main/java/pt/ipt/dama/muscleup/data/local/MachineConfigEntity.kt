package pt.ipt.dama.muscleup.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import pt.ipt.dama.muscleup.model.MachineConfig

@Entity(
    tableName = "machine_configs",
    foreignKeys = [ForeignKey(
        entity = ExerciseEntity::class,
        parentColumns = ["id"],
        childColumns = ["exerciseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("exerciseId")]
)
data class MachineConfigEntity(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val name: String,
    val description: String,
    val createdAt: Long,
    val angleDegrees: Float? = null,
    // Passo 8.3 — id do documento correspondente na API (Mongo _id). Null enquanto ainda
    // não foi sincronizado (criado só localmente / offline).
    val remoteId: String? = null
)

fun MachineConfigEntity.toModel() = MachineConfig(
    id = id,
    name = name,
    description = description,
    angleDegrees = angleDegrees
)

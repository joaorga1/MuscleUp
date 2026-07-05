package pt.ipt.dama.muscleup.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import pt.ipt.dama.muscleup.model.MachineConfig

/** Entidade Room que representa uma configuração de máquina de um exercício, guardada localmente. */
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
    val remoteId: String? = null
)

/** Converte a entidade da base de dados no modelo de domínio [MachineConfig]. */
fun MachineConfigEntity.toModel() = MachineConfig(
    id = id,
    name = name,
    description = description,
    angleDegrees = angleDegrees
)

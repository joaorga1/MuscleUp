package pt.ipt.dama.muscleup.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import pt.ipt.dama.muscleup.model.Workout
import pt.ipt.dama.muscleup.model.WorkoutType

/** Entidade Room que representa um treino, guardada localmente. */
@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val type: String,   // Nome do valor de WorkoutType.
    val remoteId: String? = null
)

/** Converte a entidade da base de dados no modelo de domínio [Workout]. */
fun WorkoutEntity.toModel(exercises: List<pt.ipt.dama.muscleup.model.Exercise> = emptyList()) = Workout(
    id = id,
    userId = userId,
    title = title,
    description = description,
    type = WorkoutType.valueOf(type),
    exercises = exercises
)



package pt.ipt.dama.muscleup.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import pt.ipt.dama.muscleup.model.Workout
import pt.ipt.dama.muscleup.model.WorkoutType

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val type: String,   // WorkoutType.name
    // Passo 8.3 — id do documento correspondente na API (Mongo _id). Null enquanto ainda
    // não foi sincronizado (criado só localmente / offline).
    val remoteId: String? = null
)

fun WorkoutEntity.toModel(exercises: List<pt.ipt.dama.muscleup.model.Exercise> = emptyList()) = Workout(
    id = id,
    userId = userId,
    title = title,
    description = description,
    type = WorkoutType.valueOf(type),
    exercises = exercises
)



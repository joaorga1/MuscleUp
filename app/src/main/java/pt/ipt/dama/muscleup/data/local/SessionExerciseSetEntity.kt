package pt.ipt.dama.muscleup.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import pt.ipt.dama.muscleup.model.SessionExerciseSet

@Entity(
    tableName = "session_exercise_sets",
    foreignKeys = [ForeignKey(
        entity = ExerciseSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class SessionExerciseSetEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val reps: Int,
    val durationSeconds: Int,
    val weightKg: Float,
    val setOrder: Int,
    val createdAt: Long
)

fun SessionExerciseSetEntity.toModel() = SessionExerciseSet(
    id = id,
    sessionId = sessionId,
    reps = reps,
    durationSeconds = durationSeconds,
    weightKg = weightKg,
    setOrder = setOrder
)


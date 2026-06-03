package pt.ipt.dama.muscleup.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_sessions",
    foreignKeys = [ForeignKey(
        entity = ExerciseEntity::class,
        parentColumns = ["id"],
        childColumns = ["exerciseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("exerciseId"), Index("userId")]
)
data class ExerciseSessionEntity(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val userId: String,
    val createdAt: Long,
    val finishedAt: Long? = null,
    val status: String = "DRAFT"
)



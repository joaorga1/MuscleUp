package pt.ipt.dama.muscleup.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ExerciseSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionSet(set: SessionExerciseSetEntity)

    @Query("SELECT * FROM exercise_sessions WHERE exerciseId = :exerciseId ORDER BY createdAt DESC")
    fun getSessionsForExercise(exerciseId: String): Flow<List<ExerciseSessionEntity>>

    @Query("SELECT * FROM session_exercise_sets WHERE sessionId = :sessionId ORDER BY setOrder ASC")
    fun getSetsForSession(sessionId: String): Flow<List<SessionExerciseSetEntity>>

    @Query("SELECT * FROM exercise_sessions WHERE exerciseId = :exerciseId AND userId = :userId AND status = 'DRAFT' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestDraftSession(exerciseId: String, userId: String): ExerciseSessionEntity?

    @Query("SELECT * FROM session_exercise_sets WHERE sessionId = :sessionId ORDER BY setOrder ASC")
    suspend fun getSetsForSessionOnce(sessionId: String): List<SessionExerciseSetEntity>

    @Query("DELETE FROM session_exercise_sets WHERE id = :setId")
    suspend fun deleteSessionSetById(setId: String)

    @Query("DELETE FROM exercise_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

    @Query("UPDATE exercise_sessions SET finishedAt = :finishedAt, status = 'FINISHED' WHERE id = :sessionId")
    suspend fun finalizeSession(sessionId: String, finishedAt: Long)

    @Query("SELECT COALESCE(MAX(setOrder), 0) + 1 FROM session_exercise_sets WHERE sessionId = :sessionId")
    suspend fun getNextSetOrder(sessionId: String): Int
}


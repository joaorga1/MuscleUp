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

    @Query("SELECT * FROM exercise_sessions WHERE exerciseId = :exerciseId AND userId = :userId AND status = 'FINISHED' ORDER BY createdAt DESC")
    fun getFinishedSessionsForExercise(exerciseId: String, userId: String): Flow<List<ExerciseSessionEntity>>

    @Query("SELECT * FROM session_exercise_sets WHERE sessionId = :sessionId ORDER BY setOrder ASC")
    fun getSetsForSession(sessionId: String): Flow<List<SessionExerciseSetEntity>>

    @Query(
        """
        SELECT ss.*
        FROM session_exercise_sets ss
        INNER JOIN exercise_sessions s ON s.id = ss.sessionId
        WHERE s.exerciseId = :exerciseId
          AND s.userId = :userId
          AND s.status = 'FINISHED'
        ORDER BY s.createdAt DESC, ss.setOrder ASC
        """
    )
    fun getFinishedSetsForExercise(exerciseId: String, userId: String): Flow<List<SessionExerciseSetEntity>>

    @Query("SELECT * FROM exercise_sessions WHERE exerciseId = :exerciseId AND userId = :userId AND status = 'DRAFT' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestDraftSession(exerciseId: String, userId: String): ExerciseSessionEntity?

    @Query("SELECT * FROM exercise_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionByIdOnce(id: String): ExerciseSessionEntity?

    @Query("SELECT * FROM exercise_sessions WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getSessionByRemoteId(remoteId: String): ExerciseSessionEntity?

    @Query("UPDATE exercise_sessions SET remoteId = :remoteId WHERE id = :id")
    suspend fun updateSessionRemoteId(id: String, remoteId: String)

    @Query("SELECT * FROM session_exercise_sets WHERE id = :id LIMIT 1")
    suspend fun getSetByIdOnce(id: String): SessionExerciseSetEntity?

    @Query("SELECT * FROM session_exercise_sets WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getSetByRemoteId(remoteId: String): SessionExerciseSetEntity?

    @Query("UPDATE session_exercise_sets SET remoteId = :remoteId WHERE id = :id")
    suspend fun updateSetRemoteId(id: String, remoteId: String)

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


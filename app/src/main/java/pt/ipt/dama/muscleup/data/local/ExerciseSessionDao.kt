package pt.ipt.dama.muscleup.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Acesso às tabelas de sessões de treino de um exercício e das séries realizadas em cada sessão. */
@Dao
interface ExerciseSessionDao {

    /** Insere uma nova sessão, substituindo uma existente com o mesmo identificador. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ExerciseSessionEntity)

    /** Insere uma nova série de uma sessão, substituindo uma existente com o mesmo identificador. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionSet(set: SessionExerciseSetEntity)

    /** Observa em tempo real todas as sessões de um exercício. */
    @Query("SELECT * FROM exercise_sessions WHERE exerciseId = :exerciseId ORDER BY createdAt DESC")
    fun getSessionsForExercise(exerciseId: String): Flow<List<ExerciseSessionEntity>>

    /** Observa em tempo real as sessões já terminadas de um exercício para um utilizador. */
    @Query("SELECT * FROM exercise_sessions WHERE exerciseId = :exerciseId AND userId = :userId AND status = 'FINISHED' ORDER BY createdAt DESC")
    fun getFinishedSessionsForExercise(exerciseId: String, userId: String): Flow<List<ExerciseSessionEntity>>

    /** Observa em tempo real as séries de uma sessão, ordenadas pela ordem em que foram feitas. */
    @Query("SELECT * FROM session_exercise_sets WHERE sessionId = :sessionId ORDER BY setOrder ASC")
    fun getSetsForSession(sessionId: String): Flow<List<SessionExerciseSetEntity>>

    /** Observa em tempo real todas as séries das sessões já terminadas de um exercício. */
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

    /** Obtém a sessão em curso (rascunho) mais recente de um exercício para um utilizador, caso exista. */
    @Query("SELECT * FROM exercise_sessions WHERE exerciseId = :exerciseId AND userId = :userId AND status = 'DRAFT' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestDraftSession(exerciseId: String, userId: String): ExerciseSessionEntity?

    /** Obtém uma sessão pelo identificador local, sem observação contínua. */
    @Query("SELECT * FROM exercise_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionByIdOnce(id: String): ExerciseSessionEntity?

    /** Obtém uma sessão pelo identificador atribuído pela API remota. */
    @Query("SELECT * FROM exercise_sessions WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getSessionByRemoteId(remoteId: String): ExerciseSessionEntity?

    /** Devolve todos os remoteIds de sessões não nulos do exercício, usado no pull para verificar duplicados com uma só consulta. */
    @Query("SELECT remoteId FROM exercise_sessions WHERE exerciseId = :exerciseId AND remoteId IS NOT NULL")
    suspend fun getAllSessionRemoteIdsForExercise(exerciseId: String): List<String>

    /** Atualiza o identificador remoto de uma sessão depois de sincronizada com a API. */
    @Query("UPDATE exercise_sessions SET remoteId = :remoteId WHERE id = :id")
    suspend fun updateSessionRemoteId(id: String, remoteId: String)

    /** Obtém uma série de sessão pelo identificador local, sem observação contínua. */
    @Query("SELECT * FROM session_exercise_sets WHERE id = :id LIMIT 1")
    suspend fun getSetByIdOnce(id: String): SessionExerciseSetEntity?

    /** Obtém uma série de sessão pelo identificador atribuído pela API remota. */
    @Query("SELECT * FROM session_exercise_sets WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getSetByRemoteId(remoteId: String): SessionExerciseSetEntity?

    /** Devolve todos os remoteIds de séries não nulos do exercício, usado no pull para verificar duplicados com uma só consulta. */
    @Query("""
        SELECT ss.remoteId FROM session_exercise_sets ss
        INNER JOIN exercise_sessions s ON s.id = ss.sessionId
        WHERE s.exerciseId = :exerciseId AND ss.remoteId IS NOT NULL
    """)
    suspend fun getAllSetRemoteIdsForExercise(exerciseId: String): List<String>

    /** Atualiza o identificador remoto de uma série de sessão depois de sincronizada com a API. */
    @Query("UPDATE session_exercise_sets SET remoteId = :remoteId WHERE id = :id")
    suspend fun updateSetRemoteId(id: String, remoteId: String)

    /** Obtém as séries de uma sessão numa única leitura, sem observação contínua. */
    @Query("SELECT * FROM session_exercise_sets WHERE sessionId = :sessionId ORDER BY setOrder ASC")
    suspend fun getSetsForSessionOnce(sessionId: String): List<SessionExerciseSetEntity>

    /** Remove uma série de sessão pelo identificador local. */
    @Query("DELETE FROM session_exercise_sets WHERE id = :setId")
    suspend fun deleteSessionSetById(setId: String)

    /** Remove uma sessão pelo identificador local. */
    @Query("DELETE FROM exercise_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

    /** Marca uma sessão como terminada, guardando o instante de finalização. */
    @Query("UPDATE exercise_sessions SET finishedAt = :finishedAt, status = 'FINISHED' WHERE id = :sessionId")
    suspend fun finalizeSession(sessionId: String, finishedAt: Long)

    /** Calcula o número de ordem seguinte para uma nova série de uma sessão. */
    @Query("SELECT COALESCE(MAX(setOrder), 0) + 1 FROM session_exercise_sets WHERE sessionId = :sessionId")
    suspend fun getNextSetOrder(sessionId: String): Int
}

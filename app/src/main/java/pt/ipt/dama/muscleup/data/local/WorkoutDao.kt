package pt.ipt.dama.muscleup.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Acesso à tabela de treinos (workouts). */
@Dao
interface WorkoutDao {

    /** Observa em tempo real todos os treinos de um utilizador. */
    @Query("SELECT * FROM workouts WHERE userId = :userId")
    fun getWorkoutsForUser(userId: String): Flow<List<WorkoutEntity>>

    /** Observa em tempo real um treino pelo seu identificador local. */
    @Query("SELECT * FROM workouts WHERE id = :id")
    fun getWorkoutById(id: String): Flow<WorkoutEntity?>

    /** Obtém um treino pelo identificador local, sem observação contínua. */
    @Query("SELECT * FROM workouts WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: String): WorkoutEntity?

    /** Obtém um treino pelo identificador atribuído pela API remota. */
    @Query("SELECT * FROM workouts WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): WorkoutEntity?

    /** Devolve todos os remoteIds não nulos, usado no pull para verificar duplicados com uma só consulta. */
    @Query("SELECT remoteId FROM workouts WHERE remoteId IS NOT NULL")
    suspend fun getAllRemoteIds(): List<String>

    /** Atualiza o identificador remoto de um treino depois de sincronizado com a API. */
    @Query("UPDATE workouts SET remoteId = :remoteId WHERE id = :id")
    suspend fun updateRemoteId(id: String, remoteId: String)

    /** Conta o número de treinos de um utilizador. */
    @Query("SELECT COUNT(*) FROM workouts WHERE userId = :userId")
    suspend fun getCountForUser(userId: String): Int

    /**
     * Verifica se já existe um treino com o mesmo título, descrição e tipo para o mesmo utilizador.
     * O parâmetro [excludeId] deve ser usado ao editar, para não bloquear o próprio treino alterado.
     */
    @Query("""
        SELECT COUNT(*) > 0 FROM workouts
        WHERE userId = :userId
          AND (:excludeId IS NULL OR id != :excludeId)
          AND LOWER(TRIM(title)) = LOWER(TRIM(:title))
          AND LOWER(TRIM(description)) = LOWER(TRIM(:description))
          AND type = :type
    """)
    suspend fun isDuplicate(userId: String, excludeId: String?, title: String, description: String, type: String): Boolean

    /** Insere um novo treino, substituindo um existente com o mesmo identificador. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workout: WorkoutEntity)

    /** Atualiza os dados de um treino já existente. */
    @Update
    suspend fun update(workout: WorkoutEntity)

    /** Remove um treino pelo identificador local. */
    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteById(id: String)
}

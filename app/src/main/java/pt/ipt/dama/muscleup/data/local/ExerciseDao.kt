package pt.ipt.dama.muscleup.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Acesso à tabela de exercícios (exercises) da base de dados local. */
@Dao
interface ExerciseDao {

    /** Observa em tempo real todos os exercícios de um treino. */
    @Query("SELECT * FROM exercises WHERE workoutId = :workoutId")
    fun getExercisesForWorkout(workoutId: String): Flow<List<ExerciseEntity>>

    /** Observa em tempo real um exercício pelo seu identificador local. */
    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    fun getExerciseById(exerciseId: String): Flow<ExerciseEntity?>

    /** Obtém um exercício pelo identificador local, sem observação contínua. */
    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: String): ExerciseEntity?

    /** Obtém um exercício pelo identificador atribuído pela API remota. */
    @Query("SELECT * FROM exercises WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): ExerciseEntity?

    /** Devolve todos os remoteIds não nulos do treino, usado no pull para verificar duplicados com uma só consulta. */
    @Query("SELECT remoteId FROM exercises WHERE workoutId = :workoutId AND remoteId IS NOT NULL")
    suspend fun getAllRemoteIdsForWorkout(workoutId: String): List<String>

    /** Atualiza o identificador remoto de um exercício depois de sincronizado com a API. */
    @Query("UPDATE exercises SET remoteId = :remoteId WHERE id = :id")
    suspend fun updateRemoteId(id: String, remoteId: String)

    /**
     * Verifica se já existe um exercício com o mesmo nome no mesmo treino.
     * O parâmetro [excludeId] deve ser usado ao editar, para não bloquear o próprio exercício alterado.
     */
    @Query("""
        SELECT COUNT(*) > 0 FROM exercises
        WHERE workoutId = :workoutId
          AND (:excludeId IS NULL OR id != :excludeId)
          AND LOWER(TRIM(name)) = LOWER(TRIM(:name))
    """)
    suspend fun isDuplicate(workoutId: String, excludeId: String?, name: String): Boolean

    /** Insere um novo exercício, substituindo um existente com o mesmo identificador. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: ExerciseEntity)

    /** Atualiza os dados de um exercício já existente. */
    @Update
    suspend fun update(exercise: ExerciseEntity)

    /** Remove um exercício pelo identificador local. */
    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteById(id: String)
}

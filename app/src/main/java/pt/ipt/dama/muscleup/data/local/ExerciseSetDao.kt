package pt.ipt.dama.muscleup.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Acesso à tabela de séries de exercício (exercise_sets). */
@Dao
interface ExerciseSetDao {

    /** Observa em tempo real todas as séries de um exercício, pela ordem em que foram criadas. */
    @Query("SELECT * FROM exercise_sets WHERE exerciseId = :exerciseId ORDER BY seriesOrder ASC, createdAt ASC, rowid ASC")
    fun getSetsForExercise(exerciseId: String): Flow<List<ExerciseSetEntity>>

    /** Calcula o número de ordem seguinte para uma nova série do exercício. */
    @Query("SELECT COALESCE(MAX(seriesOrder), 0) + 1 FROM exercise_sets WHERE exerciseId = :exerciseId")
    suspend fun getNextSeriesOrder(exerciseId: String): Int

    /** Obtém uma série pelo identificador local, sem observação contínua. */
    @Query("SELECT * FROM exercise_sets WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: String): ExerciseSetEntity?

    /** Obtém uma série pelo identificador atribuído pela API remota. */
    @Query("SELECT * FROM exercise_sets WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): ExerciseSetEntity?

    /** Devolve todos os remoteIds não nulos do exercício, usado no pull para verificar duplicados com uma só consulta. */
    @Query("SELECT remoteId FROM exercise_sets WHERE exerciseId = :exerciseId AND remoteId IS NOT NULL")
    suspend fun getAllRemoteIdsForExercise(exerciseId: String): List<String>

    /** Atualiza o identificador remoto de uma série depois de sincronizada com a API. */
    @Query("UPDATE exercise_sets SET remoteId = :remoteId WHERE id = :id")
    suspend fun updateRemoteId(id: String, remoteId: String)

    /** Insere uma nova série, substituindo uma existente com o mesmo identificador. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(set: ExerciseSetEntity)

    /** Remove uma série pelo identificador local. */
    @Query("DELETE FROM exercise_sets WHERE id = :setId")
    suspend fun deleteById(setId: String)
}

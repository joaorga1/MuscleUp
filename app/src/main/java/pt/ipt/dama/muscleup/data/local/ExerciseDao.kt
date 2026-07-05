package pt.ipt.dama.muscleup.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises WHERE workoutId = :workoutId")
    fun getExercisesForWorkout(workoutId: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    fun getExerciseById(exerciseId: String): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): ExerciseEntity?

    /** Devolve todos os remoteIds não nulos do workout — usado no pull para verificar duplicados com uma só query. */
    @Query("SELECT remoteId FROM exercises WHERE workoutId = :workoutId AND remoteId IS NOT NULL")
    suspend fun getAllRemoteIdsForWorkout(workoutId: String): List<String>

    @Query("UPDATE exercises SET remoteId = :remoteId WHERE id = :id")
    suspend fun updateRemoteId(id: String, remoteId: String)

    /** True se já existe um exercício com o mesmo nome no mesmo workout.
     *  Passa [excludeId] ao editar para não bloquear o próprio exercício que estamos a alterar. */
    @Query("""
        SELECT COUNT(*) > 0 FROM exercises
        WHERE workoutId = :workoutId
          AND (:excludeId IS NULL OR id != :excludeId)
          AND LOWER(TRIM(name)) = LOWER(TRIM(:name))
    """)
    suspend fun isDuplicate(workoutId: String, excludeId: String?, name: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: ExerciseEntity)

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteById(id: String)
}


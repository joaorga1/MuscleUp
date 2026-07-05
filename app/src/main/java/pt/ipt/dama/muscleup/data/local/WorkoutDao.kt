package pt.ipt.dama.muscleup.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Query("SELECT * FROM workouts WHERE userId = :userId")
    fun getWorkoutsForUser(userId: String): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    fun getWorkoutById(id: String): Flow<WorkoutEntity?>

    @Query("SELECT * FROM workouts WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: String): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): WorkoutEntity?

    /** Devolve todos os remoteIds não nulos — usado no pull para verificar duplicados com uma só query. */
    @Query("SELECT remoteId FROM workouts WHERE remoteId IS NOT NULL")
    suspend fun getAllRemoteIds(): List<String>

    @Query("UPDATE workouts SET remoteId = :remoteId WHERE id = :id")
    suspend fun updateRemoteId(id: String, remoteId: String)

    @Query("SELECT COUNT(*) FROM workouts WHERE userId = :userId")
    suspend fun getCountForUser(userId: String): Int

    /** True se já existe um treino com o mesmo (title + description + type) para o mesmo user.
     *  Passa [excludeId] ao editar para não bloquear o próprio workout que estamos a alterar. */
    @Query("""
        SELECT COUNT(*) > 0 FROM workouts
        WHERE userId = :userId
          AND (:excludeId IS NULL OR id != :excludeId)
          AND LOWER(TRIM(title)) = LOWER(TRIM(:title))
          AND LOWER(TRIM(description)) = LOWER(TRIM(:description))
          AND type = :type
    """)
    suspend fun isDuplicate(userId: String, excludeId: String?, title: String, description: String, type: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workout: WorkoutEntity)

    @Update
    suspend fun update(workout: WorkoutEntity)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteById(id: String)
}


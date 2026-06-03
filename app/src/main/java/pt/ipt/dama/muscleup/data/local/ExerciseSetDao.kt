package pt.ipt.dama.muscleup.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseSetDao {

    @Query("SELECT * FROM exercise_sets WHERE exerciseId = :exerciseId ORDER BY seriesOrder ASC, createdAt ASC, rowid ASC")
    fun getSetsForExercise(exerciseId: String): Flow<List<ExerciseSetEntity>>

    @Query("SELECT COALESCE(MAX(seriesOrder), 0) + 1 FROM exercise_sets WHERE exerciseId = :exerciseId")
    suspend fun getNextSeriesOrder(exerciseId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(set: ExerciseSetEntity)

    @Query("DELETE FROM exercise_sets WHERE id = :setId")
    suspend fun deleteById(setId: String)
}



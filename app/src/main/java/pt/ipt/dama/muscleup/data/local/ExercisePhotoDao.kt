package pt.ipt.dama.muscleup.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExercisePhotoDao {

    @Query("SELECT * FROM exercise_photos WHERE exerciseId = :exerciseId ORDER BY createdAt DESC")
    fun getPhotosForExercise(exerciseId: String): Flow<List<ExercisePhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: ExercisePhotoEntity)

    @Query("SELECT uri FROM exercise_photos WHERE id = :id")
    suspend fun getUriById(id: String): String?

    @Query("DELETE FROM exercise_photos WHERE id = :id")
    suspend fun deleteById(id: String)
}


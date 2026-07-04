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

    @Query("SELECT * FROM exercise_photos WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: String): ExercisePhotoEntity?

    @Query("SELECT * FROM exercise_photos WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): ExercisePhotoEntity?

    @Query("UPDATE exercise_photos SET remoteId = :remoteId WHERE id = :id")
    suspend fun updateRemoteId(id: String, remoteId: String)

    @Query("DELETE FROM exercise_photos WHERE id = :id")
    suspend fun deleteById(id: String)
}



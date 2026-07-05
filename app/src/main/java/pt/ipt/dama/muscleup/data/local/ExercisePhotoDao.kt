package pt.ipt.dama.muscleup.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Acesso à tabela de fotografias associadas a exercícios (exercise_photos). */
@Dao
interface ExercisePhotoDao {

    /** Observa em tempo real todas as fotografias de um exercício, da mais recente para a mais antiga. */
    @Query("SELECT * FROM exercise_photos WHERE exerciseId = :exerciseId ORDER BY createdAt DESC")
    fun getPhotosForExercise(exerciseId: String): Flow<List<ExercisePhotoEntity>>

    /** Insere uma nova fotografia, substituindo uma existente com o mesmo identificador. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: ExercisePhotoEntity)

    /** Obtém o URI de uma fotografia pelo seu identificador local. */
    @Query("SELECT uri FROM exercise_photos WHERE id = :id")
    suspend fun getUriById(id: String): String?

    /** Obtém uma fotografia pelo identificador local, sem observação contínua. */
    @Query("SELECT * FROM exercise_photos WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: String): ExercisePhotoEntity?

    /** Obtém uma fotografia pelo identificador atribuído pela API remota. */
    @Query("SELECT * FROM exercise_photos WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): ExercisePhotoEntity?

    /** Devolve todos os remoteIds não nulos do exercício, usado no pull para verificar duplicados com uma só consulta. */
    @Query("SELECT remoteId FROM exercise_photos WHERE exerciseId = :exerciseId AND remoteId IS NOT NULL")
    suspend fun getAllRemoteIdsForExercise(exerciseId: String): List<String>

    /** Atualiza o identificador remoto de uma fotografia depois de sincronizada com a API. */
    @Query("UPDATE exercise_photos SET remoteId = :remoteId WHERE id = :id")
    suspend fun updateRemoteId(id: String, remoteId: String)

    /** Remove uma fotografia pelo identificador local. */
    @Query("DELETE FROM exercise_photos WHERE id = :id")
    suspend fun deleteById(id: String)
}

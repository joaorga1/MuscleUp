package pt.ipt.dama.muscleup.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PendingSyncDao {

    @Insert
    suspend fun insert(entry: PendingSyncEntity): Long

    @Query("SELECT * FROM pending_sync ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingSyncEntity>

    @Query("SELECT * FROM pending_sync WHERE localId = :localId AND entityType = :entityType")
    suspend fun getAllFor(localId: String, entityType: String): List<PendingSyncEntity>

    @Query("DELETE FROM pending_sync WHERE localId = :localId AND entityType = :entityType")
    suspend fun deleteAllFor(localId: String, entityType: String)

    @Query("DELETE FROM pending_sync WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE pending_sync SET attempts = attempts + 1 WHERE id = :id")
    suspend fun incrementAttempts(id: Long)
}


package pt.ipt.dama.muscleup.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Acesso à fila de operações pendentes de sincronização com a API (pending_sync). */
@Dao
interface PendingSyncDao {

    /** Adiciona uma nova operação à fila de sincronização. */
    @Insert
    suspend fun insert(entry: PendingSyncEntity): Long

    /** Devolve todas as operações pendentes, pela ordem em que foram criadas. */
    @Query("SELECT * FROM pending_sync ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingSyncEntity>

    /** Devolve todas as operações pendentes de uma entidade específica. */
    @Query("SELECT * FROM pending_sync WHERE localId = :localId AND entityType = :entityType")
    suspend fun getAllFor(localId: String, entityType: String): List<PendingSyncEntity>

    /** Remove todas as operações pendentes de uma entidade específica. */
    @Query("DELETE FROM pending_sync WHERE localId = :localId AND entityType = :entityType")
    suspend fun deleteAllFor(localId: String, entityType: String)

    /** Remove uma operação pendente pelo seu identificador. */
    @Query("DELETE FROM pending_sync WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Incrementa o número de tentativas de envio de uma operação pendente. */
    @Query("UPDATE pending_sync SET attempts = attempts + 1 WHERE id = :id")
    suspend fun incrementAttempts(id: Long)

    /** Observa em tempo real o número de operações ainda por sincronizar, usado no ecrã de Definições. */
    @Query("SELECT COUNT(*) FROM pending_sync")
    fun observeCount(): Flow<Int>
}

package pt.ipt.dama.muscleup.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Acesso à tabela de configurações de máquina (machine_configs). */
@Dao
interface MachineConfigDao {

    /** Observa em tempo real todas as configurações de máquina de um exercício. */
    @Query("SELECT * FROM machine_configs WHERE exerciseId = :exerciseId ORDER BY createdAt ASC")
    fun getConfigsForExercise(exerciseId: String): Flow<List<MachineConfigEntity>>

    /** Obtém uma configuração pelo identificador local, sem observação contínua. */
    @Query("SELECT * FROM machine_configs WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: String): MachineConfigEntity?

    /** Obtém uma configuração pelo identificador atribuído pela API remota. */
    @Query("SELECT * FROM machine_configs WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): MachineConfigEntity?

    /** Devolve todos os remoteIds não nulos do exercício, usado no pull para verificar duplicados com uma só consulta. */
    @Query("SELECT remoteId FROM machine_configs WHERE exerciseId = :exerciseId AND remoteId IS NOT NULL")
    suspend fun getAllRemoteIdsForExercise(exerciseId: String): List<String>

    /** Atualiza o identificador remoto de uma configuração depois de sincronizada com a API. */
    @Query("UPDATE machine_configs SET remoteId = :remoteId WHERE id = :id")
    suspend fun updateRemoteId(id: String, remoteId: String)

    /** Insere uma nova configuração, substituindo uma existente com o mesmo identificador. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: MachineConfigEntity)

    /** Remove uma configuração pelo identificador local. */
    @Query("DELETE FROM machine_configs WHERE id = :id")
    suspend fun deleteById(id: String)
}

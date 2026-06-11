package pt.ipt.dama.muscleup.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MachineConfigDao {

    @Query("SELECT * FROM machine_configs WHERE exerciseId = :exerciseId ORDER BY createdAt ASC")
    fun getConfigsForExercise(exerciseId: String): Flow<List<MachineConfigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: MachineConfigEntity)

    @Query("DELETE FROM machine_configs WHERE id = :id")
    suspend fun deleteById(id: String)
}


package pt.ipt.dama.muscleup.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fila de operações pendentes de sincronização com a API, seguindo o padrão de fila de saída.
 * Toda a escrita, seja criação, atualização ou remoção, é primeiro gravada no Room, de forma
 * instantânea e segura offline, e depois é registada aqui uma entrada para ser enviada
 * à API assim que houver rede disponível.
 */
@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,   // Tipo de entidade afetada, por exemplo "WORKOUT".
    val operation: String,    // Operação a realizar: "CREATE", "UPDATE" ou "DELETE".
    val localId: String,      // Identificador local (UUID) da entidade afetada.
    val payloadJson: String?, // Estado serializado em JSON necessário para reenviar à API.
    val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0
)


package pt.ipt.dama.muscleup.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Passo 8.3 — Fila de operações pendentes de sincronização com a API ("outbox pattern").
 * Toda a escrita (create/update/delete) grava primeiro no Room (instantâneo, offline-safe)
 * e enfileira aqui uma entrada para ser enviada à API assim que houver rede.
 */
@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,   // ex: "WORKOUT"
    val operation: String,    // "CREATE" | "UPDATE" | "DELETE"
    val localId: String,      // id local (UUID) da entidade afetada
    val payloadJson: String?, // estado serializado (JSON) necessário para reenviar à API
    val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0
)


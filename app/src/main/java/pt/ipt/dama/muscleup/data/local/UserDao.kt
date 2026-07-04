package pt.ipt.dama.muscleup.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    fun getUserByEmailFlow(email: String): Flow<UserEntity?>

    @Query("UPDATE users SET profilePhotoUri = :uri WHERE email = :email")
    suspend fun updateProfilePhotoUri(email: String, uri: String)

    @Query("UPDATE users SET name = :name WHERE email = :email")
    suspend fun updateName(email: String, name: String)
}

/**
 * Passo 8.2 — Mantém uma cópia local (cache offline) dos dados do utilizador
 * autenticado via API, para a UI continuar a funcionar sem rede
 * (ex: mostrar a foto de perfil já descarregada anteriormente).
 * Não é a fonte de verdade — a API é. `passwordHash` deixa de ser usado
 * para validação (isso passa a ser feito no servidor), fica vazio.
 */
suspend fun UserDao.upsertMirror(name: String, email: String, profilePhotoUri: String?) {
    val existing = findByEmail(email)
    if (existing == null) {
        insert(UserEntity(name = name, email = email, passwordHash = "", profilePhotoUri = profilePhotoUri))
    } else {
        updateName(email, name)
        updateProfilePhotoUri(email, profilePhotoUri ?: "")
    }
}


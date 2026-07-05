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
 * Insere ou atualiza a cópia local (mirror) dos dados do utilizador autenticado via API.
 *
 * - Se o utilizador ainda não existir localmente, cria um novo registo.
 * - Se já existir, atualiza o nome e a foto de perfil.
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


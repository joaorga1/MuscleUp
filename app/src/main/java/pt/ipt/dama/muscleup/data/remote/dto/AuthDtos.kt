package pt.ipt.dama.muscleup.data.remote.dto

// ---------------------------------------------------------------------------------
// Passo 8.1 — DTOs de Autenticação / Utilizador (secção 6 do API_SPEC.md)
// ---------------------------------------------------------------------------------

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)


data class RefreshResponse(
    val accessToken: String
)

data class AuthResponse(
    val user: UserDto,
    val accessToken: String,
    val refreshToken: String
)

data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val profilePhotoUri: String?,
    val role: String
)

data class UpdateNameRequest(
    val name: String
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
    val confirmPassword: String
)

/** Resposta de PUT /api/users/me/password (API real, ver API_TESTING.md) */
data class MessageResponse(
    val message: String
)

/** Resposta de POST/DELETE /api/users/me/photo — devolve só o campo alterado */
data class ProfilePhotoResponse(
    val profilePhotoUri: String?
)




package pt.ipt.dama.muscleup.data.remote.dto

// Modelos de dados de transferência (DTOs) relativos à autenticação e ao utilizador.

/** Dados enviados para registar um novo utilizador. */
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

/** Dados enviados para autenticar um utilizador existente. */
data class LoginRequest(
    val email: String,
    val password: String
)

/** Resposta com o novo token de acesso, obtida ao renovar a sessão. */
data class RefreshResponse(
    val accessToken: String
)

/** Resposta devolvida após registo ou autenticação, com os dados do utilizador e os tokens da sessão. */
data class AuthResponse(
    val user: UserDto,
    val accessToken: String,
    val refreshToken: String
)

/** Dados de um utilizador devolvidos pela API. */
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val profilePhotoUri: String?,
    val role: String
)

/** Dados enviados para atualizar o nome do utilizador. */
data class UpdateNameRequest(
    val name: String
)

/** Dados enviados para alterar a palavra-passe do utilizador. */
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
    val confirmPassword: String
)

/** Resposta simples com uma mensagem informativa, devolvida por vários endpoints da API. */
data class MessageResponse(
    val message: String
)

/** Resposta ao enviar ou remover a fotografia de perfil, contendo apenas o campo alterado. */
data class ProfilePhotoResponse(
    val profilePhotoUri: String?
)



package pt.ipt.dama.muscleup.data.remote

import okhttp3.MultipartBody
import pt.ipt.dama.muscleup.data.remote.dto.AddRecordedSetResponse
import pt.ipt.dama.muscleup.data.remote.dto.AuthResponse
import pt.ipt.dama.muscleup.data.remote.dto.ChangePasswordRequest
import pt.ipt.dama.muscleup.data.remote.dto.ExerciseDto
import pt.ipt.dama.muscleup.data.remote.dto.ExercisePhotoDto
import pt.ipt.dama.muscleup.data.remote.dto.ExerciseRequest
import pt.ipt.dama.muscleup.data.remote.dto.ExerciseSessionDto
import pt.ipt.dama.muscleup.data.remote.dto.ExerciseSetDto
import pt.ipt.dama.muscleup.data.remote.dto.ExerciseSetRequest
import pt.ipt.dama.muscleup.data.remote.dto.FinalizeSessionResponse
import pt.ipt.dama.muscleup.data.remote.dto.LoginRequest
import pt.ipt.dama.muscleup.data.remote.dto.MachineConfigDto
import pt.ipt.dama.muscleup.data.remote.dto.MachineConfigRequest
import pt.ipt.dama.muscleup.data.remote.dto.MessageResponse
import pt.ipt.dama.muscleup.data.remote.dto.ProfilePhotoResponse
import pt.ipt.dama.muscleup.data.remote.dto.RegisterRequest
import pt.ipt.dama.muscleup.data.remote.dto.SessionSetRequest
import pt.ipt.dama.muscleup.data.remote.dto.UpdateNameRequest
import pt.ipt.dama.muscleup.data.remote.dto.UserDto
import pt.ipt.dama.muscleup.data.remote.dto.WorkoutDto
import pt.ipt.dama.muscleup.data.remote.dto.WorkoutRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Interface Retrofit com todos os endpoints da API remota.
 * Todas as rotas, exceto as de autenticação, registo, atualização de token e verificação
 * de saúde, exigem o cabeçalho Authorization, anexado automaticamente pelo [AuthInterceptor].
 */
interface ApiService {

    // ---------------------------------------------------------------------
    // Estado do servidor
    // ---------------------------------------------------------------------

    /** Verifica se a API está acessível. Não requer autenticação. */
    @GET("api/health")
    suspend fun checkHealth(): Response<Unit>

    // ---------------------------------------------------------------------
    // Autenticação e utilizadores
    // ---------------------------------------------------------------------

    /** Regista um novo utilizador na API. */
    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    /** Autentica um utilizador existente na API. */
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    /** Termina a sessão do utilizador no servidor. */
    @POST("api/auth/logout")
    suspend fun logout(): Response<Unit>

    /** Obtém os dados do utilizador atualmente autenticado. */
    @GET("api/users/me")
    suspend fun getCurrentUser(): Response<UserDto>

    /** Atualiza o nome do utilizador atualmente autenticado. */
    @PATCH("api/users/me")
    suspend fun updateName(@Body body: UpdateNameRequest): Response<UserDto>

    /** Altera a palavra-passe do utilizador atualmente autenticado. */
    @PUT("api/users/me/password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Response<MessageResponse>

    /** Envia uma nova fotografia de perfil para o utilizador atualmente autenticado. */
    @Multipart
    @POST("api/users/me/photo")
    suspend fun uploadProfilePhoto(@Part photo: MultipartBody.Part): Response<ProfilePhotoResponse>

    /** Remove a fotografia de perfil do utilizador atualmente autenticado. */
    @DELETE("api/users/me/photo")
    suspend fun deleteProfilePhoto(): Response<ProfilePhotoResponse>

    // ---------------------------------------------------------------------
    // Treinos
    // ---------------------------------------------------------------------

    /** Obtém todos os treinos do utilizador autenticado. */
    @GET("api/workouts")
    suspend fun getWorkouts(): Response<List<WorkoutDto>>

    /** Cria um novo treino. */
    @POST("api/workouts")
    suspend fun createWorkout(@Body body: WorkoutRequest): Response<WorkoutDto>

    /** Atualiza um treino existente. */
    @PUT("api/workouts/{workoutId}")
    suspend fun updateWorkout(
        @Path("workoutId") workoutId: String,
        @Body body: WorkoutRequest
    ): Response<WorkoutDto>

    /** Remove um treino. */
    @DELETE("api/workouts/{workoutId}")
    suspend fun deleteWorkout(@Path("workoutId") workoutId: String): Response<Unit>

    // ---------------------------------------------------------------------
    // Exercícios
    // ---------------------------------------------------------------------

    /** Obtém todos os exercícios de um treino. */
    @GET("api/workouts/{workoutId}/exercises")
    suspend fun getExercises(@Path("workoutId") workoutId: String): Response<List<ExerciseDto>>

    /** Cria um novo exercício num treino. */
    @POST("api/workouts/{workoutId}/exercises")
    suspend fun createExercise(
        @Path("workoutId") workoutId: String,
        @Body body: ExerciseRequest
    ): Response<ExerciseDto>

    /** Atualiza um exercício existente. */
    @PUT("api/exercises/{exerciseId}")
    suspend fun updateExercise(
        @Path("exerciseId") exerciseId: String,
        @Body body: ExerciseRequest
    ): Response<ExerciseDto>

    /** Remove um exercício. */
    @DELETE("api/exercises/{exerciseId}")
    suspend fun deleteExercise(@Path("exerciseId") exerciseId: String): Response<Unit>

    // ---------------------------------------------------------------------
    // Séries pré-definidas de exercício (metas)
    // ---------------------------------------------------------------------

    /** Obtém as séries pré-definidas de um exercício. */
    @GET("api/exercises/{exerciseId}/sets")
    suspend fun getExerciseSets(@Path("exerciseId") exerciseId: String): Response<List<ExerciseSetDto>>

    /** Adiciona uma nova série pré-definida a um exercício. */
    @POST("api/exercises/{exerciseId}/sets")
    suspend fun addExerciseSet(
        @Path("exerciseId") exerciseId: String,
        @Body body: ExerciseSetRequest
    ): Response<ExerciseSetDto>

    /** Remove uma série pré-definida de exercício. */
    @DELETE("api/sets/{setId}")
    suspend fun deleteExerciseSet(@Path("setId") setId: String): Response<Unit>

    // ---------------------------------------------------------------------
    // Configurações de máquina
    // ---------------------------------------------------------------------

    /** Obtém as configurações de máquina de um exercício. */
    @GET("api/exercises/{exerciseId}/machine-configs")
    suspend fun getMachineConfigs(@Path("exerciseId") exerciseId: String): Response<List<MachineConfigDto>>

    /** Adiciona uma nova configuração de máquina a um exercício. */
    @POST("api/exercises/{exerciseId}/machine-configs")
    suspend fun addMachineConfig(
        @Path("exerciseId") exerciseId: String,
        @Body body: MachineConfigRequest
    ): Response<MachineConfigDto>

    /** Remove uma configuração de máquina. */
    @DELETE("api/machine-configs/{configId}")
    suspend fun deleteMachineConfig(@Path("configId") configId: String): Response<Unit>

    // ---------------------------------------------------------------------
    // Sessões de exercício, registo em tempo real
    // ---------------------------------------------------------------------

    /** Regista uma nova série realizada na sessão em curso (rascunho) de um exercício. */
    @POST("api/exercises/{exerciseId}/sessions/draft/sets")
    suspend fun addRecordedSet(
        @Path("exerciseId") exerciseId: String,
        @Body body: SessionSetRequest
    ): Response<AddRecordedSetResponse>

    /** Remove uma série registada numa sessão. */
    @DELETE("api/session-sets/{sessionSetId}")
    suspend fun deleteRecordedSet(@Path("sessionSetId") sessionSetId: String): Response<Unit>

    /** Finaliza a sessão em curso (rascunho) de um exercício. */
    @POST("api/exercises/{exerciseId}/sessions/draft/finalize")
    suspend fun finalizeSession(@Path("exerciseId") exerciseId: String): Response<FinalizeSessionResponse>

    /** Descarta a sessão em curso (rascunho) de um exercício, sem a finalizar. */
    @DELETE("api/exercises/{exerciseId}/sessions/draft")
    suspend fun discardDraftSession(@Path("exerciseId") exerciseId: String): Response<Unit>

    /** Obtém o histórico de sessões de um exercício, filtrado por estado. */
    @GET("api/exercises/{exerciseId}/sessions")
    suspend fun getSessionHistory(
        @Path("exerciseId") exerciseId: String,
        @Query("status") status: String = "FINISHED"
    ): Response<List<ExerciseSessionDto>>

    // ---------------------------------------------------------------------
    // Fotografias de exercício
    // ---------------------------------------------------------------------

    /** Obtém todas as fotografias de um exercício. */
    @GET("api/exercises/{exerciseId}/photos")
    suspend fun getExercisePhotos(@Path("exerciseId") exerciseId: String): Response<List<ExercisePhotoDto>>

    /** Envia uma nova fotografia para um exercício. */
    @Multipart
    @POST("api/exercises/{exerciseId}/photos")
    suspend fun uploadExercisePhoto(
        @Path("exerciseId") exerciseId: String,
        @Part photo: MultipartBody.Part
    ): Response<ExercisePhotoDto>

    /** Remove uma fotografia de exercício. */
    @DELETE("api/photos/{photoId}")
    suspend fun deleteExercisePhoto(@Path("photoId") photoId: String): Response<Unit>
}



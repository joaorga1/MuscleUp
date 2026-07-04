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
 * Interface Retrofit com todos os endpoints da API (ver API_TESTING.md / endpoints reais).
 * Todas as rotas (exceto auth/register/login/refresh/health) exigem o header Authorization,
 * anexado automaticamente pelo [AuthInterceptor].
 */
interface ApiService {

    // ---------------------------------------------------------------------
    // Health
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // Auth / Users
    // ---------------------------------------------------------------------

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>


    @POST("api/auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("api/users/me")
    suspend fun getCurrentUser(): Response<UserDto>

    @PATCH("api/users/me")
    suspend fun updateName(@Body body: UpdateNameRequest): Response<UserDto>

    @PUT("api/users/me/password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Response<MessageResponse>

    @Multipart
    @POST("api/users/me/photo")
    suspend fun uploadProfilePhoto(@Part photo: MultipartBody.Part): Response<ProfilePhotoResponse>

    @DELETE("api/users/me/photo")
    suspend fun deleteProfilePhoto(): Response<ProfilePhotoResponse>

    // ---------------------------------------------------------------------
    // 7. Workouts
    // ---------------------------------------------------------------------

    @GET("api/workouts")
    suspend fun getWorkouts(): Response<List<WorkoutDto>>


    @POST("api/workouts")
    suspend fun createWorkout(@Body body: WorkoutRequest): Response<WorkoutDto>

    @PUT("api/workouts/{workoutId}")
    suspend fun updateWorkout(
        @Path("workoutId") workoutId: String,
        @Body body: WorkoutRequest
    ): Response<WorkoutDto>

    @DELETE("api/workouts/{workoutId}")
    suspend fun deleteWorkout(@Path("workoutId") workoutId: String): Response<Unit>

    // ---------------------------------------------------------------------
    // 8. Exercises
    // ---------------------------------------------------------------------

    @GET("api/workouts/{workoutId}/exercises")
    suspend fun getExercises(@Path("workoutId") workoutId: String): Response<List<ExerciseDto>>

    @POST("api/workouts/{workoutId}/exercises")
    suspend fun createExercise(
        @Path("workoutId") workoutId: String,
        @Body body: ExerciseRequest
    ): Response<ExerciseDto>

    @PUT("api/exercises/{exerciseId}")
    suspend fun updateExercise(
        @Path("exerciseId") exerciseId: String,
        @Body body: ExerciseRequest
    ): Response<ExerciseDto>

    @DELETE("api/exercises/{exerciseId}")
    suspend fun deleteExercise(@Path("exerciseId") exerciseId: String): Response<Unit>

    // ---------------------------------------------------------------------
    // 9. Exercise Sets (pré-definidos / "Meta")
    // ---------------------------------------------------------------------

    @GET("api/exercises/{exerciseId}/sets")
    suspend fun getExerciseSets(@Path("exerciseId") exerciseId: String): Response<List<ExerciseSetDto>>

    @POST("api/exercises/{exerciseId}/sets")
    suspend fun addExerciseSet(
        @Path("exerciseId") exerciseId: String,
        @Body body: ExerciseSetRequest
    ): Response<ExerciseSetDto>

    @DELETE("api/sets/{setId}")
    suspend fun deleteExerciseSet(@Path("setId") setId: String): Response<Unit>

    // ---------------------------------------------------------------------
    // 10. Machine Configs
    // ---------------------------------------------------------------------

    @GET("api/exercises/{exerciseId}/machine-configs")
    suspend fun getMachineConfigs(@Path("exerciseId") exerciseId: String): Response<List<MachineConfigDto>>

    @POST("api/exercises/{exerciseId}/machine-configs")
    suspend fun addMachineConfig(
        @Path("exerciseId") exerciseId: String,
        @Body body: MachineConfigRequest
    ): Response<MachineConfigDto>

    @DELETE("api/machine-configs/{configId}")
    suspend fun deleteMachineConfig(@Path("configId") configId: String): Response<Unit>

    // ---------------------------------------------------------------------
    // 11. Exercise Sessions (registo em tempo real)
    // ---------------------------------------------------------------------


    @POST("api/exercises/{exerciseId}/sessions/draft/sets")
    suspend fun addRecordedSet(
        @Path("exerciseId") exerciseId: String,
        @Body body: SessionSetRequest
    ): Response<AddRecordedSetResponse>

    @DELETE("api/session-sets/{sessionSetId}")
    suspend fun deleteRecordedSet(@Path("sessionSetId") sessionSetId: String): Response<Unit>

    @POST("api/exercises/{exerciseId}/sessions/draft/finalize")
    suspend fun finalizeSession(@Path("exerciseId") exerciseId: String): Response<FinalizeSessionResponse>

    @DELETE("api/exercises/{exerciseId}/sessions/draft")
    suspend fun discardDraftSession(@Path("exerciseId") exerciseId: String): Response<Unit>

    @GET("api/exercises/{exerciseId}/sessions")
    suspend fun getSessionHistory(
        @Path("exerciseId") exerciseId: String,
        @Query("status") status: String = "FINISHED"
    ): Response<List<ExerciseSessionDto>>

    // ---------------------------------------------------------------------
    // 12. Exercise Photos
    // ---------------------------------------------------------------------

    @GET("api/exercises/{exerciseId}/photos")
    suspend fun getExercisePhotos(@Path("exerciseId") exerciseId: String): Response<List<ExercisePhotoDto>>

    @Multipart
    @POST("api/exercises/{exerciseId}/photos")
    suspend fun uploadExercisePhoto(
        @Path("exerciseId") exerciseId: String,
        @Part photo: MultipartBody.Part
    ): Response<ExercisePhotoDto>

    @DELETE("api/photos/{photoId}")
    suspend fun deleteExercisePhoto(@Path("photoId") photoId: String): Response<Unit>
}




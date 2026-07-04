package pt.ipt.dama.muscleup.data.remote

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import pt.ipt.dama.muscleup.BuildConfig
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Corpo de erro devolvido pela API: `{ "code": "...", "message": "..." }` */
data class ApiErrorBody(val code: String = "UNKNOWN_ERROR", val message: String = "")

/** Envelope de erro da API: `{ "error": { "code": "...", "message": "..." } }` */
data class ApiErrorEnvelope(val error: ApiErrorBody? = null)

private const val MAX_PHOTO_BYTES = 10L * 1024 * 1024 // 10 MB

/** Converte um Uri local (câmara/galeria) num MultipartBody.Part pronto a enviar para a API. */
fun uriToMultipart(context: Context, uri: Uri, partName: String = "photo"): MultipartBody.Part {
    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: throw IOException("Não foi possível ler o ficheiro selecionado")
    if (bytes.size > MAX_PHOTO_BYTES) {
        throw IOException("Ficheiro demasiado grande (${bytes.size / (1024 * 1024)} MB). Máximo: 10 MB.")
    }
    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
    val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(partName, "upload.$extension", requestBody)
}

/**
 * Passo 8.1 — Constrói o singleton do Retrofit/OkHttpClient apontado para [BASE_URL].
 * Inclui logging de pedidos (apenas útil durante desenvolvimento), o [AuthInterceptor]
 * para anexar o token JWT automaticamente, e o [TokenAuthenticator] para o renovar
 * sozinho quando expira (accessToken dura só 15min).
 */
object RetrofitClient {

    /**
     * URL base da API — definida em local.properties (chave API_BASE_URL) e injetada
     * via BuildConfig, para não ficar exposta/commitada no código-fonte.
     */
    private val BASE_URL = BuildConfig.API_BASE_URL

    @Volatile
    private var apiService: ApiService? = null

    private val gson = Gson()

    fun getApiService(tokenManager: TokenManager): ApiService {
        return apiService ?: synchronized(this) {
            apiService ?: buildRetrofit(tokenManager).create(ApiService::class.java).also {
                apiService = it
            }
        }
    }

    private fun buildRetrofit(tokenManager: TokenManager): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val tokenRefresher = TokenRefresher(BASE_URL)

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager, tokenRefresher))
            .addInterceptor(loggingInterceptor)
            .authenticator(TokenAuthenticator(tokenManager, tokenRefresher))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /** Faz parsing do corpo de erro padrão `{ "error": { "code", "message" } }` da API. */
    fun <T> parseError(response: Response<T>): ApiErrorBody {
        return try {
            val errorBody = response.errorBody()?.string()
            val envelope = gson.fromJson(errorBody, ApiErrorEnvelope::class.java)
            envelope?.error ?: ApiErrorBody(code = "UNKNOWN_ERROR", message = "Erro ${response.code()}")
        } catch (_: Exception) {
            ApiErrorBody(code = "UNKNOWN_ERROR", message = "Erro ${response.code()}")
        }
    }
}






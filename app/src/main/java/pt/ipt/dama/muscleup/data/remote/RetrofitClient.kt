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
import pt.ipt.dama.muscleup.R
import pt.ipt.dama.muscleup.data.session.LanguagePreferences
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Corpo de erro devolvido pela API: `{ "code": "...", "message": "..." }` */
data class ApiErrorBody(val code: String = "UNKNOWN_ERROR", val message: String = "")

/** Envelope de erro da API: `{ "error": { "code": "...", "message": "..." } }` */
data class ApiErrorEnvelope(val error: ApiErrorBody? = null)

/**
 * Erro lançado por [uriToMultipart] quando o ficheiro escolhido excede o limite de tamanho.
 * Distinto de um [IOException] "genérico" de rede para que os ViewModels não confundam
 * "ficheiro demasiado grande" com "sem internet" — a mensagem já vem localizada.
 */
class PhotoTooLargeException(message: String) : IOException(message)

private const val MAX_PHOTO_BYTES = 10L * 1024 * 1024 // 10 MB

/** Converte um Uri local (câmara/galeria) num MultipartBody.Part pronto a enviar para a API. */
fun uriToMultipart(context: Context, uri: Uri, partName: String = "photo"): MultipartBody.Part {
    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: throw IOException(context.getString(R.string.error_read_selected_file))
    if (bytes.size > MAX_PHOTO_BYTES) {
        val sizeMb = "%.1f".format(Locale.US, bytes.size / (1024.0 * 1024.0))
        throw PhotoTooLargeException(context.getString(R.string.exercise_error_photo_too_large, sizeMb))
    }
    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
    val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(partName, "upload.$extension", requestBody)
}

/**
 * Anexa o header `Accept-Language` (pt/en) consoante o idioma atual da app, para que a API
 * (Node.js — só devolve mensagens em português por omissão) responda também em inglês
 * quando é esse o idioma escolhido pelo utilizador. Sem este header, mensagens de erro
 * geradas pelo servidor (ex: "Credenciais inválidas") ficariam sempre em português.
 */
class AcceptLanguageInterceptor(private val context: Context) : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val language = LanguagePreferences(context).getEffectiveLanguage()
        val request = chain.request().newBuilder()
            .header("Accept-Language", language)
            .build()
        return chain.proceed(request)
    }
}

/**
 * Passo 8.1 — Constrói o singleton do Retrofit/OkHttpClient apontado para [BASE_URL].
 * Inclui logging de pedidos (apenas útil durante desenvolvimento), o [AuthInterceptor]
 * para anexar o token JWT automaticamente, e o [TokenAuthenticator] para o renovar
 * sozinho quando expira (accessToken dura só 15min).
 */
object RetrofitClient {

    private val BASE_URL = BuildConfig.API_BASE_URL

    @Volatile private var apiService: ApiService? = null
    @Volatile private var okHttpClient: OkHttpClient? = null

    private val gson = Gson()

    /**
     * Devolve o OkHttpClient singleton com os interceptors de autenticação já configurados.
     * Partilhado entre Retrofit e o ImageLoader do Coil — garante que pedidos de imagens
     * para a API (ex: foto de perfil) também enviam o header `Authorization: Bearer`.
     */
    fun getOkHttpClient(tokenManager: TokenManager, context: Context): OkHttpClient {
        return okHttpClient ?: synchronized(this) {
            okHttpClient ?: buildOkHttpClient(tokenManager, context.applicationContext).also {
                okHttpClient = it
            }
        }
    }

    fun getApiService(tokenManager: TokenManager, context: Context): ApiService {
        return apiService ?: synchronized(this) {
            apiService ?: buildRetrofit(
                getOkHttpClient(tokenManager, context.applicationContext)
            ).create(ApiService::class.java).also { apiService = it }
        }
    }

    private fun buildOkHttpClient(tokenManager: TokenManager, context: Context): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val tokenRefresher = TokenRefresher(BASE_URL, context)
        return OkHttpClient.Builder()
            .addInterceptor(AcceptLanguageInterceptor(context))
            .addInterceptor(AuthInterceptor(tokenManager, tokenRefresher))
            .addInterceptor(loggingInterceptor)
            .authenticator(TokenAuthenticator(tokenManager, tokenRefresher))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private fun buildRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /** Faz parsing do corpo de erro padrão `{ "error": { "code", "message" } }` da API. */
    fun <T> parseError(response: Response<T>, context: Context): ApiErrorBody {
        val fallbackMessage = context.getString(R.string.error_api_with_code, response.code())
        return try {
            val errorBody = response.errorBody()?.string()
            val envelope = gson.fromJson(errorBody, ApiErrorEnvelope::class.java)
            envelope?.error?.takeIf { it.message.isNotBlank() }
                ?: ApiErrorBody(code = "UNKNOWN_ERROR", message = fallbackMessage)
        } catch (_: Exception) {
            ApiErrorBody(code = "UNKNOWN_ERROR", message = fallbackMessage)
        }
    }
}






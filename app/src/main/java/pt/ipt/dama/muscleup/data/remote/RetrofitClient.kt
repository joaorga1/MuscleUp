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

/** Corpo de erro devolvido pela API, no formato `{ "code": "...", "message": "..." }`. */
data class ApiErrorBody(val code: String = "UNKNOWN_ERROR", val message: String = "")

/** Envelope de erro da API, no formato `{ "error": { "code": "...", "message": "..." } }`. */
data class ApiErrorEnvelope(val error: ApiErrorBody? = null)

/**
 * Erro lançado por [uriToMultipart] quando o ficheiro escolhido excede o limite de tamanho.
 */
class PhotoTooLargeException(message: String) : IOException(message)

private const val MAX_PHOTO_BYTES = 10L * 1024 * 1024 // 10 MB

/** Converte um Uri local, vindo da câmara ou da galeria, num MultipartBody.Part pronto a enviar para a API. */
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
 * Anexa o cabeçalho Accept-Language, português ou inglês, consoante o idioma atual da app
 */
class AcceptLanguageInterceptor(private val context: Context) : okhttp3.Interceptor {
    /** Adiciona o cabeçalho Accept-Language a cada pedido antes de o encaminhar. */
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val language = LanguagePreferences(context).getEffectiveLanguage()
        val request = chain.request().newBuilder()
            .header("Accept-Language", language)
            .build()
        return chain.proceed(request)
    }
}

/**
 * Constrói o singleton do Retrofit e do OkHttpClient apontado para o endereço base da API.
 * Inclui registo de pedidos, útil apenas durante o desenvolvimento, o [AuthInterceptor]
 * para anexar o token JWT automaticamente, e o [TokenAuthenticator] para o renovar
 * sozinho quando expira, uma vez que o accessToken dura apenas 15 minutos.
 */
object RetrofitClient {

    private val BASE_URL = BuildConfig.API_BASE_URL

    @Volatile private var apiService: ApiService? = null
    @Volatile private var okHttpClient: OkHttpClient? = null

    private val gson = Gson()

    /**
     * Devolve o OkHttpClient singleton com os interceptores de autenticação já configurados.
     * É partilhado entre o Retrofit e o ImageLoader do Coil, o que garante que os pedidos
     * de imagens para a API
     */
    fun getOkHttpClient(tokenManager: TokenManager, context: Context): OkHttpClient {
        return okHttpClient ?: synchronized(this) {
            okHttpClient ?: buildOkHttpClient(tokenManager, context.applicationContext).also {
                okHttpClient = it
            }
        }
    }

    /** Devolve o serviço de API singleton, criando o Retrofit na primeira utilização. */
    fun getApiService(tokenManager: TokenManager, context: Context): ApiService {
        return apiService ?: synchronized(this) {
            apiService ?: buildRetrofit(
                getOkHttpClient(tokenManager, context.applicationContext)
            ).create(ApiService::class.java).also { apiService = it }
        }
    }

    /** Monta o OkHttpClient com todos os interceptores e o autenticador de tokens. */
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

    /** Monta a instância do Retrofit apontada para o endereço base da API. */
    private fun buildRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /** Extrai o corpo de erro padrão da API, no formato `{ "error": { "code", "message" } }`. */
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




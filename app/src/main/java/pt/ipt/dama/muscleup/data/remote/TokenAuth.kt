package pt.ipt.dama.muscleup.data.remote

import android.content.Context
import androidx.core.content.edit
import android.util.Log
import com.auth0.android.jwt.JWT
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import pt.ipt.dama.muscleup.data.remote.dto.RefreshResponse
import pt.ipt.dama.muscleup.data.session.LanguagePreferences
import java.util.concurrent.TimeUnit

private const val TAG = "TokenAuth"

/** Emite um evento de logout forçado quando o token de atualização é rejeitado pelo servidor. */
object AuthStateManager {
    private val _forceLogout = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val forceLogout: SharedFlow<Unit> = _forceLogout.asSharedFlow()

    /** Emite o evento de logout forçado. */
    fun triggerForceLogout() {
        _forceLogout.tryEmit(Unit)
    }
}

// Chaves usadas para guardar os tokens em SharedPreferences.
private const val PREFS_NAME        = "muscleup_tokens"
private const val KEY_ACCESS_TOKEN  = "access_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"

/** Guarda e lê os tokens JWT em SharedPreferences. */
class TokenManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Guarda o par de tokens. */
    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit {
            putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
        }
    }

    /** Atualiza apenas o token de acesso. */
    fun updateAccessToken(accessToken: String) {
        prefs.edit { putString(KEY_ACCESS_TOKEN, accessToken) }
    }

    /** Devolve o token de acesso guardado. */
    fun getAccessToken(): String?  = prefs.getString(KEY_ACCESS_TOKEN,  null)

    /** Devolve o token de atualização guardado. */
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    /** Indica se existe uma sessão ativa. */
    fun hasSession(): Boolean = !getAccessToken().isNullOrBlank()

    /** Verifica se o token de acesso já expirou. */
    fun isAccessTokenExpired(leewaySeconds: Long = 10): Boolean {
        val token = getAccessToken() ?: return true
        return try { JWT(token).isExpired(leewaySeconds) } catch (_: Exception) { true }
    }

    /** Remove todos os tokens guardados. */
    fun clear() { prefs.edit { clear() } }
}

/** Renova o token de acesso via POST /api/auth/refresh, usando um cliente sem interceptores para evitar recursão. */
class TokenRefresher(private val baseUrl: String, private val context: Context) {
    private val gson          = Gson()
    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    /** Renova o token de acesso de forma segura entre threads. Força logout se o servidor rejeitar o token de atualização. */
    @Synchronized
    fun refresh(tokenManager: TokenManager): String? {
        if (!tokenManager.isAccessTokenExpired()) {
            return tokenManager.getAccessToken()
        }
        val refreshToken = tokenManager.getRefreshToken() ?: return null
        return doRefresh(refreshToken, tokenManager, isRetry = false)
    }

    /** Executa o pedido HTTP de renovação, repetindo uma vez em caso de falha transitória do servidor. */
    private fun doRefresh(refreshToken: String, tokenManager: TokenManager, isRetry: Boolean): String? {
        return try {
            val body = gson.toJson(mapOf("refreshToken" to refreshToken))
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(baseUrl.trimEnd('/') + "/api/auth/refresh")
                .header("Accept-Language", LanguagePreferences(context).getEffectiveLanguage())
                .post(body)
                .build()
            refreshClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = try { response.body.string() } catch (_: Exception) { "" }
                    Log.w(TAG, "Falha ao renovar o token (código=${response.code}, retry=$isRetry): $errorBody")
                    if (response.code == 401 || response.code == 403) {
                        if (!isRetry) {
                            Thread.sleep(1500)
                            return doRefresh(refreshToken, tokenManager, isRetry = true)
                        }
                        Log.w(TAG, "Renovação de token rejeitada após nova tentativa, a forçar logout.")
                        tokenManager.clear()
                        AuthStateManager.triggerForceLogout()
                    }
                    return null
                }
                val newToken = gson.fromJson(response.body.string(), RefreshResponse::class.java).accessToken
                tokenManager.updateAccessToken(newToken)
                Log.d(TAG, "Token de acesso renovado com sucesso.")
                newToken
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao renovar o token devido a um erro de rede: ${e.message}")
            null
        }
    }
}

/** Interceptor que verifica proativamente a expiração do token e o renova antes de cada pedido protegido. */
class AuthInterceptor(
    private val tokenManager: TokenManager,
    private val tokenRefresher: TokenRefresher
) : Interceptor {
    private val publicPaths = listOf("api/auth/register", "api/auth/login", "api/auth/refresh", "api/health")

    /** Anexa o token de acesso ao pedido, renovando-o primeiro se já tiver expirado. */
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath.removePrefix("/")
        if (publicPaths.any { path.endsWith(it) }) return chain.proceed(original)
        var token = tokenManager.getAccessToken()
        if (!token.isNullOrBlank() && tokenManager.isAccessTokenExpired()) {
            token = tokenRefresher.refresh(tokenManager) ?: token
        }
        val request = if (!token.isNullOrBlank())
            original.newBuilder().addHeader("Authorization", "Bearer $token").build()
        else original
        return chain.proceed(request)
    }
}

/** Authenticator reativo chamado pelo OkHttp após um erro 401; renova o token e repete o pedido. */
class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val tokenRefresher: TokenRefresher
) : Authenticator {
    /** Repete o pedido com um novo token após um erro 401. */
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null
        if (tokenManager.getRefreshToken() == null) return null
        val newToken = tokenRefresher.refresh(tokenManager) ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }

    /** Conta o número de respostas encadeadas para evitar ciclos infinitos. */
    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) { count++; prior = prior.priorResponse }
        return count
    }
}

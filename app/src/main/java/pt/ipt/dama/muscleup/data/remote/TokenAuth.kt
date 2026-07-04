package pt.ipt.dama.muscleup.data.remote

import android.content.Context
import androidx.core.content.edit
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
import java.util.concurrent.TimeUnit

// ── Auth State ────────────────────────────────────────────────────────────────────────────────────
/**
 * Singleton que emite um evento de "força logout" quando o refreshToken expirou
 * ou foi rejeitado pelo servidor — sem precisar de uma referência direta ao NavController.
 *
 * Qualquer camada da app pode chamar [triggerForceLogout].
 * A UI (AppNavigation) observa [forceLogout] e navega para o ecrã de login.
 */
object AuthStateManager {
    private val _forceLogout = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val forceLogout: SharedFlow<Unit> = _forceLogout.asSharedFlow()

    fun triggerForceLogout() {
        _forceLogout.tryEmit(Unit)
    }
}

// ── Constantes de SharedPreferences ──────────────────────────────────────────────────────────────
private const val PREFS_NAME        = "muscleup_tokens"
private const val KEY_ACCESS_TOKEN  = "access_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"
/**
 * Guarda / lê os tokens JWT (accessToken + refreshToken) em SharedPreferences.
 * O accessToken dura 15 min; o refreshToken dura 1 ano.
 */
class TokenManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit {
            putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
        }
    }
    fun updateAccessToken(accessToken: String) {
        prefs.edit { putString(KEY_ACCESS_TOKEN, accessToken) }
    }
    fun getAccessToken(): String?  = prefs.getString(KEY_ACCESS_TOKEN,  null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    fun hasSession(): Boolean = !getAccessToken().isNullOrBlank()
    /** Lê o claim `exp` do JWT para saber se o accessToken expirou sem precisar de um 401. */
    fun isAccessTokenExpired(leewaySeconds: Long = 10): Boolean {
        val token = getAccessToken() ?: return true
        return try { JWT(token).isExpired(leewaySeconds) } catch (_: Exception) { true }
    }
    fun clear() { prefs.edit { clear() } }
}
/**
 * Faz o pedido HTTP `POST /api/auth/refresh` com um OkHttpClient "nu" (sem interceptors)
 * para evitar recursão. Partilhado entre [AuthInterceptor] (renovação proativa) e
 * [TokenAuthenticator] (fallback reativo após 401).
 */
class TokenRefresher(private val baseUrl: String) {
    private val gson          = Gson()
    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Renova o accessToken de forma thread-safe:
     * - Se dois pedidos chegam ao mesmo tempo com o token expirado, apenas UM faz o
     *   refresh; o segundo reutiliza o token já renovado (double-check no início).
     * - Só limpa a sessão e força logout se o servidor rejeitar explicitamente o
     *   refreshToken (401 / 403). Erros de rede não apagam a sessão.
     */
    @Synchronized
    fun refresh(tokenManager: TokenManager): String? {
        // Double-check: outra thread pode já ter renovado enquanto esperávamos o lock.
        if (!tokenManager.isAccessTokenExpired()) {
            return tokenManager.getAccessToken()
        }
        // Sem refreshToken = sessão já foi limpa (logout manual ou 1.ª execução).
        // Não disparar forceLogout aqui — não há sessão ativa para expirar.
        val refreshToken = tokenManager.getRefreshToken() ?: return null
        return try {
            val body = gson.toJson(mapOf("refreshToken" to refreshToken))
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(baseUrl.trimEnd('/') + "/api/auth/refresh")
                .post(body)
                .build()
            refreshClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    // ÚNICO sítio onde se dispara forceLogout:
                    // o servidor recusou explicitamente o refreshToken (sessão verdadeiramente expirada).
                    if (response.code == 401 || response.code == 403) {
                        tokenManager.clear()
                        AuthStateManager.triggerForceLogout()
                    }
                    return null
                }
                val newToken = gson.fromJson(response.body.string(), RefreshResponse::class.java).accessToken
                tokenManager.updateAccessToken(newToken)
                newToken
            }
        } catch (_: Exception) {
            // Erro de rede (sem ligação, timeout, etc.) — não apaga a sessão.
            null
        }
    }
}
/**
 * Interceptor proativo: antes de cada pedido protegido, verifica se o accessToken já
 * expirou (via `exp` do JWT) e renova-o com [TokenRefresher] sem gastar um pedido a receber 401.
 */
class AuthInterceptor(
    private val tokenManager: TokenManager,
    private val tokenRefresher: TokenRefresher
) : Interceptor {
    private val publicPaths = listOf("api/auth/register", "api/auth/login", "api/auth/refresh", "api/health")
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
/**
 * Authenticator reativo: fallback chamado pelo OkHttp quando um pedido recebe 401.
 * Renova o accessToken com [TokenRefresher] e repete o pedido original automaticamente.
 * Não dispara forceLogout aqui — o [TokenRefresher] é o único responsável por isso.
 */
class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val tokenRefresher: TokenRefresher
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null           // evita loop infinito
        if (tokenManager.getRefreshToken() == null) return null // sessão já limpa, não fazer nada
        val newToken = tokenRefresher.refresh(tokenManager) ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }
    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) { count++; prior = prior.priorResponse }
        return count
    }
}

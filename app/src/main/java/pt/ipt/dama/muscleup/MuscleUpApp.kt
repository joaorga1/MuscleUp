package pt.ipt.dama.muscleup

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pt.ipt.dama.muscleup.data.local.AppDatabase
import pt.ipt.dama.muscleup.data.local.upsertMirror
import pt.ipt.dama.muscleup.data.remote.ApiService
import pt.ipt.dama.muscleup.data.remote.RetrofitClient
import pt.ipt.dama.muscleup.data.remote.TokenManager
import pt.ipt.dama.muscleup.data.session.LanguagePreferences
import pt.ipt.dama.muscleup.data.session.SessionPreferences
import pt.ipt.dama.muscleup.data.session.ThemePreferences
import pt.ipt.dama.muscleup.data.session.UserSession
import pt.ipt.dama.muscleup.data.sync.SyncManager
import pt.ipt.dama.muscleup.data.sync.SyncScheduler
import pt.ipt.dama.muscleup.ui.theme.ThemeState

class MuscleUpApp : Application(), ImageLoaderFactory {

    // Passo 10.1 (fix) — aplica o idioma escolhido pelo utilizador também ao contexto
    // da Application, para as mensagens de erro dos ViewModels (que usam
    // `getApplication<Application>().getString(...)`) respeitarem a escolha.
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(applyAppLocale(base))
    }

    /**
     * Configura o ImageLoader singleton do Coil para usar o mesmo OkHttpClient (com
     * AuthInterceptor) que o Retrofit — sem isto, pedidos de imagens à API (ex: fotos
     * de perfil servidas pelo servidor) não enviam o header Authorization e a API
     * recusa-os com 401, mostrando só o fundo de cor em vez da imagem.
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient { RetrofitClient.getOkHttpClient(tokenManager, this) }
            .build()
    }

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val sessionPreferences: SessionPreferences by lazy { SessionPreferences(this) }

    // Passo 10.1 — preferência de tema (Sistema/Claro/Escuro) e idioma (PT/EN)
    val themePreferences: ThemePreferences by lazy { ThemePreferences(this) }
    val languagePreferences: LanguagePreferences by lazy { LanguagePreferences(this) }

    // Passo 8.1 — Camada de rede (API remota)
    val tokenManager: TokenManager by lazy { TokenManager(this) }
    val apiService: ApiService by lazy { RetrofitClient.getApiService(tokenManager, this) }

    // Passo 8.3 — Sincronização offline-first (fila outbox)
    val syncManager: SyncManager by lazy {
        SyncManager(
            apiService,
            database.pendingSyncDao(),
            database.workoutDao(),
            database.exerciseDao(),
            database.exerciseSetDao(),
            database.machineConfigDao(),
            database.exercisePhotoDao(),
            database.exerciseSessionDao(),
            this
        )
    }

    /**
     * Agenda a sincronização da fila pendente via WorkManager: só corre com rede disponível,
     * com backoff exponencial, e sobrevive à app/processo ser morto (ver [SyncScheduler]).
     */
    fun triggerSync() {
        SyncScheduler.requestSync(this)
    }

    override fun onCreate() {
        super.onCreate()
        ThemeState.select(themePreferences.getSavedMode())
        restoreSessionIfValid()
    }

    private fun restoreSessionIfValid() {
        // Passo 8.2 — sessão local (1 ano) só é válida se também houver um token JWT guardado.
        if (!sessionPreferences.isValid() || !tokenManager.hasSession()) {
            sessionPreferences.clear()
            tokenManager.clear()
            UserSession.clear()
            return
        }
        val email = sessionPreferences.getSavedEmail() ?: return
        val name = sessionPreferences.getSavedName()
        // Usa os dados guardados nas preferências para resposta imediata na UI (offline-friendly)
        UserSession.set(name = name, email = email)
        // Confirma em background que o token ainda é válido na API e atualiza o cache local.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.getCurrentUser()
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    UserSession.set(name = user.name, email = user.email)
                    sessionPreferences.save(email = user.email, name = user.name)
                    database.userDao().upsertMirror(user.name, user.email, user.profilePhotoUri)
                }
                // Nota: não tratamos 401 aqui explicitamente.
                // Se o refreshToken for rejeitado pelo servidor, o TokenRefresher.refresh()
                // já chama tokenManager.clear() + AuthStateManager.triggerForceLogout(),
                // que navega para Login de forma limpa e coordenada.
            } catch (_: Exception) {
                // Sem rede: mantém a sessão local, não força logout (offline-first).
            }
        }
        // Passo 8.3 — esvazia a fila de sincronização pendente (ex: mudanças feitas offline) assim que há rede.
        triggerSync()
    }
}

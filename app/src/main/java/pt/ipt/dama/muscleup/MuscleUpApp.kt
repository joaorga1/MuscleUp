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

/**
 * Classe Application da aplicação MuscleUp.
 * Centraliza a criação das dependências principais e disponibiliza-as a toda a aplicação.
 */
class MuscleUpApp : Application(), ImageLoaderFactory {

    /** Aplica o idioma guardado ao contexto da Application antes de qualquer recurso ser resolvido. */
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(applyAppLocale(base))
    }

    /**
     * Configura o ImageLoader do Coil com o mesmo OkHttpClient do Retrofit,
     * para que os pedidos de imagens à API incluam o cabeçalho Authorization.
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient { RetrofitClient.getOkHttpClient(tokenManager, this) }
            .build()
    }

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val sessionPreferences: SessionPreferences by lazy { SessionPreferences(this) }
    val themePreferences: ThemePreferences by lazy { ThemePreferences(this) }
    val languagePreferences: LanguagePreferences by lazy { LanguagePreferences(this) }
    val tokenManager: TokenManager by lazy { TokenManager(this) }
    val apiService: ApiService by lazy { RetrofitClient.getApiService(tokenManager, this) }
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

    /** Agenda a sincronização da fila via WorkManager. */
    fun triggerSync() {
        SyncScheduler.requestSync(this)
    }

    /** Aplica o tema guardado e tenta restaurar a sessão do utilizador. */
    override fun onCreate() {
        super.onCreate()
        ThemeState.select(themePreferences.getSavedMode())
        restoreSessionIfValid()
    }

    /** Restaura a sessão local se válida e confirma o token em segundo plano. */
    private fun restoreSessionIfValid() {
        if (!sessionPreferences.isValid() || !tokenManager.hasSession()) {
            sessionPreferences.clear()
            tokenManager.clear()
            UserSession.clear()
            return
        }
        val email = sessionPreferences.getSavedEmail() ?: return
        val name = sessionPreferences.getSavedName()
        UserSession.set(name = name, email = email)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.getCurrentUser()
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    UserSession.set(name = user.name, email = user.email)
                    sessionPreferences.save(email = user.email, name = user.name)
                    database.userDao().upsertMirror(user.name, user.email, user.profilePhotoUri)
                }
            } catch (_: Exception) {
                // Sem rede: mantém a sessão local.
            }
        }
        triggerSync()
    }
}

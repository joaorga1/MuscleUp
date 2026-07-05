package pt.ipt.dama.muscleup.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.ipt.dama.muscleup.MuscleUpApp
import pt.ipt.dama.muscleup.R
import pt.ipt.dama.muscleup.data.sync.SyncScheduler
import pt.ipt.dama.muscleup.ui.theme.ThemeMode
import pt.ipt.dama.muscleup.ui.theme.ThemeState
import java.util.Locale

/**
 * Passo 10.1 — Ecrã de Definições: tema (Sistema/Claro/Escuro), idioma (PT/EN) e logout.
 *
 * O idioma é guardado em [pt.ipt.dama.muscleup.data.session.LanguagePreferences] (SharedPreferences
 * simples) e aplicado manualmente em `attachBaseContext()` (ver `LocaleUtils.kt`) — por isso o
 * ecrã que chama `setLanguage()` tem de forçar um `recreate()` da Activity para o efeito ser
 * imediato (ver SettingsScreen).
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MuscleUpApp
    private val themePreferences = app.themePreferences
    private val languagePreferences = app.languagePreferences
    private val pendingSyncDao = app.database.pendingSyncDao()

    val currentThemeMode: ThemeMode get() = ThemeState.mode

    /** Nº de operações (treinos/exercícios/fotos/séries...) ainda por sincronizar com a API. */
    val pendingSyncCount: StateFlow<Int> = pendingSyncDao.observeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    fun setThemeMode(mode: ThemeMode) {
        ThemeState.select(mode)
        themePreferences.save(mode)
    }

    /**
     * "pt" ou "en". Se o utilizador ainda não escolheu explicitamente (lista vazia),
     * reflete o idioma efetivamente em uso nesse momento (`Locale.getDefault()`, que já
     * segue o sistema) em vez de assumir sempre "pt" — evita o chip errado ficar marcado.
     */
    fun currentLanguageTag(): String {
        val saved = languagePreferences.getSavedLanguage()
        if (saved != null) return saved
        return if (Locale.getDefault().language == "en") "en" else "pt"
    }

    /** [languageTag] é "pt" ou "en". Português usa sempre a variante de Portugal (pt-PT). */
    fun setLanguage(languageTag: String) {
        languagePreferences.save(languageTag)
    }

    /**
     * Botão "Forçar sincronização": delega no [pt.ipt.dama.muscleup.data.sync.SyncManager]
     * (dono da lógica de negócio de sincronização) a tentativa imediata de esvaziar a fila.
     * Este ViewModel só trata da parte de apresentação: agenda um retry em background via
     * WorkManager se ainda sobrar alguma coisa, e escolhe a mensagem certa para o [uiEvent].
     */
    fun forceSync() {
        viewModelScope.launch {
            val remaining = app.syncManager.forcePushNow()
            if (remaining > 0) SyncScheduler.forceSyncNow(app)
            _uiEvent.emit(
                if (remaining == 0) app.getString(R.string.settings_sync_success)
                else app.getString(R.string.settings_sync_still_pending, remaining)
            )
        }
    }
}









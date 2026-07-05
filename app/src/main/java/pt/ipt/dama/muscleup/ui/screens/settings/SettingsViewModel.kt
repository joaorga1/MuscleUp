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
 * ViewModel do ecrã de Definições.
 *
 * Gere as preferências de tema (Sistema/Claro/Escuro) via [pt.ipt.dama.muscleup.ui.theme.ThemeState]
 * e de idioma (PT/EN) via [pt.ipt.dama.muscleup.data.session.LanguagePreferences].
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

    /** Guarda o idioma escolhido pelo utilizador. O parâmetro [languageTag] deve ser "pt" ou "en". */
    fun setLanguage(languageTag: String) {
        languagePreferences.save(languageTag)
    }

    /** Tenta esvaziar a fila imediatamente e agenda nova tentativa pelo WorkManager se necessário. */
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







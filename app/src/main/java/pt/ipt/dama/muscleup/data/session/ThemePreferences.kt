package pt.ipt.dama.muscleup.data.session

import android.content.Context
import androidx.core.content.edit
import pt.ipt.dama.muscleup.ui.theme.ThemeMode

private const val PREFS_NAME = "muscleup_settings"
private const val KEY_THEME_MODE = "theme_mode"

/** Persiste a escolha de tema (Sistema, Claro ou Escuro) em SharedPreferences. */
class ThemePreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Devolve o tema guardado, ou [ThemeMode.SYSTEM] por omissão. */
    fun getSavedMode(): ThemeMode = ThemeMode.fromName(prefs.getString(KEY_THEME_MODE, null))

    /** Guarda o tema escolhido. */
    fun save(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
    }
}


package pt.ipt.dama.muscleup.data.session

import android.content.Context
import androidx.core.content.edit
import pt.ipt.dama.muscleup.ui.theme.ThemeMode

private const val PREFS_NAME = "muscleup_settings"
private const val KEY_THEME_MODE = "theme_mode"

/** Passo 10.1 — persiste a escolha de tema (Sistema/Claro/Escuro) entre arranques da app. */
class ThemePreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSavedMode(): ThemeMode = ThemeMode.fromName(prefs.getString(KEY_THEME_MODE, null))

    fun save(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
    }
}


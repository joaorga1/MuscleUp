package pt.ipt.dama.muscleup.data.session

import android.content.Context
import androidx.core.content.edit
import java.util.Locale

private const val PREFS_NAME = "muscleup_settings"
private const val KEY_LANGUAGE = "language_tag"

/**
 * Persiste a escolha de idioma do utilizador ("pt" ou "en") em SharedPreferences.
 * Aplicada de forma síncrona em attachBaseContext para evitar condições de corrida.
 */
class LanguagePreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Devolve o idioma guardado, ou nulo se o utilizador ainda não escolheu. */
    fun getSavedLanguage(): String? = prefs.getString(KEY_LANGUAGE, null)

    /** Guarda o idioma escolhido ("pt" ou "en"). */
    fun save(languageTag: String) {
        prefs.edit { putString(KEY_LANGUAGE, languageTag) }
    }

    /** Devolve o idioma efetivo ("pt" ou "en"): o guardado ou, se não existir, o do sistema. */
    fun getEffectiveLanguage(): String {
        getSavedLanguage()?.let { return it }
        return if (Locale.getDefault().language == "en") "en" else "pt"
    }
}




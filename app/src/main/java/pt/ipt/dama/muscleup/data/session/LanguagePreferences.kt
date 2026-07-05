package pt.ipt.dama.muscleup.data.session

import android.content.Context
import androidx.core.content.edit
import java.util.Locale

private const val PREFS_NAME = "muscleup_settings"
private const val KEY_LANGUAGE = "language_tag"

/**
 * Passo 10.1 (fix) — persiste a escolha de idioma ("pt" ou "en") entre arranques da app.
 *
 * Nota de arquitetura: a primeira versão desta funcionalidade usava
 * `AppCompatDelegate.setApplicationLocales()` (API "per-app language" do AndroidX/Android 13+).
 * Foi abandonada porque a aplicação do idioma pelo sistema é *assíncrona*: ao chamar
 * `recreate()` logo a seguir para o efeito ser imediato, por vezes a Activity recriava-se
 * antes do sistema ter mesmo aplicado o novo locale, e a app ficava presa no idioma anterior
 * (condição de corrida). Guardando a escolha nós próprios em SharedPreferences e aplicando-a
 * de forma síncrona em `attachBaseContext()` (ver [pt.ipt.dama.muscleup.applyAppLocale]),
 * a troca de idioma passa a ser 100% determinística, em qualquer versão do Android.
 */
class LanguagePreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** "pt" ou "en", ou null se o utilizador ainda não escolheu explicitamente (segue o sistema). */
    fun getSavedLanguage(): String? = prefs.getString(KEY_LANGUAGE, null)

    /** [languageTag] deve ser "pt" ou "en". */
    fun save(languageTag: String) {
        prefs.edit { putString(KEY_LANGUAGE, languageTag) }
    }

    /**
     * "pt" ou "en" — o idioma efetivamente usado pela UI da app agora mesmo:
     * a escolha explícita do utilizador, ou (se ainda não escolheu) o idioma do sistema
     * quando é suportado, caindo para "pt" (idioma por omissão dos recursos da app) caso
     * contrário. Usado para enviar o header `Accept-Language` à API, para que mensagens
     * de erro geradas pelo servidor (ex: "Credenciais inválidas") venham no idioma certo.
     */
    fun getEffectiveLanguage(): String {
        getSavedLanguage()?.let { return it }
        return if (Locale.getDefault().language == "en") "en" else "pt"
    }
}




package pt.ipt.dama.muscleup

import android.content.Context
import android.content.res.Configuration
import pt.ipt.dama.muscleup.data.session.LanguagePreferences
import java.util.Locale

/**
 * Passo 10.1 (fix v2) — aplica manualmente aos recursos de [context] o idioma escolhido
 * pelo utilizador, guardado de forma própria e determinística em [LanguagePreferences].
 *
 * A primeira versão usava `AppCompatDelegate.setApplicationLocales()`, mas essa aplicação
 * é feita de forma assíncrona pelo sistema — chamar `recreate()` logo a seguir por vezes
 * recriava a Activity antes do novo locale estar mesmo aplicado, prendendo a app no idioma
 * anterior. Guardando e lendo a escolha nós próprios (síncrono), a troca fica 100% fiável.
 */
fun applyAppLocale(context: Context): Context {
    val tag = LanguagePreferences(context).getSavedLanguage() ?: return context
    val locale = if (tag == "en") Locale.forLanguageTag("en") else Locale.forLanguageTag("pt-PT")
    Locale.setDefault(locale)
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    return context.createConfigurationContext(config)
}





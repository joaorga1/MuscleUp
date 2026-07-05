package pt.ipt.dama.muscleup

import android.content.Context
import android.content.res.Configuration
import pt.ipt.dama.muscleup.data.session.LanguagePreferences
import java.util.Locale

/**
 * Aplica ao contexto fornecido o idioma escolhido pelo utilizador.
 * Lido de forma síncrona para garantir que o idioma já está aplicado antes de a Activity recriar os recursos.
 */
fun applyAppLocale(context: Context): Context {
    val tag = LanguagePreferences(context).getSavedLanguage() ?: return context
    val locale = if (tag == "en") Locale.forLanguageTag("en") else Locale.forLanguageTag("pt-PT")
    Locale.setDefault(locale)
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    return context.createConfigurationContext(config)
}



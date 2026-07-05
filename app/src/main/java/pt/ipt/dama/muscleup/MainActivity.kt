package pt.ipt.dama.muscleup

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import pt.ipt.dama.muscleup.ui.navigation.AppNavigation
import pt.ipt.dama.muscleup.ui.navigation.Screen
import pt.ipt.dama.muscleup.ui.theme.MuscleUpTheme
import pt.ipt.dama.muscleup.ui.theme.ThemeMode
import pt.ipt.dama.muscleup.ui.theme.ThemeState

class MainActivity : ComponentActivity() {

    // Passo 10.1 (fix) — aplica o idioma escolhido pelo utilizador antes de qualquer
    // recurso ser resolvido nesta Activity (ver LocaleUtils.kt).
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(applyAppLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MuscleUpApp
        val startDestination = if (app.sessionPreferences.isValid()) {
            Screen.Home.route
        } else {
            Screen.Login.route
        }

        setContent {
            // Passo 10.1 — o utilizador pode forçar Claro/Escuro nas Definições;
            // "Sistema" (default) continua a seguir o tema do dispositivo.
            val darkTheme = when (ThemeState.mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            MuscleUpTheme(darkTheme = darkTheme) {
                AppNavigation(startDestination = startDestination)
            }
        }
    }
}

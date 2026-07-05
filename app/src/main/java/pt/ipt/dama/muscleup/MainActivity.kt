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

/**
 * Atividade principal e único ponto de entrada da interface da aplicação.
 *
 * Define o destino inicial de navegação consoante exista ou não uma sessão válida
 * guardada e aplica o tema (claro, escuro ou seguir o sistema) escolhido pelo utilizador.
 */
class MainActivity : ComponentActivity() {

    /**
     * Aplica o idioma escolhido pelo utilizador antes de qualquer recurso ser resolvido
     * nesta Activity, para que os textos apresentados já respeitem essa escolha.
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(applyAppLocale(newBase))
    }

    /**
     * Inicializa a interface, decidindo o ecrã inicial (sessão ativa ou login)
     * e configurando o tema visual da aplicação.
     */
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
            // O utilizador pode forçar o tema Claro ou Escuro nas Definições.
            // A opção Sistema, que é a predefinida, segue o tema do dispositivo.
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

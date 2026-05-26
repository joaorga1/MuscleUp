package pt.ipt.dama.muscleup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pt.ipt.dama.muscleup.ui.navigation.AppNavigation
import pt.ipt.dama.muscleup.ui.navigation.Screen
import pt.ipt.dama.muscleup.ui.theme.MuscleUpTheme

class MainActivity : ComponentActivity() {
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
            MuscleUpTheme {
                AppNavigation(startDestination = startDestination)
            }
        }
    }
}

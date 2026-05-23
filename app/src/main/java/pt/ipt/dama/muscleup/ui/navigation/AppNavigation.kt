package pt.ipt.dama.muscleup.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pt.ipt.dama.muscleup.ui.screens.ExerciseScreen
import pt.ipt.dama.muscleup.ui.screens.HomeScreen
import pt.ipt.dama.muscleup.ui.screens.LoginScreen
import pt.ipt.dama.muscleup.ui.screens.ProfileScreen
import pt.ipt.dama.muscleup.ui.screens.RegisterScreen
import pt.ipt.dama.muscleup.ui.screens.WorkoutScreen

sealed class Screen(val route: String) {
    object Login    : Screen("login")
    object Register : Screen("register")
    object Home     : Screen("home")
    object Workout  : Screen("workout")
    object Exercise : Screen("exercise")
    object Profile  : Screen("profile")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route)    { LoginScreen() }
        composable(Screen.Register.route) { RegisterScreen() }
        composable(Screen.Home.route)     { HomeScreen() }
        composable(Screen.Workout.route)  { WorkoutScreen() }
        composable(Screen.Exercise.route) { ExerciseScreen() }
        composable(Screen.Profile.route)  { ProfileScreen() }
    }
}

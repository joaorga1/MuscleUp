package pt.ipt.dama.muscleup.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pt.ipt.dama.muscleup.ui.screens.auth.LoginScreen
import pt.ipt.dama.muscleup.ui.screens.auth.RegisterScreen
import pt.ipt.dama.muscleup.ui.screens.exercise.ExerciseScreen
import pt.ipt.dama.muscleup.ui.screens.home.HomeScreen
import pt.ipt.dama.muscleup.ui.screens.home.HomeViewModel
import pt.ipt.dama.muscleup.ui.screens.home.WorkoutFormScreen
import pt.ipt.dama.muscleup.ui.screens.profile.ProfileScreen
import pt.ipt.dama.muscleup.ui.screens.workout.WorkoutScreen

sealed class Screen(val route: String) {
    object Login    : Screen("login")
    object Register : Screen("register")
    object Home     : Screen("home")
    object WorkoutForm : Screen("workout_form?workoutId={workoutId}") {
        fun create() = "workout_form?workoutId="
        fun edit(workoutId: String) = "workout_form?workoutId=$workoutId"
    }
    object Workout  : Screen("workout/{workoutId}") {
        fun go(workoutId: String) = "workout/$workoutId"
    }
    object Exercise : Screen("exercise")
    object Profile  : Screen("profile")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route
) {
    val homeViewModel: HomeViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route)    { LoginScreen(navController) }
        composable(Screen.Register.route) { RegisterScreen(navController) }
        composable(Screen.Home.route)     { HomeScreen(navController, homeViewModel) }
        composable(
            route = Screen.WorkoutForm.route,
            arguments = listOf(navArgument("workoutId") {
                type = NavType.StringType
                defaultValue = ""
            })
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getString("workoutId").orEmpty()
            WorkoutFormScreen(navController, homeViewModel, workoutId.ifBlank { null })
        }
        composable(
            route = Screen.Workout.route,
            arguments = listOf(navArgument("workoutId") { type = NavType.StringType })
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getString("workoutId") ?: ""
            WorkoutScreen(navController = navController, workoutId = workoutId, viewModel = homeViewModel)
        }
        composable(Screen.Exercise.route) { ExerciseScreen() }
        composable(Screen.Profile.route)  { ProfileScreen(navController) }
    }
}

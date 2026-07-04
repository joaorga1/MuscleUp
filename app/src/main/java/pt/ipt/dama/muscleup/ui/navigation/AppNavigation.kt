package pt.ipt.dama.muscleup.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pt.ipt.dama.muscleup.data.remote.AuthStateManager
import pt.ipt.dama.muscleup.data.session.UserSession
import pt.ipt.dama.muscleup.ui.screens.auth.LoginScreen
import pt.ipt.dama.muscleup.ui.screens.auth.RegisterScreen
import pt.ipt.dama.muscleup.ui.screens.exercise.ExerciseScreen
import pt.ipt.dama.muscleup.ui.screens.exercise.ExerciseViewModel
import pt.ipt.dama.muscleup.ui.screens.home.HomeScreen
import pt.ipt.dama.muscleup.ui.screens.home.HomeViewModel
import pt.ipt.dama.muscleup.ui.screens.home.WorkoutFormScreen
import pt.ipt.dama.muscleup.ui.screens.profile.ProfileScreen
import pt.ipt.dama.muscleup.ui.screens.workout.ExerciseFormScreen
import pt.ipt.dama.muscleup.ui.screens.workout.WorkoutScreen
import pt.ipt.dama.muscleup.ui.screens.workout.WorkoutViewModel

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
    object ExerciseForm : Screen("exercise_form/{workoutId}?exerciseId={exerciseId}") {
        fun create(workoutId: String) = "exercise_form/$workoutId?exerciseId="
        fun edit(workoutId: String, exerciseId: String) = "exercise_form/$workoutId?exerciseId=$exerciseId"
    }
    object Exercise : Screen("exercise/{exerciseId}") {
        fun go(exerciseId: String) = "exercise/$exerciseId"
    }
    object Profile  : Screen("profile")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route
) {
    val homeViewModel: HomeViewModel = viewModel()

    // Centralized logout handler
    val handleLogout = {
        homeViewModel.logout()
        navController.navigate(Screen.Login.route) {
            popUpTo(0) { inclusive = true }
        }
    }

    // Observa eventos de "sessão expirada" vindos de qualquer camada da app
    // (ex: refreshToken rejeitado pelo servidor durante um pedido HTTP em background).
    // Guarda: só actua se ainda houver sessão activa, para não entrar em loop.
    LaunchedEffect(Unit) {
        AuthStateManager.forceLogout.collect {
            if (UserSession.currentUserEmail.isNotBlank()) {
                homeViewModel.logout()
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        enterTransition = { fadeIn(animationSpec = tween(200)) },
        exitTransition = { fadeOut(animationSpec = tween(200)) },
        popEnterTransition = { fadeIn(animationSpec = tween(200)) },
        popExitTransition = { fadeOut(animationSpec = tween(200)) }
    ) {
        composable(Screen.Login.route)    { LoginScreen(navController) }
        composable(Screen.Register.route) { RegisterScreen(navController) }
        composable(Screen.Home.route)     { HomeScreen(navController, homeViewModel, handleLogout) }
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
            val workoutViewModel: WorkoutViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = WorkoutViewModel.factory(workoutId)
            )
            WorkoutScreen(navController = navController, viewModel = workoutViewModel, onLogout = handleLogout)
        }
        composable(
            route = Screen.ExerciseForm.route,
            arguments = listOf(
                navArgument("workoutId") { type = NavType.StringType },
                navArgument("exerciseId") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getString("workoutId") ?: ""
            val exerciseId = backStackEntry.arguments?.getString("exerciseId").orEmpty()

            // Tenta partilhar o WorkoutViewModel com o WorkoutScreen para o mesmo workoutId.
            // Se a back stack não tiver o WorkoutScreen (ex: restauro de estado pelo Android),
            // navega para Home em vez de crashar.
            val workoutBackStackEntry = remember(backStackEntry) {
                try {
                    navController.getBackStackEntry(Screen.Workout.go(workoutId))
                } catch (_: IllegalArgumentException) {
                    null
                }
            }

            if (workoutBackStackEntry == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            } else {
                val workoutViewModel: WorkoutViewModel = viewModel(
                    viewModelStoreOwner = workoutBackStackEntry,
                    factory = WorkoutViewModel.factory(workoutId)
                )
                ExerciseFormScreen(
                    navController = navController,
                    viewModel = workoutViewModel,
                    exerciseId = exerciseId.ifBlank { null }
                )
            }
        }
        composable(
            route = Screen.Exercise.route,
            arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: ""
            val exerciseViewModel: ExerciseViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = ExerciseViewModel.factory(exerciseId)
            )
            ExerciseScreen(navController = navController, viewModel = exerciseViewModel, onLogout = handleLogout)
        }
        composable(Screen.Profile.route)  { ProfileScreen(navController, handleLogout) }
    }
}

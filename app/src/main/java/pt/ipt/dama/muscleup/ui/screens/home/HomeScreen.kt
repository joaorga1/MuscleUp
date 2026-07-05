package pt.ipt.dama.muscleup.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import pt.ipt.dama.muscleup.R
import pt.ipt.dama.muscleup.model.Workout
import pt.ipt.dama.muscleup.ui.components.AppTopBar
import pt.ipt.dama.muscleup.ui.components.EmptyState
import pt.ipt.dama.muscleup.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel,
    onLogout: () -> Unit = {}
) {
    LaunchedEffect(Unit) {
        viewModel.refreshSessionUser()
    }

    val workouts by viewModel.workouts.collectAsState()
    val userName = viewModel.userName
    val profilePhotoUri by viewModel.profilePhotoUri.collectAsState()
    var workoutToDelete by remember { mutableStateOf<Workout?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.home_title),
                showAvatar = true,
                userName = userName,
                profilePhotoUri = profilePhotoUri,
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onLogoutClick = onLogout
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.WorkoutForm.create()) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_desc_add))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (workouts.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Star,
                title = stringResource(R.string.empty_workouts_title),
                subtitle = stringResource(R.string.empty_workouts_subtitle),
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(workouts, key = { it.id }) { workout ->
                    val dismissState = rememberSwipeToDismissBoxState()

                    LaunchedEffect(dismissState.currentValue) {
                        when (dismissState.currentValue) {
                            SwipeToDismissBoxValue.EndToStart -> {
                                workoutToDelete = workout
                                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                            }
                            SwipeToDismissBoxValue.StartToEnd -> {
                                navController.navigate(Screen.WorkoutForm.edit(workout.id))
                                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                            }
                            else -> {}
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = true,
                        backgroundContent = {
                            val isEditSwipe = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
                            val bgColor = if (isEditSwipe) MaterialTheme.colorScheme.primary else Color.Red
                            val icon = if (isEditSwipe) Icons.Default.Edit else Icons.Default.Delete
                            val alignment = if (isEditSwipe) Alignment.CenterStart else Alignment.CenterEnd
                            val padding = if (isEditSwipe) Modifier.padding(start = 20.dp) else Modifier.padding(end = 20.dp)

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(bgColor, shape = MaterialTheme.shapes.medium)
                                    .then(padding),
                                contentAlignment = alignment
                            ) {
                                Icon(imageVector = icon, contentDescription = null, tint = Color.White)
                            }
                        }
                    ) {
                        WorkoutCard(
                            workout = workout,
                            onClick = { navController.navigate(Screen.Workout.go(workout.id)) }
                        )
                    }
                }
            }
        }

        // Diálogo de confirmação
        workoutToDelete?.let { workout ->
            AlertDialog(
                onDismissRequest = { workoutToDelete = null },
                title = { Text(stringResource(R.string.home_delete_workout_title)) },
                text = { Text(stringResource(R.string.delete_exercise_message, workout.title)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteWorkout(workout.id)
                        workoutToDelete = null
                    }) {
                        Text(stringResource(R.string.action_delete), color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { workoutToDelete = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}


@Composable
fun WorkoutCard(workout: Workout, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = workout.title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(workout.type.labelRes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = workout.description,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

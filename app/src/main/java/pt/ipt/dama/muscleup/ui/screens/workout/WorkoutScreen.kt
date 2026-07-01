package pt.ipt.dama.muscleup.ui.screens.workout

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
import pt.ipt.dama.muscleup.model.Exercise
import pt.ipt.dama.muscleup.ui.components.AppTopBar
import pt.ipt.dama.muscleup.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    navController: NavController,
    viewModel: WorkoutViewModel,
    onLogout: () -> Unit = {}
) {
    val workout by viewModel.workout.collectAsState()
    val profilePhotoUri by viewModel.profilePhotoUri.collectAsState()
    var exerciseToDelete by remember { mutableStateOf<Exercise?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = workout?.title ?: "Treino",
                showBackButton = true,
                onBackClick = { navController.popBackStack() },
                showAvatar = true,
                userName = viewModel.userName,
                profilePhotoUri = profilePhotoUri,
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onLogoutClick = onLogout
            )
        },
        floatingActionButton = {
            if (workout != null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FloatingActionButton(onClick = { navController.navigate(Screen.WorkoutForm.edit(viewModel.workoutId)) }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.content_desc_edit))
                    }
                    FloatingActionButton(onClick = { navController.navigate(Screen.ExerciseForm.create(viewModel.workoutId)) }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_desc_add))
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (workout == null) {
            Text(
                text = "Treino não encontrado",
                modifier = Modifier.padding(innerPadding).padding(16.dp)
            )
        } else {
            val currentWorkout = workout!!
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = stringResource(currentWorkout.type.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = currentWorkout.description,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                }
                items(currentWorkout.exercises, key = { it.id }) { exercise ->
                    val dismissState = rememberSwipeToDismissBoxState()

                    LaunchedEffect(dismissState.currentValue) {
                        when (dismissState.currentValue) {
                            SwipeToDismissBoxValue.StartToEnd -> {
                                navController.navigate(Screen.ExerciseForm.edit(viewModel.workoutId, exercise.id))
                                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                            }
                            SwipeToDismissBoxValue.EndToStart -> {
                                exerciseToDelete = exercise
                                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                            }
                            else -> {}
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary
                                SwipeToDismissBoxValue.EndToStart -> Color.Red
                                else -> Color.Transparent
                            }
                            val icon = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                                else -> Icons.Default.Delete
                            }
                            val alignment = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                else -> Alignment.CenterEnd
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color, shape = MaterialTheme.shapes.medium)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = alignment
                            ) {
                                Icon(icon, contentDescription = null, tint = Color.White)
                            }
                        }
                    ) {
                        ExerciseCard(
                            exercise = exercise,
                            onClick = { navController.navigate(Screen.Exercise.go(exercise.id)) }
                        )
                    }
                }
            }
        }

        exerciseToDelete?.let { exercise ->
            AlertDialog(
                onDismissRequest = { exerciseToDelete = null },
                title = { Text(stringResource(R.string.delete_exercise_title)) },
                text = { Text(stringResource(R.string.delete_exercise_message, exercise.name)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteExercise(exercise.id)
                        exerciseToDelete = null
                    }) {
                        Text(stringResource(R.string.action_delete), color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { exerciseToDelete = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun ExerciseCard(exercise: Exercise, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = exercise.name, style = MaterialTheme.typography.titleLarge)
            Text(
                text = exercise.targetMuscle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = exercise.description,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (exercise.sets.isNotEmpty()) {
                Text(
                    text = "${exercise.sets.size} séries",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

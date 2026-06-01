package pt.ipt.dama.muscleup.ui.screens.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import pt.ipt.dama.muscleup.R
import pt.ipt.dama.muscleup.ui.components.AppTopBar

@Composable
fun ExerciseFormScreen(
    navController: NavController,
    viewModel: WorkoutViewModel,
    exerciseId: String? = null
) {
    val workout by viewModel.workout.collectAsState()
    val existingExercise = exerciseId?.let { id -> workout?.exercises?.find { it.id == id } }

    var name by remember(existingExercise) { mutableStateOf(existingExercise?.name ?: "") }
    var description by remember(existingExercise) { mutableStateOf(existingExercise?.description ?: "") }
    var targetMuscle by remember(existingExercise) { mutableStateOf(existingExercise?.targetMuscle ?: "") }

    val title = if (exerciseId != null)
        stringResource(R.string.exercise_form_title_edit)
    else
        stringResource(R.string.exercise_form_title_new)

    Scaffold(
        topBar = {
            AppTopBar(
                title = title,
                showBackButton = true,
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.exercise_form_field_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = targetMuscle,
                onValueChange = { targetMuscle = it },
                label = { Text(stringResource(R.string.exercise_form_field_target_muscle)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.exercise_form_field_description)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        if (existingExercise != null) {
                            viewModel.editExercise(existingExercise.id, name, description, targetMuscle)
                        } else {
                            viewModel.addExercise(name, description, targetMuscle)
                        }
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.exercise_form_save))
            }
        }
    }
}

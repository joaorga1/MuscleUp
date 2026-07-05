package pt.ipt.dama.muscleup.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import pt.ipt.dama.muscleup.R
import pt.ipt.dama.muscleup.model.WorkoutType
import pt.ipt.dama.muscleup.ui.components.AppTopBar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutFormScreen(
    navController: NavController,
    viewModel: HomeViewModel,
    workoutId: String? = null
) {
    val existing = workoutId?.let { viewModel.getWorkoutById(it) }
    val isEditing = existing != null

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var selectedType by remember { mutableStateOf<WorkoutType?>(existing?.type) }
    var titleError by remember { mutableStateOf(false) }
    var typeError by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.uiEvent.collect { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(Unit) { viewModel.navigateBack.collect { navController.popBackStack() } }

    val screenTitle = if (isEditing)
        stringResource(R.string.workout_form_title_edit)
    else
        stringResource(R.string.workout_form_title_new)

    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            AppTopBar(
                title = screenTitle,
                showBackButton = true,
                onBackClick = { navController.popBackStack() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.imePadding()) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; titleError = false },
                label = { Text(stringResource(R.string.workout_form_field_title)) },
                isError = titleError,
                supportingText = { if (titleError) Text(stringResource(R.string.workout_form_error_title_required)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.workout_form_field_description)) },
                minLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.workout_form_field_type),
                style = MaterialTheme.typography.labelLarge,
                color = if (typeError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkoutType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = {
                            focusManager.clearFocus()
                            selectedType = type
                            typeError = false
                        },
                        label = { Text(stringResource(type.labelRes)) }
                    )
                }
            }

            if (typeError) {
                Text(
                    text = stringResource(R.string.workout_form_error_type_required),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    titleError = title.isBlank()
                    typeError = selectedType == null
                    if (!titleError && !typeError) {
                        if (isEditing) {
                            viewModel.editWorkout(workoutId, title.trim(), description.trim(), selectedType!!)
                        } else {
                            viewModel.addWorkout(title.trim(), description.trim(), selectedType!!)
                        }
                        // navegação feita pelo LaunchedEffect que reage a navigateBack
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.workout_form_save))
            }
        }
    }
}


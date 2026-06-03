package pt.ipt.dama.muscleup.ui.screens.exercise

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TextButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.navigation.NavController
import pt.ipt.dama.muscleup.model.Exercise
import pt.ipt.dama.muscleup.ui.components.AppTopBar
import pt.ipt.dama.muscleup.ui.navigation.Screen
import pt.ipt.dama.muscleup.model.SessionExerciseSet

@Composable
fun ExerciseScreen(
    navController: NavController,
    viewModel: ExerciseViewModel,
    onLogout: () -> Unit = {}
) {
    val exercise by viewModel.exercise.collectAsState()
    var selectedTabIndex by rememberSaveable(exercise?.id) { mutableIntStateOf(0) }

    val tabs = listOf(
        "Pré-definição",
        "Últimas Execuções",
        "PR",
        "Registo",
        "Config. Máquina"
    )

    Scaffold(
        topBar = {
            AppTopBar(
                title = exercise?.name ?: "Exercício",
                showBackButton = true,
                onBackClick = { navController.popBackStack() },
                showAvatar = true,
                userName = viewModel.userName,
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onLogoutClick = onLogout
            )
        }
    ) { innerPadding ->
        if (exercise == null) {
            Text(
                text = "Exercício não encontrado",
                modifier = Modifier.padding(innerPadding).padding(16.dp)
            )
        } else {
            val currentExercise = exercise!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Tab Row
                PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, tabName ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(tabName) }
                        )
                    }
                }

                // Tab Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (selectedTabIndex) {
                        0 -> ExercisePreDefinitionTab(
                            exercise = currentExercise,
                            onAddSet = { reps, weightKg, durationSeconds ->
                                viewModel.addPredefinedSet(reps, weightKg, durationSeconds)
                            },
                            onRemoveSet = { setId -> viewModel.removePredefinedSet(setId) }
                        )
                        1 -> ExerciseHistoryTab(exercise = currentExercise)
                        2 -> ExercisePersonalRecordTab(exercise = currentExercise)
                        3 -> ExerciseRecordTab(
                            exercise = currentExercise,
                            currentSessionSets = viewModel.currentSessionSets.collectAsState().value,
                            onAddSet = { reps, weightKg, durationSeconds ->
                                viewModel.addRecordedSet(reps, weightKg, durationSeconds)
                            },
                            onRemoveSet = { setId -> viewModel.removeRecordedSet(setId) },
                            onFinalize = { viewModel.finalizeSession() },
                            onClear = { viewModel.clearSession() }
                        )
                        4 -> ExerciseMachineConfigTab(exercise = currentExercise)
                    }
                }
            }
        }
    }
}

@Composable
fun ExercisePreDefinitionTab(
    exercise: Exercise,
    onAddSet: (reps: Int, weightKg: Float?, durationSeconds: Int?) -> Unit,
    onRemoveSet: (setId: String) -> Unit
) {
    var repsInput by rememberSaveable(exercise.id) { mutableStateOf("") }
    var weightInput by rememberSaveable(exercise.id) { mutableStateOf("") }
    var timeInput by rememberSaveable(exercise.id) { mutableStateOf("") }

    val predefinedSets = exercise.sets
    val repsText = repsInput.trim()
    val reps = if (repsText.isBlank()) 1 else repsText.toIntOrNull() ?: -1
    val weightText = weightInput.trim()
    val timeText = timeInput.trim()
    val weightKg = if (weightText.isBlank()) null else weightText.toFloatOrNull()
    val durationSeconds = if (timeText.isBlank()) null else timeText.toIntOrNull()
    val hasInvalidWeight = weightText.isNotBlank() && (weightKg == null || weightKg <= 0f)
    val hasInvalidTime = timeText.isNotBlank() && (durationSeconds == null || durationSeconds <= 0)
    val hasNoTarget = weightKg == null && durationSeconds == null
    val isFormValid = reps > 0 && !hasInvalidWeight && !hasInvalidTime && !hasNoTarget

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text("Pré-definição — ${exercise.name}")
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = repsInput,
            onValueChange = {
                repsInput = it
            },
            label = { Text("Repetições") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = weightInput,
            onValueChange = {
                weightInput = it
            },
            label = { Text("Peso (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = timeInput,
            onValueChange = {
                timeInput = it
            },
            label = { Text("Tempo (s)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                onAddSet(reps, weightKg, durationSeconds)

                repsInput = ""
                weightInput = ""
                timeInput = ""
            },
            enabled = isFormValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Adicionar série")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Séries")
        Spacer(modifier = Modifier.height(8.dp))

        predefinedSets.forEachIndexed { index, set ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildString {
                        append("Série ${index + 1}: ${set.reps} reps")
                        if (set.weightKg > 0f) append(" . ${set.weightKg}kg")
                        if (set.durationSeconds > 0) append(" . ${set.durationSeconds}s")
                    }
                )

                TextButton(onClick = { onRemoveSet(set.id) }) {
                    Text("Remover")
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
fun ExerciseHistoryTab(exercise: Exercise) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Últimas Execuções — ${exercise.name}")
    }
}

@Composable
fun ExercisePersonalRecordTab(exercise: Exercise) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("PR (Recorde Pessoal) — ${exercise.name}")
    }
}

@Composable
fun ExerciseRecordTab(
    exercise: Exercise,
    currentSessionSets: List<SessionExerciseSet>,
    onAddSet: (reps: Int, weightKg: Float?, durationSeconds: Int?) -> Unit,
    onRemoveSet: (setId: String) -> Unit,
    onFinalize: () -> Unit,
    onClear: () -> Unit
) {
    var repsInput by rememberSaveable(exercise.id) { mutableStateOf("") }
    var weightInput by rememberSaveable(exercise.id) { mutableStateOf("") }
    var timeInput by rememberSaveable(exercise.id) { mutableStateOf("") }

    val repsText = repsInput.trim()
    val reps = if (repsText.isBlank()) 1 else repsText.toIntOrNull() ?: -1
    val weightText = weightInput.trim()
    val timeText = timeInput.trim()
    val weightKg = if (weightText.isBlank()) null else weightText.toFloatOrNull()
    val durationSeconds = if (timeText.isBlank()) null else timeText.toIntOrNull()
    val hasInvalidWeight = weightText.isNotBlank() && (weightKg == null || weightKg <= 0f)
    val hasInvalidTime = timeText.isNotBlank() && (durationSeconds == null || durationSeconds <= 0)
    val hasNoTarget = weightKg == null && durationSeconds == null
    val isFormValid = reps > 0 && !hasInvalidWeight && !hasInvalidTime && !hasNoTarget

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text("Registo — ${exercise.name}")
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = repsInput,
            onValueChange = { repsInput = it },
            label = { Text("Repetições") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = weightInput,
            onValueChange = { weightInput = it },
            label = { Text("Peso (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = timeInput,
            onValueChange = { timeInput = it },
            label = { Text("Tempo (s)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                onAddSet(reps, weightKg, durationSeconds)
                repsInput = ""
                weightInput = ""
                timeInput = ""
            },
            enabled = isFormValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Adicionar série")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Séries registadas: ${currentSessionSets.size}")
        Spacer(modifier = Modifier.height(8.dp))

        currentSessionSets.forEach { set ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildString {
                        append("Série ${set.setOrder}: ${set.reps} reps")
                        if (set.weightKg > 0f) append(" . ${set.weightKg}kg")
                        if (set.durationSeconds > 0) append(" . ${set.durationSeconds}s")
                    }
                )

                TextButton(onClick = { onRemoveSet(set.id) }) {
                    Text("Remover")
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (currentSessionSets.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onFinalize,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Finalizar")
                }
                TextButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Limpar")
                }
            }
        }
    }
}

@Composable
fun ExerciseMachineConfigTab(exercise: Exercise) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Config. Máquina — ${exercise.name}")
        Text("Configurações: ${exercise.machineConfigs.size}")
    }
}


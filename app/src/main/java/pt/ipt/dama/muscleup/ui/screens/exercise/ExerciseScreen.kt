package pt.ipt.dama.muscleup.ui.screens.exercise

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import pt.ipt.dama.muscleup.model.Exercise
import pt.ipt.dama.muscleup.ui.components.AppTopBar
import pt.ipt.dama.muscleup.ui.navigation.Screen

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
                        0 -> ExercisePreDefinitionTab(exercise = currentExercise)
                        1 -> ExerciseHistoryTab(exercise = currentExercise)
                        2 -> ExercisePersonalRecordTab(exercise = currentExercise)
                        3 -> ExerciseRecordTab(exercise = currentExercise)
                        4 -> ExerciseMachineConfigTab(exercise = currentExercise)
                    }
                }
            }
        }
    }
}

@Composable
fun ExercisePreDefinitionTab(exercise: Exercise) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Pré-definição — ${exercise.name}")
        Text("Séries: ${exercise.sets.size}")
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
fun ExerciseRecordTab(exercise: Exercise) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Registo — ${exercise.name}")
    }
}

@Composable
fun ExerciseMachineConfigTab(exercise: Exercise) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Config. Máquina — ${exercise.name}")
        Text("Configurações: ${exercise.machineConfigs.size}")
    }
}


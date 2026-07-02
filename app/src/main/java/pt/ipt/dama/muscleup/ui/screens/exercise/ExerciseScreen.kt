package pt.ipt.dama.muscleup.ui.screens.exercise

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TextButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.AsyncImage
import pt.ipt.dama.muscleup.model.Exercise
import pt.ipt.dama.muscleup.model.ExercisePhoto
import pt.ipt.dama.muscleup.ui.components.AppTopBar
import pt.ipt.dama.muscleup.ui.navigation.Screen
import pt.ipt.dama.muscleup.model.SessionExerciseSet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExerciseScreen(
    navController: NavController,
    viewModel: ExerciseViewModel,
    onLogout: () -> Unit = {}
) {
    val exercise by viewModel.exercise.collectAsState()
    val profilePhotoUri by viewModel.profilePhotoUri.collectAsState()
    val currentSessionSets by viewModel.currentSessionSets.collectAsState()
    val historySessions by viewModel.historySessions.collectAsState()
    val personalRecord by viewModel.personalRecord.collectAsState()
    val photos by viewModel.photos.collectAsState()
    var selectedTabIndex by rememberSaveable(exercise?.id) { mutableIntStateOf(0) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { snackbarHostState.showSnackbar(it) }
    }

    val context = LocalContext.current
    var showPhotoDialog by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) pendingCameraUri?.let { viewModel.addPhoto(it.toString()) }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) pendingCameraUri?.let { cameraLauncher.launch(it) }
    }

    fun launchCamera() {
        val uri = viewModel.createPhotoUri(context)
        pendingCameraUri = uri
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            viewModel.addPhoto(uri.toString())
        }
    }

    val tabs = listOf(
        "Pré-definição",
        "Últimas Execuções",
        "PR",
        "Registo",
        "Config. Máquina"
    )

    if (showPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false },
            title = { Text("Adicionar foto") },
            text = {
                Column {
                    TextButton(
                        onClick = { showPhotoDialog = false; launchCamera() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Câmara") }
                    TextButton(
                        onClick = {
                            showPhotoDialog = false
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Galeria") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = exercise?.name ?: "Exercício",
                showBackButton = true,
                onBackClick = { navController.popBackStack() },
                showAvatar = true,
                userName = viewModel.userName,
                profilePhotoUri = profilePhotoUri,
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onLogoutClick = onLogout
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                ExercisePhotoGallery(
                    photos = photos,
                    onAddClick = { showPhotoDialog = true },
                    onRemove = { photoId -> viewModel.removePhoto(photoId) }
                )
                HorizontalDivider()

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
                        1 -> ExerciseHistoryTab(
                            exercise = currentExercise,
                            sessions = historySessions
                        )
                        2 -> ExercisePersonalRecordTab(
                            exercise = currentExercise,
                            personalRecord = personalRecord
                        )
                        3 -> ExerciseRecordTab(
                            exercise = currentExercise,
                            currentSessionSets = currentSessionSets,
                            onAddSet = { reps, weightKg, durationSeconds ->
                                viewModel.addRecordedSet(reps, weightKg, durationSeconds)
                            },
                            onRemoveSet = { setId -> viewModel.removeRecordedSet(setId) },
                            onFinalize = { viewModel.finalizeSession() },
                            onClear = { viewModel.clearSession() }
                        )
                        4 -> ExerciseMachineConfigTab(
                            exercise = currentExercise,
                            onAddConfig = { name, description, angle ->
                                viewModel.addMachineConfig(name, description, angle)
                            },
                            onRemoveConfig = { configId -> viewModel.removeMachineConfig(configId) }
                        )
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
    var isEditing by rememberSaveable(exercise.id) { mutableStateOf(exercise.sets.isEmpty()) }
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
        if (!isEditing) {
            Button(
                onClick = { isEditing = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Editar pré-definição")
            }
        } else {
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onAddSet(reps, weightKg, durationSeconds)
                        repsInput = ""
                        weightInput = ""
                        timeInput = ""
                    },
                    enabled = isFormValid,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Adicionar série")
                }

                TextButton(
                    onClick = {
                        repsInput = ""
                        weightInput = ""
                        timeInput = ""
                        isEditing = false
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Concluir edição")
                }
            }
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

                if (isEditing) {
                    TextButton(onClick = { onRemoveSet(set.id) }) {
                        Text("Remover")
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
fun ExerciseHistoryTab(
    exercise: Exercise,
    sessions: List<ExerciseHistorySession>
) {
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text("Últimas Execuções — ${exercise.name}")
        Spacer(modifier = Modifier.height(12.dp))

        if (sessions.isEmpty()) {
            Text("Sem sessões finalizadas para este exercício.")
            return@Column
        }

        sessions.forEach { session ->
            val finishedLabel = session.finishedAt?.let { formatter.format(Date(it)) } ?: "-"
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Sessão de $finishedLabel")
                    Text("${session.sets.size} séries")
                    Spacer(modifier = Modifier.height(8.dp))

                    // Keep historical sets in their natural order (1 -> N).
                    val orderedSets = session.sets.sortedBy { it.setOrder }
                    orderedSets.forEachIndexed { setIndex, set ->
                        Text(
                            text = buildString {
                                append("Série ${set.setOrder}: ${set.reps} reps")
                                if (set.weightKg > 0f) append(" . ${set.weightKg}kg")
                                if (set.durationSeconds > 0) append(" . ${set.durationSeconds}s")
                            }
                        )

                        if (setIndex < orderedSets.lastIndex) {
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ExercisePersonalRecordTab(
    exercise: Exercise,
    personalRecord: ExercisePersonalRecord
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("PR (Recorde Pessoal) — ${exercise.name}")
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = personalRecord.maxWeightKg?.let { weight ->
                val repsLabel = personalRecord.maxWeightReps?.let { " x ${it} reps" }.orEmpty()
                "Maior peso: ${weight}kg${repsLabel}"
            }
                ?: "Maior peso: sem registos"
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = personalRecord.maxDurationSeconds?.let { "Maior tempo: ${it}s" }
                ?: "Maior tempo: sem registos"
        )
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
fun ExerciseMachineConfigTab(
    exercise: Exercise,
    onAddConfig: (name: String, description: String, angleDegrees: Float?) -> Unit,
    onRemoveConfig: (configId: String) -> Unit
) {
    var isEditing by rememberSaveable(exercise.id) { mutableStateOf(false) }
    var nameInput by rememberSaveable(exercise.id) { mutableStateOf("") }
    var descriptionInput by rememberSaveable(exercise.id) { mutableStateOf("") }
    var angleInput by rememberSaveable(exercise.id) { mutableStateOf("") }

    val angleText = angleInput.trim()
    val angleValue = if (angleText.isBlank()) null else angleText.toFloatOrNull()
    val hasInvalidAngle = angleText.isNotBlank() && angleValue == null
    val isFormValid = nameInput.trim().isNotBlank() && descriptionInput.trim().isNotBlank() && !hasInvalidAngle

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text("Config. Máquina — ${exercise.name}")
        Spacer(modifier = Modifier.height(12.dp))

        if (!isEditing) {
            Button(
                onClick = { isEditing = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Editar configurações")
            }
        } else {
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Nome") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = descriptionInput,
                onValueChange = { descriptionInput = it },
                label = { Text("Descrição") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            AngleInputField(
                value = angleInput,
                onValueChange = { angleInput = it },
                isError = hasInvalidAngle
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onAddConfig(nameInput.trim(), descriptionInput.trim(), angleValue)
                        nameInput = ""
                        descriptionInput = ""
                        angleInput = ""
                    },
                    enabled = isFormValid,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Adicionar")
                }
                TextButton(
                    onClick = {
                        nameInput = ""
                        descriptionInput = ""
                        angleInput = ""
                        isEditing = false
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Concluir edição")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (exercise.machineConfigs.isEmpty()) {
            Text("Sem configurações definidas.")
        } else {
            exercise.machineConfigs.forEach { config ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(config.name)
                        Text(config.description)
                        config.angleDegrees?.let { angle ->
                            Text(
                                text = "Ângulo: ${"%.1f".format(angle)}°",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (isEditing) {
                        TextButton(onClick = { onRemoveConfig(config.id) }) {
                            Text("Remover")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Campo opcional para o ângulo da máquina. Pode ser preenchido manualmente
 * ou, se o dispositivo tiver acelerómetro, através do botão "Usar sensor"
 * que copia a leitura atual do inclinómetro para o campo.
 */
@Composable
fun AngleInputField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean
) {
    val inclinometerViewModel: InclinometerViewModel = viewModel()
    val liveAngle by inclinometerViewModel.angleDegrees.collectAsState()

    DisposableEffect(Unit) {
        inclinometerViewModel.start()
        onDispose { inclinometerViewModel.stop() }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Ângulo (°) — opcional") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = isError,
        supportingText = if (isError) {
            { Text("Ângulo inválido") }
        } else null,
        trailingIcon = if (inclinometerViewModel.isSensorAvailable) {
            {
                TextButton(onClick = { onValueChange("%.1f".format(liveAngle)) }) {
                    Text("Usar sensor")
                }
            }
        } else null,
        modifier = Modifier.fillMaxWidth()
    )
}


@Composable
fun ExercisePhotoGallery(
    photos: List<ExercisePhoto>,
    onAddClick: () -> Unit,
    onRemove: (photoId: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Fotos",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onAddClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar foto",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(photos, key = { it.id }) { photo ->
                Box(modifier = Modifier.size(84.dp)) {
                    AsyncImage(
                        model = photo.uri.toUri(),
                        contentDescription = "Foto do exercício",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                    IconButton(
                        onClick = { onRemove(photo.id) },
                        modifier = Modifier
                            .size(22.dp)
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remover foto",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        if (photos.isEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Sem fotos. Toca em + para adicionar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


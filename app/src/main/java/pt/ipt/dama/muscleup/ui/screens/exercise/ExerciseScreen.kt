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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import kotlin.math.abs

/** Tolerância (em graus) para considerar o ângulo atual "correto" face ao valor guardado. */
private const val ANGLE_TOLERANCE_DEGREES = 2f

/** Converte texto para ângulo aceitando tanto "," como "." como separador decimal (PT usa vírgula). */
private fun parseAngle(text: String): Float? = text.trim().replace(',', '.').toFloatOrNull()


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
                    .verticalScroll(rememberScrollState())
                    .imePadding()
            ) {
                ExercisePhotoGallery(
                    photos = photos,
                    onAddClick = { showPhotoDialog = true },
                    onRemove = { photoId -> viewModel.removePhoto(photoId) }
                )
                HorizontalDivider()

                // Ecrã único, ordenado pelo fluxo real de uso no ginásio:
                // 1) configurar a máquina, 2) decidir carga/reps, 3) registar a série.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MachineConfigSection(
                        exercise = currentExercise,
                        onAddConfig = { name, description, angle ->
                            viewModel.addMachineConfig(name, description, angle)
                        },
                        onRemoveConfig = { configId -> viewModel.removeMachineConfig(configId) }
                    )

                    TargetSummarySection(
                        exercise = currentExercise,
                        historySessions = historySessions,
                        personalRecord = personalRecord,
                        onAddPredefinedSet = { reps, weightKg, durationSeconds ->
                            viewModel.addPredefinedSet(reps, weightKg, durationSeconds)
                        },
                        onRemovePredefinedSet = { setId -> viewModel.removePredefinedSet(setId) }
                    )

                    RegisterSetSection(
                        currentSessionSets = currentSessionSets,
                        onAddSet = { reps, weightKg, durationSeconds ->
                            viewModel.addRecordedSet(reps, weightKg, durationSeconds)
                        },
                        onRemoveSet = { setId -> viewModel.removeRecordedSet(setId) },
                        onFinalize = { viewModel.finalizeSession() },
                        onClear = { viewModel.clearSession() }
                    )
                }
            }
        }
    }
}

/** Card com título usado para cada secção do ecrã de exercício. */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

/**
 * Configuração da máquina (assento, encosto, ângulo, etc.). É a primeira secção
 * porque é a primeira coisa que se verifica fisicamente ao sentar na máquina.
 */
@Composable
fun MachineConfigSection(
    exercise: Exercise,
    onAddConfig: (name: String, description: String, angleDegrees: Float?) -> Unit,
    onRemoveConfig: (configId: String) -> Unit
) {
    var isEditing by rememberSaveable(exercise.id) { mutableStateOf(false) }
    var nameInput by rememberSaveable(exercise.id) { mutableStateOf("") }
    var descriptionInput by rememberSaveable(exercise.id) { mutableStateOf("") }
    var angleInput by rememberSaveable(exercise.id) { mutableStateOf("") }
    var checkingConfigId by rememberSaveable(exercise.id) { mutableStateOf<String?>(null) }

    val angleText = angleInput.trim()
    val angleValue = if (angleText.isBlank()) null else parseAngle(angleText)
    val hasInvalidAngle = angleText.isNotBlank() && angleValue == null
    val hasDescriptionOrAngle = descriptionInput.trim().isNotBlank() || angleValue != null
    val isFormValid = nameInput.trim().isNotBlank() && hasDescriptionOrAngle && !hasInvalidAngle

    SectionCard(title = "Configuração da Máquina") {
        if (exercise.machineConfigs.isEmpty()) {
            Text("Sem configurações definidas.", style = MaterialTheme.typography.bodySmall)
        } else {
            exercise.machineConfigs.forEach { config ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(config.name, fontWeight = FontWeight.SemiBold)
                        if (config.description.isNotBlank()) {
                            Text(config.description, style = MaterialTheme.typography.bodySmall)
                        }
                        config.angleDegrees?.let { angle ->
                            Text(
                                text = "Ângulo: ${"%.1f".format(angle)}°",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (config.angleDegrees != null) {
                        TextButton(onClick = { checkingConfigId = config.id }) { Text("Verificar") }
                    }
                    if (isEditing) {
                        TextButton(onClick = { onRemoveConfig(config.id) }) { Text("Remover") }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (!isEditing) {
            TextButton(onClick = { isEditing = true }) {
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
                label = { Text("Descrição (opcional)") },
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
    }

    val configBeingChecked = exercise.machineConfigs.find { it.id == checkingConfigId }
    val targetAngle = configBeingChecked?.angleDegrees
    if (configBeingChecked != null && targetAngle != null) {
        AngleCheckDialog(
            configName = configBeingChecked.name,
            targetAngle = targetAngle,
            onDismiss = { checkingConfigId = null }
        )
    }
}

/**
 * Diálogo que compara, em tempo real, o ângulo lido pelo acelerómetro com o
 * ângulo guardado numa configuração de máquina — para não teres de decorar
 * nem adivinhar o valor sempre que te sentas.
 */
@Composable
fun AngleCheckDialog(
    configName: String,
    targetAngle: Float,
    onDismiss: () -> Unit
) {
    val inclinometerViewModel: InclinometerViewModel = viewModel()
    val liveAngle by inclinometerViewModel.angleDegrees.collectAsState()

    DisposableEffect(Unit) {
        inclinometerViewModel.start()
        onDispose { inclinometerViewModel.stop() }
    }

    val diff = liveAngle - targetAngle
    val withinTolerance = abs(diff) <= ANGLE_TOLERANCE_DEGREES
    val successColor = Color(0xFF4CAF50)
    val angleColor = if (withinTolerance) successColor else MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Verificar ângulo", textAlign = TextAlign.Center)
                Text(
                    text = configName,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            if (!inclinometerViewModel.isSensorAvailable) {
                Text("Sensor de inclinação não disponível neste dispositivo.")
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Alvo: ${"%.1f".format(targetAngle)}°",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${"%.1f".format(liveAngle)}°",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = angleColor
                    )
                    if (!withinTolerance) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val direction = if (diff > 0) "Baixa" else "Sobe"
                        Text(
                            text = "$direction ${"%.1f".format(abs(diff))}°",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

/**
 * Objetivo desta série: junta a meta pré-definida, a última execução e o
 * recorde pessoal — a decisão de "aumento ou mantenho a carga" é uma decisão
 * só, por isso a informação está toda junta em vez de espalhada por 3 tabs.
 */
@Composable
fun TargetSummarySection(
    exercise: Exercise,
    historySessions: List<ExerciseHistorySession>,
    personalRecord: ExercisePersonalRecord,
    onAddPredefinedSet: (reps: Int, weightKg: Float?, durationSeconds: Int?) -> Unit,
    onRemovePredefinedSet: (setId: String) -> Unit
) {
    var isEditing by rememberSaveable(exercise.id) { mutableStateOf(exercise.sets.isEmpty()) }
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

    val formatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val lastSession = historySessions.firstOrNull()

    SectionCard(title = "Objetivo desta série") {
        Text("Meta", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        if (exercise.sets.isEmpty()) {
            Text("Sem meta definida.", style = MaterialTheme.typography.bodySmall)
        } else {
            exercise.sets.forEachIndexed { index, set ->
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
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (isEditing) {
                        TextButton(onClick = { onRemovePredefinedSet(set.id) }) { Text("Remover") }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        Text("Última vez", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        if (lastSession == null) {
            Text("Sem sessões anteriores.", style = MaterialTheme.typography.bodySmall)
        } else {
            val label = lastSession.finishedAt?.let { formatter.format(Date(it)) } ?: "-"
            Text("Sessão de $label", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            lastSession.sets.sortedBy { it.setOrder }.forEach { set ->
                Text(
                    text = buildString {
                        append("Série ${set.setOrder}: ${set.reps} reps")
                        if (set.weightKg > 0f) append(" . ${set.weightKg}kg")
                        if (set.durationSeconds > 0) append(" . ${set.durationSeconds}s")
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        Text("PR (Recorde Pessoal)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = personalRecord.maxWeightKg?.let { weight ->
                val repsLabel = personalRecord.maxWeightReps?.let { " x ${it} reps" }.orEmpty()
                "Maior peso: ${weight}kg${repsLabel}"
            } ?: "Maior peso: sem registos",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = personalRecord.maxDurationSeconds?.let { "Maior tempo: ${it}s" } ?: "Maior tempo: sem registos",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (!isEditing) {
            TextButton(onClick = { isEditing = true }) {
                Text("Editar meta")
            }
        } else {
            Text(
                text = if (exercise.sets.isEmpty()) "Configurar pré-definição" else "Editar pré-definição",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onAddPredefinedSet(reps, weightKg, durationSeconds)
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
    }
}

/** Registo em tempo real das séries desta sessão — a ação principal, sempre visível. */
@Composable
fun RegisterSetSection(
    currentSessionSets: List<SessionExerciseSet>,
    onAddSet: (reps: Int, weightKg: Float?, durationSeconds: Int?) -> Unit,
    onRemoveSet: (setId: String) -> Unit,
    onFinalize: () -> Unit,
    onClear: () -> Unit
) {
    var repsInput by rememberSaveable { mutableStateOf("") }
    var weightInput by rememberSaveable { mutableStateOf("") }
    var timeInput by rememberSaveable { mutableStateOf("") }

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

    SectionCard(title = "Registar Série") {
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
            { Text("Ângulo inválido. Ex: 30 ou 30,5") }
        } else null,
        trailingIcon = if (inclinometerViewModel.isSensorAvailable) {
            {
                TextButton(onClick = { onValueChange("%.1f".format(Locale.US, liveAngle)) }) {
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


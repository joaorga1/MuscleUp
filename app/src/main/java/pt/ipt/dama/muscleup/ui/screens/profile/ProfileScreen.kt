package pt.ipt.dama.muscleup.ui.screens.profile

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import pt.ipt.dama.muscleup.R
import pt.ipt.dama.muscleup.ui.components.AppTopBar
import pt.ipt.dama.muscleup.util.rememberImageModel
import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable
fun ProfileScreen(
    navController: NavController,
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val profilePhotoUri by viewModel.profilePhotoUri.collectAsState()

    val context = LocalContext.current
    var showPhotoDialog by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var isEditingName by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(viewModel.userName) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { snackbarHostState.showSnackbar(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) pendingCameraUri?.let { viewModel.saveProfilePhoto(it.toString()) }
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
            viewModel.saveProfilePhoto(uri.toString())
        }
    }

    val initials = viewModel.userName
        .trim().split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifBlank { "?" }

    // Resets when the URI changes (ex: após upload bem-sucedido, força nova tentativa)
    var imageLoadFailed by remember(profilePhotoUri) { mutableStateOf(false) }

    // Diálogo foto
    if (showPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false },
            title = { Text(stringResource(R.string.content_desc_profile_photo)) },
            text = {
                Column {
                    TextButton(
                        onClick = { showPhotoDialog = false; launchCamera() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.action_camera)) }
                    TextButton(
                        onClick = {
                            showPhotoDialog = false
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.action_gallery)) }
                    if (profilePhotoUri != null) {
                        TextButton(
                            onClick = { showPhotoDialog = false; viewModel.removeProfilePhoto() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.content_desc_remove_photo), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.profile_title),
                showBackButton = true,
                onBackClick = { navController.popBackStack() },
                onLogoutClick = onLogout
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ─── Avatar editável ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clickable { showPhotoDialog = true }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePhotoUri != null && !imageLoadFailed) {
                        AsyncImage(
                            model = rememberImageModel(profilePhotoUri),
                            contentDescription = stringResource(R.string.content_desc_profile_photo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            onError = { imageLoadFailed = true }
                        )
                    } else {
                        Text(
                            text = initials,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.content_desc_edit_photo),
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // ─── Informação do utilizador ──────────────────────────────
            Text(
                text = stringResource(R.string.profile_section_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Nome (editável)
            if (isEditingName) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(stringResource(R.string.field_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.saveName(nameInput)
                            isEditingName = false
                        },
                        enabled = nameInput.trim().isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.action_save)) }
                    TextButton(
                        onClick = {
                            nameInput = viewModel.userName
                            isEditingName = false
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.action_cancel)) }
                }
            } else {
                ProfileInfoRow(
                    label = stringResource(R.string.field_name),
                    value = viewModel.userName.ifBlank { "—" },
                    onEdit = { nameInput = viewModel.userName; isEditingName = true }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Email (só leitura — é o identificador da conta)
            ProfileInfoRow(label = stringResource(R.string.field_email), value = viewModel.userEmail.ifBlank { "—" })

            Spacer(modifier = Modifier.height(40.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showPasswordDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.profile_change_password))
                }
                Button(
                    onClick = onLogout,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.profile_logout_button))
                }
            }
        }
    }

    // Diálogo de alterar password
    if (showPasswordDialog) {
        var currentPw by remember { mutableStateOf("") }
        var newPw by remember { mutableStateOf("") }
        var confirmPw by remember { mutableStateOf("") }
        val passwordError by viewModel.passwordDialogError.collectAsState()

        LaunchedEffect(Unit) {
            viewModel.passwordSuccess.collect {
                showPasswordDialog = false
                viewModel.clearPasswordError()
            }
        }

        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                viewModel.clearPasswordError()
            },
            title = { Text(stringResource(R.string.profile_change_password)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentPw,
                        onValueChange = { currentPw = it; viewModel.clearPasswordError() },
                        label = { Text(stringResource(R.string.profile_field_current_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPw,
                        onValueChange = { newPw = it; viewModel.clearPasswordError() },
                        label = { Text(stringResource(R.string.profile_field_new_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPw,
                        onValueChange = { confirmPw = it; viewModel.clearPasswordError() },
                        label = { Text(stringResource(R.string.profile_field_confirm_new_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (passwordError != null) {
                        Text(
                            text = passwordError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.changePassword(currentPw, newPw, confirmPw) }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordDialog = false
                    viewModel.clearPasswordError()
                }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String,
    onEdit: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        if (onEdit != null) {
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.content_desc_edit_field_prefix) + label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

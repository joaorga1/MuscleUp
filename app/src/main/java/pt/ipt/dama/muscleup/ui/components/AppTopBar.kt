package pt.ipt.dama.muscleup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import pt.ipt.dama.muscleup.R
import pt.ipt.dama.muscleup.util.rememberImageModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    showAvatar: Boolean = false,
    userName: String = "",
    profilePhotoUri: String? = null,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onAppInfoClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_desc_back))
                }
            }
        },
        actions = {
            if (showAvatar) {
                UserAvatar(
                    userName = userName,
                    profilePhotoUri = profilePhotoUri,
                    onProfileClick = onProfileClick,
                    onSettingsClick = onSettingsClick,
                    onAppInfoClick = onAppInfoClick,
                    onLogoutClick = onLogoutClick
                )
            }
        }
    )
}

/** Avatar do utilizador, mostrando a fotografia de perfil ou a inicial do nome, com menu de ações associado. */
@Composable
fun UserAvatar(
    userName: String,
    modifier: Modifier = Modifier,
    profilePhotoUri: String? = null,
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onAppInfoClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
) {
    val initial = userName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    var expanded by remember { mutableStateOf(false) }
    // O estado de falha na imagem é reiniciado quando o URI muda, por exemplo após
    // envio ou remoção da fotografia de perfil.
    var imageLoadFailed by remember(profilePhotoUri) { mutableStateOf(false) }

    Box(modifier = modifier.padding(end = 12.dp)) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable { expanded = true },
            contentAlignment = Alignment.Center
        ) {
            if (profilePhotoUri != null && !imageLoadFailed) {
                AsyncImage(
                    model = rememberImageModel(profilePhotoUri),
                    contentDescription = stringResource(R.string.content_desc_profile_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    onError = { imageLoadFailed = true }
                )
            } else {
                Text(
                    text = initial,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_profile)) },
                onClick = { expanded = false; onProfileClick() }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_settings)) },
                onClick = { expanded = false; onSettingsClick() }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_app_info)) },
                onClick = { expanded = false; onAppInfoClick() }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_logout)) },
                onClick = { expanded = false; onLogoutClick() }
            )
        }
    }
}
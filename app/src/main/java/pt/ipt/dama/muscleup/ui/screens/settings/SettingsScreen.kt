package pt.ipt.dama.muscleup.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import pt.ipt.dama.muscleup.R
import pt.ipt.dama.muscleup.ui.components.AppTopBar
import pt.ipt.dama.muscleup.ui.theme.ThemeMode

/** Passo 10.1 — Ecrã de Definições: tema, idioma e logout. */
@Composable
fun SettingsScreen(
    navController: NavController,
    onLogout: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    var themeMode by remember { mutableStateOf(viewModel.currentThemeMode) }
    var languageTag by remember { mutableStateOf(viewModel.currentLanguageTag()) }
    val activity = LocalActivity.current
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { snackbarHostState.showSnackbar(it) }
    }

    fun changeLanguage(tag: String) {
        languageTag = tag
        viewModel.setLanguage(tag)
        // A app não usa AppCompatActivity, por isso a mudança de idioma só é aplicada
        // aos recursos em attachBaseContext() — é preciso recriar a Activity para ter efeito.
        activity?.recreate()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings_title),
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
                .padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_section_appearance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = themeMode == ThemeMode.SYSTEM,
                    onClick = { themeMode = ThemeMode.SYSTEM; viewModel.setThemeMode(ThemeMode.SYSTEM) },
                    label = { Text(stringResource(R.string.settings_theme_system)) }
                )
                FilterChip(
                    selected = themeMode == ThemeMode.LIGHT,
                    onClick = { themeMode = ThemeMode.LIGHT; viewModel.setThemeMode(ThemeMode.LIGHT) },
                    label = { Text(stringResource(R.string.settings_theme_light)) }
                )
                FilterChip(
                    selected = themeMode == ThemeMode.DARK,
                    onClick = { themeMode = ThemeMode.DARK; viewModel.setThemeMode(ThemeMode.DARK) },
                    label = { Text(stringResource(R.string.settings_theme_dark)) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.settings_section_language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = languageTag == "pt",
                    onClick = { changeLanguage("pt") },
                    label = { Text(stringResource(R.string.settings_language_pt)) }
                )
                FilterChip(
                    selected = languageTag == "en",
                    onClick = { changeLanguage("en") },
                    label = { Text(stringResource(R.string.settings_language_en)) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.settings_section_sync),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (pendingSyncCount > 0)
                    stringResource(R.string.settings_sync_pending_count, pendingSyncCount)
                else
                    stringResource(R.string.settings_sync_all_synced),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { viewModel.forceSync() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_force_sync))
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.settings_section_account),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.settings_logout))
            }
        }
    }
}







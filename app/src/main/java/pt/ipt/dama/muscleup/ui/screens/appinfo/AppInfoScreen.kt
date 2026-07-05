package pt.ipt.dama.muscleup.ui.screens.appinfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import pt.ipt.dama.muscleup.R
import pt.ipt.dama.muscleup.ui.components.AppTopBar

/**
 * Ecrã de informação da aplicação.
 *
 * Apresenta os metadados académicos do trabalho: identificação do curso e unidade curricular,
 * autores, bibliotecas/frameworks de terceiros utilizadas e credenciais de acesso de teste.
 *
 * @param navController Controlador de navegação usado para regressar ao ecrã anterior.
 */
@Composable
fun AppInfoScreen(navController: NavController) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.appinfo_title),
                showBackButton = true,
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─── Informação académica ─────────────────────────────────
            AppInfoCard(title = stringResource(R.string.appinfo_section_academic)) {
                AppInfoRow(stringResource(R.string.appinfo_label_institute),   stringResource(R.string.appinfo_value_institute))
                AppInfoRow(stringResource(R.string.appinfo_label_course),      stringResource(R.string.appinfo_value_course))
                AppInfoRow(stringResource(R.string.appinfo_label_unit),        stringResource(R.string.appinfo_value_unit))
                AppInfoRow(stringResource(R.string.appinfo_label_year),        stringResource(R.string.appinfo_value_year))
            }

            // ─── Autores ──────────────────────────────────────────────
            AppInfoCard(title = stringResource(R.string.appinfo_section_authors)) {
                AppInfoRow(stringResource(R.string.appinfo_label_author1_number), stringResource(R.string.appinfo_value_author1_number))
                AppInfoRow(stringResource(R.string.appinfo_label_author1_name),   stringResource(R.string.appinfo_value_author1_name))
            }

            // ─── Bibliotecas e frameworks ─────────────────────────────
            AppInfoCard(title = stringResource(R.string.appinfo_section_libraries)) {
                val libs = listOf(
                    Triple("Jetpack Compose",        "2026.06.01 (BOM)",  "https://developer.android.com/jetpack/compose"),
                    Triple("Room",                   "2.8.4",             "https://developer.android.com/training/data-storage/room"),
                    Triple("Retrofit",               "3.0.0",             "https://square.github.io/retrofit/"),
                    Triple("OkHttp",                 "5.4.0",             "https://square.github.io/okhttp/"),
                    Triple("Coil",                   "2.7.0",             "https://coil-kt.github.io/coil/"),
                    Triple("Navigation Compose",     "2.9.8",             "https://developer.android.com/jetpack/compose/navigation"),
                    Triple("WorkManager",            "2.11.2",            "https://developer.android.com/topic/libraries/architecture/workmanager"),
                    Triple("JWT Decode (Auth0)",     "2.0.2",             "https://github.com/auth0/JWTDecode.Android"),
                    Triple("KSP",                    "2.3.8",             "https://github.com/google/ksp"),
                )
                libs.forEachIndexed { index, (name, version, source) ->
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.appinfo_lib_version, version),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = source,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/** Cartão com título de secção e conteúdo variável. */
@Composable
private fun AppInfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

/** Par rótulo–valor dentro de uma secção. */
@Composable
private fun AppInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.55f)
        )
    }
}







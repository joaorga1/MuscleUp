package pt.ipt.dama.muscleup.ui.screens.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import pt.ipt.dama.muscleup.ui.components.AppTopBar

@Composable
fun ProfileScreen(
    navController: NavController,
    onLogout: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Perfil",
                showBackButton = true,
                onBackClick = { navController.popBackStack() },
                onLogoutClick = onLogout
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text("Em construção")
        }
    }
}

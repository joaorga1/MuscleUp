package pt.ipt.dama.muscleup.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark como padrão — usado na maioria dos casos
private val DarkColorScheme = darkColorScheme(
    primary = Orange500,
    onPrimary = White90,
    secondary = Amber500,
    onSecondary = White90,
    background = Dark900,
    onBackground = White90,
    surface = Dark800,
    onSurface = White90,
    surfaceVariant = Dark800,
    onSurfaceVariant = White60,
    error = ErrorRed,
    onError = Dark900
)

private val LightColorScheme = lightColorScheme(
    primary = Orange500,
    onPrimary = White90,
    secondary = Amber500,
    onSecondary = Dark900,
    background = White90,
    onBackground = Dark900,
    surface = Color(0xFFFFFFFF),
    onSurface = Dark900,
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = White60,
    error = ErrorRed,
    onError = White90
)

@Composable
fun MuscleUpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
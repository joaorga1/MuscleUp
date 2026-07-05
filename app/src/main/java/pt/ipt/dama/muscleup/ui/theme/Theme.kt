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
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = OrangeContainerDark,
    onPrimaryContainer = OnOrangeContainerDark,
    secondary = Amber500,
    onSecondary = Dark900,
    secondaryContainer = AmberContainerDark,
    onSecondaryContainer = OnAmberContainerDark,
    tertiary = Amber300,
    onTertiary = Dark900,
    background = Dark900,
    onBackground = White90,
    surface = Dark800,
    onSurface = White90,
    surfaceVariant = Dark800,
    onSurfaceVariant = White60,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorRed,
    onError = Dark900,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    inverseSurface = White90,
    inverseOnSurface = Dark900,
    inversePrimary = Orange300,
    surfaceTint = Orange500
)

private val LightColorScheme = lightColorScheme(
    primary = Orange500,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = OrangeContainerLight,
    onPrimaryContainer = OnOrangeContainerLight,
    secondary = Amber500,
    onSecondary = Dark900,
    secondaryContainer = AmberContainerLight,
    onSecondaryContainer = OnAmberContainerLight,
    tertiary = Orange300,
    onTertiary = Dark900,
    background = Color(0xFFFFFFFF),
    onBackground = Dark900,
    surface = Color(0xFFFFFFFF),
    onSurface = Dark900,
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorRedLight,
    onError = Color(0xFFFFFFFF),
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    inverseSurface = Dark800,
    inverseOnSurface = White90,
    inversePrimary = Orange300,
    surfaceTint = Orange500
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
package pt.ipt.dama.muscleup.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Passo 10.1 — modo de tema escolhido pelo utilizador nas Definições. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromName(value: String?): ThemeMode =
            entries.find { it.name == value } ?: SYSTEM
    }
}

/**
 * Passo 10.1 — estado do tema em memória, observável pelo Compose (`mutableStateOf`).
 * Alterado nas Definições, lido pelo `MainActivity` para decidir o `darkTheme` do
 * `MuscleUpTheme` — muda a app inteira em tempo real, sem reiniciar a Activity.
 * A persistência entre arranques é feita à parte por [pt.ipt.dama.muscleup.data.session.ThemePreferences].
 */
object ThemeState {
    var mode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    fun select(newMode: ThemeMode) {
        mode = newMode
    }
}




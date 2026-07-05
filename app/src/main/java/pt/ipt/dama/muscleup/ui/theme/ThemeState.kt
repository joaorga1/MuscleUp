package pt.ipt.dama.muscleup.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Modo de tema escolhido pelo utilizador. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        /** Converte o nome guardado em preferências para o valor correspondente. */
        fun fromName(value: String?): ThemeMode =
            entries.find { it.name == value } ?: SYSTEM
    }
}

/**
 * Estado do tema em memória, observável pelo Compose.
 * Alterado nas Definições; lido pela MainActivity para aplicar o tema em tempo real.
 * A persistência entre arranques é feita por [pt.ipt.dama.muscleup.data.session.ThemePreferences].
 */
object ThemeState {
    var mode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    /** Atualiza o tema. */
    fun select(newMode: ThemeMode) {
        mode = newMode
    }
}



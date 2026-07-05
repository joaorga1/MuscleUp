package pt.ipt.dama.muscleup.util

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * A API devolve a fotografia de perfil como uma data URI em base64, enquanto outras origens, como uma cache antiga
 * ou envios locais, podem gerar um caminho ou URI de ficheiro normal e por isso é preciso garantir que o AsyncImage do Coil consegue lidar com ambos os casos.
 */
fun toImageModel(uriOrDataUrl: String?): Any? {
    if (uriOrDataUrl.isNullOrBlank()) return null

    if (uriOrDataUrl.startsWith("data:image", ignoreCase = true)) {
        val base64Part = uriOrDataUrl.substringAfter(",", missingDelimiterValue = "")
        if (base64Part.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    return uriOrDataUrl
}

/** Versão com remember para evitar recomputações a cada recomposição. */
@Composable
fun rememberImageModel(uriOrDataUrl: String?): Any? =
    remember(uriOrDataUrl) { toImageModel(uriOrDataUrl) }



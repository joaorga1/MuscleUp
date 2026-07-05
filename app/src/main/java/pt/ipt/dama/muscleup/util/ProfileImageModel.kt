package pt.ipt.dama.muscleup.util

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * A API devolve a foto de perfil como uma "data URI" em base64
 * (ex: "data:image/jpeg;base64,/9j/4AAQ..."), enquanto outras origens
 * (ex: cache antiga ou uploads locais) podem gerar um caminho/URI de
 * ficheiro "normal" (file://, content://, http(s)://).
 *
 * O Coil não sabe carregar strings "data:" diretamente, por isso é
 * preciso descodificar o base64 para um Bitmap antes de o passar
 * ao AsyncImage. Para os outros formatos, a própria string serve de
 * modelo (Coil já sabe tratar file/content/http).
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

/**
 * Versão "remember" para usar diretamente em Composables, evitando
 * descodificar o base64 em cada recomposição.
 */
@Composable
fun rememberImageModel(uriOrDataUrl: String?): Any? =
    remember(uriOrDataUrl) { toImageModel(uriOrDataUrl) }



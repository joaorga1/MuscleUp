package pt.ipt.dama.muscleup.data.session

import android.content.Context
import androidx.core.content.edit

private const val PREFS_NAME = "muscleup_session"
private const val KEY_EMAIL = "session_email"
private const val KEY_NAME = "session_name"
private const val KEY_TIMESTAMP = "session_timestamp"
private const val SESSION_DURATION_MS = 365L * 24 * 60 * 60 * 1000

/** Persiste a sessão local do utilizador em SharedPreferences, válida durante um ano. */
class SessionPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Guarda os dados da sessão. */
    fun save(email: String, name: String) {
        prefs.edit {
            putString(KEY_EMAIL, email)
                .putString(KEY_NAME, name)
                .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
        }
    }

    /** Remove os dados da sessão. */
    fun clear() {
        prefs.edit { clear() }
    }

    /** Devolve o email guardado, se existir. */
    fun getSavedEmail(): String? = prefs.getString(KEY_EMAIL, null)

    /** Devolve o nome guardado, ou uma cadeia vazia se não existir. */
    fun getSavedName(): String = prefs.getString(KEY_NAME, "") ?: ""

    /** Indica se a sessão ainda é válida (existe e tem menos de um ano). */
    fun isValid(): Boolean {
        val email = getSavedEmail() ?: return false
        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)
        val age = System.currentTimeMillis() - timestamp
        return email.isNotBlank() && age < SESSION_DURATION_MS
    }
}



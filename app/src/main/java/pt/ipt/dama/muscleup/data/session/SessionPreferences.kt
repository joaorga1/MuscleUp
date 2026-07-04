package pt.ipt.dama.muscleup.data.session

import android.content.Context
import androidx.core.content.edit

private const val PREFS_NAME = "muscleup_session"
private const val KEY_EMAIL = "session_email"
private const val KEY_NAME = "session_name"
private const val KEY_TIMESTAMP = "session_timestamp"
private const val SESSION_DURATION_MS = 365L * 24 * 60 * 60 * 1000 // 1 ano em ms

class SessionPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(email: String, name: String) {
        prefs.edit {
            putString(KEY_EMAIL, email)
                .putString(KEY_NAME, name)
                .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
        }
    }

    fun clear() {
        prefs.edit { clear() }
    }

    fun getSavedEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun getSavedName(): String = prefs.getString(KEY_NAME, "") ?: ""

    fun isValid(): Boolean {
        val email = getSavedEmail() ?: return false
        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)
        val age = System.currentTimeMillis() - timestamp
        return email.isNotBlank() && age < SESSION_DURATION_MS
    }
}



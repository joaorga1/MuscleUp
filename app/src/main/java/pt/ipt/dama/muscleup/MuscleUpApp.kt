package pt.ipt.dama.muscleup

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pt.ipt.dama.muscleup.data.local.AppDatabase
import pt.ipt.dama.muscleup.data.session.SessionPreferences
import pt.ipt.dama.muscleup.data.session.UserSession

class MuscleUpApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val sessionPreferences: SessionPreferences by lazy { SessionPreferences(this) }

    override fun onCreate() {
        super.onCreate()
        restoreSessionIfValid()
    }

    private fun restoreSessionIfValid() {
        if (!sessionPreferences.isValid()) {
            sessionPreferences.clear()
            return
        }
        val email = sessionPreferences.getSavedEmail() ?: return
        val name = sessionPreferences.getSavedName()
        // Usa o nome guardado nas preferências para resposta imediata na UI
        UserSession.set(name = name, email = email)
        // Verifica em background se o utilizador ainda existe na BD
        CoroutineScope(Dispatchers.IO).launch {
            val user = database.userDao().findByEmail(email)
            if (user == null) {
                UserSession.clear()
                sessionPreferences.clear()
            }
        }
    }
}

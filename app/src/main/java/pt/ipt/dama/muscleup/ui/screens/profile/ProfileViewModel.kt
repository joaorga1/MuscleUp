package pt.ipt.dama.muscleup.ui.screens.profile

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.ipt.dama.muscleup.data.local.AppDatabase
import pt.ipt.dama.muscleup.data.session.UserSession
import java.io.File

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val userDao = db.userDao()

    val userName: String get() = UserSession.currentUserName
    val userEmail: String get() = UserSession.currentUserEmail

    val profilePhotoUri: StateFlow<String?> = userDao
        .getUserByEmailFlow(userEmail)
        .map { it?.profilePhotoUri?.ifBlank { null } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    fun saveProfilePhoto(uri: String) {
        viewModelScope.launch {
            try {
                userDao.updateProfilePhotoUri(userEmail, uri)
            } catch (_: Exception) {
                _uiEvent.emit("Erro ao guardar foto. Tenta novamente.")
            }
        }
    }

    fun removeProfilePhoto() {
        viewModelScope.launch {
            try {
                userDao.updateProfilePhotoUri(userEmail, "")
            } catch (_: Exception) {
                _uiEvent.emit("Erro ao remover foto. Tenta novamente.")
            }
        }
    }

    fun createPhotoUri(context: Context): Uri {
        val file = File(context.filesDir, "profile_photo.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun saveName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            viewModelScope.launch { _uiEvent.emit("O nome não pode estar vazio") }
            return
        }
        viewModelScope.launch {
            try {
                userDao.updateName(userEmail, trimmed)
                UserSession.set(trimmed, userEmail, UserSession.currentUserId)
            } catch (_: Exception) {
                _uiEvent.emit("Erro ao guardar nome. Tenta novamente.")
            }
        }
    }
}

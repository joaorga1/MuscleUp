package pt.ipt.dama.muscleup.ui.screens.profile

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _passwordDialogError = MutableStateFlow<String?>(null)
    val passwordDialogError: StateFlow<String?> = _passwordDialogError.asStateFlow()

    private val _passwordSuccess = MutableSharedFlow<Unit>()
    val passwordSuccess: SharedFlow<Unit> = _passwordSuccess.asSharedFlow()

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

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            _passwordDialogError.value = null
            when {
                currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank() ->
                    _passwordDialogError.value = "Preenche todos os campos"
                newPassword.length < 6 ->
                    _passwordDialogError.value = "A nova password deve ter pelo menos 6 caracteres"
                newPassword == currentPassword ->
                    _passwordDialogError.value = "A nova password não pode ser igual à atual"
                newPassword != confirmPassword ->
                    _passwordDialogError.value = "As passwords não coincidem"
                else -> {
                    try {
                        val user = userDao.findByEmail(userEmail)
                        if (user == null || user.passwordHash != hashPassword(currentPassword)) {
                            _passwordDialogError.value = "Password atual incorreta"
                        } else {
                            userDao.updatePassword(userEmail, hashPassword(newPassword))
                            _passwordSuccess.emit(Unit)
                            _uiEvent.emit("Password alterada com sucesso!")
                        }
                    } catch (_: Exception) {
                        _passwordDialogError.value = "Erro ao alterar password. Tenta novamente."
                    }
                }
            }
        }
    }

    fun clearPasswordError() {
        _passwordDialogError.value = null
    }

    private fun hashPassword(password: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(password.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

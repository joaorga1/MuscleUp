package pt.ipt.dama.muscleup.ui.screens.profile

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.ipt.dama.muscleup.MuscleUpApp
import pt.ipt.dama.muscleup.data.local.upsertMirror
import pt.ipt.dama.muscleup.data.remote.uriToMultipart
import pt.ipt.dama.muscleup.data.remote.RetrofitClient
import pt.ipt.dama.muscleup.data.remote.dto.ChangePasswordRequest
import pt.ipt.dama.muscleup.data.remote.dto.UpdateNameRequest
import pt.ipt.dama.muscleup.data.session.UserSession
import java.io.File
import java.io.IOException

/**
 * Passo 8.2 — Perfil real via API (GET/PATCH /users/me, PUT /users/me/password,
 * POST/DELETE /users/me/photo). O Room continua a ser usado só como cache
 * offline (leitura instantânea + funcionamento sem rede).
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MuscleUpApp
    private val apiService = app.apiService
    private val userDao = app.database.userDao()
    private val sessionPreferences = app.sessionPreferences

    val userName: String get() = UserSession.currentUserName
    val userEmail: String get() = UserSession.currentUserEmail

    private val _profilePhotoUri = MutableStateFlow<String?>(null)
    val profilePhotoUri: StateFlow<String?> = _profilePhotoUri.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    private val _passwordDialogError = MutableStateFlow<String?>(null)
    val passwordDialogError: StateFlow<String?> = _passwordDialogError.asStateFlow()

    private val _passwordSuccess = MutableSharedFlow<Unit>()
    val passwordSuccess: SharedFlow<Unit> = _passwordSuccess.asSharedFlow()

    init {
        viewModelScope.launch {
            // 1. Mostra já o cache local (Room) — resposta instantânea, funciona offline.
            userDao.findByEmail(userEmail)?.profilePhotoUri?.ifBlank { null }?.let {
                _profilePhotoUri.value = it
            }
            // 2. Confirma/atualiza com a API (fonte de verdade).
            refreshFromApi()
        }
    }

    private suspend fun refreshFromApi() {
        try {
            val response = apiService.getCurrentUser()
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                _profilePhotoUri.value = user.profilePhotoUri
                UserSession.set(user.name, user.email)
                sessionPreferences.save(email = user.email, name = user.name)
                userDao.upsertMirror(user.name, user.email, user.profilePhotoUri)
            }
        } catch (_: Exception) {
            // Sem rede: mantém o que já estava no cache local.
        }
    }

    fun saveProfilePhoto(uriString: String) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val part = uriToMultipart(context, uriString.toUri(), "photo")
                val response = apiService.uploadProfilePhoto(part)
                if (response.isSuccessful && response.body() != null) {
                    val newUri = response.body()!!.profilePhotoUri
                    _profilePhotoUri.value = newUri
                    userDao.upsertMirror(userName, userEmail, newUri)
                } else {
                    val error = RetrofitClient.parseError(response)
                    _uiEvent.emit(error.message)
                }
            } catch (_: IOException) {
                _uiEvent.emit("Sem ligação à internet. Tenta novamente.")
            } catch (_: Exception) {
                _uiEvent.emit("Erro ao guardar foto. Tenta novamente.")
            }
        }
    }

    fun removeProfilePhoto() {
        viewModelScope.launch {
            try {
                val response = apiService.deleteProfilePhoto()
                if (response.isSuccessful) {
                    _profilePhotoUri.value = null
                    userDao.upsertMirror(userName, userEmail, null)
                } else {
                    val error = RetrofitClient.parseError(response)
                    _uiEvent.emit(error.message)
                }
            } catch (_: IOException) {
                _uiEvent.emit("Sem ligação à internet. Tenta novamente.")
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
                val response = apiService.updateName(UpdateNameRequest(trimmed))
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    UserSession.set(user.name, user.email)
                    sessionPreferences.save(email = user.email, name = user.name)
                    userDao.upsertMirror(user.name, user.email, user.profilePhotoUri)
                } else {
                    val error = RetrofitClient.parseError(response)
                    _uiEvent.emit(error.message)
                }
            } catch (_: IOException) {
                _uiEvent.emit("Sem ligação à internet. Tenta novamente.")
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
                        val response = apiService.changePassword(
                            ChangePasswordRequest(currentPassword, newPassword, confirmPassword)
                        )
                        if (response.isSuccessful) {
                            _passwordSuccess.emit(Unit)
                            _uiEvent.emit(response.body()?.message ?: "Password alterada com sucesso!")
                        } else {
                            val error = RetrofitClient.parseError(response)
                            _passwordDialogError.value = error.message
                        }
                    } catch (_: IOException) {
                        _passwordDialogError.value = "Sem ligação à internet. Tenta novamente."
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
}

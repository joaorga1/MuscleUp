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
import pt.ipt.dama.muscleup.R
import pt.ipt.dama.muscleup.data.local.upsertMirror
import pt.ipt.dama.muscleup.data.remote.uriToMultipart
import pt.ipt.dama.muscleup.data.remote.PhotoTooLargeException
import pt.ipt.dama.muscleup.data.remote.RetrofitClient
import pt.ipt.dama.muscleup.data.remote.dto.ChangePasswordRequest
import pt.ipt.dama.muscleup.data.remote.dto.UpdateNameRequest
import pt.ipt.dama.muscleup.data.session.UserSession
import java.io.File
import java.io.IOException

/** ViewModel do ecrã de perfil: lê e atualiza os dados do utilizador via API. */
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
            userDao.findByEmail(userEmail)?.profilePhotoUri?.ifBlank { null }?.let {
                _profilePhotoUri.value = it
            }
            // Confirma e atualiza depois com a API, que é a fonte de verdade.
            refreshFromApi()
        }
    }

    /** Atualiza os dados do perfil a partir da API. */
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
            // Sem rede: mantém o que já estava na cópia local.
        }
    }

    /** Envia uma fotografia de perfil para a API. */
    fun saveProfilePhoto(uriString: String) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val part = try {
                uriToMultipart(context, uriString.toUri(), "photo")
            } catch (e: PhotoTooLargeException) {
                _uiEvent.emit(e.message ?: app.getString(R.string.exercise_error_save_photo))
                return@launch
            } catch (_: Exception) {
                _uiEvent.emit(app.getString(R.string.error_read_selected_file))
                return@launch
            }
            try {
                val response = apiService.uploadProfilePhoto(part)
                if (response.isSuccessful && response.body() != null) {
                    val newUri = response.body()!!.profilePhotoUri
                    _profilePhotoUri.value = newUri
                    userDao.upsertMirror(userName, userEmail, newUri)
                } else {
                    val error = RetrofitClient.parseError(response, app)
                    _uiEvent.emit(error.message)
                }
            } catch (_: IOException) {
                _uiEvent.emit(app.getString(R.string.error_no_internet))
            } catch (_: Exception) {
                _uiEvent.emit(app.getString(R.string.exercise_error_save_photo))
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
                    val error = RetrofitClient.parseError(response, app)
                    _uiEvent.emit(error.message)
                }
            } catch (_: IOException) {
                _uiEvent.emit(app.getString(R.string.error_no_internet))
            } catch (_: Exception) {
                _uiEvent.emit(app.getString(R.string.exercise_error_remove_photo))
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
            viewModelScope.launch { _uiEvent.emit(app.getString(R.string.profile_error_name_empty)) }
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
                    val error = RetrofitClient.parseError(response, app)
                    _uiEvent.emit(error.message)
                }
            } catch (_: IOException) {
                _uiEvent.emit(app.getString(R.string.error_no_internet))
            } catch (_: Exception) {
                _uiEvent.emit(app.getString(R.string.profile_error_save_name))
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            _passwordDialogError.value = null
            when {
                currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank() ->
                    _passwordDialogError.value = app.getString(R.string.error_fill_fields)
                newPassword.length < 6 ->
                    _passwordDialogError.value = app.getString(R.string.profile_error_password_too_short)
                newPassword == currentPassword ->
                    _passwordDialogError.value = app.getString(R.string.profile_error_password_same)
                newPassword != confirmPassword ->
                    _passwordDialogError.value = app.getString(R.string.error_passwords_dont_match)
                else -> {
                    try {
                        val response = apiService.changePassword(
                            ChangePasswordRequest(currentPassword, newPassword, confirmPassword)
                        )
                        if (response.isSuccessful) {
                            _passwordSuccess.emit(Unit)
                            _uiEvent.emit(response.body()?.message ?: app.getString(R.string.profile_password_changed_success))
                        } else {
                            val error = RetrofitClient.parseError(response, app)
                            _passwordDialogError.value = error.message
                        }
                    } catch (_: IOException) {
                        _passwordDialogError.value = app.getString(R.string.error_no_internet)
                    } catch (_: Exception) {
                        _passwordDialogError.value = app.getString(R.string.profile_error_change_password)
                    }
                }
            }
        }
    }

    /** Limpa a mensagem de erro do diálogo de alteração de palavra-passe. */
    fun clearPasswordError() {
        _passwordDialogError.value = null
    }
}

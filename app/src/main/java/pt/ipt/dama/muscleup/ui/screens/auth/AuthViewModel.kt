package pt.ipt.dama.muscleup.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.ipt.dama.muscleup.MuscleUpApp
import pt.ipt.dama.muscleup.R
import pt.ipt.dama.muscleup.data.local.upsertMirror
import pt.ipt.dama.muscleup.data.remote.RetrofitClient
import pt.ipt.dama.muscleup.data.remote.dto.AuthResponse
import pt.ipt.dama.muscleup.data.remote.dto.LoginRequest
import pt.ipt.dama.muscleup.data.remote.dto.RegisterRequest
import pt.ipt.dama.muscleup.data.session.UserSession
import java.io.IOException

/** Estados possíveis do ecrã de autenticação. */
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val userName: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

/** ViewModel de autenticação: login e registo via API. */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MuscleUpApp
    private val apiService = app.apiService
    private val tokenManager = app.tokenManager
    private val userDao = app.database.userDao()
    private val sessionPreferences = app.sessionPreferences

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    fun resetState() { _uiState.value = AuthUiState.Idle }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error(app.getString(R.string.error_fill_fields))
            return
        }
        if (!email.contains("@")) {
            _uiState.value = AuthUiState.Error(app.getString(R.string.error_invalid_email))
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val response = apiService.login(LoginRequest(email.trim(), password))
                if (response.isSuccessful && response.body() != null) {
                    onAuthSuccess(response.body()!!)
                } else {
                    val error = RetrofitClient.parseError(response, app)
                    _uiState.value = AuthUiState.Error(error.message)
                }
            } catch (_: IOException) {
                _uiState.value = AuthUiState.Error(app.getString(R.string.error_no_internet_retry))
            } catch (_: Exception) {
                _uiState.value = AuthUiState.Error(app.getString(R.string.error_unexpected))
            }
        }
    }

    /** Regista um novo utilizador na API, validando os campos antes de os enviar. */
    fun register(name: String, email: String, password: String, confirmPassword: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _uiState.value = AuthUiState.Error(app.getString(R.string.error_fill_fields))
            return
        }
        if (!email.contains("@")) {
            _uiState.value = AuthUiState.Error(app.getString(R.string.error_invalid_email))
            return
        }
        if (password != confirmPassword) {
            _uiState.value = AuthUiState.Error(app.getString(R.string.error_passwords_dont_match))
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val response = apiService.register(RegisterRequest(name.trim(), email.trim(), password))
                if (response.isSuccessful && response.body() != null) {
                    onAuthSuccess(response.body()!!)
                } else {
                    val error = RetrofitClient.parseError(response, app)
                    _uiState.value = AuthUiState.Error(error.message)
                }
            } catch (_: IOException) {
                _uiState.value = AuthUiState.Error(app.getString(R.string.error_no_internet_retry))
            } catch (_: Exception) {
                _uiState.value = AuthUiState.Error(app.getString(R.string.error_unexpected))
            }
        }
    }

    /** Guarda tokens e dados do utilizador após autenticação bem-sucedida. */
    private suspend fun onAuthSuccess(auth: AuthResponse) {
        tokenManager.saveTokens(auth.accessToken, auth.refreshToken)
        UserSession.set(name = auth.user.name, email = auth.user.email)
        sessionPreferences.save(email = auth.user.email, name = auth.user.name)
        userDao.upsertMirror(auth.user.name, auth.user.email, auth.user.profilePhotoUri)
        _uiState.value = AuthUiState.Success(auth.user.name)
    }
}



package pt.ipt.dama.muscleup.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.ipt.dama.muscleup.MuscleUpApp
import pt.ipt.dama.muscleup.data.local.upsertMirror
import pt.ipt.dama.muscleup.data.remote.RetrofitClient
import pt.ipt.dama.muscleup.data.remote.dto.AuthResponse
import pt.ipt.dama.muscleup.data.remote.dto.LoginRequest
import pt.ipt.dama.muscleup.data.remote.dto.RegisterRequest
import pt.ipt.dama.muscleup.data.session.UserSession
import java.io.IOException

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val userName: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

/**
 * Passo 8.2 — Autenticação real via API (POST /api/auth/login, /register).
 * O JWT (accessToken/refreshToken) é guardado pelo TokenManager e a sessão
 * local (1 ano) continua a ser gerida por SessionPreferences, para permitir
 * login automático offline.
 */
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
            _uiState.value = AuthUiState.Error("Preenche todos os campos")
            return
        }
        if (!email.contains("@")) {
            _uiState.value = AuthUiState.Error("Email inválido")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val response = apiService.login(LoginRequest(email.trim(), password))
                if (response.isSuccessful && response.body() != null) {
                    onAuthSuccess(response.body()!!)
                } else {
                    val error = RetrofitClient.parseError(response)
                    _uiState.value = AuthUiState.Error(error.message)
                }
            } catch (_: IOException) {
                _uiState.value = AuthUiState.Error("Sem ligação à internet. Verifica a tua rede e tenta novamente.")
            } catch (_: Exception) {
                _uiState.value = AuthUiState.Error("Erro inesperado. Tenta novamente.")
            }
        }
    }

    fun register(name: String, email: String, password: String, confirmPassword: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _uiState.value = AuthUiState.Error("Preenche todos os campos")
            return
        }
        if (!email.contains("@")) {
            _uiState.value = AuthUiState.Error("Email inválido")
            return
        }
        if (password != confirmPassword) {
            _uiState.value = AuthUiState.Error("As passwords não coincidem")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val response = apiService.register(RegisterRequest(name.trim(), email.trim(), password))
                if (response.isSuccessful && response.body() != null) {
                    onAuthSuccess(response.body()!!)
                } else {
                    val error = RetrofitClient.parseError(response)
                    _uiState.value = AuthUiState.Error(error.message)
                }
            } catch (_: IOException) {
                _uiState.value = AuthUiState.Error("Sem ligação à internet. Verifica a tua rede e tenta novamente.")
            } catch (_: Exception) {
                _uiState.value = AuthUiState.Error("Erro inesperado. Tenta novamente.")
            }
        }
    }

    private suspend fun onAuthSuccess(auth: AuthResponse) {
        tokenManager.saveTokens(auth.accessToken, auth.refreshToken)
        // userId local mantém-se = email (comportamento já existente), para não quebrar
        // o particionamento local de treinos/exercícios/sessões (Room) por utilizador.
        UserSession.set(name = auth.user.name, email = auth.user.email)
        sessionPreferences.save(email = auth.user.email, name = auth.user.name)
        // Cache local (offline) do perfil — não é fonte de verdade, só para leitura rápida/offline.
        userDao.upsertMirror(auth.user.name, auth.user.email, auth.user.profilePhotoUri)
        _uiState.value = AuthUiState.Success(auth.user.name)
    }
}

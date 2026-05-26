package pt.ipt.dama.muscleup.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.ipt.dama.muscleup.MuscleUpApp
import pt.ipt.dama.muscleup.data.local.UserEntity
import pt.ipt.dama.muscleup.data.session.UserSession
import java.security.MessageDigest

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val userName: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao = (application as MuscleUpApp).database.userDao()
    private val sessionPreferences = (application as MuscleUpApp).sessionPreferences

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
            val user = userDao.findByEmail(email)
            if (user == null || user.passwordHash != hashPassword(password)) {
                _uiState.value = AuthUiState.Error("Email ou password incorretos")
            } else {
                UserSession.set(name = user.name, email = user.email)
                sessionPreferences.save(email = user.email, name = user.name)
                _uiState.value = AuthUiState.Success(user.name)
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
            val existing = userDao.findByEmail(email)
            if (existing != null) {
                _uiState.value = AuthUiState.Error("Email já registado")
                return@launch
            }
            userDao.insert(UserEntity(name = name, email = email, passwordHash = hashPassword(password)))
            UserSession.set(name = name, email = email)
            sessionPreferences.save(email = email, name = name)
            _uiState.value = AuthUiState.Success(name)
        }
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

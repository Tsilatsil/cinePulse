package com.cinepulse.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinepulse.app.ui.auth.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val isRegister: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class AuthViewModel(private val auth: AuthManager) : ViewModel() {
    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun onEmail(v: String) = _state.update { it.copy(email = v.trim()) }
    fun onPassword(v: String) = _state.update { it.copy(password = v) }
    fun onName(v: String) = _state.update { it.copy(displayName = v.trim()) }
    fun toggleMode() = _state.update { it.copy(isRegister = !it.isRegister, error = null) }

    fun submit() {
        val s = _state.value
        if (s.email.isBlank() || s.password.length < 6) {
            _state.update { it.copy(error = "Email required, password ≥ 6 chars") }
            return
        }
        if (s.isRegister && s.displayName.isBlank()) {
            _state.update { it.copy(error = "Display name required") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val result =
                if (s.isRegister) auth.register(s.email, s.password, s.displayName)
                else auth.login(s.email, s.password)
            result.onSuccess { _state.update { it.copy(loading = false, success = true) } }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.message ?: "Failed") }
                }
        }
    }

    fun reset() {
        if (_state.value.email.isBlank()) return
        viewModelScope.launch {
            auth.resetPassword(_state.value.email).onSuccess {
                _state.update { it.copy(error = "Reset email sent") }
            }
        }
    }
}
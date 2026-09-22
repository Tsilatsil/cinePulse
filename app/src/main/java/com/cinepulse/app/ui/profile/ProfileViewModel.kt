package com.cinepulse.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinepulse.app.data.repository.UserMediaRepository
import com.cinepulse.app.ui.auth.AuthManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val totalLogged: Int = 0,
    val completed: Int = 0,
    val watching: Int = 0,
    val streakDays: Int = 0,
    val achievements: List<Achievement> = emptyList()
)

data class Achievement(val title: String, val description: String)

class ProfileViewModel(
    private val repo: UserMediaRepository,
    private val authManager: AuthManager
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun signOut() {
        authManager.logout()
    }
}

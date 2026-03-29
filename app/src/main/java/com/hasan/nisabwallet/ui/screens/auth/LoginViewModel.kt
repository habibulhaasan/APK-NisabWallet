package com.hasan.nisabwallet.ui.screens.auth

import androidx.lifecycle.viewModelScope
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.repository.AuthRepository
import com.hasan.nisabwallet.ui.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String       = "",
    val password: String    = "",
    val isLoading: Boolean  = false,
    val loginSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email, errorMessage = null)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password, errorMessage = null)
    }

    fun login() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please fill in all fields")
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.login(state.email.trim(), state.password)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false, loginSuccess = true
                )
                is Result.Error   -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = friendlyError(result.message)
                )
                else -> Unit
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun friendlyError(raw: String): String = when {
        raw.contains("password", ignoreCase = true)  -> "Incorrect email or password."
        raw.contains("no user",  ignoreCase = true)  -> "No account found with this email."
        raw.contains("network",  ignoreCase = true)  -> "Network error. Please check your connection."
        raw.contains("blocked",  ignoreCase = true)  -> "Too many attempts. Try again later."
        else -> raw
    }
}

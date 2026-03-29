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

data class RegisterUiState(
    val displayName: String     = "",
    val email: String           = "",
    val password: String        = "",
    val confirmPassword: String = "",
    val isLoading: Boolean      = false,
    val registerSuccess: Boolean = false,
    val errorMessage: String?   = null
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    fun onDisplayNameChange(v: String) { _uiState.value = _uiState.value.copy(displayName = v, errorMessage = null) }
    fun onEmailChange(v: String)       { _uiState.value = _uiState.value.copy(email = v,       errorMessage = null) }
    fun onPasswordChange(v: String)    { _uiState.value = _uiState.value.copy(password = v,    errorMessage = null) }
    fun onConfirmPasswordChange(v: String) { _uiState.value = _uiState.value.copy(confirmPassword = v, errorMessage = null) }

    fun register() {
        val s = _uiState.value
        when {
            s.displayName.isBlank()          -> setError("Please enter your name")
            s.email.isBlank()                -> setError("Please enter your email")
            !s.email.contains("@")           -> setError("Please enter a valid email")
            s.password.length < 6            -> setError("Password must be at least 6 characters")
            s.password != s.confirmPassword  -> setError("Passwords do not match")
            else -> {
                viewModelScope.launch {
                    _uiState.value = s.copy(isLoading = true, errorMessage = null)
                    when (val result = authRepository.register(
                        email       = s.email.trim(),
                        password    = s.password,
                        displayName = s.displayName.trim()
                    )) {
                        is Result.Success -> _uiState.value = _uiState.value.copy(
                            isLoading = false, registerSuccess = true
                        )
                        is Result.Error   -> _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = if (result.message.contains("already"))
                                "An account with this email already exists."
                            else result.message
                        )
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun setError(msg: String) {
        _uiState.value = _uiState.value.copy(errorMessage = msg)
    }
}

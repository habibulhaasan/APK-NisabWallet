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

data class ForgotPasswordUiState(
    val email: String        = "",
    val isLoading: Boolean   = false,
    val emailSent: Boolean   = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState = _uiState.asStateFlow()

    fun onEmailChange(v: String) {
        _uiState.value = _uiState.value.copy(email = v, errorMessage = null)
    }

    fun sendReset() {
        val s = _uiState.value
        if (s.email.isBlank() || !s.email.contains("@")) {
            _uiState.value = s.copy(errorMessage = "Please enter a valid email address")
            return
        }
        viewModelScope.launch {
            _uiState.value = s.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.sendPasswordReset(s.email.trim())) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false, emailSent = true
                )
                is Result.Error   -> _uiState.value = _uiState.value.copy(
                    isLoading = false, errorMessage = result.message
                )
                else -> Unit
            }
        }
    }
}

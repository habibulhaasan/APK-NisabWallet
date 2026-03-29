package com.hasan.nisabwallet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel providing:
 * - isLoading state
 * - error event channel (one-shot UI events)
 * - safe launch with error handling
 *
 * Every feature ViewModel extends this.
 */
abstract class BaseViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent = _errorEvent.asSharedFlow()

    private val _successEvent = MutableSharedFlow<String>()
    val successEvent = _successEvent.asSharedFlow()

    /** Coroutine exception handler that pushes errors to the UI */
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        viewModelScope.launch {
            _isLoading.value = false
            _errorEvent.emit(throwable.message ?: "An unexpected error occurred")
        }
    }

    /**
     * Launch a coroutine with automatic loading state and error handling.
     * @param showLoading Whether to set isLoading = true while running
     */
    protected fun launchSafe(
        showLoading: Boolean = true,
        block: suspend CoroutineScope.() -> Unit
    ) {
        viewModelScope.launch(exceptionHandler) {
            if (showLoading) _isLoading.value = true
            try {
                block()
            } finally {
                if (showLoading) _isLoading.value = false
            }
        }
    }

    protected fun emitError(message: String) {
        viewModelScope.launch { _errorEvent.emit(message) }
    }

    protected fun emitSuccess(message: String) {
        viewModelScope.launch { _successEvent.emit(message) }
    }

    protected fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
}

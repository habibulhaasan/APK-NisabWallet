package com.hasan.nisabwallet.data

/**
 * Generic result wrapper used by all repository methods.
 * Eliminates the need for try/catch in every ViewModel.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()

    val isSuccess get() = this is Success
    val isError   get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data

    fun getOrDefault(default: @UnsafeVariance T): T =
        (this as? Success)?.data ?: default

    inline fun onSuccess(block: (T) -> Unit): Result<T> {
        if (this is Success) block(data)
        return this
    }

    inline fun onError(block: (String) -> Unit): Result<T> {
        if (this is Error) block(message)
        return this
    }
}

/** Convenience extension to wrap a suspending block in a Result */
suspend fun <T> safeCall(block: suspend () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: Exception) {
        Result.Error(e.message ?: "Unknown error", e)
    }
}

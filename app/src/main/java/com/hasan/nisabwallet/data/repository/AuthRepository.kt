package com.hasan.nisabwallet.data.repository

import com.hasan.nisabwallet.data.Result

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun register(email: String, password: String, displayName: String): Result<Unit>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun signOut()
    fun getCurrentUserId(): String?
    fun isLoggedIn(): Boolean
}

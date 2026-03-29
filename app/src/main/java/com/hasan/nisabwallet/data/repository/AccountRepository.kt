package com.hasan.nisabwallet.data.repository

import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    /** Real-time stream of all accounts — served from cache when offline */
    fun getAccountsFlow(userId: String): Flow<List<Account>>

    /** One-shot fetch (for use cases that need a snapshot) */
    suspend fun getAccounts(userId: String): Result<List<Account>>

    suspend fun getAccount(userId: String, docId: String): Result<Account>

    suspend fun addAccount(userId: String, account: Account): Result<String>

    suspend fun updateAccount(userId: String, docId: String, account: Account): Result<Unit>

    /** Only allowed when account has zero balance and no linked transactions */
    suspend fun deleteAccount(userId: String, docId: String): Result<Unit>

    /** Atomic balance update — used by transaction/transfer/loan operations */
    suspend fun updateBalance(userId: String, docId: String, newBalance: Double): Result<Unit>

    /** Increment ribaBalance when an interest transaction is recorded */
    suspend fun updateRibaBalance(userId: String, docId: String, delta: Double): Result<Unit>
}

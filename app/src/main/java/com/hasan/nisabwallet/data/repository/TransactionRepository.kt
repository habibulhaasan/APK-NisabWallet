package com.hasan.nisabwallet.data.repository

import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {

    /** Real-time stream — all transactions, newest first */
    fun getTransactionsFlow(userId: String): Flow<List<Transaction>>

    /** Stream filtered by account */
    fun getTransactionsByAccountFlow(userId: String, accountId: String): Flow<List<Transaction>>

    /** One-shot fetch for a date range — used by Analytics & Budget tracking */
    suspend fun getTransactionsForMonth(
        userId: String,
        year: Int,
        month: Int
    ): Result<List<Transaction>>

    suspend fun getTransactionsForDateRange(
        userId: String,
        startDate: String,
        endDate: String
    ): Result<List<Transaction>>

    suspend fun getTransaction(userId: String, docId: String): Result<Transaction>

    /**
     * Add transaction AND update account balance atomically.
     * Also sets isRiba on account if category.isRiba == true.
     */
    suspend fun addTransaction(
        userId: String,
        transaction: Transaction,
        currentAccountBalance: Double,
        isRibaCategory: Boolean
    ): Result<String>

    /**
     * Edit transaction — recalculates and corrects account balance delta.
     */
    suspend fun updateTransaction(
        userId: String,
        docId: String,
        updated: Transaction,
        old: Transaction,
        currentAccountBalance: Double
    ): Result<Unit>

    /**
     * Delete transaction — reverses its effect on account balance.
     */
    suspend fun deleteTransaction(
        userId: String,
        docId: String,
        transaction: Transaction,
        currentAccountBalance: Double
    ): Result<Unit>

    /** Recent N transactions for dashboard widget */
    suspend fun getRecentTransactions(userId: String, limit: Int = 10): Result<List<Transaction>>

    /** Monthly summary — total income and expense */
    suspend fun getMonthlySummary(
        userId: String,
        year: Int,
        month: Int
    ): Result<MonthlySummary>
}

data class MonthlySummary(
    val totalIncome: Double  = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double   = totalIncome - totalExpense
)

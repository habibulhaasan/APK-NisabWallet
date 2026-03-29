package com.hasan.nisabwallet.data.repository

import com.hasan.nisabwallet.data.model.Account
import com.hasan.nisabwallet.data.model.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Dashboard-specific repository that combines data from multiple
 * collections into a single aggregated stream.
 */
interface DashboardRepository {
    fun getAccountsFlow(userId: String): Flow<List<Account>>
    fun getRecentTransactionsFlow(userId: String, limit: Int = 10): Flow<List<Transaction>>
    suspend fun getMonthlyIncome(userId: String, year: Int, month: Int): Double
    suspend fun getMonthlyExpense(userId: String, year: Int, month: Int): Double
    suspend fun getActiveLoansTotal(userId: String): Double
    suspend fun getActiveLendingsTotal(userId: String): Double
    suspend fun getGoalsTotal(userId: String): Double
    suspend fun getInvestmentsTotal(userId: String): Double
    suspend fun getJewelleryTotal(userId: String): Double
    suspend fun getActiveZakatCycle(userId: String): ZakatCycleSummary?
    suspend fun getNisabThreshold(userId: String): Double
}

data class ZakatCycleSummary(
    val cycleId: String     = "",
    val startDate: String   = "",
    val status: String      = "",
    val startWealth: Double = 0.0,
    val nisabThreshold: Double = 0.0
)

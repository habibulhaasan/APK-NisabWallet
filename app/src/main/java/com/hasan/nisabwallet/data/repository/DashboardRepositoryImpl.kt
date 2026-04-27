package com.hasan.nisabwallet.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hasan.nisabwallet.data.firebase.FirestorePaths
import com.hasan.nisabwallet.data.firebase.snapshotFlow
import com.hasan.nisabwallet.data.model.Account
import com.hasan.nisabwallet.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : DashboardRepository {

    // Real-time flows are fine as-is — Firestore handles these efficiently

    override fun getAccountsFlow(userId: String): Flow<List<Account>> =
        FirestorePaths.accounts(db, userId)
            .snapshotFlow()
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(Account::class.java)?.copy(id = it.id) }
            }
            .flowOn(Dispatchers.Default) // Move mapping off main thread

    override fun getRecentTransactionsFlow(userId: String, limit: Int): Flow<List<Transaction>> =
        FirestorePaths.transactions(db, userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .snapshotFlow()
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(Transaction::class.java)?.copy(id = it.id) }
            }
            .flowOn(Dispatchers.Default)

    override suspend fun getMonthlyIncome(userId: String, year: Int, month: Int): Double =
        withContext(Dispatchers.IO) {
            val (start, end) = monthRange(year, month)
            FirestorePaths.transactions(db, userId)
                .whereEqualTo("type", "Income")
                .whereGreaterThanOrEqualTo("date", start)
                .whereLessThan("date", end) // FIX: use strict less-than on first day of NEXT month
                .get().await()
                .documents
                .sumOf { it.getDouble("amount") ?: 0.0 }
        }

    override suspend fun getMonthlyExpense(userId: String, year: Int, month: Int): Double =
        withContext(Dispatchers.IO) {
            val (start, end) = monthRange(year, month)
            FirestorePaths.transactions(db, userId)
                .whereEqualTo("type", "Expense")
                .whereGreaterThanOrEqualTo("date", start)
                .whereLessThan("date", end)
                .get().await()
                .documents
                .sumOf { it.getDouble("amount") ?: 0.0 }
        }

    override suspend fun getActiveLoansTotal(userId: String): Double =
        withContext(Dispatchers.IO) {
            FirestorePaths.loans(db, userId)
                .whereEqualTo("status", "active")
                .get().await()
                .documents
                .sumOf { it.getDouble("remainingBalance") ?: it.getDouble("principalAmount") ?: 0.0 }
        }

    override suspend fun getActiveLendingsTotal(userId: String): Double =
        withContext(Dispatchers.IO) {
            FirestorePaths.lendings(db, userId)
                .whereEqualTo("status", "active")
                .get().await()
                .documents
                .sumOf { it.getDouble("remainingAmount") ?: it.getDouble("amount") ?: 0.0 }
        }

    override suspend fun getGoalsTotal(userId: String): Double =
        withContext(Dispatchers.IO) {
            FirestorePaths.financialGoals(db, userId)
                .whereEqualTo("status", "active")
                .get().await()
                .documents
                .sumOf { it.getDouble("currentAmount") ?: 0.0 }
        }

    override suspend fun getInvestmentsTotal(userId: String): Double =
        withContext(Dispatchers.IO) {
            FirestorePaths.investments(db, userId)
                .whereEqualTo("status", "active")
                .get().await()
                .documents
                .sumOf { doc ->
                    val qty    = doc.getDouble("quantity") ?: 1.0
                    val curVal = doc.getDouble("currentValue") ?: 0.0
                    qty * curVal
                }
        }

    override suspend fun getJewelleryTotal(userId: String): Double =
        withContext(Dispatchers.IO) {
            FirestorePaths.jewellery(db, userId)
                .get().await()
                .documents
                .sumOf { it.getDouble("marketValue") ?: 0.0 }
        }

    override suspend fun getActiveZakatCycle(userId: String): ZakatCycleSummary? =
        withContext(Dispatchers.IO) {
            val snap = FirestorePaths.zakatCycles(db, userId)
                .whereEqualTo("status", "active")
                .limit(1)
                .get().await()
            if (snap.isEmpty) return@withContext null
            val doc = snap.documents.first()
            ZakatCycleSummary(
                cycleId        = doc.id,
                startDate      = doc.getString("startDate") ?: "",
                status         = doc.getString("status") ?: "",
                startWealth    = doc.getDouble("startWealth") ?: 0.0,
                nisabThreshold = doc.getDouble("nisabThreshold") ?: 0.0
            )
        }

    override suspend fun getNisabThreshold(userId: String): Double =
        withContext(Dispatchers.IO) {
            val snap = FirestorePaths.settings(db, userId).get().await()
            if (snap.isEmpty) return@withContext 0.0
            snap.documents
                .firstOrNull { it.contains("nisabThreshold") }
                ?.getDouble("nisabThreshold") ?: 0.0
        }

    // FIX: Use first day of next month as exclusive upper bound
    // This correctly handles Feb (28/29 days), Apr/Jun/Sep/Nov (30 days)
    private fun monthRange(year: Int, month: Int): Pair<String, String> {
        val startDate = LocalDate.of(year, month, 1)
        val endDate   = startDate.plusMonths(1) // First day of next month
        return startDate.toString() to endDate.toString()
    }
}
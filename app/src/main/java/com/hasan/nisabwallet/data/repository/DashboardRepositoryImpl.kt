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

    // ── Real-time flows ───────────────────────────────────────────────────────

    override fun getAccountsFlow(userId: String): Flow<List<Account>> =
        FirestorePaths.accounts(db, userId)
            .snapshotFlow()
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(Account::class.java)?.copy(id = it.id) }
            }

    override fun getRecentTransactionsFlow(
        userId: String,
        limit: Int
    ): Flow<List<Transaction>> =
        FirestorePaths.transactions(db, userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .snapshotFlow()
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(Transaction::class.java)?.copy(id = it.id) }
            }

    // ── Monthly totals ────────────────────────────────────────────────────────

    override suspend fun getMonthlyIncome(userId: String, year: Int, month: Int): Double {
        val (start, end) = monthRange(year, month)
        return FirestorePaths.transactions(db, userId)
            .whereEqualTo("type", "Income")
            .whereGreaterThanOrEqualTo("date", start)
            .whereLessThanOrEqualTo("date", end)
            .get().await()
            .documents
            .sumOf { it.getDouble("amount") ?: 0.0 }
    }

    override suspend fun getMonthlyExpense(userId: String, year: Int, month: Int): Double {
        val (start, end) = monthRange(year, month)
        return FirestorePaths.transactions(db, userId)
            .whereEqualTo("type", "Expense")
            .whereGreaterThanOrEqualTo("date", start)
            .whereLessThanOrEqualTo("date", end)
            .get().await()
            .documents
            .sumOf { it.getDouble("amount") ?: 0.0 }
    }

    // ── Feature module totals (one-shot) ──────────────────────────────────────

    override suspend fun getActiveLoansTotal(userId: String): Double =
        FirestorePaths.loans(db, userId)
            .whereEqualTo("status", "active")
            .get().await()
            .documents
            .sumOf { it.getDouble("remainingBalance") ?: it.getDouble("principalAmount") ?: 0.0 }

    override suspend fun getActiveLendingsTotal(userId: String): Double =
        FirestorePaths.lendings(db, userId)
            .whereEqualTo("status", "active")
            .get().await()
            .documents
            .sumOf { it.getDouble("remainingAmount") ?: it.getDouble("amount") ?: 0.0 }

    override suspend fun getGoalsTotal(userId: String): Double =
        FirestorePaths.financialGoals(db, userId)
            .whereEqualTo("status", "active")
            .get().await()
            .documents
            .sumOf { it.getDouble("currentAmount") ?: 0.0 }

    override suspend fun getInvestmentsTotal(userId: String): Double =
        FirestorePaths.investments(db, userId)
            .whereEqualTo("status", "active")
            .get().await()
            .documents
            .sumOf { doc ->
                val qty     = doc.getDouble("quantity") ?: 1.0
                val curVal  = doc.getDouble("currentValue") ?: 0.0
                qty * curVal
            }

    override suspend fun getJewelleryTotal(userId: String): Double =
        FirestorePaths.jewellery(db, userId)
            .get().await()
            .documents
            .sumOf { it.getDouble("marketValue") ?: 0.0 }

    // ── Zakat ─────────────────────────────────────────────────────────────────

    override suspend fun getActiveZakatCycle(userId: String): ZakatCycleSummary? {
        val snap = FirestorePaths.zakatCycles(db, userId)
            .whereEqualTo("status", "active")
            .limit(1)
            .get().await()
        if (snap.isEmpty) return null
        val doc = snap.documents.first()
        return ZakatCycleSummary(
            cycleId        = doc.id,
            startDate      = doc.getString("startDate") ?: "",
            status         = doc.getString("status") ?: "",
            startWealth    = doc.getDouble("startWealth") ?: 0.0,
            nisabThreshold = doc.getDouble("nisabThreshold") ?: 0.0
        )
    }

    override suspend fun getNisabThreshold(userId: String): Double {
        val snap = FirestorePaths.settings(db, userId).get().await()
        if (snap.isEmpty) return 0.0
        return snap.documents
            .firstOrNull { it.contains("nisabThreshold") }
            ?.getDouble("nisabThreshold") ?: 0.0
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun monthRange(year: Int, month: Int): Pair<String, String> {
        val start = "%04d-%02d-01".format(year, month)
        val end   = "%04d-%02d-31".format(year, month)
        return start to end
    }
}

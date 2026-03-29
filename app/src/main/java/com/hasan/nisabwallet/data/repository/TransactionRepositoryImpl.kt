package com.hasan.nisabwallet.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.firebase.FirestorePaths
import com.hasan.nisabwallet.data.firebase.snapshotFlow
import com.hasan.nisabwallet.data.model.Transaction
import com.hasan.nisabwallet.data.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : TransactionRepository {

    // ── Real-time flows ───────────────────────────────────────────────────────

    override fun getTransactionsFlow(userId: String): Flow<List<Transaction>> =
        FirestorePaths.transactions(db, userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .snapshotFlow()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                }
            }

    override fun getTransactionsByAccountFlow(
        userId: String,
        accountId: String
    ): Flow<List<Transaction>> =
        FirestorePaths.transactions(db, userId)
            .whereEqualTo("accountId", accountId)
            .orderBy("date", Query.Direction.DESCENDING)
            .snapshotFlow()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                }
            }

    // ── One-shot fetches ──────────────────────────────────────────────────────

    override suspend fun getTransactionsForMonth(
        userId: String,
        year: Int,
        month: Int
    ): Result<List<Transaction>> = safeCall {
        val startDate = "%04d-%02d-01".format(year, month)
        val endDate   = "%04d-%02d-31".format(year, month)
        fetchByDateRange(userId, startDate, endDate)
    }

    override suspend fun getTransactionsForDateRange(
        userId: String,
        startDate: String,
        endDate: String
    ): Result<List<Transaction>> = safeCall {
        fetchByDateRange(userId, startDate, endDate)
    }

    override suspend fun getTransaction(userId: String, docId: String): Result<Transaction> =
        safeCall {
            val doc = FirestorePaths.transactions(db, userId).document(docId).get().await()
            doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                ?: error("Transaction not found")
        }

    override suspend fun getRecentTransactions(
        userId: String,
        limit: Int
    ): Result<List<Transaction>> = safeCall {
        FirestorePaths.transactions(db, userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get().await()
            .documents
            .mapNotNull { doc -> doc.toObject(Transaction::class.java)?.copy(id = doc.id) }
    }

    override suspend fun getMonthlySummary(
        userId: String,
        year: Int,
        month: Int
    ): Result<MonthlySummary> = safeCall {
        val txns = getTransactionsForMonth(userId, year, month).getOrDefault(emptyList())
        MonthlySummary(
            totalIncome  = txns.filter { it.isIncome  }.sumOf { it.amount },
            totalExpense = txns.filter { it.isExpense }.sumOf { it.amount }
        )
    }

    // ── Write operations (all atomic via Firestore batch / runTransaction) ────

    override suspend fun addTransaction(
        userId: String,
        transaction: Transaction,
        currentAccountBalance: Double,
        isRibaCategory: Boolean
    ): Result<String> = safeCall {
        val batch   = db.batch()
        val txnRef  = FirestorePaths.transactions(db, userId).document()
        val accRef  = FirestorePaths.accounts(db, userId).document(transaction.accountId)

        // 1. Write transaction document
        val txnData = buildTransactionMap(transaction).toMutableMap()
        txnData["transactionId"] = UUID.randomUUID().toString()
        txnData["createdAt"]     = FieldValue.serverTimestamp()
        batch.set(txnRef, txnData)

        // 2. Update account balance
        val newBalance = if (transaction.isIncome)
            currentAccountBalance + transaction.amount
        else
            currentAccountBalance - transaction.amount

        val accUpdates = mutableMapOf<String, Any>(
            "balance"   to newBalance,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        // 3. If riba category → increment ribaBalance
        if (isRibaCategory && transaction.isIncome) {
            accUpdates["ribaBalance"] = FieldValue.increment(transaction.amount)
        }
        batch.update(accRef, accUpdates)

        batch.commit().await()
        txnRef.id
    }

    override suspend fun updateTransaction(
        userId: String,
        docId: String,
        updated: Transaction,
        old: Transaction,
        currentAccountBalance: Double
    ): Result<Unit> = safeCall {
        val batch  = db.batch()
        val txnRef = FirestorePaths.transactions(db, userId).document(docId)

        // 1. Update transaction
        val data = buildTransactionMap(updated).toMutableMap()
        data["updatedAt"] = FieldValue.serverTimestamp()
        batch.update(txnRef, data)

        // 2. Reverse old balance effect, apply new effect
        val reversedBalance = if (old.isIncome)
            currentAccountBalance - old.amount
        else
            currentAccountBalance + old.amount

        val finalBalance = if (updated.isIncome)
            reversedBalance + updated.amount
        else
            reversedBalance - updated.amount

        val accRef = FirestorePaths.accounts(db, userId).document(updated.accountId)
        batch.update(accRef, mapOf(
            "balance"   to finalBalance,
            "updatedAt" to FieldValue.serverTimestamp()
        ))

        batch.commit().await()
    }

    override suspend fun deleteTransaction(
        userId: String,
        docId: String,
        transaction: Transaction,
        currentAccountBalance: Double
    ): Result<Unit> = safeCall {
        val batch  = db.batch()
        val txnRef = FirestorePaths.transactions(db, userId).document(docId)

        // 1. Delete transaction
        batch.delete(txnRef)

        // 2. Reverse balance effect
        val newBalance = if (transaction.isIncome)
            currentAccountBalance - transaction.amount
        else
            currentAccountBalance + transaction.amount

        val accRef = FirestorePaths.accounts(db, userId).document(transaction.accountId)
        batch.update(accRef, mapOf(
            "balance"   to newBalance,
            "updatedAt" to FieldValue.serverTimestamp()
        ))

        batch.commit().await()
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private suspend fun fetchByDateRange(
        userId: String,
        startDate: String,
        endDate: String
    ): List<Transaction> =
        FirestorePaths.transactions(db, userId)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .orderBy("date", Query.Direction.DESCENDING)
            .get().await()
            .documents
            .mapNotNull { doc -> doc.toObject(Transaction::class.java)?.copy(id = doc.id) }

    private fun buildTransactionMap(t: Transaction): Map<String, Any?> = mapOf(
        "type"          to t.type,
        "amount"        to t.amount,
        "accountId"     to t.accountId,
        "accountName"   to t.accountName,
        "categoryId"    to t.categoryId,
        "categoryName"  to t.categoryName,
        "categoryColor" to t.categoryColor,
        "description"   to t.description,
        "date"          to t.date,
        "isRiba"        to t.isRiba,
        "isSadaqah"     to t.isSadaqah,
        "ribaRefId"     to t.ribaRefId,
        "recurringId"   to t.recurringId
    )
}

package com.hasan.nisabwallet.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.firebase.FirestorePaths
import com.hasan.nisabwallet.data.firebase.snapshotFlow
import com.hasan.nisabwallet.data.model.RecurringTransaction
import com.hasan.nisabwallet.data.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

interface RecurringRepository {
    fun getRecurringFlow(userId: String): Flow<List<RecurringTransaction>>
    suspend fun addRecurring(userId: String, r: RecurringTransaction): Result<String>
    suspend fun updateRecurring(userId: String, docId: String, r: RecurringTransaction): Result<Unit>
    suspend fun togglePause(userId: String, docId: String, currentStatus: String): Result<Unit>
    suspend fun deleteRecurring(userId: String, docId: String): Result<Unit>
    suspend fun executeRecurring(
        userId: String,
        r: RecurringTransaction,
        accountBalance: Double
    ): Result<Unit>
}

class RecurringRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : RecurringRepository {

    override fun getRecurringFlow(userId: String): Flow<List<RecurringTransaction>> =
        FirestorePaths.recurringTransactions(db, userId)
            .snapshotFlow()
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(RecurringTransaction::class.java)?.copy(id = it.id) }
                    .sortedWith(compareBy({ it.status != "active" }, { it.nextDueDate }))
            }

    override suspend fun addRecurring(userId: String, r: RecurringTransaction): Result<String> = safeCall {
        FirestorePaths.recurringTransactions(db, userId).add(mapOf(
            "recurringId"    to UUID.randomUUID().toString(),
            "type"           to r.type,
            "amount"         to r.amount,
            "accountId"      to r.accountId,
            "accountName"    to r.accountName,
            "categoryId"     to r.categoryId,
            "categoryName"   to r.categoryName,
            "categoryColor"  to r.categoryColor,
            "frequency"      to r.frequency,
            "interval"       to r.interval,
            "dayOfWeek"      to r.dayOfWeek,
            "dayOfMonth"     to r.dayOfMonth,
            "startDate"      to r.startDate,
            "endCondition"   to r.endCondition,
            "endDate"        to r.endDate,
            "occurrences"    to r.occurrences,
            "completedCount" to 0,
            "status"         to "active",
            "nextDueDate"    to r.startDate,
            "description"    to r.description,
            "createdAt"      to FieldValue.serverTimestamp(),
            "updatedAt"      to FieldValue.serverTimestamp()
        )).await().id
    }

    override suspend fun updateRecurring(userId: String, docId: String, r: RecurringTransaction): Result<Unit> = safeCall {
        FirestorePaths.recurringTransactions(db, userId).document(docId).update(mapOf(
            "amount" to r.amount, "description" to r.description,
            "frequency" to r.frequency, "interval" to r.interval,
            "endCondition" to r.endCondition, "endDate" to r.endDate,
            "occurrences" to r.occurrences, "updatedAt" to FieldValue.serverTimestamp()
        )).await()
    }

    override suspend fun togglePause(userId: String, docId: String, currentStatus: String): Result<Unit> = safeCall {
        val newStatus = if (currentStatus == "active") "paused" else "active"
        FirestorePaths.recurringTransactions(db, userId).document(docId)
            .update("status", newStatus, "updatedAt", FieldValue.serverTimestamp()).await()
    }

    override suspend fun deleteRecurring(userId: String, docId: String): Result<Unit> = safeCall {
        FirestorePaths.recurringTransactions(db, userId).document(docId).delete().await()
    }

    override suspend fun executeRecurring(
        userId: String,
        r: RecurringTransaction,
        accountBalance: Double
    ): Result<Unit> = safeCall {
        val batch  = db.batch()
        val recRef = FirestorePaths.recurringTransactions(db, userId).document(r.id)
        val accRef = FirestorePaths.accounts(db, userId).document(r.accountId)
        val txnRef = FirestorePaths.transactions(db, userId).document()

        // 1. Record transaction
        batch.set(txnRef, mapOf(
            "transactionId" to UUID.randomUUID().toString(),
            "type"          to r.type,
            "amount"        to r.amount,
            "accountId"     to r.accountId,
            "accountName"   to r.accountName,
            "categoryId"    to r.categoryId,
            "categoryName"  to r.categoryName,
            "categoryColor" to r.categoryColor,
            "description"   to r.description.ifBlank { "${r.frequencyLabel} ${r.type.lowercase()}" },
            "date"          to LocalDate.now().toString(),
            "isRiba"        to false,
            "recurringId"   to r.id,
            "createdAt"     to FieldValue.serverTimestamp()
        ))

        // 2. Update account balance
        val newBalance = if (r.isIncome) accountBalance + r.amount else accountBalance - r.amount
        batch.update(accRef, "balance", newBalance, "updatedAt", FieldValue.serverTimestamp())

        // 3. Calculate next due date
        val nextDue = calculateNextDueDate(r.nextDueDate, r.frequency, r.interval)
        val newCount = r.completedCount + 1
        val newStatus = when {
            r.endCondition == "after" && r.occurrences != null && newCount >= r.occurrences -> "completed"
            r.endCondition == "until" && r.endDate.isNotBlank() &&
                    try { !LocalDate.parse(nextDue).isBefore(LocalDate.parse(r.endDate)) } catch (e: Exception) { false } -> "completed"
            else -> "active"
        }

        batch.update(recRef, mapOf(
            "nextDueDate"    to nextDue,
            "completedCount" to newCount,
            "status"         to newStatus,
            "updatedAt"      to FieldValue.serverTimestamp()
        ))

        // 4. Log execution
        val logRef = FirestorePaths.recurringLogs(db, userId).document()
        batch.set(logRef, mapOf(
            "recurringId"   to r.id,
            "amount"        to r.amount,
            "executedAt"    to FieldValue.serverTimestamp(),
            "transactionId" to txnRef.id
        ))

        batch.commit().await()
    }

    private fun calculateNextDueDate(current: String, frequency: String, interval: Int): String {
        return try {
            val date = LocalDate.parse(current)
            when (frequency) {
                "daily"   -> date.plusDays(interval.toLong())
                "weekly"  -> date.plusWeeks(interval.toLong())
                "monthly" -> date.plusMonths(interval.toLong())
                "yearly"  -> date.plusYears(interval.toLong())
                else      -> date.plusMonths(1)
            }.toString()
        } catch (e: Exception) { current }
    }
}

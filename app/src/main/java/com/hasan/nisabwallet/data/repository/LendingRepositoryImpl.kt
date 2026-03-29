package com.hasan.nisabwallet.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.firebase.FirestorePaths
import com.hasan.nisabwallet.data.firebase.snapshotFlow
import com.hasan.nisabwallet.data.model.Lending
import com.hasan.nisabwallet.data.model.LendingRepayment
import com.hasan.nisabwallet.data.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class LendingRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : LendingRepository {

    override fun getLendingsFlow(userId: String): Flow<List<Lending>> =
        FirestorePaths.lendings(db, userId)
            .snapshotFlow()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(Lending::class.java)?.copy(id = doc.id)
                }.sortedByDescending { it.createdAt }
            }

    override suspend fun addLending(
        userId: String, lending: Lending, accountBalance: Double
    ): Result<String> = safeCall {
        val batch      = db.batch()
        val lendRef    = FirestorePaths.lendings(db, userId).document()
        val accRef     = FirestorePaths.accounts(db, userId).document(lending.accountId)

        batch.set(lendRef, mapOf(
            "lendingId"        to UUID.randomUUID().toString(),
            "borrowerName"     to lending.borrowerName,
            "phone"            to lending.phone,
            "amount"           to lending.amount,
            "remainingAmount"  to lending.amount,
            "dueDate"          to lending.dueDate,
            "purpose"          to lending.purpose,
            "accountId"        to lending.accountId,
            "accountName"      to lending.accountName,
            "status"           to "active",
            "countForZakat"    to lending.countForZakat,
            "notes"            to lending.notes,
            "repaymentHistory" to emptyList<Map<String, Any>>(),
            "createdAt"        to FieldValue.serverTimestamp(),
            "updatedAt"        to FieldValue.serverTimestamp()
        ))

        // Debit account (money going out)
        batch.update(accRef, mapOf(
            "balance"   to accountBalance - lending.amount,
            "updatedAt" to FieldValue.serverTimestamp()
        ))

        // Record expense transaction
        val txnRef = FirestorePaths.transactions(db, userId).document()
        batch.set(txnRef, mapOf(
            "transactionId" to UUID.randomUUID().toString(),
            "type"          to "Expense",
            "amount"        to lending.amount,
            "accountId"     to lending.accountId,
            "accountName"   to lending.accountName,
            "categoryName"  to "Lending Given",
            "description"   to "Lent to ${lending.borrowerName}${if (lending.purpose.isNotBlank()) " — ${lending.purpose}" else ""}",
            "date"          to java.time.LocalDate.now().toString(),
            "isRiba"        to false,
            "createdAt"     to FieldValue.serverTimestamp()
        ))

        batch.commit().await()
        lendRef.id
    }

    override suspend fun updateLending(
        userId: String, docId: String, lending: Lending
    ): Result<Unit> = safeCall {
        FirestorePaths.lendings(db, userId).document(docId).update(mapOf(
            "borrowerName"  to lending.borrowerName,
            "phone"         to lending.phone,
            "dueDate"       to lending.dueDate,
            "purpose"       to lending.purpose,
            "countForZakat" to lending.countForZakat,
            "notes"         to lending.notes,
            "updatedAt"     to FieldValue.serverTimestamp()
        )).await()
    }

    override suspend fun deleteLending(userId: String, docId: String): Result<Unit> = safeCall {
        FirestorePaths.lendings(db, userId).document(docId).delete().await()
    }

    override suspend fun recordRepayment(
        userId: String,
        lendingDocId: String,
        repayment: LendingRepayment,
        currentLending: Lending,
        accountBalance: Double
    ): Result<Unit> = safeCall {
        val batch   = db.batch()
        val lendRef = FirestorePaths.lendings(db, userId).document(lendingDocId)
        val accRef  = FirestorePaths.accounts(db, userId).document(currentLending.accountId)

        val newRemaining   = maxOf(0.0, currentLending.remainingAmount - repayment.amount)
        val updatedHistory = currentLending.repaymentHistory + repayment
        val newStatus      = if (newRemaining <= 0.0) "returned" else "active"

        batch.update(lendRef, mapOf(
            "remainingAmount"  to newRemaining,
            "status"           to newStatus,
            "repaymentHistory" to updatedHistory.map { r ->
                mapOf("amount" to r.amount, "date" to r.date,
                      "notes" to r.notes, "transactionId" to r.transactionId)
            },
            "updatedAt" to FieldValue.serverTimestamp()
        ))

        // Credit account (money coming back)
        batch.update(accRef, mapOf(
            "balance"   to accountBalance + repayment.amount,
            "updatedAt" to FieldValue.serverTimestamp()
        ))

        // Record income transaction
        val txnRef = FirestorePaths.transactions(db, userId).document()
        batch.set(txnRef, mapOf(
            "transactionId" to UUID.randomUUID().toString(),
            "type"          to "Income",
            "amount"        to repayment.amount,
            "accountId"     to currentLending.accountId,
            "accountName"   to currentLending.accountName,
            "categoryName"  to "Lending Received",
            "description"   to "Repayment from ${currentLending.borrowerName}",
            "date"          to repayment.date,
            "isRiba"        to false,
            "createdAt"     to FieldValue.serverTimestamp()
        ))

        batch.commit().await()
    }

    override suspend fun markAsReturned(userId: String, docId: String): Result<Unit> = safeCall {
        FirestorePaths.lendings(db, userId).document(docId).update(
            "status", "returned",
            "remainingAmount", 0.0,
            "updatedAt", FieldValue.serverTimestamp()
        ).await()
    }
}

package com.hasan.nisabwallet.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.firebase.FirestorePaths
import com.hasan.nisabwallet.data.firebase.snapshotFlow
import com.hasan.nisabwallet.data.model.Loan
import com.hasan.nisabwallet.data.model.LoanPayment
import com.hasan.nisabwallet.data.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class LoanRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : LoanRepository {

    override fun getLoansFlow(userId: String): Flow<List<Loan>> =
        FirestorePaths.loans(db, userId)
            .snapshotFlow()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(Loan::class.java)?.copy(id = doc.id)
                }.sortedByDescending { it.createdAt }
            }

    override suspend fun getLoan(userId: String, docId: String): Result<Loan> = safeCall {
        val doc = FirestorePaths.loans(db, userId).document(docId).get().await()
        doc.toObject(Loan::class.java)?.copy(id = doc.id) ?: error("Loan not found")
    }

    override suspend fun addLoan(
        userId: String,
        loan: Loan,
        accountBalance: Double
    ): Result<String> = safeCall {
        val batch   = db.batch()
        val loanRef = FirestorePaths.loans(db, userId).document()
        val accRef  = FirestorePaths.accounts(db, userId).document(loan.accountId)

        // Write loan doc
        batch.set(loanRef, buildLoanMap(loan).toMutableMap().apply {
            put("loanId",           UUID.randomUUID().toString())
            put("remainingBalance", loan.principalAmount)
            put("paymentHistory",   emptyList<Map<String, Any>>())
            put("status",           "active")
            put("createdAt",        FieldValue.serverTimestamp())
            put("updatedAt",        FieldValue.serverTimestamp())
        })

        // Credit account (loan received = money in)
        batch.update(accRef, mapOf(
            "balance"   to accountBalance + loan.principalAmount,
            "updatedAt" to FieldValue.serverTimestamp()
        ))

        // Also record income transaction for the loan
        val txnRef = FirestorePaths.transactions(db, userId).document()
        batch.set(txnRef, mapOf(
            "transactionId" to UUID.randomUUID().toString(),
            "type"          to "Income",
            "amount"        to loan.principalAmount,
            "accountId"     to loan.accountId,
            "accountName"   to loan.accountName,
            "categoryName"  to "Loan Received",
            "description"   to "Loan from ${loan.lenderName}",
            "date"          to loan.startDate,
            "isRiba"        to false,
            "createdAt"     to FieldValue.serverTimestamp()
        ))

        batch.commit().await()
        loanRef.id
    }

    override suspend fun updateLoan(
        userId: String, docId: String, loan: Loan
    ): Result<Unit> = safeCall {
        FirestorePaths.loans(db, userId).document(docId).update(
            buildLoanMap(loan).toMutableMap().apply {
                put("updatedAt", FieldValue.serverTimestamp())
            }
        ).await()
    }

    override suspend fun deleteLoan(userId: String, docId: String): Result<Unit> = safeCall {
        FirestorePaths.loans(db, userId).document(docId).delete().await()
    }

    override suspend fun recordPayment(
        userId: String,
        loanDocId: String,
        payment: LoanPayment,
        currentLoan: Loan,
        accountBalance: Double
    ): Result<Unit> = safeCall {
        val batch   = db.batch()
        val loanRef = FirestorePaths.loans(db, userId).document(loanDocId)
        val accRef  = FirestorePaths.accounts(db, userId).document(currentLoan.accountId)

        val newRemaining    = maxOf(0.0, currentLoan.remainingBalance - payment.amount)
        val updatedHistory  = currentLoan.paymentHistory + payment
        val newStatus       = if (newRemaining <= 0.0) "paid" else "active"

        // Update loan
        batch.update(loanRef, mapOf(
            "remainingBalance" to newRemaining,
            "paymentHistory"   to updatedHistory.map { p ->
                mapOf("amount" to p.amount, "date" to p.date,
                      "notes" to p.notes, "transactionId" to p.transactionId)
            },
            "status"    to newStatus,
            "updatedAt" to FieldValue.serverTimestamp()
        ))

        // Debit account
        batch.update(accRef, mapOf(
            "balance"   to accountBalance - payment.amount,
            "updatedAt" to FieldValue.serverTimestamp()
        ))

        // Record expense transaction
        val txnRef = FirestorePaths.transactions(db, userId).document()
        batch.set(txnRef, mapOf(
            "transactionId" to UUID.randomUUID().toString(),
            "type"          to "Expense",
            "amount"        to payment.amount,
            "accountId"     to currentLoan.accountId,
            "accountName"   to currentLoan.accountName,
            "categoryName"  to "Loan Payment",
            "description"   to "Payment to ${currentLoan.lenderName}${if (payment.notes.isNotBlank()) " — ${payment.notes}" else ""}",
            "date"          to payment.date,
            "isRiba"        to false,
            "createdAt"     to FieldValue.serverTimestamp()
        ))

        batch.commit().await()
    }

    override suspend fun markAsPaid(userId: String, docId: String): Result<Unit> = safeCall {
        FirestorePaths.loans(db, userId).document(docId).update(
            "status", "paid",
            "remainingBalance", 0.0,
            "updatedAt", FieldValue.serverTimestamp()
        ).await()
    }

    private fun buildLoanMap(loan: Loan): Map<String, Any?> = mapOf(
        "lenderName"      to loan.lenderName,
        "loanType"        to loan.loanType,
        "principalAmount" to loan.principalAmount,
        "interestRate"    to loan.interestRate,
        "monthlyPayment"  to loan.monthlyPayment,
        "totalMonths"     to loan.totalMonths,
        "startDate"       to loan.startDate,
        "accountId"       to loan.accountId,
        "accountName"     to loan.accountName,
        "notes"           to loan.notes,
        "enableReminders" to loan.enableReminders
    )
}

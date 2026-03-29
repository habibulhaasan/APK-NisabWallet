package com.hasan.nisabwallet.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Mirrors web app loans/page.js
 * loanType: "qard-hasan" (interest-free) | "conventional"
 */
data class Loan(
    @DocumentId
    val id: String              = "",
    val loanId: String          = "",
    val lenderName: String      = "",
    val loanType: String        = "qard-hasan",  // "qard-hasan" | "conventional"
    val principalAmount: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val interestRate: Double    = 0.0,           // % per annum
    val monthlyPayment: Double  = 0.0,
    val totalMonths: Int        = 0,
    val startDate: String       = "",
    val accountId: String       = "",
    val accountName: String     = "",
    val notes: String           = "",
    val status: String          = "active",      // "active" | "paid"
    val enableReminders: Boolean = true,
    val paymentHistory: List<LoanPayment> = emptyList(),
    @ServerTimestamp
    val createdAt: Date?        = null,
    @ServerTimestamp
    val updatedAt: Date?        = null
) {
    val isQardHasan: Boolean    get() = loanType == "qard-hasan"
    val isPaid: Boolean         get() = status == "paid"
    val totalPaid: Double       get() = paymentHistory.sumOf { it.amount }
    val progressPercent: Float  get() = if (principalAmount > 0)
        (totalPaid / principalAmount).toFloat().coerceIn(0f, 1f) else 0f
}

data class LoanPayment(
    val amount: Double    = 0.0,
    val date: String      = "",
    val notes: String     = "",
    val transactionId: String = ""
)

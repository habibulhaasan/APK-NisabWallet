package com.hasan.nisabwallet.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Money YOU lent to someone else.
 * countForZakat: user decides if this lending is recoverable enough to include in Zakat wealth.
 */
data class Lending(
    @DocumentId
    val id: String               = "",
    val lendingId: String        = "",
    val borrowerName: String     = "",
    val phone: String            = "",
    val amount: Double           = 0.0,
    val remainingAmount: Double  = 0.0,
    val dueDate: String          = "",
    val purpose: String          = "",
    val accountId: String        = "",
    val accountName: String      = "",
    val status: String           = "active",     // "active" | "returned"
    val countForZakat: Boolean   = true,
    val notes: String            = "",
    val repaymentHistory: List<LendingRepayment> = emptyList(),
    @ServerTimestamp
    val createdAt: Date?         = null,
    @ServerTimestamp
    val updatedAt: Date?         = null
) {
    val isReturned: Boolean       get() = status == "returned"
    val totalRepaid: Double       get() = repaymentHistory.sumOf { it.amount }
    val progressPercent: Float    get() = if (amount > 0)
        (totalRepaid / amount).toFloat().coerceIn(0f, 1f) else 0f
    val isOverdue: Boolean        get() {
        if (dueDate.isBlank() || isReturned) return false
        return try {
            java.time.LocalDate.parse(dueDate).isBefore(java.time.LocalDate.now())
        } catch (e: Exception) { false }
    }
}

data class LendingRepayment(
    val amount: Double    = 0.0,
    val date: String      = "",
    val notes: String     = "",
    val transactionId: String = ""
)

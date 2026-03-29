package com.hasan.nisabwallet.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Mirrors web app recurringTransactionsCollections.js
 * frequency: "daily" | "weekly" | "monthly" | "yearly"
 * endCondition: "never" | "until" | "after"
 */
data class RecurringTransaction(
    @DocumentId
    val id: String              = "",
    val recurringId: String     = "",
    val type: String            = "Expense",   // "Income" | "Expense"
    val amount: Double          = 0.0,
    val accountId: String       = "",
    val accountName: String     = "",
    val categoryId: String      = "",
    val categoryName: String    = "",
    val categoryColor: String   = "#6B7280",
    val frequency: String       = "monthly",   // "daily"|"weekly"|"monthly"|"yearly"
    val interval: Int           = 1,
    val dayOfWeek: Int?         = null,
    val dayOfMonth: Int?        = null,
    val startDate: String       = "",
    val endCondition: String    = "never",     // "never"|"until"|"after"
    val endDate: String         = "",
    val occurrences: Int?       = null,
    val completedCount: Int     = 0,
    val status: String          = "active",    // "active"|"paused"|"completed"
    val nextDueDate: String     = "",
    val description: String     = "",
    @ServerTimestamp
    val createdAt: Date?        = null,
    @ServerTimestamp
    val updatedAt: Date?        = null
) {
    val isIncome: Boolean  get() = type == "Income"
    val isOverdue: Boolean get() = try {
        nextDueDate.isNotBlank() && status == "active" &&
        java.time.LocalDate.parse(nextDueDate).isBefore(java.time.LocalDate.now())
    } catch (e: Exception) { false }

    val frequencyLabel: String get() = when (frequency) {
        "daily"   -> if (interval == 1) "Daily" else "Every $interval days"
        "weekly"  -> if (interval == 1) "Weekly" else "Every $interval weeks"
        "monthly" -> if (interval == 1) "Monthly" else "Every $interval months"
        "yearly"  -> "Yearly"
        else      -> frequency
    }
}

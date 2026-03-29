package com.hasan.nisabwallet.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Financial goal linked to a specific account.
 * currentAmount is "allocated" from the linked account's balance.
 * Available balance = account.balance - sum(active goal currentAmounts)
 */
data class Goal(
    @DocumentId
    val id: String               = "",
    val goalId: String           = "",
    val goalName: String         = "",
    val category: String         = "other",
    val targetAmount: Double     = 0.0,
    val currentAmount: Double    = 0.0,
    val targetDate: String       = "",
    val monthlyContribution: Double = 0.0,
    val linkedAccountId: String  = "",
    val linkedAccountName: String = "",
    val priority: String         = "medium",   // "low" | "medium" | "high"
    val description: String      = "",
    val status: String           = "active",   // "active" | "completed" | "cancelled"
    val enableNotifications: Boolean = true,
    @ServerTimestamp
    val createdAt: Date?         = null,
    @ServerTimestamp
    val updatedAt: Date?         = null
) {
    val progressPercent: Float get() =
        if (targetAmount > 0) (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val remaining: Double      get() = maxOf(0.0, targetAmount - currentAmount)
    val isCompleted: Boolean   get() = status == "completed" || currentAmount >= targetAmount

    val categoryLabel: String get() = when (category) {
        "emergency_fund"  -> "Emergency Fund"
        "house"           -> "House / Property"
        "car"             -> "Car / Vehicle"
        "education"       -> "Education"
        "hajj"            -> "Hajj / Umrah"
        "wedding"         -> "Wedding"
        "business"        -> "Business"
        "retirement"      -> "Retirement"
        "travel"          -> "Travel"
        "gadget"          -> "Gadget / Electronics"
        else              -> "Other"
    }

    val priorityColor: String get() = when (priority) {
        "high"   -> "#EF4444"
        "medium" -> "#F59E0B"
        else     -> "#10B981"
    }
}

package com.hasan.nisabwallet.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Mirrors web app budgetCollections.js
 * Budget = spending limit for one category in one calendar month.
 */
data class Budget(
    @DocumentId
    val id: String          = "",
    val budgetId: String    = "",
    val categoryId: String  = "",
    val categoryName: String = "",
    val categoryColor: String = "#6B7280",
    val amount: Double      = 0.0,       // limit
    val spent: Double       = 0.0,       // calculated client-side from transactions
    val year: Int           = 0,
    val month: Int          = 0,         // 1–12
    val rollover: Boolean   = false,
    val notes: String       = "",
    @ServerTimestamp
    val createdAt: Date?    = null,
    @ServerTimestamp
    val updatedAt: Date?    = null
) {
    val remaining: Double     get() = maxOf(0.0, amount - spent)
    val spentPercent: Float   get() = if (amount > 0) (spent / amount).toFloat().coerceIn(0f, 1f) else 0f
    val isOverBudget: Boolean get() = spent > amount
    val monthLabel: String    get() = "%04d-%02d".format(year, month)
}

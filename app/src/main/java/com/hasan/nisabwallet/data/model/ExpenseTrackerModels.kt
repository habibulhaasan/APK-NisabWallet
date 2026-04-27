// app/src/main/java/com/hasan/nisabwallet/data/model/ExpenseTrackerModels.kt
package com.hasan.nisabwallet.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// ── Expense Tracker Tab (type configuration) ──────────────────────────────────
data class ExpenseTrackerTab(
    @DocumentId
    val id: String           = "",
    val title: String        = "",
    val categoryId: String   = "",
    val color: String        = "emerald",
    val defaultAmount: Double = 0.0,
    val unit: String         = "",
    val order: Int           = 0
)

// ── Monthly grid data ─────────────────────────────────────────────────────────
// Stored at users/{uid}/expenseTrackerData/{YYYY-MM}
// tabs: { tabId -> { dayOfMonth -> amount } }
data class ExpenseTrackerMonthData(
    @DocumentId
    val id: String                               = "",
    val month: String                            = "",  // YYYY-MM
    val tabs: Map<String, Map<String, Double>>   = emptyMap(),
    val confirmedAt: Date?                       = null,
    val transactionIds: List<String>             = emptyList(),
    val totalAmount: Double                      = 0.0,
    val accountId: String                        = "",
    val note: String                             = ""
)

// ── Yearly grid data ──────────────────────────────────────────────────────────
// Stored at users/{uid}/expenseTrackerData/{YYYY}
data class ExpenseTrackerYearData(
    @DocumentId
    val id: String                               = "",
    val year: String                             = "",
    val tabs: Map<String, Map<String, Double>>   = emptyMap(),
    val confirmedAt: Date?                       = null,
    val totalAmount: Double                      = 0.0
)
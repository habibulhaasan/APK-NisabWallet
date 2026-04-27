// app/src/main/java/com/hasan/nisabwallet/data/model/GroceryModels.kt
package com.hasan.nisabwallet.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// ── Master item in grocery list ───────────────────────────────────────────────
data class GroceryItem(
    @DocumentId
    val id: String           = "",
    val name: String         = "",
    val unit: String         = "pcs",
    val defaultQty: Double   = 1.0,
    val defaultUnitPrice: Double = 0.0,
    val category: String     = "",
    val archived: Boolean    = false
)

// ── Per-item entry stored inside a monthly document ──────────────────────────
data class GroceryMonthEntry(
    val itemId: String       = "",
    val qty: Double          = 1.0,
    val unitPrice: Double    = 0.0,
    val bought: Boolean      = false,
    val boughtPrice: Double? = null
)

// ── Monthly document: users/{uid}/groceryMonths/{YYYY-MM} ─────────────────────
data class GroceryMonthData(
    @DocumentId
    val id: String                       = "",
    val month: String                    = "",
    val items: List<GroceryMonthEntry>   = emptyList(),
    val confirmedAt: Date?               = null,
    val transactionIds: List<String>     = emptyList(),
    val totalAmount: Double              = 0.0,
    val accountId: String                = "",
    val note: String                     = "",
    val recordedItemIds: List<String>    = emptyList()
)
package com.hasan.nisabwallet.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class ShoppingCart(
    @DocumentId
    val id: String          = "",
    val cartId: String      = "",
    val name: String        = "",
    val status: String      = "draft",   // "draft" | "completed"
    val totalAmount: Double = 0.0,
    val totalItems: Int     = 0,
    val confirmedItems: Int = 0,
    val pendingItems: Int   = 0,
    @ServerTimestamp
    val createdAt: Date?    = null,
    @ServerTimestamp
    val updatedAt: Date?    = null
)

data class ShoppingItem(
    @DocumentId
    val id: String          = "",
    val name: String        = "",
    val quantity: Double    = 1.0,
    val unit: String        = "pcs",
    val estimatedPrice: Double = 0.0,
    val actualPrice: Double = 0.0,
    val category: String    = "",
    val status: String      = "pending",  // "pending" | "confirmed" | "skipped"
    val notes: String       = "",
    @ServerTimestamp
    val createdAt: Date?    = null
) {
    val displayPrice: Double get() = if (actualPrice > 0) actualPrice else estimatedPrice
    val total: Double        get() = displayPrice * quantity
}

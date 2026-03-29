package com.hasan.nisabwallet.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Category(
    @DocumentId
    val id: String          = "",
    val categoryId: String  = "",
    val name: String        = "",
    val type: String        = "",   // "Income" | "Expense"
    val color: String       = "#6B7280",
    val isSystem: Boolean   = false,
    val isDefault: Boolean  = false,
    val isRiba: Boolean     = false,
    @ServerTimestamp
    val createdAt: Date?    = null
)

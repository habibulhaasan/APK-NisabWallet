package com.hasan.nisabwallet.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents a transfer between two accounts.
 * Both accounts are updated atomically via Firestore batch write.
 */
data class Transfer(
    @DocumentId
    val id: String              = "",
    val transferId: String      = "",
    val fromAccountId: String   = "",
    val fromAccountName: String = "",
    val toAccountId: String     = "",
    val toAccountName: String   = "",
    val amount: Double          = 0.0,
    val description: String     = "",
    val date: String            = "",   // ISO yyyy-MM-dd
    @ServerTimestamp
    val createdAt: Date?        = null
)

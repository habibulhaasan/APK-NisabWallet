package com.hasan.nisabwallet.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class ZakatCycle(
    @DocumentId
    val id: String              = "",
    val cycleId: String         = "",
    val startDate: String       = "",      // Gregorian ISO
    val hijriStartDate: String  = "",      // e.g. "15/9/1445"
    val status: String          = "active",// "active" | "paid" | "completed"
    val startWealth: Double     = 0.0,
    val nisabThreshold: Double  = 0.0,
    val zakatAmount: Double     = 0.0,
    val paidAmount: Double      = 0.0,
    @ServerTimestamp
    val createdAt: Date?        = null,
    @ServerTimestamp
    val updatedAt: Date?        = null
)

data class ZakatPayment(
    @DocumentId
    val id: String          = "",
    val paymentId: String   = "",
    val cycleId: String     = "",
    val amount: Double      = 0.0,
    val accountId: String   = "",
    val accountName: String = "",
    val paymentDate: String = "",
    val notes: String       = "",
    @ServerTimestamp
    val createdAt: Date?    = null
)

data class NisabSettings(
    val nisabThreshold: Double  = 0.0,
    val silverPerGram: Double   = 0.0,
    val goldPerGram: Double     = 0.0,
    val lastUpdated: String     = ""
)

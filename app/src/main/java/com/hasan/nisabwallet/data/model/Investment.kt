package com.hasan.nisabwallet.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Investment(
    @DocumentId
    val id: String              = "",
    val investmentId: String    = "",
    val name: String            = "",
    val type: String            = "other",
    val purchasePrice: Double   = 0.0,
    val currentValue: Double    = 0.0,
    val quantity: Double        = 1.0,
    val purchaseDate: String    = "",
    val maturityDate: String    = "",
    val status: String          = "active",   // "active"|"matured"|"sold"|"closed"
    val notes: String           = "",
    val dividends: List<DividendEntry> = emptyList(),
    val totalDividends: Double  = 0.0,
    val broker: String          = "",
    val accountId: String       = "",
    val accountName: String     = "",
    @ServerTimestamp
    val createdAt: Date?        = null,
    @ServerTimestamp
    val updatedAt: Date?        = null
) {
    val totalInvested: Double      get() = purchasePrice * quantity
    val totalCurrentValue: Double  get() = currentValue * quantity
    val absoluteReturn: Double     get() = totalCurrentValue - totalInvested
    val percentageReturn: Double   get() = if (totalInvested > 0) (absoluteReturn / totalInvested) * 100 else 0.0
    val totalReturns: Double       get() = absoluteReturn + totalDividends
    val isProfit: Boolean          get() = absoluteReturn >= 0

    val typeLabel: String get() = when (type) {
        "stock"                -> "Stock"
        "mutual_fund"          -> "Mutual Fund"
        "dps"                  -> "DPS"
        "fdr"                  -> "FDR"
        "savings_certificate"  -> "Savings Certificate"
        "bond"                 -> "Bond"
        "ppf"                  -> "PPF"
        "pension_fund"         -> "Pension Fund"
        "crypto"               -> "Cryptocurrency"
        "real_estate"          -> "Real Estate"
        "gold"                 -> "Gold"
        else                   -> "Other"
    }

    val typeColor: String get() = when (type) {
        "stock"               -> "#3B82F6"
        "mutual_fund"         -> "#10B981"
        "dps"                 -> "#F59E0B"
        "fdr"                 -> "#8B5CF6"
        "savings_certificate" -> "#EC4899"
        "bond"                -> "#6366F1"
        "ppf"                 -> "#14B8A6"
        "pension_fund"        -> "#F97316"
        "crypto"              -> "#EAB308"
        "real_estate"         -> "#84CC16"
        "gold"                -> "#FBBF24"
        else                  -> "#6B7280"
    }
}

data class DividendEntry(
    val amount: Double   = 0.0,
    val date: String     = "",
    val notes: String    = "",
    val recordedAt: String = ""
)

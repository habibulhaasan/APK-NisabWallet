package com.hasan.nisabwallet.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Mirrors the web app's account structure from firestoreCollections.js
 *
 * Account types: cash | bank | mobile_banking | savings |
 *                investment | credit_card | other
 */
data class Account(
    @DocumentId
    val id: String           = "",
    val accountId: String    = "",
    val name: String         = "",
    val type: String         = "cash",
    val balance: Double      = 0.0,
    val currency: String     = "BDT",
    val color: String        = "#10B981",
    val icon: String         = "wallet",
    val isDefault: Boolean   = false,
    val ribaBalance: Double  = 0.0,    // accumulated interest — excluded from Zakat
    val description: String  = "",
    @ServerTimestamp
    val createdAt: Date?     = null,
    @ServerTimestamp
    val updatedAt: Date?     = null
) {
    /** Balance minus Riba — used for Zakat calculation */
    val purifiedBalance: Double get() = maxOf(0.0, balance - ribaBalance)

    /** Display-friendly type label */
    val typeLabel: String get() = when (type) {
        "cash"           -> "Cash"
        "bank"           -> "Bank Account"
        "mobile_banking" -> "Mobile Banking"
        "savings"        -> "Savings Account"
        "investment"     -> "Investment Account"
        "credit_card"    -> "Credit Card"
        else             -> "Other"
    }
}

/** Account types available for creation */
enum class AccountType(val key: String, val label: String, val color: String) {
    CASH("cash",                    "Cash",             "#10B981"),
    BANK("bank",                    "Bank Account",     "#3B82F6"),
    MOBILE_BANKING("mobile_banking","Mobile Banking",   "#8B5CF6"),
    SAVINGS("savings",              "Savings Account",  "#F59E0B"),
    INVESTMENT("investment",        "Investment",       "#06B6D4"),
    CREDIT_CARD("credit_card",      "Credit Card",      "#EF4444"),
    OTHER("other",                  "Other",            "#6B7280");

    companion object {
        fun fromKey(key: String) = values().firstOrNull { it.key == key } ?: CASH
    }
}

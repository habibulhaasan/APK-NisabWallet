package com.hasan.nisabwallet.ui.screens.transactions

data class TransactionFilter(
    val type: String       = "All",       // "All" | "Income" | "Expense"
    val categoryId: String = "",          // "" = all categories
    val accountId: String  = "",          // "" = all accounts
    val startDate: String  = "",          // ISO yyyy-MM-dd, "" = no filter
    val endDate: String    = "",
    val searchQuery: String = ""
)

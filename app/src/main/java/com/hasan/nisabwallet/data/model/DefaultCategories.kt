package com.hasan.nisabwallet.data.model

/**
 * System categories seeded on first login.
 * Mirrors src/lib/defaultData.js from the web app exactly.
 * isSystem = true → cannot be deleted by the user.
 * isRiba   = true → transactions in this category appear in Riba Tracker.
 */
object DefaultCategories {

    val INCOME: List<Category> = listOf(
        Category(name = "Salary",               type = "Income", color = "#10B981", isSystem = true, isDefault = true),
        Category(name = "Business Income",       type = "Income", color = "#3B82F6", isSystem = true, isDefault = true),
        Category(name = "Interest / Riba",       type = "Income", color = "#F59E0B", isSystem = true, isDefault = true, isRiba = true),
        Category(name = "Jewellery Redemption",  type = "Income", color = "#F59E0B", isSystem = true, isDefault = true),
        Category(name = "Investment Return",     type = "Income", color = "#06B6D4", isSystem = true, isDefault = true),
        Category(name = "Loan Received",         type = "Income", color = "#8B5CF6", isSystem = true, isDefault = true),
        Category(name = "Lending Received",      type = "Income", color = "#84CC16", isSystem = true, isDefault = true),
    )

    val EXPENSE: List<Category> = listOf(
        Category(name = "Food & Dining",         type = "Expense", color = "#EC4899", isSystem = true, isDefault = true),
        Category(name = "Transport",             type = "Expense", color = "#F97316", isSystem = true, isDefault = true),
        Category(name = "Housing & Rent",        type = "Expense", color = "#8B5CF6", isSystem = true, isDefault = true),
        Category(name = "Utilities",             type = "Expense", color = "#06B6D4", isSystem = true, isDefault = true),
        Category(name = "Healthcare",            type = "Expense", color = "#EF4444", isSystem = true, isDefault = true),
        Category(name = "Education",             type = "Expense", color = "#3B82F6", isSystem = true, isDefault = true),
        Category(name = "Shopping",              type = "Expense", color = "#F59E0B", isSystem = true, isDefault = true),
        Category(name = "Entertainment",         type = "Expense", color = "#6366F1", isSystem = true, isDefault = true),
        Category(name = "Loan Payment",          type = "Expense", color = "#64748B", isSystem = true, isDefault = true),
        Category(name = "Lending Given",         type = "Expense", color = "#84CC16", isSystem = true, isDefault = true),
        Category(name = "Zakat Payment",         type = "Expense", color = "#10B981", isSystem = true, isDefault = true),
        Category(name = "Sadaqah / Charity",     type = "Expense", color = "#10B981", isSystem = true, isDefault = true),
        Category(name = "Jewellery Purchase",    type = "Expense", color = "#FBBF24", isSystem = true, isDefault = true),
        Category(name = "Investment Purchase",   type = "Expense", color = "#06B6D4", isSystem = true, isDefault = true),
    )

    val ALL: List<Category> = INCOME + EXPENSE
}

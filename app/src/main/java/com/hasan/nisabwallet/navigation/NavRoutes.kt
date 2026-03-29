package com.hasan.nisabwallet.navigation

/**
 * Single source of truth for all navigation routes.
 * Using sealed class + object/data class pattern for type safety.
 */
sealed class Screen(val route: String) {

    // ── Auth ──────────────────────────────────────────────────────────────
    object Login           : Screen("login")
    object Register        : Screen("register")
    object ForgotPassword  : Screen("forgot_password")

    // ── Main ─────────────────────────────────────────────────────────────
    object Dashboard       : Screen("dashboard")

    // ── Finance ──────────────────────────────────────────────────────────
    object Accounts        : Screen("accounts")
    object Transactions    : Screen("transactions")
    object Transfer        : Screen("transfer")
    object Categories      : Screen("categories")

    // ── Feature modules ──────────────────────────────────────────────────
    object Budgets         : Screen("budgets")
    object BudgetDetail    : Screen("budget_detail/{categoryId}") {
        fun createRoute(categoryId: String) = "budget_detail/$categoryId"
    }

    object Loans           : Screen("loans")
    object LoanDetail      : Screen("loan_detail/{loanId}") {
        fun createRoute(loanId: String) = "loan_detail/$loanId"
    }

    object Lending         : Screen("lending")
    object LendingDetail   : Screen("lending_detail/{lendingId}") {
        fun createRoute(lendingId: String) = "lending_detail/$lendingId"
    }

    object Goals           : Screen("goals")
    object GoalDetail      : Screen("goal_detail/{goalId}") {
        fun createRoute(goalId: String) = "goal_detail/$goalId"
    }

    object Investments     : Screen("investments")
    object InvestmentDetail : Screen("investment_detail/{investmentId}") {
        fun createRoute(investmentId: String) = "investment_detail/$investmentId"
    }

    object Jewellery       : Screen("jewellery")
    object Zakat           : Screen("zakat")
    object Riba            : Screen("riba")

    // ── Analytics ────────────────────────────────────────────────────────
    object Analytics       : Screen("analytics")

    // ── Recurring ────────────────────────────────────────────────────────
    object Recurring       : Screen("recurring")
    object RecurringDetail : Screen("recurring_detail/{recurringId}") {
        fun createRoute(recurringId: String) = "recurring_detail/$recurringId"
    }
    object RecurringPending : Screen("recurring_pending")

    // ── Shopping ─────────────────────────────────────────────────────────
    object Shopping        : Screen("shopping")
    object ShoppingCart    : Screen("shopping_cart/{cartId}") {
        fun createRoute(cartId: String) = "shopping_cart/$cartId"
    }

    // ── Tax ──────────────────────────────────────────────────────────────
    object Tax             : Screen("tax")
    object TaxDetail       : Screen("tax_detail/{taxYearId}") {
        fun createRoute(taxYearId: String) = "tax_detail/$taxYearId"
    }
    object TaxSetup        : Screen("tax_setup")

    // ── Utility ──────────────────────────────────────────────────────────
    object Feedback        : Screen("feedback")
    object Settings        : Screen("settings")
    object UserGuide       : Screen("user_guide")
}

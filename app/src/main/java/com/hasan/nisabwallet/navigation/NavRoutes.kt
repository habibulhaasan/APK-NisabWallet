package com.hasan.nisabwallet.navigation

sealed class Screen(val route: String) {
    // Auth
    object Login           : Screen("login")
    object Register        : Screen("register")
    object ForgotPassword  : Screen("forgot_password")

    // Bottom Nav Roots
    object Dashboard       : Screen("dashboard")
    object Transactions    : Screen("transactions")
    object Accounts        : Screen("accounts")
    object More            : Screen("more")

    // Finance (accessible from drawer/more)
    object Transfer        : Screen("transfer")
    object Categories      : Screen("categories")

    // Add inside sealed class Screen:
    object DailyExpense  : Screen("daily_expense")
    object MonthlyGrocery: Screen("monthly_grocery")

    // Feature modules
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
    object Analytics       : Screen("analytics")
    object Recurring       : Screen("recurring")
    object RecurringDetail : Screen("recurring_detail/{recurringId}") {
        fun createRoute(recurringId: String) = "recurring_detail/$recurringId"
    }
    object RecurringPending : Screen("recurring_pending")
    object Shopping        : Screen("shopping")
    object ShoppingCart    : Screen("shopping_cart/{cartId}") {
        fun createRoute(cartId: String) = "shopping_cart/$cartId"
    }
    object Tax             : Screen("tax")
    object TaxDetail       : Screen("tax_detail/{taxYearId}") {
        fun createRoute(taxYearId: String) = "tax_detail/$taxYearId"
    }
    object TaxSetup        : Screen("tax_setup")
    object Feedback        : Screen("feedback")
    object Settings        : Screen("settings")
    object UserGuide       : Screen("user_guide")
}

// Bottom navigation items — only 4 to keep it clean
sealed class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Home : BottomNavItem(
        Screen.Dashboard,
        "Home",
        androidx.compose.material.icons.Icons.Outlined.Home,
        androidx.compose.material.icons.Icons.Filled.Home
    )
    object Transactions : BottomNavItem(
        Screen.Transactions,
        "Transactions",
        androidx.compose.material.icons.Icons.Outlined.Receipt,
        androidx.compose.material.icons.Icons.Filled.Receipt
    )
    object Accounts : BottomNavItem(
        Screen.Accounts,
        "Accounts",
        androidx.compose.material.icons.Icons.Outlined.AccountBalance,
        androidx.compose.material.icons.Icons.Filled.AccountBalance
    )
    object More : BottomNavItem(
        Screen.More,
        "More",
        androidx.compose.material.icons.Icons.Outlined.GridView,
        androidx.compose.material.icons.Icons.Filled.GridView
    )
}
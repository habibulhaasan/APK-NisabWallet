package com.hasan.nisabwallet.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Login           : Screen("login")
    object Register        : Screen("register")
    object ForgotPassword  : Screen("forgot_password")
    object Dashboard       : Screen("dashboard")
    object Transactions    : Screen("transactions")
    object Accounts        : Screen("accounts")
    object More            : Screen("more")
    object Transfer        : Screen("transfer")
    object Categories      : Screen("categories")
    object DailyExpense    : Screen("daily_expense")
    object MonthlyGrocery  : Screen("monthly_grocery")
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

sealed class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    object Home : BottomNavItem(
        Screen.Dashboard,
        "Home",
        Icons.Outlined.Home,
        Icons.Filled.Home
    )
    object Transactions : BottomNavItem(
        Screen.Transactions,
        "Transactions",
        Icons.Outlined.ReceiptLong,
        Icons.Filled.Receipt
    )
    object Accounts : BottomNavItem(
        Screen.Accounts,
        "Accounts",
        Icons.Outlined.AccountBalanceWallet,
        Icons.Filled.AccountBalance
    )
    object More : BottomNavItem(
        Screen.More,
        "More",
        Icons.Outlined.GridView,
        Icons.Filled.GridView
    )
}
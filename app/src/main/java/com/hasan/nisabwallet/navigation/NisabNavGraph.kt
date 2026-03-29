package com.hasan.nisabwallet.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hasan.nisabwallet.ui.screens.auth.ForgotPasswordScreen
import com.hasan.nisabwallet.ui.screens.auth.LoginScreen
import com.hasan.nisabwallet.ui.screens.auth.RegisterScreen
import com.hasan.nisabwallet.ui.screens.dashboard.DashboardScreen
import com.hasan.nisabwallet.ui.screens.accounts.AccountsScreen
import com.hasan.nisabwallet.ui.screens.transactions.TransactionsScreen
import com.hasan.nisabwallet.ui.screens.categories.CategoriesScreen
import com.hasan.nisabwallet.ui.screens.transfer.TransferScreen
import com.hasan.nisabwallet.ui.screens.budgets.BudgetsScreen
import com.hasan.nisabwallet.ui.screens.budgets.BudgetDetailScreen
import com.hasan.nisabwallet.ui.screens.loans.LoansScreen
import com.hasan.nisabwallet.ui.screens.loans.LoanDetailScreen
import com.hasan.nisabwallet.ui.screens.lending.LendingScreen
import com.hasan.nisabwallet.ui.screens.lending.LendingDetailScreen
import com.hasan.nisabwallet.ui.screens.goals.GoalsScreen
import com.hasan.nisabwallet.ui.screens.goals.GoalDetailScreen
import com.hasan.nisabwallet.ui.screens.investments.InvestmentsScreen
import com.hasan.nisabwallet.ui.screens.investments.InvestmentDetailScreen
import com.hasan.nisabwallet.ui.screens.jewellery.JewelleryScreen
import com.hasan.nisabwallet.ui.screens.zakat.ZakatScreen
import com.hasan.nisabwallet.ui.screens.riba.RibaScreen
import com.hasan.nisabwallet.ui.screens.analytics.AnalyticsScreen
import com.hasan.nisabwallet.ui.screens.recurring.RecurringScreen
import com.hasan.nisabwallet.ui.screens.recurring.RecurringDetailScreen
import com.hasan.nisabwallet.ui.screens.recurring.RecurringPendingScreen
import com.hasan.nisabwallet.ui.screens.shopping.ShoppingScreen
import com.hasan.nisabwallet.ui.screens.shopping.ShoppingCartScreen
import com.hasan.nisabwallet.ui.screens.tax.TaxScreen
import com.hasan.nisabwallet.ui.screens.tax.TaxDetailScreen
import com.hasan.nisabwallet.ui.screens.tax.TaxSetupScreen
import com.hasan.nisabwallet.ui.screens.feedback.FeedbackScreen
import com.hasan.nisabwallet.ui.screens.settings.SettingsScreen
import com.hasan.nisabwallet.ui.screens.guide.UserGuideScreen
import com.hasan.nisabwallet.ui.viewmodel.AuthState

@Composable
fun NisabNavGraph(
    authState: AuthState,
    navController: NavHostController = rememberNavController()
) {
    if (authState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (authState.isLoggedIn) Screen.Dashboard.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startDestination) {

        // Auth
        composable(Screen.Login.route)          { LoginScreen(navController) }
        composable(Screen.Register.route)       { RegisterScreen(navController) }
        composable(Screen.ForgotPassword.route) { ForgotPasswordScreen(navController) }

        // Main
        composable(Screen.Dashboard.route)      { DashboardScreen(navController) }

        // Finance
        composable(Screen.Accounts.route)       { AccountsScreen(navController) }
        composable(Screen.Transactions.route)   { TransactionsScreen(navController) }
        composable(Screen.Transfer.route)       { TransferScreen(navController) }
        composable(Screen.Categories.route)     { CategoriesScreen(navController) }

        // Budgets
        composable(Screen.Budgets.route)        { BudgetsScreen(navController) }
        composable(Screen.BudgetDetail.route,
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) { BudgetDetailScreen(navController, it.arguments?.getString("categoryId") ?: "") }

        // Loans
        composable(Screen.Loans.route)          { LoansScreen(navController) }
        composable(Screen.LoanDetail.route,
            arguments = listOf(navArgument("loanId") { type = NavType.StringType })
        ) { LoanDetailScreen(navController, it.arguments?.getString("loanId") ?: "") }

        // Lending
        composable(Screen.Lending.route)        { LendingScreen(navController) }
        composable(Screen.LendingDetail.route,
            arguments = listOf(navArgument("lendingId") { type = NavType.StringType })
        ) { LendingDetailScreen(navController, it.arguments?.getString("lendingId") ?: "") }

        // Goals
        composable(Screen.Goals.route)          { GoalsScreen(navController) }
        composable(Screen.GoalDetail.route,
            arguments = listOf(navArgument("goalId") { type = NavType.StringType })
        ) { GoalDetailScreen(navController, it.arguments?.getString("goalId") ?: "") }

        // Investments
        composable(Screen.Investments.route)    { InvestmentsScreen(navController) }
        composable(Screen.InvestmentDetail.route,
            arguments = listOf(navArgument("investmentId") { type = NavType.StringType })
        ) { InvestmentDetailScreen(navController, it.arguments?.getString("investmentId") ?: "") }

        // Feature modules
        composable(Screen.Jewellery.route)      { JewelleryScreen(navController) }
        composable(Screen.Zakat.route)          { ZakatScreen(navController) }
        composable(Screen.Riba.route)           { RibaScreen(navController) }
        composable(Screen.Analytics.route)      { AnalyticsScreen(navController) }

        // Recurring
        composable(Screen.Recurring.route)      { RecurringScreen(navController) }
        composable(Screen.RecurringPending.route) { RecurringPendingScreen(navController) }
        composable(Screen.RecurringDetail.route,
            arguments = listOf(navArgument("recurringId") { type = NavType.StringType })
        ) { RecurringDetailScreen(navController, it.arguments?.getString("recurringId") ?: "") }

        // Shopping
        composable(Screen.Shopping.route)       { ShoppingScreen(navController) }
        composable(Screen.ShoppingCart.route,
            arguments = listOf(navArgument("cartId") { type = NavType.StringType })
        ) { ShoppingCartScreen(navController, it.arguments?.getString("cartId") ?: "") }

        // Tax
        composable(Screen.Tax.route)            { TaxScreen(navController) }
        composable(Screen.TaxSetup.route)       { TaxSetupScreen(navController) }
        composable(Screen.TaxDetail.route,
            arguments = listOf(navArgument("taxYearId") { type = NavType.StringType })
        ) { TaxDetailScreen(navController, it.arguments?.getString("taxYearId") ?: "") }

        // Utility
        composable(Screen.Feedback.route)       { FeedbackScreen(navController) }
        composable(Screen.Settings.route)       { SettingsScreen(navController) }
        composable(Screen.UserGuide.route)      { UserGuideScreen(navController) }
    }
}

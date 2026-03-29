package com.hasan.nisabwallet.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hasan.nisabwallet.data.model.Account
import com.hasan.nisabwallet.navigation.Screen
import com.hasan.nisabwallet.ui.theme.Emerald600
import com.hasan.nisabwallet.utils.ZakatStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Assalamu Alaikum,", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(uiState.userName, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing, onRefresh = viewModel::refresh,
            modifier     = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {

                // ── Net worth hero ────────────────────────────────────────
                item {
                    Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 24.dp, vertical = 28.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("Net Worth", color = MaterialTheme.colorScheme.onPrimary.copy(0.8f),
                                style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(6.dp))
                            Text("৳${"%,.0f".format(uiState.netWorth)}", fontSize = 40.sp,
                                fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary)
                            Text("Total balance: ৳${"%,.0f".format(uiState.totalBalance)}",
                                color = MaterialTheme.colorScheme.onPrimary.copy(0.7f),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // ── Monthly summary ───────────────────────────────────────
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(uiState.currentMonth, style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth()) {
                                MonthStat("Income", uiState.thisMonthIncome, Color(0xFF10B981), Icons.Default.TrendingUp, Modifier.weight(1f))
                                VerticalDivider(Modifier.height(48.dp).padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.outline.copy(0.2f))
                                MonthStat("Expense", uiState.thisMonthExpense, Color(0xFFEF4444), Icons.Default.TrendingDown, Modifier.weight(1f))
                                VerticalDivider(Modifier.height(48.dp).padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.outline.copy(0.2f))
                                MonthStat("Saved", maxOf(0.0, uiState.thisMonthIncome - uiState.thisMonthExpense),
                                    MaterialTheme.colorScheme.primary, Icons.Default.Savings, Modifier.weight(1f))
                            }
                        }
                    }
                }

                // ── Zakat widget ──────────────────────────────────────────
                item { ZakatWidget(uiState, onClick = { navController.navigate(Screen.Zakat.route) }) }

                // ── Quick actions ─────────────────────────────────────────
                item {
                    Text("Quick actions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))
                }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val actions = listOf(
                            Triple(Icons.Default.Add,            "Add Txn",     Screen.Transactions.route),
                            Triple(Icons.Default.SwapHoriz,      "Transfer",    Screen.Transfer.route),
                            Triple(Icons.Default.AccountBalance, "Accounts",    Screen.Accounts.route),
                            Triple(Icons.Default.BarChart,       "Analytics",   Screen.Analytics.route),
                            Triple(Icons.Default.ShoppingCart,   "Shopping",    Screen.Shopping.route),
                            Triple(Icons.Default.Star,           "Zakat",       Screen.Zakat.route),
                            Triple(Icons.Default.TrendingUp,     "Invest",      Screen.Investments.route),
                            Triple(Icons.Default.Diamond,        "Jewellery",   Screen.Jewellery.route),
                        )
                        items(actions) { (icon, label, route) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clip(RoundedCornerShape(14.dp))
                                    .clickable { navController.navigate(route) }
                                    .padding(8.dp).width(64.dp)) {
                                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer), Alignment.Center) {
                                    Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp,
                                    textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
                            }
                        }
                    }
                }

                // ── Accounts strip ────────────────────────────────────────
                if (uiState.accounts.isNotEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                            Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("Accounts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { navController.navigate(Screen.Accounts.route) }) {
                                Text("See all", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(uiState.accounts.take(5), key = { it.id }) { account ->
                                val accentColor = runCatching {
                                    Color(android.graphics.Color.parseColor(account.color))
                                }.getOrDefault(Emerald600)
                                Card(modifier = Modifier.width(150.dp), shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = accentColor.copy(0.1f)),
                                    onClick = { navController.navigate(Screen.Accounts.route) }) {
                                    Column(Modifier.padding(14.dp)) {
                                        Icon(Icons.Default.AccountBalanceWallet, null, Modifier.size(20.dp), tint = accentColor)
                                        Spacer(Modifier.height(8.dp))
                                        Text(account.name, style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis, color = accentColor)
                                        Text("৳${"%,.0f".format(account.balance)}", style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Wealth breakdown ──────────────────────────────────────
                val wealthRows = listOf(
                    Triple(Icons.Default.TrendingUp, "Investments", uiState.totalInvestments to Screen.Investments.route),
                    Triple(Icons.Default.Diamond,    "Jewellery",   uiState.totalJewellery   to Screen.Jewellery.route),
                    Triple(Icons.Default.People,     "Lendings",    uiState.totalLendings     to Screen.Lending.route),
                    Triple(Icons.Default.Flag,       "Goals",       uiState.totalGoals        to Screen.Goals.route),
                    Triple(Icons.Default.CreditCard, "Loans owed",  uiState.totalLoans        to Screen.Loans.route),
                ).filter { it.third.first > 0 }

                if (wealthRows.isNotEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(1.dp)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Wealth breakdown", style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                                wealthRows.forEachIndexed { i, (icon, label, pair) ->
                                    val (amount, route) = pair
                                    Row(Modifier.fillMaxWidth().clickable { navController.navigate(route) }
                                        .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.width(10.dp))
                                        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                        Text("৳${"%,.0f".format(amount)}", style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (label == "Loans owed") Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface)
                                        Icon(Icons.Default.ChevronRight, null, Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
                                    }
                                    if (i < wealthRows.lastIndex)
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.12f))
                                }
                            }
                        }
                    }
                }

                // ── Recent transactions ───────────────────────────────────
                if (uiState.recentTransactions.isNotEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                            Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("Recent transactions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { navController.navigate(Screen.Transactions.route) }) {
                                Text("See all", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    items(uiState.recentTransactions, key = { it.id }) { txn ->
                        val catColor = runCatching { Color(android.graphics.Color.parseColor(txn.categoryColor)) }.getOrDefault(Emerald600)
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp), shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(0.5.dp)) {
                            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(catColor.copy(0.15f)), Alignment.Center) {
                                    Text(txn.categoryName.take(1).uppercase(), color = catColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(txn.categoryName, style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(txn.date, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${if (txn.isIncome) "+" else "-"}৳${"%,.0f".format(txn.amount)}",
                                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold,
                                    color = if (txn.isIncome) Color(0xFF10B981) else Color(0xFFEF4444))
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun MonthStat(label: String, amount: Double, color: Color, icon: ImageVector, modifier: Modifier) {
    Column(modifier.padding(horizontal = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(14.dp), tint = color)
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
        Spacer(Modifier.height(4.dp))
        Text("৳${"%,.0f".format(amount)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ZakatWidget(uiState: DashboardUiState, onClick: () -> Unit) {
    val statusColor = runCatching { Color(android.graphics.Color.parseColor(uiState.zakatStatus.colorHex)) }.getOrDefault(Emerald600)
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable(onClick = onClick), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(0.08f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(0.3f))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(statusColor.copy(0.15f)), Alignment.Center) {
                Icon(Icons.Default.Star, null, Modifier.size(22.dp), tint = statusColor)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Zakat", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(uiState.zakatStatus.label, style = MaterialTheme.typography.bodySmall,
                    color = statusColor, fontWeight = FontWeight.SemiBold)
                when (uiState.zakatStatus) {
                    ZakatStatus.DUE ->
                        if (uiState.zakatAmount > 0)
                            Text("Due: ৳${"%,.0f".format(uiState.zakatAmount)}", style = MaterialTheme.typography.labelSmall,
                                color = statusColor, fontWeight = FontWeight.SemiBold)
                    ZakatStatus.MONITORING ->
                        if (uiState.daysUntilZakat > 0)
                            Text("${uiState.daysUntilZakat} days remaining", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> {}
                }
            }
            Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

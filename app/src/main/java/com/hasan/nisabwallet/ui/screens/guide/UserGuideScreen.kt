package com.hasan.nisabwallet.ui.screens.guide

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hasan.nisabwallet.ui.theme.Emerald600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserGuideScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Guide", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Getting Started with Nisab Wallet",
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Nisab Wallet is an Islamic personal finance tracker. Here's how to get started:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val sections = listOf(
                GuideSection(Icons.Default.AccountBalance, "Accounts",
                    "Create accounts for your cash, bank, and mobile banking. All transactions and transfers link to accounts. Your account balance updates automatically."),
                GuideSection(Icons.Default.ReceiptLong, "Transactions",
                    "Record your income and expenses. Assign each transaction a category. The account balance is updated instantly. Riba (interest) income is automatically flagged."),
                GuideSection(Icons.Default.SwapHoriz, "Transfers",
                    "Move money between your own accounts. Both balances update atomically — no manual adjustments needed."),
                GuideSection(Icons.Default.Star, "Zakat",
                    "Set your Nisab threshold. Start a monitoring cycle when your wealth crosses Nisab. The app tracks one Hijri year and alerts you when Zakat is due (2.5% of zakatable wealth)."),
                GuideSection(Icons.Default.Warning, "Riba Tracker",
                    "Any income categorised as 'Interest / Riba' appears here. Give the amount as Sadaqah to purify your wealth. The app records the Sadaqah as an expense automatically."),
                GuideSection(Icons.Default.PieChart, "Budgets",
                    "Set monthly spending limits per category. The budget bar turns amber at 80% and red when exceeded. Copy budgets to next month with one tap."),
                GuideSection(Icons.Default.TrendingUp, "Investments",
                    "Track stocks, mutual funds, DPS, FDR, savings certificates, gold, crypto, and more. Record dividends and update market values to see live returns."),
                GuideSection(Icons.Default.Diamond, "Jewellery",
                    "Enter weight in Vori, Ana, Roti, and Point — the app converts to grams automatically. Toggle Zakat inclusion per item (worn jewellery may be exempt — consult your scholar)."),
                GuideSection(Icons.Default.Repeat, "Recurring",
                    "Set up daily, weekly, monthly, or yearly transactions. Overdue items show in the pending queue for one-tap execution."),
                GuideSection(Icons.Default.ShoppingCart, "Shopping Lists",
                    "Create multiple shopping lists. Check off items as you shop. The app tracks your estimated total."),
            )

            items(sections.size) { i ->
                val s = sections[i]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(s.icon, null, Modifier.size(22.dp).padding(top = 2.dp), tint = Emerald600)
                        Column {
                            Text(s.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text(s.body, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

private data class GuideSection(val icon: ImageVector, val title: String, val body: String)

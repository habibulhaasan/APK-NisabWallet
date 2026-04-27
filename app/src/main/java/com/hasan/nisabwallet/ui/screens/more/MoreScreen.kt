package com.hasan.nisabwallet.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hasan.nisabwallet.navigation.Screen
import com.hasan.nisabwallet.ui.viewmodel.AuthViewModel

private data class MoreItem(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val route: String,
    val badge: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val sections = listOf(
        "Finance" to listOf(
            MoreItem("Transfer",    Icons.Default.SwapHoriz,      Color(0xFF3B82F6), Screen.Transfer.route),
            MoreItem("Budgets",     Icons.Default.PieChart,       Color(0xFF8B5CF6), Screen.Budgets.route),
            MoreItem("Analytics",   Icons.Default.BarChart,       Color(0xFF06B6D4), Screen.Analytics.route),
            MoreItem("Categories",  Icons.Default.Category,       Color(0xFF6B7280), Screen.Categories.route),
            MoreItem("Recurring",   Icons.Default.Repeat,         Color(0xFF10B981), Screen.Recurring.route),
        ),
        "Wealth" to listOf(
            MoreItem("Investments", Icons.Default.TrendingUp,     Color(0xFF3B82F6), Screen.Investments.route),
            MoreItem("Jewellery",   Icons.Default.Diamond,        Color(0xFFFBBF24), Screen.Jewellery.route),
            MoreItem("Goals",       Icons.Default.TrackChanges,   Color(0xFF10B981), Screen.Goals.route),
            MoreItem("Loans",       Icons.Default.AccountBalance, Color(0xFFEF4444), Screen.Loans.route),
            MoreItem("Lendings",    Icons.Default.People,         Color(0xFF84CC16), Screen.Lending.route),
        ),
        "Islamic Finance" to listOf(
            MoreItem("Zakat",       Icons.Default.Star,           Color(0xFF059669), Screen.Zakat.route),
            MoreItem("Riba",        Icons.Default.Warning,        Color(0xFFF59E0B), Screen.Riba.route),
        ),
        // Replace the existing Utilities section items list:
        "Utilities" to listOf(
            MoreItem("Shopping",       Icons.Default.ShoppingCart,   Color(0xFFEC4899), Screen.Shopping.route),
            MoreItem("Daily Expense",  Icons.Default.BarChart,       Color(0xFF3B82F6), Screen.DailyExpense.route),
            MoreItem("Grocery Plan",   Icons.Default.LocalGroceryStore, Color(0xFF10B981), Screen.MonthlyGrocery.route),
            MoreItem("Tax File",       Icons.Default.Description,    Color(0xFF6366F1), Screen.Tax.route, badge = "Soon"),
        ),
        "App" to listOf(
            MoreItem("Settings",    Icons.Default.Settings,       Color(0xFF6B7280), Screen.Settings.route),
            MoreItem("User Guide",  Icons.Default.MenuBook,       Color(0xFF3B82F6), Screen.UserGuide.route),
            MoreItem("Feedback",    Icons.Default.Feedback,       Color(0xFF10B981), Screen.Feedback.route),
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("More", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            sections.forEach { (sectionTitle, items) ->
                MoreSection(
                    title    = sectionTitle,
                    items    = items,
                    onNavigate = { navController.navigate(it) }
                )
            }

            // Sign out button at bottom
            OutlinedButton(
                onClick  = { authViewModel.signOut() },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFEF4444)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)
                )
            ) {
                Icon(Icons.Default.ExitToApp, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MoreSection(
    title: String,
    items: List<MoreItem>,
    onNavigate: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style    = MaterialTheme.typography.labelMedium,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        LazyVerticalGrid(
            columns             = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement   = Arrangement.spacedBy(10.dp),
            // Grid inside ScrollView needs a fixed height — calculate from item count
            modifier = Modifier.height(
                (((items.size + 2) / 3) * 96 + ((items.size + 2) / 3 - 1) * 10).dp
            )
        ) {
            items(items) { item ->
                MoreGridItem(item = item, onClick = { onNavigate(item.route) })
            }
        }
    }
}

@Composable
private fun MoreGridItem(item: MoreItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(item.color.copy(alpha = 0.08f))
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(item.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier    = Modifier.size(22.dp),
                        tint        = item.color
                    )
                }
                // Badge
                item.badge?.let { badge ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(badge, fontSize = 7.sp, color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(
                item.label,
                style     = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color     = MaterialTheme.colorScheme.onSurface,
                fontSize  = 11.sp,
                maxLines  = 1
            )
        }
    }
}
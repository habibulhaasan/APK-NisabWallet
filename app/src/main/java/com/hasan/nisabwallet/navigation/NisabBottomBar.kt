package com.hasan.nisabwallet.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NisabBottomBar(
    currentRoute: String,
    onNavigate: (Screen) -> Unit
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Transactions,
        BottomNavItem.Accounts,
        BottomNavItem.More
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier       = Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        items.forEach { item ->
            val selected = when (item) {
                is BottomNavItem.Home         -> currentRoute == Screen.Dashboard.route
                is BottomNavItem.Transactions -> currentRoute == Screen.Transactions.route
                is BottomNavItem.Accounts     -> currentRoute == Screen.Accounts.route
                is BottomNavItem.More         -> currentRoute == Screen.More.route ||
                    // Keep "More" highlighted when inside any of its sub-screens
                    currentRoute in listOf(
                        Screen.Budgets.route, Screen.Goals.route, Screen.Loans.route,
                        Screen.Lending.route, Screen.Investments.route, Screen.Jewellery.route,
                        Screen.Zakat.route, Screen.Riba.route, Screen.Analytics.route,
                        Screen.Recurring.route, Screen.Shopping.route, Screen.Settings.route,
                        Screen.Feedback.route, Screen.UserGuide.route, Screen.Tax.route,
                        Screen.Categories.route
                    )
                else -> false
            }

            NavigationBarItem(
                selected = selected,
                onClick  = { onNavigate(item.screen) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        item.label,
                        fontSize   = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = MaterialTheme.colorScheme.primary,
                    selectedTextColor   = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor      = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
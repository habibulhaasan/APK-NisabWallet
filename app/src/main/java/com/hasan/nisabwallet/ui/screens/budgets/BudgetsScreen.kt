package com.hasan.nisabwallet.ui.screens.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hasan.nisabwallet.data.model.Budget
import com.hasan.nisabwallet.ui.components.*
import com.hasan.nisabwallet.ui.components.SimpleDropdown
import com.hasan.nisabwallet.ui.theme.Emerald600
import java.time.Month

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    navController: NavController,
    viewModel: BudgetsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            if (!uiState.showFormSheet) snackbarHostState.showSnackbar(it); viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState.budgets.isNotEmpty()) {
                        IconButton(onClick = viewModel::showCopyDialog) {
                            Icon(Icons.Default.ContentCopy, "Copy to next month")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = viewModel::showAddSheet,
                containerColor = MaterialTheme.colorScheme.primary,
                shape          = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Default.Add, "Add budget", tint = MaterialTheme.colorScheme.onPrimary) }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(Modifier.fillMaxSize().padding(padding)) {

            // ── Month navigator ───────────────────────────────────────────
            MonthNavigator(
                year    = uiState.selectedYear,
                month   = uiState.selectedMonth,
                onPrev  = viewModel::prevMonth,
                onNext  = viewModel::nextMonth
            )

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            // ── Summary strip ─────────────────────────────────────────────
            if (uiState.budgets.isNotEmpty()) {
                BudgetSummaryStrip(
                    totalBudgeted = uiState.totalBudgeted,
                    totalSpent    = uiState.totalSpent,
                    modifier      = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (uiState.budgets.isEmpty()) {
                EmptyState(
                    icon     = Icons.Default.PieChart,
                    title    = "No budgets for this month",
                    subtitle = "Tap + to set spending limits, or copy from a previous month",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier       = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.budgets, key = { it.id }) { budget ->
                        BudgetCard(
                            budget   = budget,
                            onEdit   = { viewModel.showEditSheet(budget) },
                            onDelete = { viewModel.showDeleteDialog(budget) }
                        )
                    }
                }
            }
        }
    }

    // ── Form sheet ────────────────────────────────────────────────────────────
    if (uiState.showFormSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissSheet,
            sheetState       = sheetState,
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            BudgetFormSheet(uiState = uiState, viewModel = viewModel, onCancel = viewModel::dismissSheet)
        }
    }

    // ── Delete dialog ─────────────────────────────────────────────────────────
    if (uiState.showDeleteDialog) {
        ConfirmDialog(
            title         = "Delete Budget",
            message       = "Remove budget for \"${uiState.deletingBudget?.categoryName}\"?",
            confirmText   = "Delete",
            isDestructive = true,
            onConfirm     = viewModel::deleteBudget,
            onDismiss     = viewModel::dismissDeleteDialog
        )
    }

    // ── Copy to next month dialog ─────────────────────────────────────────────
    if (uiState.showCopyDialog) {
        val nextMonth = java.time.LocalDate.of(uiState.selectedYear, uiState.selectedMonth, 1).plusMonths(1)
        AlertDialog(
            onDismissRequest = viewModel::dismissCopyDialog,
            title   = { Text("Copy to next month", fontWeight = FontWeight.SemiBold) },
            text    = {
                Text(
                    "Copy all ${uiState.budgets.size} budgets to " +
                    "${nextMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${nextMonth.year}?"
                )
            },
            confirmButton = {
                Button(onClick = viewModel::copyToNextMonth) {
                    if (uiState.isCopying) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary)
                    } else Text("Copy")
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissCopyDialog) { Text("Cancel") } },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ── Month navigator bar ───────────────────────────────────────────────────────
@Composable
private fun MonthNavigator(year: Int, month: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous month")
        }
        Text(
            "${Month.of(month).name.lowercase().replaceFirstChar { it.uppercase() }} $year",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next month")
        }
    }
}

// ── Budget summary strip ──────────────────────────────────────────────────────
@Composable
private fun BudgetSummaryStrip(
    totalBudgeted: Double,
    totalSpent: Double,
    modifier: Modifier = Modifier
) {
    val remaining = totalBudgeted - totalSpent
    val pct       = if (totalBudgeted > 0) (totalSpent / totalBudgeted).coerceIn(0.0, 1.0) else 0.0
    val barColor  = when {
        pct >= 1.0  -> Color(0xFFEF4444)
        pct >= 0.8  -> Color(0xFFF59E0B)
        else        -> Emerald600
    }

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column {
                    Text("Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("৳${"%,.0f".format(totalSpent)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Budget", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("৳${"%,.0f".format(totalBudgeted)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            LinearProgressIndicator(
                progress       = { pct.toFloat() },
                modifier       = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color          = barColor,
                trackColor     = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                if (remaining >= 0) "৳${"%,.0f".format(remaining)} remaining"
                else "৳${"%,.0f".format(-remaining)} over budget",
                style  = MaterialTheme.typography.bodySmall,
                color  = if (remaining >= 0) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFEF4444),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Budget card ───────────────────────────────────────────────────────────────
@Composable
private fun BudgetCard(budget: Budget, onEdit: () -> Unit, onDelete: () -> Unit) {
    val catColor = runCatching {
        Color(android.graphics.Color.parseColor(budget.categoryColor))
    }.getOrDefault(Emerald600)

    val barColor = when {
        budget.isOverBudget         -> Color(0xFFEF4444)
        budget.spentPercent >= 0.8f -> Color(0xFFF59E0B)
        else                        -> catColor
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(10.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(catColor)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    budget.categoryName,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.weight(1f)
                )
                if (budget.rollover) {
                    Box(
                        Modifier.clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("ROLLOVER", style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(4.dp))
                }
                IconButton(onClick = onEdit,   Modifier.size(28.dp)) { Icon(Icons.Default.Edit,   null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onDelete, Modifier.size(28.dp)) { Icon(Icons.Default.Delete, null, Modifier.size(14.dp), tint = Color(0xFFEF4444)) }
            }

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(
                    "৳${"%,.0f".format(budget.spent)} spent",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (budget.isOverBudget) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "of ৳${"%,.0f".format(budget.amount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress   = { budget.spentPercent },
                modifier   = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color      = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Text(
                if (budget.isOverBudget)
                    "৳${"%,.0f".format(budget.spent - budget.amount)} over budget"
                else
                    "৳${"%,.0f".format(budget.remaining)} remaining · ${(budget.spentPercent * 100).toInt()}% used",
                style  = MaterialTheme.typography.labelSmall,
                color  = if (budget.isOverBudget) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Budget form sheet ─────────────────────────────────────────────────────────
@Composable
private fun BudgetFormSheet(
    uiState: BudgetsUiState,
    viewModel: BudgetsViewModel,
    onCancel: () -> Unit
) {
    val isEdit = uiState.editingBudget != null
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(if (isEdit) "Edit Budget" else "New Budget",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Close") }
        }

        uiState.errorMessage?.let { ErrorCard(it) }

        if (!isEdit) {
            // Filter out categories already budgeted this month
            val budgetedIds = uiState.budgets.map { it.categoryId }.toSet()
            val available   = uiState.categories.filter { it.id !in budgetedIds }
            SimpleDropdown(
                label       = "Category",
                selectedId  = uiState.formCategoryId,
                options     = available.map { it.id to it.name },
                onSelected  = viewModel::onCategoryChange,
                leadingIcon = Icons.Default.Category
            )
        } else {
            OutlinedTextField(
                value = uiState.editingBudget?.categoryName ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        NisabTextField(
            value         = uiState.formAmount,
            onValueChange = viewModel::onAmountChange,
            label         = "Budget limit (৳)",
            leadingIcon   = Icons.Default.MonetizationOn,
            keyboardType  = KeyboardType.Decimal
        )

        NisabTextField(
            value         = uiState.formNotes,
            onValueChange = viewModel::onNotesChange,
            label         = "Notes (optional)",
            leadingIcon   = Icons.Default.Notes,
            singleLine    = false
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("Rollover unspent amount", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("Add remainder to next month's budget", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = uiState.formRollover, onCheckedChange = viewModel::onRolloverChange)
        }

        NisabButton(if (isEdit) "Update Budget" else "Add Budget",
            onClick = viewModel::saveBudget, isLoading = uiState.isSaving)
    }
}

@Composable
fun BudgetDetailScreen(navController: NavController, categoryId: String = "") {
    BudgetsScreen(navController = navController)
}

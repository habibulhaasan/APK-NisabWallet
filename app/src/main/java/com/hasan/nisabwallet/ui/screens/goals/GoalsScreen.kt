package com.hasan.nisabwallet.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hasan.nisabwallet.data.model.Goal
import com.hasan.nisabwallet.ui.components.*
import com.hasan.nisabwallet.ui.components.SimpleDropdown
import com.hasan.nisabwallet.ui.theme.Emerald600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    navController: NavController,
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Goals", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showAddSheet,
                containerColor = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Default.Add, "Add", tint = MaterialTheme.colorScheme.onPrimary) }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // ── Overview card ─────────────────────────────────────────────
            if (uiState.totalTargeted > 0) {
                Card(Modifier.fillMaxWidth().padding(16.dp), RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceAround) {
                        GoalStat("Saved",   "৳${"%,.0f".format(uiState.totalSaved)}",    Emerald600)
                        VerticalDivider(Modifier.height(40.dp))
                        GoalStat("Target",  "৳${"%,.0f".format(uiState.totalTargeted)}",  MaterialTheme.colorScheme.onPrimaryContainer)
                        VerticalDivider(Modifier.height(40.dp))
                        GoalStat("Goals",   "${uiState.goals.size}",                       MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // ── Filter ────────────────────────────────────────────────────
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("active" to "Active", "completed" to "Completed", "all" to "All").forEach { (k, l) ->
                    FilterChip(selected = uiState.filterStatus == k, onClick = { viewModel.setFilter(k) },
                        label = { Text(l, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary))
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }

            if (uiState.goals.isEmpty()) {
                EmptyState(Icons.Default.TrackChanges, "No goals yet",
                    "Set a savings goal and track your progress", Modifier.weight(1f))
            } else {
                LazyColumn(Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(uiState.goals, key = { it.id }) { goal ->
                        GoalCard(goal = goal,
                            onEdit = { viewModel.showEditSheet(goal) },
                            onDelete = { viewModel.showDeleteDialog(goal) },
                            onDeposit = { viewModel.showDepositSheet(goal) },
                            onWithdraw = { viewModel.showWithdrawSheet(goal) })
                    }
                }
            }
        }
    }

    if (uiState.showFormSheet) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissForms, sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            GoalFormSheet(uiState, viewModel, viewModel::dismissForms)
        }
    }
    if (uiState.showDepositSheet) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissForms, sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            AmountSheet("Add to Goal", uiState.selectedGoal?.goalName ?: "",
                "Available: ৳${"%,.0f".format(uiState.availableBalances[uiState.selectedGoal?.linkedAccountId] ?: 0.0)}",
                uiState.transactionAmount, viewModel::onTransactionAmountChange,
                uiState.errorMessage, viewModel::deposit, uiState.isSaving, viewModel::dismissForms)
        }
    }
    if (uiState.showWithdrawSheet) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissForms, sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            AmountSheet("Withdraw from Goal", uiState.selectedGoal?.goalName ?: "",
                "Saved: ৳${"%,.0f".format(uiState.selectedGoal?.currentAmount ?: 0.0)}",
                uiState.transactionAmount, viewModel::onTransactionAmountChange,
                uiState.errorMessage, viewModel::withdraw, uiState.isSaving, viewModel::dismissForms)
        }
    }
    if (uiState.showDeleteDialog) {
        ConfirmDialog("Delete Goal", "Delete \"${uiState.deletingGoal?.goalName}\"? Saved amount stays in your account.",
            "Delete", isDestructive = true, onConfirm = viewModel::deleteGoal, onDismiss = viewModel::dismissDeleteDialog)
    }
}

@Composable
private fun GoalStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GoalCard(goal: Goal, onEdit: () -> Unit, onDelete: () -> Unit, onDeposit: () -> Unit, onWithdraw: () -> Unit) {
    val priorityColor = runCatching { Color(android.graphics.Color.parseColor(goal.priorityColor)) }.getOrDefault(Emerald600)

    Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Circular progress
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { goal.progressPercent },
                        modifier = Modifier.size(56.dp),
                        color = if (goal.isCompleted) Emerald600 else priorityColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 5.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Text("${(goal.progressPercent * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                        fontSize = 10.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(goal.goalName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (goal.isCompleted) {
                            Icon(Icons.Default.CheckCircle, null, Modifier.size(14.dp), tint = Emerald600)
                        }
                    }
                    Text(goal.categoryLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (goal.targetDate.isNotBlank()) {
                        Text("Due ${goal.targetDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onEdit,   Modifier.size(28.dp)) { Icon(Icons.Default.Edit,   null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onDelete, Modifier.size(28.dp)) { Icon(Icons.Default.Delete, null, Modifier.size(14.dp), tint = Color(0xFFEF4444)) }
            }

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column {
                    Text("Saved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("৳${"%,.0f".format(goal.currentAmount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Emerald600)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("৳${"%,.0f".format(goal.remaining)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Target", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("৳${"%,.0f".format(goal.targetAmount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            if (!goal.isCompleted) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onWithdraw, Modifier.weight(1f), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                        Icon(Icons.Default.Remove, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("Withdraw", fontSize = 13.sp)
                    }
                    Button(onClick = onDeposit, Modifier.weight(1f), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                        Icon(Icons.Default.Add, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("Add Funds", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountSheet(
    title: String, subtitle: String, hint: String,
    amount: String, onAmountChange: (String) -> Unit,
    errorMessage: String?, onConfirm: () -> Unit, isLoading: Boolean, onCancel: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Close") }
        }
        errorMessage?.let { ErrorCard(it) }
        Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        NisabTextField(amount, onAmountChange, "Amount (৳)", leadingIcon = Icons.Default.MonetizationOn, keyboardType = KeyboardType.Decimal)
        NisabButton(title, onConfirm, isLoading = isLoading)
    }
}

@Composable
private fun GoalFormSheet(uiState: GoalsUiState, viewModel: GoalsViewModel, onCancel: () -> Unit) {
    val isEdit = uiState.editingGoal != null
    val categories = listOf(
        "emergency_fund" to "Emergency Fund", "house" to "House / Property",
        "car" to "Car / Vehicle", "education" to "Education",
        "hajj" to "Hajj / Umrah", "wedding" to "Wedding",
        "business" to "Business", "retirement" to "Retirement",
        "travel" to "Travel", "gadget" to "Gadget / Electronics", "other" to "Other"
    )
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(if (isEdit) "Edit Goal" else "New Goal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Close") }
        }
        uiState.errorMessage?.let { ErrorCard(it) }
        NisabTextField(uiState.formGoalName, viewModel::onGoalNameChange, "Goal name", leadingIcon = Icons.Default.TrackChanges)
        SimpleDropdown("Category", uiState.formCategory, categories, viewModel::onCategoryChange, Icons.Default.Category)
        NisabTextField(uiState.formTargetAmount, viewModel::onTargetAmountChange, "Target amount (৳)", leadingIcon = Icons.Default.MonetizationOn, keyboardType = KeyboardType.Decimal)
        NisabTextField(uiState.formTargetDate, viewModel::onTargetDateChange, "Target date (yyyy-MM-dd, optional)", leadingIcon = Icons.Default.CalendarToday)
        NisabTextField(uiState.formMonthly, viewModel::onMonthlyChange, "Monthly contribution (৳, optional)", leadingIcon = Icons.Default.CalendarMonth, keyboardType = KeyboardType.Decimal)
        SimpleDropdown("Linked account", uiState.formAccountId,
            uiState.accounts.map { acc ->
                val avail = uiState.availableBalances[acc.id] ?: acc.balance
                acc.id to "${acc.name} · ৳${"%,.0f".format(avail)} available"
            }, viewModel::onAccountChange, Icons.Default.AccountBalance)
        SimpleDropdown("Priority", uiState.formPriority,
            listOf("high" to "High", "medium" to "Medium", "low" to "Low"),
            viewModel::onPriorityChange, Icons.Default.Flag)
        NisabTextField(uiState.formDescription, viewModel::onDescriptionChange, "Description (optional)", leadingIcon = Icons.Default.Notes, singleLine = false)
        NisabButton(if (isEdit) "Update Goal" else "Create Goal", viewModel::saveGoal, isLoading = uiState.isSaving)
    }
}

@Composable
fun GoalDetailScreen(navController: NavController, goalId: String = "") {
    GoalsScreen(navController = navController)
}

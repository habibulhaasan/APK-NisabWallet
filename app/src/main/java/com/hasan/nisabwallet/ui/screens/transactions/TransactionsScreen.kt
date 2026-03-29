package com.hasan.nisabwallet.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hasan.nisabwallet.data.model.Transaction
import com.hasan.nisabwallet.ui.components.*
import com.hasan.nisabwallet.ui.theme.Emerald600
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    navController: NavController,
    viewModel: TransactionsViewModel = hiltViewModel()
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
                title = { Text("Transactions", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { viewModel.showAddForm() },
                containerColor = MaterialTheme.colorScheme.primary,
                shape          = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Default.Add, "Add", tint = MaterialTheme.colorScheme.onPrimary) }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Summary cards ─────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard("Income",  uiState.totalIncome,  Color(0xFF10B981), Modifier.weight(1f))
                SummaryCard("Expense", uiState.totalExpense, Color(0xFFEF4444), Modifier.weight(1f))
            }

            // ── Type filter chips ─────────────────────────────────────────
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("All", "Income", "Expense")) { type ->
                    FilterChip(
                        selected = uiState.filter.type == type,
                        onClick  = { viewModel.updateFilter(uiState.filter.copy(type = type)) },
                        label    = { Text(type, fontSize = 13.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor     = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (uiState.filteredTransactions.isEmpty()) {
                EmptyState(
                    icon     = Icons.Default.ReceiptLong,
                    title    = "No transactions",
                    subtitle = "Tap + to record your first transaction",
                    modifier = Modifier.weight(1f)
                )
            } else {
                // ── Grouped list ──────────────────────────────────────────
                val grouped = uiState.filteredTransactions
                    .groupBy { it.date }
                    .toSortedMap(reverseOrder())

                LazyColumn(
                    modifier       = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp)
                ) {
                    grouped.forEach { (date, txns) ->
                        item(key = "hdr_$date") {
                            DateHeader(
                                date    = date,
                                dayNet  = txns.sumOf { if (it.isIncome) it.amount else -it.amount }
                            )
                        }
                        items(txns, key = { it.id }) { txn ->
                            TransactionItem(txn, onEdit = { viewModel.showEditForm(txn) },
                                onDelete = { viewModel.showDeleteDialog(txn) })
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }

    if (uiState.showFormSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissForm,
            sheetState       = sheetState,
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            TransactionFormSheet(uiState = uiState, viewModel = viewModel, onCancel = viewModel::dismissForm)
        }
    }

    if (uiState.showDeleteDialog) {
        ConfirmDialog(
            title         = "Delete Transaction",
            message       = "Delete this transaction? The account balance will be reversed.",
            confirmText   = "Delete",
            isDestructive = true,
            onConfirm     = viewModel::deleteTransaction,
            onDismiss     = viewModel::dismissDeleteDialog
        )
    }
}

@Composable
private fun SummaryCard(label: String, amount: Double, color: Color, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text("৳${"%,.0f".format(amount)}", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun DateHeader(date: String, dayNet: Double) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(formatDateHeader(date), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        Text(
            "${if (dayNet >= 0) "+" else ""}৳${"%,.0f".format(dayNet)}",
            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
            color = if (dayNet >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
        )
    }
}

@Composable
private fun TransactionItem(
    transaction: Transaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val catColor = runCatching {
        Color(android.graphics.Color.parseColor(transaction.categoryColor))
    }.getOrDefault(Emerald600)

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier         = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                    .background(catColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(transaction.categoryName.take(1).uppercase(), color = catColor,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(transaction.categoryName, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (transaction.description.isNotBlank()) {
                    Text(transaction.description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(transaction.accountName, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    if (transaction.isRiba) {
                        Text("RIBA", style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (transaction.isIncome) "+" else "-"}৳${"%,.0f".format(transaction.amount)}",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = if (transaction.isIncome) Color(0xFF10B981) else Color(0xFFEF4444)
                )
                Row {
                    IconButton(onClick = onEdit,   Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit,   "Edit",   Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    IconButton(onClick = onDelete, Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, "Delete", Modifier.size(14.dp),
                            tint = Color(0xFFEF4444)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionFormSheet(
    uiState: TransactionsUiState,
    viewModel: TransactionsViewModel,
    onCancel: () -> Unit
) {
    val isEdit = uiState.editingTransaction != null
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(if (isEdit) "Edit Transaction" else "New Transaction",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Close") }
        }

        // Income / Expense toggle
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Income" to Color(0xFF10B981), "Expense" to Color(0xFFEF4444)).forEach { (type, color) ->
                val selected = uiState.formType == type
                OutlinedButton(
                    onClick  = { viewModel.onTypeChange(type) },
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) color.copy(0.12f) else Color.Transparent),
                    border   = androidx.compose.foundation.BorderStroke(1.5.dp,
                        if (selected) color else MaterialTheme.colorScheme.outline),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Icon(if (type == "Income") Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        null, Modifier.size(16.dp), tint = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text(type, color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }

        uiState.errorMessage?.let { ErrorCard(it) }

        NisabTextField(uiState.formAmount, viewModel::onAmountChange,
            "Amount (৳)", leadingIcon = Icons.Default.MonetizationOn, keyboardType = KeyboardType.Decimal)

        SimpleDropdown("Account", uiState.formAccountId,
            uiState.accounts.map { it.id to "${it.name} (৳${"%,.0f".format(it.balance)})" },
            viewModel::onAccountChange, Icons.Default.AccountBalance)

        SimpleDropdown("Category", uiState.formCategoryId,
            uiState.categories.filter { it.type == uiState.formType }.map { it.id to it.name },
            viewModel::onCategoryChange, Icons.Default.Category)

        NisabTextField(uiState.formDescription, viewModel::onDescriptionChange,
            "Description (optional)", leadingIcon = Icons.Default.Notes, singleLine = false)

        NisabTextField(uiState.formDate, viewModel::onDateChange,
            "Date (yyyy-MM-dd)", leadingIcon = Icons.Default.CalendarToday)

        NisabButton(if (isEdit) "Update" else "Add Transaction",
            onClick = viewModel::saveTransaction, isLoading = uiState.isSaving)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDropdown(
    label: String,
    selectedId: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == selectedId }?.second ?: ""

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel, onValueChange = {}, readOnly = true,
            label = { Text(label) },
            leadingIcon = leadingIcon?.let { { Icon(it, null, Modifier.size(20.dp)) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text    = { Text("No options available", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = {}
                )
            } else {
                options.forEach { (id, name) ->
                    DropdownMenuItem(
                        text          = { Text(name) },
                        onClick       = { onSelected(id); expanded = false },
                        trailingIcon  = if (id == selectedId) {
                            { Icon(Icons.Default.Check, null, Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary) }
                        } else null
                    )
                }
            }
        }
    }
}

private fun formatDateHeader(dateStr: String): String = try {
    val date  = LocalDate.parse(dateStr)
    val today = LocalDate.now()
    when {
        date == today                -> "Today"
        date == today.minusDays(1)   -> "Yesterday"
        else -> "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${date.year}"
    }
} catch (e: Exception) { dateStr }

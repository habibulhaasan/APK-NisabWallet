package com.hasan.nisabwallet.ui.screens.transactions

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.hasan.nisabwallet.ui.screens.transfer.TransferViewModel
import com.hasan.nisabwallet.ui.screens.transfer.TransferUiState
import com.hasan.nisabwallet.ui.theme.Emerald600
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// Tab indices
private const val TAB_TRANSACTIONS = 0
private const val TAB_TRANSFER     = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    navController: NavController,
    viewModel: TransactionsViewModel  = hiltViewModel(),
    transferViewModel: TransferViewModel = hiltViewModel()
) {
    val uiState         by viewModel.uiState.collectAsStateWithLifecycle()
    val transferUiState by transferViewModel.uiState.collectAsStateWithLifecycle()
    val sheetState      = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab     by remember { mutableIntStateOf(TAB_TRANSACTIONS) }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }
    LaunchedEffect(transferUiState.successMessage) {
        transferUiState.successMessage?.let { snackbarHostState.showSnackbar(it); transferViewModel.clearMessages() }
    }
    LaunchedEffect(transferUiState.errorMessage) {
        transferUiState.errorMessage?.let { snackbarHostState.showSnackbar(it); transferViewModel.clearMessages() }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Transactions", fontWeight = FontWeight.SemiBold) },
                    actions = {
                        // FAB-like add button in the app bar — visible on both tabs
                        if (selectedTab == TAB_TRANSACTIONS) {
                            IconButton(onClick = { viewModel.showAddForm() }) {
                                Icon(Icons.Default.Add, "Add transaction",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                // Tab row sits directly under the app bar
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor   = MaterialTheme.colorScheme.surface,
                    contentColor     = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == TAB_TRANSACTIONS,
                        onClick  = { selectedTab = TAB_TRANSACTIONS },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Receipt, null, Modifier.size(16.dp))
                                Text("Transactions",
                                    fontWeight = if (selectedTab == TAB_TRANSACTIONS)
                                        FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == TAB_TRANSFER,
                        onClick  = { selectedTab = TAB_TRANSFER },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.SwapHoriz, null, Modifier.size(16.dp))
                                Text("Transfer",
                                    fontWeight = if (selectedTab == TAB_TRANSFER)
                                        FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        // Animate between the two tabs
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { it } + fadeIn() togetherWith
                    slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                    slideOutHorizontally { it } + fadeOut()
                }
            },
            modifier = Modifier.padding(padding),
            label    = "tab_animation"
        ) { tab ->
            when (tab) {
                TAB_TRANSACTIONS -> TransactionListContent(
                    uiState   = uiState,
                    viewModel = viewModel
                )
                TAB_TRANSFER -> TransferContent(
                    uiState   = transferUiState,
                    viewModel = transferViewModel
                )
            }
        }
    }

    // Transaction form sheet
    if (uiState.showFormSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissForm,
            sheetState       = sheetState,
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            TransactionFormSheet(
                uiState   = uiState,
                viewModel = viewModel,
                onCancel  = viewModel::dismissForm
            )
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

    if (transferUiState.showDeleteDialog) {
        ConfirmDialog(
            title         = "Delete Transfer",
            message       = "Delete this transfer? Both account balances will be reversed.",
            confirmText   = "Delete",
            isDestructive = true,
            onConfirm     = transferViewModel::deleteTransfer,
            onDismiss     = transferViewModel::dismissDeleteDialog
        )
    }
}

// ── Transactions tab content ──────────────────────────────────────────────────

@Composable
private fun TransactionListContent(
    uiState: TransactionsUiState,
    viewModel: TransactionsViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // Summary cards
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard("Income",  uiState.totalIncome,  Color(0xFF10B981), Modifier.weight(1f))
            SummaryCard("Expense", uiState.totalExpense, Color(0xFFEF4444), Modifier.weight(1f))
        }

        // Type filter chips
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
            return
        }

        if (uiState.filteredTransactions.isEmpty()) {
            EmptyState(
                icon     = Icons.Default.ReceiptLong,
                title    = "No transactions",
                subtitle = "Tap + in the top right to record your first transaction",
                modifier = Modifier.weight(1f)
            )
        } else {
            val grouped = uiState.filteredTransactions
                .groupBy { it.date }
                .toSortedMap(reverseOrder())

            LazyColumn(
                contentPadding = PaddingValues(
                    start  = 16.dp,
                    end    = 16.dp,
                    bottom = 16.dp
                )
            ) {
                grouped.forEach { (date, txns) ->
                    item(key = "hdr_$date") {
                        DateHeader(
                            date   = date,
                            dayNet = txns.sumOf { if (it.isIncome) it.amount else -it.amount }
                        )
                    }
                    items(txns, key = { it.id }) { txn ->
                        TransactionItem(
                            transaction = txn,
                            onEdit      = { viewModel.showEditForm(txn) },
                            onDelete    = { viewModel.showDeleteDialog(txn) }
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

// ── Transfer tab content ──────────────────────────────────────────────────────

@Composable
private fun TransferContent(
    uiState: TransferUiState,
    viewModel: com.hasan.nisabwallet.ui.screens.transfer.TransferViewModel
) {
    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick transfer form card
        item {
            TransferFormCard(uiState = uiState, viewModel = viewModel)
        }

        // Transfer history
        if (uiState.transfers.isNotEmpty()) {
            item {
                Text(
                    "Transfer History",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(top = 4.dp)
                )
            }
            items(uiState.transfers, key = { it.id }) { transfer ->
                com.hasan.nisabwallet.ui.screens.transfer.TransferHistoryItem(
                    transfer = transfer,
                    onDelete = { viewModel.showDeleteDialog(transfer) }
                )
            }
        } else {
            item {
                EmptyState(
                    icon     = Icons.Default.SwapHoriz,
                    title    = "No transfers yet",
                    subtitle = "Move money between your accounts above",
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Transfer form card (inline, compact version) ──────────────────────────────

@Composable
private fun TransferFormCard(
    uiState: TransferUiState,
    viewModel: com.hasan.nisabwallet.ui.screens.transfer.TransferViewModel
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "New Transfer",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            uiState.errorMessage?.let { ErrorCard(it) }

            // From → To visual row
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "From",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val from = uiState.accounts.find { it.id == uiState.fromAccountId }
                    AccountPill(
                        name    = from?.name ?: "Select",
                        balance = from?.balance ?: 0.0,
                        color   = Color(0xFFEF4444)
                    )
                }
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        "To",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val to = uiState.accounts.find { it.id == uiState.toAccountId }
                    AccountPill(
                        name    = to?.name ?: "Select",
                        balance = to?.balance ?: 0.0,
                        color   = Emerald600
                    )
                }
            }

            // Account selectors
            SimpleDropdown(
                label       = "From account",
                selectedId  = uiState.fromAccountId,
                options     = uiState.accounts
                    .filter { it.id != uiState.toAccountId }
                    .map    { it.id to "${it.name} · ৳${"%,.0f".format(it.balance)}" },
                onSelected  = viewModel::onFromAccountChange,
                leadingIcon = Icons.Default.AccountBalance
            )
            SimpleDropdown(
                label       = "To account",
                selectedId  = uiState.toAccountId,
                options     = uiState.accounts
                    .filter { it.id != uiState.fromAccountId }
                    .map    { it.id to "${it.name} · ৳${"%,.0f".format(it.balance)}" },
                onSelected  = viewModel::onToAccountChange,
                leadingIcon = Icons.Default.AccountBalance
            )

            NisabTextField(
                value         = uiState.amount,
                onValueChange = viewModel::onAmountChange,
                label         = "Amount (৳)",
                leadingIcon   = Icons.Default.MonetizationOn,
                keyboardType  = KeyboardType.Decimal
            )
            NisabTextField(
                value         = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label         = "Note (optional)",
                leadingIcon   = Icons.Default.Notes
            )
            NisabTextField(
                value         = uiState.date,
                onValueChange = viewModel::onDateChange,
                label         = "Date (yyyy-MM-dd)",
                leadingIcon   = Icons.Default.CalendarToday
            )

            NisabButton(
                text      = "Transfer",
                onClick   = viewModel::transfer,
                isLoading = uiState.isSaving
            )
        }
    }
}

// Small pill showing account name + balance
@Composable
private fun AccountPill(name: String, balance: Double, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Text(name, style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold, color = color,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("৳${"%,.0f".format(balance)}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Shared sub-composables (same as before, kept here) ───────────────────────

@Composable
private fun SummaryCard(label: String, amount: Double, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
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
        modifier              = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 6.dp),
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
    onEdit:   () -> Unit,
    onDelete: () -> Unit
) {
    val catColor = runCatching {
        Color(android.graphics.Color.parseColor(transaction.categoryColor))
    }.getOrDefault(Emerald600)

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier         = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDelete, Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, "Delete", Modifier.size(14.dp),
                            tint = Color(0xFFEF4444))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionFormSheet(
    uiState:   TransactionsUiState,
    viewModel: TransactionsViewModel,
    onCancel:  () -> Unit
) {
    val isEdit = uiState.editingTransaction != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(if (isEdit) "Edit Transaction" else "New Transaction",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Close") }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Income" to Color(0xFF10B981), "Expense" to Color(0xFFEF4444))
                .forEach { (type, color) ->
                    val selected = uiState.formType == type
                    OutlinedButton(
                        onClick  = { viewModel.onTypeChange(type) },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) color.copy(0.12f) else Color.Transparent
                        ),
                        border   = androidx.compose.foundation.BorderStroke(1.5.dp,
                            if (selected) color else MaterialTheme.colorScheme.outline),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            if (type == "Income") Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            null, Modifier.size(16.dp),
                            tint = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(type,
                            color      = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
        }

        uiState.errorMessage?.let { ErrorCard(it) }

        NisabTextField(uiState.formAmount, viewModel::onAmountChange,
            "Amount (৳)", leadingIcon = Icons.Default.MonetizationOn,
            keyboardType = KeyboardType.Decimal)
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

        NisabButton(
            if (isEdit) "Update" else "Add Transaction",
            onClick    = viewModel::saveTransaction,
            isLoading  = uiState.isSaving
        )
    }
}

private fun formatDateHeader(dateStr: String): String = try {
    val date  = LocalDate.parse(dateStr)
    val today = LocalDate.now()
    when {
        date == today              -> "Today"
        date == today.minusDays(1) -> "Yesterday"
        else -> "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${date.year}"
    }
} catch (e: Exception) { dateStr }
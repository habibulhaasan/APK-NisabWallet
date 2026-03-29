package com.hasan.nisabwallet.ui.screens.lending

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hasan.nisabwallet.data.model.Lending
import com.hasan.nisabwallet.ui.components.*
import com.hasan.nisabwallet.ui.screens.transactions.SimpleDropdown
import com.hasan.nisabwallet.ui.theme.Emerald600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LendingScreen(
    navController: NavController,
    viewModel: LendingViewModel = hiltViewModel()
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
                title = { Text("Lendings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
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

            // ── Summary cards ─────────────────────────────────────────────
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard("Outstanding", uiState.totalActive, Emerald600, Modifier.weight(1f))
                SummaryCard("Overdue",     uiState.totalOverdue, Color(0xFFEF4444), Modifier.weight(1f))
            }

            // ── Filter chips ──────────────────────────────────────────────
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("active" to "Active", "overdue" to "Overdue", "returned" to "Returned", "all" to "All")
                    .forEach { (key, label) ->
                        FilterChip(
                            selected = uiState.filterStatus == key,
                            onClick  = { viewModel.setFilter(key) },
                            label    = { Text(label, fontSize = 12.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor     = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }

            if (uiState.lendings.isEmpty()) {
                EmptyState(
                    icon     = Icons.Default.People,
                    title    = "No lendings found",
                    subtitle = "Tap + to record money you've lent to someone",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier       = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.lendings, key = { it.id }) { lending ->
                        LendingCard(
                            lending    = lending,
                            onEdit     = { viewModel.showEditSheet(lending) },
                            onDelete   = { viewModel.showDeleteDialog(lending) },
                            onRepay    = { viewModel.showRepaymentSheet(lending) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.showFormSheet) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissForms, sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            LendingFormSheet(uiState = uiState, viewModel = viewModel, onCancel = viewModel::dismissForms)
        }
    }

    if (uiState.showRepaymentSheet) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissForms, sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            RepaymentSheet(uiState = uiState, viewModel = viewModel, onCancel = viewModel::dismissForms)
        }
    }

    if (uiState.showDeleteDialog) {
        ConfirmDialog("Delete Lending",
            "Delete lending to \"${uiState.deletingLending?.borrowerName}\"?",
            "Delete", isDestructive = true,
            onConfirm = viewModel::deleteLending, onDismiss = viewModel::dismissDeleteDialog)
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
private fun LendingCard(
    lending: Lending,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRepay: () -> Unit
) {
    val borderColor = when {
        lending.isOverdue    -> Color(0xFFEF4444)
        lending.isReturned   -> Emerald600
        else                 -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(Emerald600.copy(alpha = 0.12f)), Alignment.Center) {
                    Text(lending.borrowerName.take(1).uppercase(),
                        color = Emerald600, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(lending.borrowerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (lending.isOverdue) {
                            Box(Modifier.clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("OVERDUE", style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                            }
                        }
                        if (lending.isReturned) {
                            Box(Modifier.clip(RoundedCornerShape(4.dp))
                                .background(Emerald600.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("RETURNED", style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp, color = Emerald600, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (lending.phone.isNotBlank()) {
                        Text(lending.phone, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onEdit,   Modifier.size(28.dp)) { Icon(Icons.Default.Edit,   null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onDelete, Modifier.size(28.dp)) { Icon(Icons.Default.Delete, null, Modifier.size(14.dp), tint = Color(0xFFEF4444)) }
            }

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column {
                    Text("Lent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("৳${"%,.0f".format(lending.amount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                if (lending.dueDate.isNotBlank()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(lending.dueDate, style = MaterialTheme.typography.bodySmall,
                            color = if (lending.isOverdue) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("৳${"%,.0f".format(lending.remainingAmount)}", style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold, color = Emerald600)
                }
            }

            LinearProgressIndicator(
                progress   = { lending.progressPercent },
                modifier   = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color      = Emerald600,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("${(lending.progressPercent * 100).toInt()}% returned · ${lending.repaymentHistory.size} payments",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (lending.countForZakat) {
                    Box(Modifier.clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("ZAKAT", style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp, color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!lending.isReturned) {
                Button(onClick = onRepay, Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
                    Icon(Icons.Default.Payments, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Record Repayment")
                }
            }
        }
    }
}

@Composable
private fun LendingFormSheet(uiState: LendingUiState, viewModel: LendingViewModel, onCancel: () -> Unit) {
    val isEdit = uiState.editingLending != null
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(if (isEdit) "Edit Lending" else "New Lending", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Close") }
        }
        uiState.errorMessage?.let { ErrorCard(it) }
        NisabTextField(uiState.formBorrowerName, viewModel::onBorrowerNameChange, "Borrower name", leadingIcon = Icons.Default.Person)
        NisabTextField(uiState.formPhone, viewModel::onPhoneChange, "Phone number (optional)", leadingIcon = Icons.Default.Phone, keyboardType = KeyboardType.Phone)
        if (!isEdit) {
            NisabTextField(uiState.formAmount, viewModel::onAmountChange, "Amount (৳)", leadingIcon = Icons.Default.MonetizationOn, keyboardType = KeyboardType.Decimal)
            SimpleDropdown("Account to debit", uiState.formAccountId,
                uiState.accounts.map { it.id to "${it.name} · ৳${"%,.0f".format(it.balance)}" },
                viewModel::onAccountChange, Icons.Default.AccountBalance)
        }
        NisabTextField(uiState.formDueDate, viewModel::onDueDateChange, "Due date (yyyy-MM-dd, optional)", leadingIcon = Icons.Default.CalendarToday)
        NisabTextField(uiState.formPurpose, viewModel::onPurposeChange, "Purpose (optional)", leadingIcon = Icons.Default.Info)
        NisabTextField(uiState.formNotes, viewModel::onNotesChange, "Notes (optional)", leadingIcon = Icons.Default.Notes, singleLine = false)
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text("Count for Zakat", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("Include this lending in your Zakatable wealth", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = uiState.formCountForZakat, onCheckedChange = viewModel::onZakatChange)
        }
        NisabButton(if (isEdit) "Update" else "Record Lending", viewModel::saveLending, isLoading = uiState.isSaving)
    }
}

@Composable
private fun RepaymentSheet(uiState: LendingUiState, viewModel: LendingViewModel, onCancel: () -> Unit) {
    val lending = uiState.selectedLending ?: return
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text("Record Repayment", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("From ${lending.borrowerName} · Remaining: ৳${"%,.0f".format(lending.remainingAmount)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Close") }
        }
        uiState.errorMessage?.let { ErrorCard(it) }
        NisabTextField(uiState.repayAmount, viewModel::onRepayAmountChange, "Repayment amount (৳)", leadingIcon = Icons.Default.MonetizationOn, keyboardType = KeyboardType.Decimal)
        NisabTextField(uiState.repayDate, viewModel::onRepayDateChange, "Date (yyyy-MM-dd)", leadingIcon = Icons.Default.CalendarToday)
        NisabTextField(uiState.repayNotes, viewModel::onRepayNotesChange, "Notes (optional)", leadingIcon = Icons.Default.Notes)
        NisabButton("Record Repayment", viewModel::recordRepayment, isLoading = uiState.isSaving)
    }
}

@Composable
fun LendingDetailScreen(navController: NavController, lendingId: String = "") {
    LendingScreen(navController = navController)
}

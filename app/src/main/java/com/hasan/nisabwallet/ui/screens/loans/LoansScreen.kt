package com.hasan.nisabwallet.ui.screens.loans

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
import com.hasan.nisabwallet.data.model.Loan
import com.hasan.nisabwallet.ui.components.*
import com.hasan.nisabwallet.ui.screens.transactions.SimpleDropdown
import com.hasan.nisabwallet.ui.theme.Emerald600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    navController: NavController,
    viewModel: LoansViewModel = hiltViewModel()
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
                title = { Text("Loans", fontWeight = FontWeight.SemiBold) },
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

            // ── Total owing card ──────────────────────────────────────────
            if (uiState.totalActiveLoans > 0) {
                Card(
                    modifier  = Modifier.fillMaxWidth().padding(16.dp),
                    shape     = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)),
                    border    = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalance, null, Modifier.size(32.dp), tint = Color(0xFFEF4444))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Total Outstanding", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("৳${"%,.0f".format(uiState.totalActiveLoans)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        }
                    }
                }
            }

            // ── Status filter chips ───────────────────────────────────────
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("active" to "Active", "paid" to "Paid", "all" to "All").forEach { (key, label) ->
                    FilterChip(
                        selected = uiState.filterStatus == key,
                        onClick  = { viewModel.setFilter(key) },
                        label    = { Text(label, fontSize = 13.sp) },
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

            if (uiState.loans.isEmpty()) {
                EmptyState(
                    icon     = Icons.Default.AccountBalance,
                    title    = if (uiState.filterStatus == "active") "No active loans" else "No loans found",
                    subtitle = "Tap + to record a loan",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier       = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.loans, key = { it.id }) { loan ->
                        LoanCard(
                            loan      = loan,
                            onEdit    = { viewModel.showEditSheet(loan) },
                            onDelete  = { viewModel.showDeleteDialog(loan) },
                            onPayment = { viewModel.showPaymentSheet(loan) }
                        )
                    }
                }
            }
        }
    }

    // ── Loan form sheet ───────────────────────────────────────────────────────
    if (uiState.showFormSheet) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissForms, sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            LoanFormSheet(uiState = uiState, viewModel = viewModel, onCancel = viewModel::dismissForms)
        }
    }

    // ── Payment sheet ─────────────────────────────────────────────────────────
    if (uiState.showPaymentSheet) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissForms, sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            PaymentSheet(uiState = uiState, viewModel = viewModel, onCancel = viewModel::dismissForms)
        }
    }

    if (uiState.showDeleteDialog) {
        ConfirmDialog("Delete Loan",
            "Delete loan from \"${uiState.deletingLoan?.lenderName}\"? Payment history will be lost.",
            "Delete", isDestructive = true,
            onConfirm = viewModel::deleteLoan, onDismiss = viewModel::dismissDeleteDialog)
    }
}

@Composable
private fun LoanCard(loan: Loan, onEdit: () -> Unit, onDelete: () -> Unit, onPayment: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(loan.lenderName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (loan.isQardHasan) {
                            Box(Modifier.clip(RoundedCornerShape(4.dp))
                                .background(Emerald600.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("QARD HASAN", style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp, color = Emerald600, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (loan.isPaid) {
                            Box(Modifier.clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("PAID", style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text("Started ${loan.startDate}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onEdit,   Modifier.size(28.dp)) { Icon(Icons.Default.Edit,   null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onDelete, Modifier.size(28.dp)) { Icon(Icons.Default.Delete, null, Modifier.size(14.dp), tint = Color(0xFFEF4444)) }
            }

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column {
                    Text("Principal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("৳${"%,.0f".format(loan.principalAmount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("৳${"%,.0f".format(loan.remainingBalance)}", style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, color = Color(0xFFEF4444))
                }
            }

            LinearProgressIndicator(
                progress   = { loan.progressPercent },
                modifier   = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color      = Emerald600,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text("${(loan.progressPercent * 100).toInt()}% paid · ${loan.paymentHistory.size} payments",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (!loan.isPaid) {
                Button(onClick = onPayment, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
                    Icon(Icons.Default.Payment, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Record Payment")
                }
            }
        }
    }
}

@Composable
private fun LoanFormSheet(uiState: LoansUiState, viewModel: LoansViewModel, onCancel: () -> Unit) {
    val isEdit = uiState.editingLoan != null
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(if (isEdit) "Edit Loan" else "New Loan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Close") }
        }
        uiState.errorMessage?.let { ErrorCard(it) }

        // Loan type toggle
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("qard-hasan" to "Qard Hasan", "conventional" to "Conventional").forEach { (key, label) ->
                val selected = uiState.formLoanType == key
                OutlinedButton(onClick = { viewModel.onLoanTypeChange(key) }, Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(0.1f) else Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(12.dp)) {
                    Text(label, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, fontSize = 13.sp)
                }
            }
        }

        NisabTextField(uiState.formLenderName, viewModel::onLenderNameChange, "Lender name", leadingIcon = Icons.Default.Person)
        NisabTextField(uiState.formPrincipal, viewModel::onPrincipalChange, "Principal amount (৳)", leadingIcon = Icons.Default.MonetizationOn, keyboardType = KeyboardType.Decimal)
        if (uiState.formLoanType == "conventional") {
            NisabTextField(uiState.formInterestRate, viewModel::onInterestRateChange, "Interest rate (% per annum)", leadingIcon = Icons.Default.Percent, keyboardType = KeyboardType.Decimal)
        }
        NisabTextField(uiState.formMonthlyPayment, viewModel::onMonthlyPaymentChange, "Monthly payment (৳)", leadingIcon = Icons.Default.CalendarMonth, keyboardType = KeyboardType.Decimal)
        NisabTextField(uiState.formTotalMonths, viewModel::onTotalMonthsChange, "Total months", leadingIcon = Icons.Default.Schedule, keyboardType = KeyboardType.Number)
        NisabTextField(uiState.formStartDate, viewModel::onStartDateChange, "Start date (yyyy-MM-dd)", leadingIcon = Icons.Default.CalendarToday)
        SimpleDropdown("Account credited", uiState.formAccountId,
            uiState.accounts.map { it.id to "${it.name} · ৳${"%,.0f".format(it.balance)}" },
            viewModel::onAccountChange, Icons.Default.AccountBalance)
        NisabTextField(uiState.formNotes, viewModel::onNotesChange, "Notes (optional)", leadingIcon = Icons.Default.Notes, singleLine = false)
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Payment reminders", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = uiState.formReminders, onCheckedChange = viewModel::onRemindersChange)
        }
        NisabButton(if (isEdit) "Update Loan" else "Add Loan", viewModel::saveLoan, isLoading = uiState.isSaving)
    }
}

@Composable
private fun PaymentSheet(uiState: LoansUiState, viewModel: LoansViewModel, onCancel: () -> Unit) {
    val loan = uiState.selectedLoan ?: return
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text("Record Payment", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Remaining: ৳${"%,.0f".format(loan.remainingBalance)}", style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFEF4444))
            }
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Close") }
        }
        uiState.errorMessage?.let { ErrorCard(it) }
        NisabTextField(uiState.paymentAmount, viewModel::onPaymentAmountChange, "Payment amount (৳)", leadingIcon = Icons.Default.MonetizationOn, keyboardType = KeyboardType.Decimal)
        NisabTextField(uiState.paymentDate, viewModel::onPaymentDateChange, "Date (yyyy-MM-dd)", leadingIcon = Icons.Default.CalendarToday)
        NisabTextField(uiState.paymentNotes, viewModel::onPaymentNotesChange, "Notes (optional)", leadingIcon = Icons.Default.Notes)
        NisabButton("Record Payment", viewModel::recordPayment, isLoading = uiState.isSaving)
    }
}

@Composable
fun LoanDetailScreen(navController: NavController, loanId: String = "") {
    LoansScreen(navController = navController)
}

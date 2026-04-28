package com.hasan.nisabwallet.ui.screens.investments

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hasan.nisabwallet.data.model.Investment
import com.hasan.nisabwallet.ui.components.*
import com.hasan.nisabwallet.ui.components.SimpleDropdown
import com.hasan.nisabwallet.ui.theme.Emerald600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsScreen(
    navController: NavController,
    viewModel: InvestmentsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Investments", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showAddSheet, containerColor = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Add, "Add", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // ── Portfolio summary card ─────────────────────────────────────
            if (uiState.totalInvested > 0) {
                Card(Modifier.fillMaxWidth().padding(16.dp), RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Portfolio Value", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                        Text("৳${"%,.0f".format(uiState.totalCurrentValue)}", fontSize = 32.sp,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            PortfolioStat("Invested", "৳${"%,.0f".format(uiState.totalInvested)}", MaterialTheme.colorScheme.onPrimary.copy(0.8f))
                            val returnColor = if (uiState.totalReturn >= 0) Color(0xFF86EFAC) else Color(0xFFFCA5A5)
                            PortfolioStat("Return", "${if (uiState.totalReturn >= 0) "+" else ""}৳${"%,.0f".format(uiState.totalReturn)}", returnColor)
                            PortfolioStat("Dividends", "৳${"%,.0f".format(uiState.totalDividends)}", Color(0xFFFDE68A))
                        }
                    }
                }
            }

            // ── Type filter chips ─────────────────────────────────────────
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val types = listOf("all" to "All", "stock" to "Stock", "mutual_fund" to "Mutual Fund",
                    "dps" to "DPS", "fdr" to "FDR", "savings_certificate" to "Sav. Cert.",
                    "crypto" to "Crypto", "gold" to "Gold", "real_estate" to "Property")
                items(types) { (k, l) ->
                    FilterChip(selected = uiState.filterType == k, onClick = { viewModel.setFilter(k) },
                        label = { Text(l, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary))
                }
            }

            if (uiState.isLoading) { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }; return@Column }

            if (uiState.investments.isEmpty()) {
                EmptyState(Icons.Default.TrendingUp, "No investments yet", "Tap + to add your first investment", Modifier.weight(1f))
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(uiState.investments, key = { it.id }) { inv ->
                        InvestmentCard(inv,
                            onEdit = { viewModel.showEditSheet(inv) },
                            onDelete = { viewModel.showDeleteDialog(inv) },
                            onUpdateValue = { viewModel.showUpdateValueSheet(inv) },
                            onDividend = { viewModel.showDividendSheet(inv) })
                    }
                }
            }
        }
    }

    if (uiState.showFormSheet) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissForms, sheetState = sheetState, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            InvestmentFormSheet(uiState, viewModel, viewModel::dismissForms)
        }
    }
    if (uiState.showUpdateValueSheet) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissForms, sheetState = sheetState, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column {
                        Text("Update Market Value", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(uiState.selectedInv?.name ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = viewModel::dismissForms) { Icon(Icons.Default.Close, "Close") }
                }
                uiState.errorMessage?.let { ErrorCard(it) }
                NisabTextField(uiState.formNewValue, viewModel::onNewValueChange, "Current value per unit (৳)", leadingIcon = Icons.Default.MonetizationOn, keyboardType = KeyboardType.Decimal)
                NisabButton("Update Value", viewModel::updateCurrentValue, isLoading = uiState.isSaving)
            }
        }
    }
    if (uiState.showDividendSheet) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissForms, sheetState = sheetState, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Record Dividend / Interest", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = viewModel::dismissForms) { Icon(Icons.Default.Close, "Close") }
                }
                uiState.errorMessage?.let { ErrorCard(it) }
                NisabTextField(uiState.formDividendAmount, viewModel::onDividendAmountChange, "Amount (৳)", leadingIcon = Icons.Default.MonetizationOn, keyboardType = KeyboardType.Decimal)
                NisabTextField(uiState.formDividendDate, viewModel::onDividendDateChange, "Date (yyyy-MM-dd)", leadingIcon = Icons.Default.CalendarToday)
                NisabTextField(uiState.formDividendNotes, viewModel::onDividendNotesChange, "Notes (optional)", leadingIcon = Icons.Default.Notes)
                NisabButton("Record Dividend", viewModel::addDividend, isLoading = uiState.isSaving)
            }
        }
    }
    if (uiState.showDeleteDialog) {
        ConfirmDialog("Delete Investment", "Delete \"${uiState.deletingInv?.name}\"?", "Delete", isDestructive = true, onConfirm = viewModel::deleteInvestment, onDismiss = viewModel::dismissDeleteDialog)
    }
}

@Composable
private fun PortfolioStat(label: String, value: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(0.7f))
        Text(value,  style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun InvestmentCard(inv: Investment, onEdit: () -> Unit, onDelete: () -> Unit, onUpdateValue: () -> Unit, onDividend: () -> Unit) {
    val typeColor = runCatching { Color(android.graphics.Color.parseColor(inv.typeColor)) }.getOrDefault(Emerald600)
    val returnColor = if (inv.isProfit) Color(0xFF10B981) else Color(0xFFEF4444)

    Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(typeColor.copy(0.15f)), Alignment.Center) {
                    Text(inv.name.take(1).uppercase(), color = typeColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(inv.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TypeBadge(inv.typeLabel, typeColor)
                        if (inv.status != "active") TypeBadge(inv.status.uppercase(), Color(0xFF6B7280))
                    }
                }
                IconButton(onClick = onEdit, Modifier.size(28.dp)) { Icon(Icons.Default.Edit, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onDelete, Modifier.size(28.dp)) { Icon(Icons.Default.Delete, null, Modifier.size(14.dp), tint = Color(0xFFEF4444)) }
            }

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column { Text("Invested", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("৳${"%,.0f".format(inv.totalInvested)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Current", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("৳${"%,.0f".format(inv.totalCurrentValue)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold) }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Return", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${if (inv.isProfit) "+" else ""}৳${"%,.0f".format(inv.absoluteReturn)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = returnColor)
                    Text("${if (inv.isProfit) "+" else ""}${"%.1f".format(inv.percentageReturn)}%", style = MaterialTheme.typography.labelSmall, color = returnColor)
                }
            }

            if (inv.totalDividends > 0) {
                Text("Dividends: ৳${"%,.0f".format(inv.totalDividends)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF59E0B))
            }

            if (inv.status == "active") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = onDividend, Modifier.weight(1f), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                        Icon(Icons.Default.Payments, null, Modifier.size(12.dp)); Spacer(Modifier.width(4.dp)); Text("Dividend", fontSize = 12.sp)
                    }
                    Button(onClick = onUpdateValue, Modifier.weight(1f), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(12.dp)); Spacer(Modifier.width(4.dp)); Text("Update Value", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeBadge(label: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(color.copy(0.12f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InvestmentFormSheet(uiState: InvestmentsUiState, viewModel: InvestmentsViewModel, onCancel: () -> Unit) {
    val isEdit = uiState.editingInv != null
    val types = listOf("stock" to "Stock", "mutual_fund" to "Mutual Fund", "dps" to "DPS",
        "fdr" to "FDR", "savings_certificate" to "Savings Certificate", "bond" to "Bond",
        "ppf" to "PPF", "pension_fund" to "Pension Fund", "crypto" to "Cryptocurrency",
        "real_estate" to "Real Estate", "gold" to "Gold", "other" to "Other")
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(if (isEdit) "Edit Investment" else "New Investment", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Close") }
        }
        uiState.errorMessage?.let { ErrorCard(it) }
        NisabTextField(uiState.formName, viewModel::onNameChange, "Investment name", leadingIcon = Icons.Default.TrendingUp)
        SimpleDropdown("Type", uiState.formType, types, viewModel::onTypeChange, Icons.Default.Category)
        NisabTextField(uiState.formPurchasePrice, viewModel::onPurchasePriceChange, "Purchase price per unit (৳)", leadingIcon = Icons.Default.MonetizationOn, keyboardType = KeyboardType.Decimal)
        NisabTextField(uiState.formCurrentValue, viewModel::onCurrentValueChange, "Current value per unit (৳)", leadingIcon = Icons.Default.TrendingUp, keyboardType = KeyboardType.Decimal)
        NisabTextField(uiState.formQuantity, viewModel::onQuantityChange, "Quantity / Units", leadingIcon = Icons.Default.Numbers, keyboardType = KeyboardType.Decimal)
        NisabTextField(uiState.formPurchaseDate, viewModel::onPurchaseDateChange, "Purchase date (yyyy-MM-dd)", leadingIcon = Icons.Default.CalendarToday)
        NisabTextField(uiState.formMaturityDate, viewModel::onMaturityDateChange, "Maturity date (optional)", leadingIcon = Icons.Default.EventAvailable)
        NisabTextField(uiState.formBroker, viewModel::onBrokerChange, "Broker / Institution (optional)", leadingIcon = Icons.Default.Business)
        if (!isEdit) {
            SimpleDropdown("Deduct from account (optional)", uiState.formAccountId,
                listOf("" to "None — no account deduction") + uiState.accounts.map { it.id to "${it.name} · ৳${"%,.0f".format(it.balance)}" },
                viewModel::onAccountChange, Icons.Default.AccountBalance)
        }
        NisabTextField(uiState.formNotes, viewModel::onNotesChange, "Notes (optional)", leadingIcon = Icons.Default.Notes, singleLine = false)
        NisabButton(if (isEdit) "Update Investment" else "Add Investment", viewModel::saveInvestment, isLoading = uiState.isSaving)
    }
}

@Composable
fun InvestmentDetailScreen(navController: NavController, investmentId: String = "") {
    InvestmentsScreen(navController = navController)
}

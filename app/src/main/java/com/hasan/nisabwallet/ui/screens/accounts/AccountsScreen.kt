package com.hasan.nisabwallet.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hasan.nisabwallet.data.model.Account
import com.hasan.nisabwallet.data.model.AccountType
import com.hasan.nisabwallet.ui.components.*
import com.hasan.nisabwallet.ui.theme.Emerald600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    navController: NavController,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accounts", fontWeight = FontWeight.SemiBold) },
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
                onClick        = viewModel::showAddSheet,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
                shape          = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Default.Add, "Add account") }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { TotalBalanceCard(uiState.totalBalance, uiState.accounts.size) }

            if (uiState.accounts.isEmpty()) {
                item {
                    EmptyState(
                        icon     = Icons.Default.AccountBalance,
                        title    = "No accounts yet",
                        subtitle = "Add your first account to start tracking finances",
                        modifier = Modifier.padding(top = 48.dp)
                    )
                }
            } else {
                item {
                    Text(
                        "Your accounts",
                        style     = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier  = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items(uiState.accounts, key = { it.id }) { account ->
                    AccountCard(
                        account  = account,
                        onEdit   = { viewModel.showEditSheet(account) },
                        onDelete = { viewModel.showDeleteDialog(account) }
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (uiState.showAddSheet || uiState.showEditSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissSheets,
            sheetState       = sheetState,
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            AccountFormSheet(
                uiState   = uiState,
                isEdit    = uiState.showEditSheet,
                onSave    = { if (uiState.showEditSheet) viewModel.updateAccount() else viewModel.addAccount() },
                onCancel  = viewModel::dismissSheets,
                viewModel = viewModel
            )
        }
    }

    if (uiState.showDeleteDialog) {
        ConfirmDialog(
            title         = "Delete Account",
            message       = "Delete \"${uiState.deletingAccount?.name}\"? This cannot be undone.",
            confirmText   = "Delete",
            isDestructive = true,
            onConfirm     = viewModel::deleteAccount,
            onDismiss     = viewModel::dismissDeleteDialog
        )
    }
}

// ── Total balance hero card ───────────────────────────────────────────────────
@Composable
private fun TotalBalanceCard(totalBalance: Double, accountCount: Int) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier            = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Total Balance",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "৳${"%,.0f".format(totalBalance)}",
                color      = MaterialTheme.colorScheme.onPrimary,
                fontSize   = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "$accountCount account${if (accountCount != 1) "s" else ""}",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// ── Account card ──────────────────────────────────────────────────────────────
@Composable
private fun AccountCard(
    account: Account,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = runCatching {
        Color(android.graphics.Color.parseColor(account.color))
    }.getOrDefault(Emerald600)

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = accountTypeIcon(account.type),
                    contentDescription = null,
                    tint               = accentColor,
                    modifier           = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(account.typeLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (account.ribaBalance > 0) {
                    Text(
                        "Riba: ৳${"%,.0f".format(account.ribaBalance)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF59E0B)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "৳${"%,.0f".format(account.balance)}",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = if (account.balance >= 0) MaterialTheme.colorScheme.onSurface else Color(0xFFEF4444)
                )
                Row {
                    IconButton(onClick = onEdit,   modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit,   "Edit",   Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, "Delete", Modifier.size(16.dp), tint = Color(0xFFEF4444))
                    }
                }
            }
        }
    }
}

// ── Account form sheet ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountFormSheet(
    uiState: AccountsUiState,
    isEdit: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    viewModel: AccountsViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                if (isEdit) "Edit Account" else "Add Account",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Close") }
        }

        uiState.errorMessage?.let { ErrorCard(it) }

        NisabTextField(
            value         = uiState.formName,
            onValueChange = viewModel::onFormNameChange,
            label         = "Account name",
            leadingIcon   = Icons.Default.AccountBalance
        )

        AccountTypeDropdown(
            selectedType   = uiState.formType,
            onTypeSelected = viewModel::onFormTypeChange
        )

        if (!isEdit) {
            NisabTextField(
                value         = uiState.formBalance,
                onValueChange = viewModel::onFormBalanceChange,
                label         = "Opening balance (৳)",
                leadingIcon   = Icons.Default.MonetizationOn,
                keyboardType  = KeyboardType.Decimal
            )
        }

        NisabTextField(
            value         = uiState.formDescription,
            onValueChange = viewModel::onFormDescriptionChange,
            label         = "Description (optional)",
            leadingIcon   = Icons.Default.Notes,
            singleLine    = false
        )

        ColorPickerRow(
            selectedColor   = uiState.formColor,
            onColorSelected = viewModel::onFormColorChange
        )

        NisabButton(
            text      = if (isEdit) "Update Account" else "Add Account",
            onClick   = onSave,
            isLoading = uiState.isSaving
        )
    }
}

// ── Account type dropdown ─────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountTypeDropdown(selectedType: String, onTypeSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = AccountType.fromKey(selectedType)

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value         = selected.label,
            onValueChange = {},
            readOnly      = true,
            label         = { Text("Account type") },
            leadingIcon   = { Icon(accountTypeIcon(selectedType), null, Modifier.size(20.dp)) },
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier      = Modifier.fillMaxWidth().menuAnchor(),
            shape         = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AccountType.values().forEach { type ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(accountTypeIcon(type.key), null, Modifier.size(18.dp),
                                tint = runCatching { Color(android.graphics.Color.parseColor(type.color)) }.getOrDefault(Emerald600))
                            Spacer(Modifier.width(10.dp))
                            Text(type.label)
                        }
                    },
                    onClick = { onTypeSelected(type.key); expanded = false }
                )
            }
        }
    }
}

// ── Colour picker row ─────────────────────────────────────────────────────────
@Composable
private fun ColorPickerRow(selectedColor: String, onColorSelected: (String) -> Unit) {
    val presets = listOf(
        "#10B981","#3B82F6","#8B5CF6","#F59E0B",
        "#EF4444","#06B6D4","#EC4899","#84CC16",
        "#F97316","#6366F1","#14B8A6","#6B7280"
    )
    Column {
        Text("Colour", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            presets.forEach { hex ->
                val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Emerald600)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(if (selectedColor == hex)
                            Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        else Modifier)
                        .clickable { onColorSelected(hex) }
                )
            }
        }
    }
}

// ── Account type → icon ───────────────────────────────────────────────────────
fun accountTypeIcon(type: String) = when (type) {
    "bank"           -> Icons.Default.AccountBalance
    "mobile_banking" -> Icons.Default.PhoneAndroid
    "savings"        -> Icons.Default.Savings
    "investment"     -> Icons.Default.TrendingUp
    "credit_card"    -> Icons.Default.CreditCard
    else             -> Icons.Default.AccountBalanceWallet
}

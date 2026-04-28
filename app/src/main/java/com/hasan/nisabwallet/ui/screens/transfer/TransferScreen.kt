package com.hasan.nisabwallet.ui.screens.transfer

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
import com.hasan.nisabwallet.data.model.Transfer
import com.hasan.nisabwallet.ui.components.*
import com.hasan.nisabwallet.ui.components.SimpleDropdown
import com.hasan.nisabwallet.ui.theme.Emerald600
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    navController: NavController,
    viewModel: TransferViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
                title = { Text("Transfer", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
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
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Transfer form card ────────────────────────────────────────
            item {
                TransferFormCard(uiState = uiState, viewModel = viewModel)
            }

            // ── Transfer history ──────────────────────────────────────────
            if (uiState.transfers.isNotEmpty()) {
                item {
                    Text(
                        "Transfer History",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.padding(top = 8.dp)
                    )
                }
                items(uiState.transfers, key = { it.id }) { transfer ->
                    TransferHistoryItem(
                        transfer = transfer,
                        onDelete = { viewModel.showDeleteDialog(transfer) }
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (uiState.showDeleteDialog) {
        ConfirmDialog(
            title         = "Delete Transfer",
            message       = "Delete this transfer? Both account balances will be reversed.",
            confirmText   = "Delete",
            isDestructive = true,
            onConfirm     = viewModel::deleteTransfer,
            onDismiss     = viewModel::dismissDeleteDialog
        )
    }
}

// ── Transfer form card ────────────────────────────────────────────────────────
@Composable
private fun TransferFormCard(
    uiState: TransferUiState,
    viewModel: TransferViewModel
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "New Transfer",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            uiState.errorMessage?.let { ErrorCard(it) }

            // From → To visual
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
                    val fromName = uiState.accounts.find { it.id == uiState.fromAccountId }?.name ?: "—"
                    val fromBal  = uiState.accounts.find { it.id == uiState.fromAccountId }?.balance ?: 0.0
                    AccountMiniCard(name = fromName, balance = fromBal, color = Color(0xFF3B82F6))
                }

                Box(
                    modifier         = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        "To",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val toName = uiState.accounts.find { it.id == uiState.toAccountId }?.name ?: "—"
                    val toBal  = uiState.accounts.find { it.id == uiState.toAccountId }?.balance ?: 0.0
                    AccountMiniCard(name = toName, balance = toBal, color = Emerald600)
                }
            }

            // From account selector
            SimpleDropdown(
                label      = "From account",
                selectedId = uiState.fromAccountId,
                options    = uiState.accounts
                    .filter { it.id != uiState.toAccountId }
                    .map    { it.id to "${it.name} · ৳${"%,.0f".format(it.balance)}" },
                onSelected = viewModel::onFromAccountChange,
                leadingIcon = Icons.Default.AccountBalance
            )

            // To account selector
            SimpleDropdown(
                label      = "To account",
                selectedId = uiState.toAccountId,
                options    = uiState.accounts
                    .filter { it.id != uiState.fromAccountId }
                    .map    { it.id to "${it.name} · ৳${"%,.0f".format(it.balance)}" },
                onSelected = viewModel::onToAccountChange,
                leadingIcon = Icons.Default.AccountBalance
            )

            // Amount
            NisabTextField(
                value         = uiState.amount,
                onValueChange = viewModel::onAmountChange,
                label         = "Amount (৳)",
                leadingIcon   = Icons.Default.MonetizationOn,
                keyboardType  = KeyboardType.Decimal
            )

            // Description
            NisabTextField(
                value         = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label         = "Note (optional)",
                leadingIcon   = Icons.Default.Notes
            )

            // Date
            NisabTextField(
                value         = uiState.date,
                onValueChange = viewModel::onDateChange,
                label         = "Date (yyyy-MM-dd)",
                leadingIcon   = Icons.Default.CalendarToday
            )

            // Transfer button
            NisabButton(
                text      = "Transfer",
                onClick   = viewModel::transfer,
                isLoading = uiState.isSaving
            )
        }
    }
}

// ── Mini account info card ────────────────────────────────────────────────────
@Composable
private fun AccountMiniCard(name: String, balance: Double, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column {
            Text(name,  style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = color)
            Text("৳${"%,.0f".format(balance)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Transfer history item ─────────────────────────────────────────────────────
@Composable
internal fun TransferHistoryItem(transfer: Transfer, onDelete: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Transfer icon
            Box(
                modifier         = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment      = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        transfer.fromAccountName,
                        style      = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color(0xFF3B82F6)
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        null,
                        Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        transfer.toAccountName,
                        style      = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = Emerald600
                    )
                }
                if (transfer.description.isNotBlank()) {
                    Text(
                        transfer.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    formatTransferDate(transfer.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "৳${"%,.0f".format(transfer.amount)}",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, "Delete", Modifier.size(14.dp), tint = Color(0xFFEF4444))
                }
            }
        }
    }
}

private fun formatTransferDate(dateStr: String): String = try {
    val date  = LocalDate.parse(dateStr)
    val today = LocalDate.now()
    when {
        date == today              -> "Today"
        date == today.minusDays(1) -> "Yesterday"
        else -> "${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${date.year}"
    }
} catch (e: Exception) { dateStr }

package com.hasan.nisabwallet.ui.screens.recurring

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
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.model.Account
import com.hasan.nisabwallet.data.model.Category
import com.hasan.nisabwallet.data.model.RecurringTransaction
import com.hasan.nisabwallet.data.repository.AccountRepository
import com.hasan.nisabwallet.data.repository.CategoryRepository
import com.hasan.nisabwallet.data.repository.RecurringRepository
import com.hasan.nisabwallet.ui.components.*
import com.hasan.nisabwallet.ui.components.SimpleDropdown
import com.hasan.nisabwallet.ui.theme.Emerald600
import com.hasan.nisabwallet.ui.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────
data class RecurringUiState(
    val recurring: List<RecurringTransaction> = emptyList(),
    val pendingQueue: List<RecurringTransaction> = emptyList(),
    val accounts: List<Account>              = emptyList(),
    val categories: List<Category>           = emptyList(),
    val isLoading: Boolean                   = true,
    val isSaving: Boolean                    = false,
    val showFormSheet: Boolean               = false,
    val showDeleteDialog: Boolean            = false,
    val editingItem: RecurringTransaction?   = null,
    val deletingItem: RecurringTransaction?  = null,
    val errorMessage: String?                = null,
    val successMessage: String?              = null,
    // Form
    val formType: String            = "Expense",
    val formAmount: String          = "",
    val formAccountId: String       = "",
    val formCategoryId: String      = "",
    val formFrequency: String       = "monthly",
    val formInterval: String        = "1",
    val formStartDate: String       = LocalDate.now().toString(),
    val formEndCondition: String    = "never",
    val formEndDate: String         = "",
    val formOccurrences: String     = "",
    val formDescription: String     = ""
)

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val recurringRepo: RecurringRepository,
    private val accountRepo: AccountRepository,
    private val categoryRepo: CategoryRepository,
    private val auth: FirebaseAuth
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(RecurringUiState())
    val uiState = _uiState.asStateFlow()
    private val userId get() = auth.currentUser?.uid ?: ""

    init {
        recurringRepo.getRecurringFlow(userId)
            .onEach { items ->
                val pending = items.filter { it.isOverdue && it.status == "active" }
                _uiState.value = _uiState.value.copy(recurring = items, pendingQueue = pending, isLoading = false)
            }
            .catch { _uiState.value = _uiState.value.copy(isLoading = false) }
            .launchIn(viewModelScope)

        accountRepo.getAccountsFlow(userId)
            .onEach { _uiState.value = _uiState.value.copy(accounts = it, formAccountId = if (_uiState.value.formAccountId.isEmpty() && it.isNotEmpty()) it.first().id else _uiState.value.formAccountId) }
            .launchIn(viewModelScope)

        categoryRepo.getCategoriesFlow(userId)
            .onEach { _uiState.value = _uiState.value.copy(categories = it) }
            .launchIn(viewModelScope)
    }

    fun showAddSheet() {
        _uiState.value = _uiState.value.copy(showFormSheet = true, editingItem = null,
            formType = "Expense", formAmount = "", formCategoryId = "",
            formFrequency = "monthly", formInterval = "1",
            formStartDate = LocalDate.now().toString(), formEndCondition = "never",
            formEndDate = "", formOccurrences = "", formDescription = "", errorMessage = null)
    }

    fun showEditSheet(r: RecurringTransaction) {
        _uiState.value = _uiState.value.copy(showFormSheet = true, editingItem = r,
            formType = r.type, formAmount = r.amount.toLong().toString(), formAccountId = r.accountId,
            formCategoryId = r.categoryId, formFrequency = r.frequency, formInterval = r.interval.toString(),
            formStartDate = r.startDate, formEndCondition = r.endCondition,
            formEndDate = r.endDate, formOccurrences = r.occurrences?.toString() ?: "",
            formDescription = r.description, errorMessage = null)
    }

    fun dismissSheet()  { _uiState.value = _uiState.value.copy(showFormSheet = false, editingItem = null, errorMessage = null) }
    fun showDeleteDialog(r: RecurringTransaction) { _uiState.value = _uiState.value.copy(showDeleteDialog = true, deletingItem = r) }
    fun dismissDeleteDialog() { _uiState.value = _uiState.value.copy(showDeleteDialog = false, deletingItem = null) }
    fun clearMessages() { _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null) }

    fun onTypeChange(v: String)        { _uiState.value = _uiState.value.copy(formType = v, formCategoryId = "") }
    fun onAmountChange(v: String)      { _uiState.value = _uiState.value.copy(formAmount = v) }
    fun onAccountChange(v: String)     { _uiState.value = _uiState.value.copy(formAccountId = v) }
    fun onCategoryChange(v: String)    { _uiState.value = _uiState.value.copy(formCategoryId = v) }
    fun onFrequencyChange(v: String)   { _uiState.value = _uiState.value.copy(formFrequency = v) }
    fun onIntervalChange(v: String)    { _uiState.value = _uiState.value.copy(formInterval = v) }
    fun onStartDateChange(v: String)   { _uiState.value = _uiState.value.copy(formStartDate = v) }
    fun onEndConditionChange(v: String){ _uiState.value = _uiState.value.copy(formEndCondition = v) }
    fun onEndDateChange(v: String)     { _uiState.value = _uiState.value.copy(formEndDate = v) }
    fun onOccurrencesChange(v: String) { _uiState.value = _uiState.value.copy(formOccurrences = v) }
    fun onDescriptionChange(v: String) { _uiState.value = _uiState.value.copy(formDescription = v) }

    fun save() {
        val s = _uiState.value
        val amt = s.formAmount.toDoubleOrNull()
        when {
            amt == null || amt <= 0         -> { _uiState.value = s.copy(errorMessage = "Enter valid amount"); return }
            s.formAccountId.isEmpty()       -> { _uiState.value = s.copy(errorMessage = "Select account"); return }
            s.formCategoryId.isEmpty()      -> { _uiState.value = s.copy(errorMessage = "Select category"); return }
        }
        val account  = s.accounts.find { it.id == s.formAccountId } ?: return
        val category = s.categories.find { it.id == s.formCategoryId } ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            val r = RecurringTransaction(
                type = s.formType, amount = amt!!, accountId = s.formAccountId,
                accountName = account.name, categoryId = s.formCategoryId,
                categoryName = category.name, categoryColor = category.color,
                frequency = s.formFrequency, interval = s.formInterval.toIntOrNull() ?: 1,
                startDate = s.formStartDate, endCondition = s.formEndCondition,
                endDate = s.formEndDate, occurrences = s.formOccurrences.toIntOrNull(),
                description = s.formDescription.trim()
            )
            val result = if (s.editingItem == null)
                recurringRepo.addRecurring(userId, r).let { if (it is Result.Success) Result.Success(Unit) else it as Result<Unit> }
            else recurringRepo.updateRecurring(userId, s.editingItem.id, r)

            when (result) {
                is Result.Success -> _uiState.value = _uiState.value.copy(isSaving = false, showFormSheet = false, successMessage = "Recurring transaction saved")
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
                else -> Unit
            }
        }
    }

    fun togglePause(r: RecurringTransaction) {
        viewModelScope.launch {
            recurringRepo.togglePause(userId, r.id, r.status)
        }
    }

    fun execute(r: RecurringTransaction) {
        val account = _uiState.value.accounts.find { it.id == r.accountId } ?: return
        if (r.isIncome.not() && r.amount > account.balance) {
            _uiState.value = _uiState.value.copy(errorMessage = "Insufficient balance in ${account.name}")
            return
        }
        viewModelScope.launch {
            when (val result = recurringRepo.executeRecurring(userId, r, account.balance)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(successMessage = "Executed: ${r.description.ifBlank { r.categoryName }}")
                is Result.Error   -> _uiState.value = _uiState.value.copy(errorMessage = result.message)
                else -> Unit
            }
        }
    }

    fun delete() {
        val r = _uiState.value.deletingItem ?: return
        viewModelScope.launch {
            when (val result = recurringRepo.deleteRecurring(userId, r.id)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(isSaving = false, showDeleteDialog = false, deletingItem = null, successMessage = "Deleted")
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
                else -> Unit
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    navController: NavController,
    viewModel: RecurringViewModel = hiltViewModel()
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
                title = { Text("Recurring", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showAddSheet, containerColor = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Add, "Add", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) { Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }; return@Scaffold }

        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // Pending queue
            if (uiState.pendingQueue.isNotEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth(), RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(0.08f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(0.3f))) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Warning, null, Modifier.size(16.dp), tint = Color(0xFFEF4444))
                                Text("${uiState.pendingQueue.size} pending execution${if (uiState.pendingQueue.size > 1) "s" else ""}",
                                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFFEF4444))
                            }
                            uiState.pendingQueue.forEach { r ->
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Column {
                                        Text(r.description.ifBlank { r.categoryName }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                        Text("Due ${r.nextDueDate}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444))
                                    }
                                    Button(onClick = { viewModel.execute(r) }, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                                        Text("Execute", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.recurring.isEmpty()) {
                item { EmptyState(Icons.Default.Repeat, "No recurring transactions", "Tap + to set up a recurring income or expense", Modifier.padding(top = 32.dp)) }
            } else {
                items(uiState.recurring, key = { it.id }) { r ->
                    RecurringCard(r, onEdit = { viewModel.showEditSheet(r) }, onDelete = { viewModel.showDeleteDialog(r) },
                        onTogglePause = { viewModel.togglePause(r) }, onExecute = { viewModel.execute(r) })
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (uiState.showFormSheet) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissSheet, sheetState = sheetState, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            RecurringFormSheet(uiState, viewModel, viewModel::dismissSheet)
        }
    }
    if (uiState.showDeleteDialog) {
        ConfirmDialog("Delete Recurring", "Remove this recurring transaction rule?", "Delete", isDestructive = true, onConfirm = viewModel::delete, onDismiss = viewModel::dismissDeleteDialog)
    }
}

@Composable
private fun RecurringCard(
    r: RecurringTransaction, onEdit: () -> Unit, onDelete: () -> Unit, onTogglePause: () -> Unit, onExecute: () -> Unit
) {
    val catColor = runCatching { Color(android.graphics.Color.parseColor(r.categoryColor)) }.getOrDefault(Emerald600)
    val amountColor = if (r.isIncome) Color(0xFF10B981) else Color(0xFFEF4444)

    Card(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (r.isOverdue) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(0.4f)) else null,
        elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(catColor.copy(0.15f)), Alignment.Center) {
                    Icon(Icons.Default.Repeat, null, Modifier.size(18.dp), tint = catColor)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(r.description.ifBlank { r.categoryName }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("${r.frequencyLabel} · ${r.accountName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatusBadge(r.status)
                        if (r.isOverdue) StatusBadge("overdue")
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${if (r.isIncome) "+" else "-"}৳${"%,.0f".format(r.amount)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = amountColor)
                    Text("Next: ${r.nextDueDate}", style = MaterialTheme.typography.labelSmall, color = if (r.isOverdue) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = onTogglePause, Modifier.weight(1f), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                    Icon(if (r.status == "active") Icons.Default.Pause else Icons.Default.PlayArrow, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (r.status == "active") "Pause" else "Resume", fontSize = 12.sp)
                }
                OutlinedButton(onClick = onEdit, Modifier.weight(1f), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                    Icon(Icons.Default.Edit, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("Edit", fontSize = 12.sp)
                }
                IconButton(onClick = onDelete, Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp), tint = Color(0xFFEF4444))
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (color, label) = when (status) {
        "active"  -> Color(0xFF10B981) to "ACTIVE"
        "paused"  -> Color(0xFF6B7280) to "PAUSED"
        "overdue" -> Color(0xFFEF4444) to "OVERDUE"
        else      -> Color(0xFF6B7280) to status.uppercase()
    }
    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(color.copy(0.12f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RecurringFormSheet(uiState: RecurringUiState, viewModel: RecurringViewModel, onCancel: () -> Unit) {
    val isEdit = uiState.editingItem != null
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(if (isEdit) "Edit Recurring" else "New Recurring Transaction", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Close") }
        }
        uiState.errorMessage?.let { ErrorCard(it) }

        // Type toggle
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Income" to Color(0xFF10B981), "Expense" to Color(0xFFEF4444)).forEach { (type, color) ->
                val sel = uiState.formType == type
                OutlinedButton(onClick = { viewModel.onTypeChange(type) }, Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (sel) color.copy(0.1f) else Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, if (sel) color else MaterialTheme.colorScheme.outline), shape = RoundedCornerShape(12.dp)) {
                    Text(type, color = if (sel) color else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }

        NisabTextField(uiState.formAmount, viewModel::onAmountChange, "Amount (৳)", leadingIcon = Icons.Default.MonetizationOn, keyboardType = KeyboardType.Decimal)
        SimpleDropdown("Account", uiState.formAccountId, uiState.accounts.map { it.id to it.name }, viewModel::onAccountChange, Icons.Default.AccountBalance)
        SimpleDropdown("Category", uiState.formCategoryId, uiState.categories.filter { it.type == uiState.formType }.map { it.id to it.name }, viewModel::onCategoryChange, Icons.Default.Category)
        SimpleDropdown("Frequency", uiState.formFrequency, listOf("daily" to "Daily", "weekly" to "Weekly", "monthly" to "Monthly", "yearly" to "Yearly"), viewModel::onFrequencyChange, Icons.Default.Repeat)
        NisabTextField(uiState.formStartDate, viewModel::onStartDateChange, "Start date (yyyy-MM-dd)", leadingIcon = Icons.Default.CalendarToday)
        SimpleDropdown("Ends", uiState.formEndCondition, listOf("never" to "Never", "until" to "Until date", "after" to "After N occurrences"), viewModel::onEndConditionChange, Icons.Default.EventBusy)
        if (uiState.formEndCondition == "until") NisabTextField(uiState.formEndDate, viewModel::onEndDateChange, "End date (yyyy-MM-dd)", leadingIcon = Icons.Default.CalendarMonth)
        if (uiState.formEndCondition == "after") NisabTextField(uiState.formOccurrences, viewModel::onOccurrencesChange, "Number of occurrences", leadingIcon = Icons.Default.Numbers, keyboardType = KeyboardType.Number)
        NisabTextField(uiState.formDescription, viewModel::onDescriptionChange, "Description (optional)", leadingIcon = Icons.Default.Notes)
        NisabButton(if (isEdit) "Update" else "Create Recurring", viewModel::save, isLoading = uiState.isSaving)
    }
}

@Composable fun RecurringDetailScreen(navController: NavController, recurringId: String = "") { RecurringScreen(navController) }
@Composable fun RecurringPendingScreen(navController: NavController) { RecurringScreen(navController) }

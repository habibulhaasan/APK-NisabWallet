package com.hasan.nisabwallet.ui.screens.riba

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.firebase.FirestorePaths
import com.hasan.nisabwallet.data.firebase.snapshotFlow
import com.hasan.nisabwallet.data.model.Account
import com.hasan.nisabwallet.data.model.Transaction
import com.hasan.nisabwallet.data.repository.AccountRepository
import com.hasan.nisabwallet.data.safeCall
import com.hasan.nisabwallet.ui.components.*
import com.hasan.nisabwallet.ui.components.SimpleDropdown
import com.hasan.nisabwallet.ui.theme.Emerald600
import com.hasan.nisabwallet.ui.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────

data class RibaUiState(
    val ribaTransactions: List<Transaction> = emptyList(),
    val accounts: List<Account>             = emptyList(),
    val isLoading: Boolean                  = true,
    val isSaving: Boolean                   = false,
    val showSadaqahSheet: Boolean           = false,
    val selectedRibaTxn: Transaction?       = null,
    val errorMessage: String?               = null,
    val successMessage: String?             = null,
    val totalRiba: Double                   = 0.0,
    val totalPurified: Double               = 0.0,
    val pendingPurification: Double         = 0.0,
    // Sadaqah form
    val formSadaqahAmount: String           = "",
    val formSadaqahAccountId: String        = "",
    val formSadaqahDate: String             = LocalDate.now().toString(),
    val formSadaqahNote: String             = ""
)

@HiltViewModel
class RibaViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val accountRepo: AccountRepository,
    private val auth: FirebaseAuth
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(RibaUiState())
    val uiState = _uiState.asStateFlow()

    private val userId get() = auth.currentUser?.uid ?: ""

    init {
        observeRibaTransactions()
        observeAccounts()
    }

    private fun observeRibaTransactions() {
        // Riba transactions = type Income where isRiba == true
        FirestorePaths.transactions(db, userId)
            .whereEqualTo("isRiba", true)
            .snapshotFlow()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                }.sortedByDescending { it.date }
            }
            .onEach { txns ->
                val totalRiba    = txns.sumOf { it.amount }
                val totalPurified = txns.filter { it.sadaqahDone }.sumOf { it.sadaqahAmount }
                _uiState.value = _uiState.value.copy(
                    ribaTransactions   = txns,
                    totalRiba          = totalRiba,
                    totalPurified      = totalPurified,
                    pendingPurification = totalRiba - totalPurified,
                    isLoading          = false
                )
            }
            .catch { _uiState.value = _uiState.value.copy(isLoading = false) }
            .launchIn(viewModelScope)
    }

    private fun observeAccounts() {
        accountRepo.getAccountsFlow(userId)
            .onEach { accounts ->
                _uiState.value = _uiState.value.copy(
                    accounts             = accounts,
                    formSadaqahAccountId = if (_uiState.value.formSadaqahAccountId.isEmpty() && accounts.isNotEmpty())
                        accounts.first().id else _uiState.value.formSadaqahAccountId
                )
            }
            .launchIn(viewModelScope)
    }

    fun showSadaqahSheet(txn: Transaction) {
        _uiState.value = _uiState.value.copy(
            showSadaqahSheet     = true,
            selectedRibaTxn      = txn,
            formSadaqahAmount    = txn.amount.toLong().toString(),
            formSadaqahAccountId = _uiState.value.accounts.firstOrNull()?.id ?: "",
            formSadaqahDate      = LocalDate.now().toString(),
            formSadaqahNote      = "",
            errorMessage         = null
        )
    }

    fun dismissSheet() {
        _uiState.value = _uiState.value.copy(
            showSadaqahSheet = false, selectedRibaTxn = null, errorMessage = null
        )
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun onSadaqahAmountChange(v: String)   { _uiState.value = _uiState.value.copy(formSadaqahAmount = v) }
    fun onSadaqahAccountChange(v: String)  { _uiState.value = _uiState.value.copy(formSadaqahAccountId = v) }
    fun onSadaqahDateChange(v: String)     { _uiState.value = _uiState.value.copy(formSadaqahDate = v) }
    fun onSadaqahNoteChange(v: String)     { _uiState.value = _uiState.value.copy(formSadaqahNote = v) }

    /**
     * Record Sadaqah to purify Riba income.
     * Mirrors web app's SadaqahModal logic:
     * 1. Find/create "Sadaqah / Charity" expense category
     * 2. Record expense transaction
     * 3. Deduct from account balance
     * 4. Mark original riba transaction as sadaqahDone = true
     * All in a single Firestore batch.
     */
    fun recordSadaqah() {
        val s   = _uiState.value
        val txn = s.selectedRibaTxn ?: return
        val amt = s.formSadaqahAmount.toDoubleOrNull()
        val account = s.accounts.find { it.id == s.formSadaqahAccountId }

        when {
            amt == null || amt <= 0          -> { _uiState.value = s.copy(errorMessage = "Enter valid sadaqah amount"); return }
            s.formSadaqahAccountId.isEmpty() -> { _uiState.value = s.copy(errorMessage = "Select an account"); return }
            account == null                  -> { _uiState.value = s.copy(errorMessage = "Account not found"); return }
            amt > account.balance            -> { _uiState.value = s.copy(errorMessage = "Insufficient balance in ${account.name}"); return }
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)

            val result: Result<Unit> = safeCall {
                val batch = db.batch()

                // 1. Find or create "Sadaqah / Charity" category
                val catRef   = FirestorePaths.categories(db, userId)
                val catSnap  = catRef.whereEqualTo("name", "Sadaqah / Charity").get()
                    .also { /* runs synchronously in safeCall */ }
                // Use a sub-call to get the category ID synchronously
                val catId = getSadaqahCategoryId()

                // 2. Record Sadaqah expense transaction
                val txnRef = FirestorePaths.transactions(db, userId).document()
                batch.set(txnRef, mapOf(
                    "transactionId" to UUID.randomUUID().toString(),
                    "type"          to "Expense",
                    "amount"        to amt!!,
                    "accountId"     to s.formSadaqahAccountId,
                    "accountName"   to account!!.name,
                    "categoryId"    to catId,
                    "categoryName"  to "Sadaqah / Charity",
                    "categoryColor" to "#10B981",
                    "description"   to (s.formSadaqahNote.ifBlank {
                        "Sadaqah — purifying Riba from \"${txn.description.ifBlank { "Interest income" }}\""
                    }),
                    "date"          to s.formSadaqahDate,
                    "isRiba"        to false,
                    "isSadaqah"     to true,
                    "ribaRefId"     to txn.id,
                    "createdAt"     to FieldValue.serverTimestamp()
                ))

                // 3. Deduct from account
                val accRef = FirestorePaths.accounts(db, userId).document(s.formSadaqahAccountId)
                batch.update(accRef, mapOf(
                    "balance"   to account.balance - amt,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))

                // 4. Mark riba transaction as purified
                val ribaRef = FirestorePaths.transactions(db, userId).document(txn.id)
                batch.update(ribaRef, mapOf(
                    "sadaqahDone"   to true,
                    "sadaqahAmount" to amt,
                    "sadaqahDate"   to s.formSadaqahDate,
                    "updatedAt"     to FieldValue.serverTimestamp()
                ))

                batch.commit()
            }

            when (result) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSaving         = false,
                    showSadaqahSheet = false,
                    successMessage   = "Sadaqah recorded. May Allah accept it. 🤲"
                )
                is Result.Error   -> _uiState.value = _uiState.value.copy(
                    isSaving     = false,
                    errorMessage = result.message
                )
                else -> Unit
            }
        }
    }

    private suspend fun getSadaqahCategoryId(): String {
        val snap = FirestorePaths.categories(db, userId)
            .whereEqualTo("name", "Sadaqah / Charity")
            .get().await()
        if (!snap.isEmpty) return snap.documents.first().id

        // Create if not exists
        val newCat = FirestorePaths.categories(db, userId).add(mapOf(
            "categoryId" to UUID.randomUUID().toString(),
            "name"       to "Sadaqah / Charity",
            "type"       to "Expense",
            "color"      to "#10B981",
            "isSystem"   to true,
            "isDefault"  to true,
            "isRiba"     to false,
            "createdAt"  to FieldValue.serverTimestamp()
        )).await()
        return newCat.id
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RibaScreen(
    navController: NavController,
    viewModel: RibaViewModel = hiltViewModel()
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
                title = { Text("Riba Tracker", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Islamic context card ──────────────────────────────────────
            item { RibaContextCard() }

            // ── Summary ───────────────────────────────────────────────────
            if (uiState.totalRiba > 0) {
                item { RibaSummaryCard(uiState) }
            }

            // ── Transaction list ──────────────────────────────────────────
            if (uiState.ribaTransactions.isEmpty()) {
                item {
                    EmptyState(
                        icon     = Icons.Default.CheckCircle,
                        title    = "No Riba detected",
                        subtitle = "Transactions categorised as \"Interest / Riba\" will appear here for purification",
                        modifier = Modifier.padding(top = 32.dp)
                    )
                }
            } else {
                item {
                    Text(
                        "Riba Transactions",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(uiState.ribaTransactions, key = { it.id }) { txn ->
                    RibaTransactionCard(
                        txn     = txn,
                        onPurify = { if (!txn.sadaqahDone) viewModel.showSadaqahSheet(txn) }
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // ── Sadaqah sheet ─────────────────────────────────────────────────────────
    if (uiState.showSadaqahSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissSheet,
            sheetState       = sheetState,
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            SadaqahSheet(uiState = uiState, viewModel = viewModel, onCancel = viewModel::dismissSheet)
        }
    }
}

// ── Islamic context card ──────────────────────────────────────────────────────
@Composable
private fun RibaContextCard() {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = Color(0xFFF59E0B).copy(alpha = 0.08f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Warning, null,
                Modifier.size(20.dp).padding(top = 2.dp),
                tint = Color(0xFFF59E0B)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "About Riba",
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFFF59E0B)
                )
                Text(
                    "Interest (Riba) income is impermissible in Islam. " +
                    "It should not be used for personal benefit. " +
                    "Give it to charity (Sadaqah) to purify your wealth — " +
                    "without expecting reward for it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ── Summary card ──────────────────────────────────────────────────────────────
@Composable
private fun RibaSummaryCard(uiState: RibaUiState) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            RibaStatColumn(
                label  = "Total Riba",
                amount = uiState.totalRiba,
                color  = Color(0xFFF59E0B)
            )
            VerticalDivider(modifier = Modifier.height(48.dp))
            RibaStatColumn(
                label  = "Purified",
                amount = uiState.totalPurified,
                color  = Emerald600
            )
            VerticalDivider(modifier = Modifier.height(48.dp))
            RibaStatColumn(
                label  = "Pending",
                amount = uiState.pendingPurification,
                color  = Color(0xFFEF4444)
            )
        }
    }
}

@Composable
private fun RibaStatColumn(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "৳${"%,.0f".format(amount)}",
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color      = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Riba transaction card ─────────────────────────────────────────────────────
@Composable
private fun RibaTransactionCard(txn: Transaction, onPurify: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border    = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (txn.sadaqahDone) Emerald600.copy(alpha = 0.3f)
            else Color(0xFFF59E0B).copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(0.5.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier         = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (txn.sadaqahDone) Icons.Default.CheckCircle else Icons.Default.Warning,
                        null,
                        Modifier.size(20.dp),
                        tint = if (txn.sadaqahDone) Emerald600 else Color(0xFFF59E0B)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        txn.description.ifBlank { "Interest Income" },
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${txn.date} · ${txn.accountName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "৳${"%,.0f".format(txn.amount)}",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFFF59E0B)
                    )
                    if (txn.sadaqahDone) {
                        Text(
                            "Purified ৳${"%,.0f".format(txn.sadaqahAmount)}",
                            style  = MaterialTheme.typography.labelSmall,
                            color  = Emerald600,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (!txn.sadaqahDone) {
                Button(
                    onClick          = onPurify,
                    modifier         = Modifier.fillMaxWidth(),
                    shape            = RoundedCornerShape(10.dp),
                    colors           = ButtonDefaults.buttonColors(
                        containerColor = Emerald600
                    ),
                    contentPadding   = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Favorite, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Give as Sadaqah")
                }
            } else {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle, null,
                        Modifier.size(14.dp), tint = Emerald600
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Purified on ${txn.sadaqahDate}",
                        style  = MaterialTheme.typography.labelSmall,
                        color  = Emerald600,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ── Sadaqah form sheet ────────────────────────────────────────────────────────
@Composable
private fun SadaqahSheet(
    uiState: RibaUiState,
    viewModel: RibaViewModel,
    onCancel: () -> Unit
) {
    val txn = uiState.selectedRibaTxn ?: return

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
            Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Give as Sadaqah",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Purify ৳${"%,.0f".format(txn.amount)} Riba",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF59E0B)
                )
            }
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Close") }
        }

        // Context
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Emerald600.copy(alpha = 0.08f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Give this amount to charity without expecting any reward or tax benefit. " +
                "This purifies your wealth from impermissible income.",
                modifier = Modifier.padding(12.dp),
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        uiState.errorMessage?.let { ErrorCard(it) }

        NisabTextField(
            value         = uiState.formSadaqahAmount,
            onValueChange = viewModel::onSadaqahAmountChange,
            label         = "Sadaqah amount (৳)",
            leadingIcon   = Icons.Default.MonetizationOn,
            keyboardType  = KeyboardType.Decimal
        )

        SimpleDropdown(
            label      = "Pay from account",
            selectedId = uiState.formSadaqahAccountId,
            options    = uiState.accounts.map {
                it.id to "${it.name} · ৳${"%,.0f".format(it.balance)}"
            },
            onSelected  = viewModel::onSadaqahAccountChange,
            leadingIcon = Icons.Default.AccountBalance
        )

        NisabTextField(
            value         = uiState.formSadaqahDate,
            onValueChange = viewModel::onSadaqahDateChange,
            label         = "Date (yyyy-MM-dd)",
            leadingIcon   = Icons.Default.CalendarToday
        )

        NisabTextField(
            value         = uiState.formSadaqahNote,
            onValueChange = viewModel::onSadaqahNoteChange,
            label         = "Note / Recipient (optional)",
            leadingIcon   = Icons.Default.Notes
        )

        NisabButton(
            text      = "Record Sadaqah",
            onClick   = viewModel::recordSadaqah,
            isLoading = uiState.isSaving,
            containerColor = Emerald600
        )

        Text(
            "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",
            modifier  = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style     = MaterialTheme.typography.bodyMedium,
            color     = Emerald600.copy(alpha = 0.6f)
        )
    }
}

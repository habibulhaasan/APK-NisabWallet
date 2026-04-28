package com.hasan.nisabwallet.ui.screens.zakat

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
import com.hasan.nisabwallet.data.model.*
import com.hasan.nisabwallet.data.repository.*
import com.hasan.nisabwallet.ui.components.*
import com.hasan.nisabwallet.ui.components.SimpleDropdown
import com.hasan.nisabwallet.ui.theme.Emerald600
import com.hasan.nisabwallet.ui.viewmodel.BaseViewModel
import com.hasan.nisabwallet.utils.ZakatStatus
import com.hasan.nisabwallet.utils.ZakatUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────
data class ZakatUiState(
    val accounts: List<Account>         = emptyList(),
    val activeCycle: ZakatCycle?        = null,
    val cycles: List<ZakatCycle>        = emptyList(),
    val payments: List<ZakatPayment>    = emptyList(),
    val nisabSettings: NisabSettings    = NisabSettings(),
    val zakatableWealth: Double         = 0.0,
    val zakatAmount: Double             = 0.0,
    val zakatStatus: ZakatStatus        = ZakatStatus.NOT_MANDATORY,
    val daysRemaining: Long             = 0L,
    val hijriStartDate: String          = "",
    val isLoading: Boolean              = true,
    val isSaving: Boolean               = false,
    val showStartCycleSheet: Boolean    = false,
    val showPaymentSheet: Boolean       = false,
    val showNisabSheet: Boolean         = false,
    val errorMessage: String?           = null,
    val successMessage: String?         = null,
    // Start cycle form
    val formStartDate: String           = LocalDate.now().toString(),
    // Payment form
    val formPaymentAmount: String       = "",
    val formPaymentAccountId: String    = "",
    val formPaymentDate: String         = LocalDate.now().toString(),
    val formPaymentNotes: String        = "",
    // Nisab form
    val formNisabThreshold: String      = "",
    val formSilverPerGram: String       = "",
    val formGoldPerGram: String         = ""
)

@HiltViewModel
class ZakatViewModel @Inject constructor(
    private val zakatRepo: ZakatRepository,
    private val accountRepo: AccountRepository,
    private val auth: FirebaseAuth
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ZakatUiState())
    val uiState = _uiState.asStateFlow()
    private val userId get() = auth.currentUser?.uid ?: ""

    init { observeAll(); loadNisabSettings() }

    private fun observeAll() {
        combine(
            accountRepo.getAccountsFlow(userId),
            zakatRepo.getCyclesFlow(userId),
            zakatRepo.getPaymentsFlow(userId)
        ) { accounts, cycles, payments ->
            val active = cycles.firstOrNull { it.status == "active" }
            val wealth = accounts.sumOf { it.purifiedBalance }
            val nisab  = _uiState.value.nisabSettings.nisabThreshold

            val status = ZakatUtils.determineZakatStatus(
                totalWealth = wealth, nisabThreshold = nisab,
                activeCycleStartDate = active?.startDate,
                isCyclePaid = active?.status == "paid"
            )
            val daysLeft    = if (active != null) ZakatUtils.daysUntilHijriAnniversary(active.startDate) else 0L
            val zakatAmount = if (status == ZakatStatus.DUE) ZakatUtils.calculateZakat(wealth) else 0.0
            val hijriStr    = if (active != null) try {
                ZakatUtils.formatHijriDate(ZakatUtils.gregorianToHijri(active.startDate))
            } catch (e: Exception) { "" } else ""

            _uiState.value = _uiState.value.copy(
                accounts        = accounts,
                cycles          = cycles,
                payments        = payments,
                activeCycle     = active,
                zakatableWealth = wealth,
                zakatAmount     = zakatAmount,
                zakatStatus     = status,
                daysRemaining   = daysLeft,
                hijriStartDate  = hijriStr,
                isLoading       = false,
                formPaymentAccountId = if (_uiState.value.formPaymentAccountId.isEmpty() && accounts.isNotEmpty()) accounts.first().id else _uiState.value.formPaymentAccountId
            )
        }
        .catch { _uiState.value = _uiState.value.copy(isLoading = false) }
        .launchIn(viewModelScope)
    }

    private fun loadNisabSettings() {
        viewModelScope.launch {
            val settings = zakatRepo.getNisabSettings(userId)
            _uiState.value = _uiState.value.copy(
                nisabSettings       = settings,
                formNisabThreshold  = if (settings.nisabThreshold > 0) settings.nisabThreshold.toLong().toString() else "",
                formSilverPerGram   = if (settings.silverPerGram > 0) settings.silverPerGram.toString() else "",
                formGoldPerGram     = if (settings.goldPerGram > 0) settings.goldPerGram.toString() else ""
            )
        }
    }

    fun showStartCycleSheet()    { _uiState.value = _uiState.value.copy(showStartCycleSheet = true, formStartDate = LocalDate.now().toString(), errorMessage = null) }
    fun showPaymentSheet()       { _uiState.value = _uiState.value.copy(showPaymentSheet = true, formPaymentAmount = "", errorMessage = null) }
    fun showNisabSheet()         { _uiState.value = _uiState.value.copy(showNisabSheet = true, errorMessage = null) }
    fun dismissSheets()          { _uiState.value = _uiState.value.copy(showStartCycleSheet = false, showPaymentSheet = false, showNisabSheet = false, errorMessage = null) }
    fun clearMessages()          { _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null) }

    fun onStartDateChange(v: String)        { _uiState.value = _uiState.value.copy(formStartDate = v) }
    fun onPaymentAmountChange(v: String)    { _uiState.value = _uiState.value.copy(formPaymentAmount = v) }
    fun onPaymentAccountChange(v: String)   { _uiState.value = _uiState.value.copy(formPaymentAccountId = v) }
    fun onPaymentDateChange(v: String)      { _uiState.value = _uiState.value.copy(formPaymentDate = v) }
    fun onPaymentNotesChange(v: String)     { _uiState.value = _uiState.value.copy(formPaymentNotes = v) }
    fun onNisabThresholdChange(v: String)   { _uiState.value = _uiState.value.copy(formNisabThreshold = v) }
    fun onSilverPerGramChange(v: String)    { _uiState.value = _uiState.value.copy(formSilverPerGram = v) }
    fun onGoldPerGramChange(v: String)      { _uiState.value = _uiState.value.copy(formGoldPerGram = v) }

    fun startCycle() {
        val s = _uiState.value
        if (s.formStartDate.isBlank()) { _uiState.value = s.copy(errorMessage = "Select start date"); return }
        val hijri = try { ZakatUtils.gregorianToHijri(s.formStartDate) } catch (e: Exception) { _uiState.value = s.copy(errorMessage = "Invalid date format"); return }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            when (val r = zakatRepo.startCycle(userId, s.formStartDate,
                hijri.formatted, s.zakatableWealth, s.nisabSettings.nisabThreshold)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(isSaving = false, showStartCycleSheet = false, successMessage = "Zakat cycle started")
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = r.message)
                else -> Unit
            }
        }
    }

    fun recordPayment() {
        val s   = _uiState.value
        val amt = s.formPaymentAmount.toDoubleOrNull()
        val account = s.accounts.find { it.id == s.formPaymentAccountId }
        when {
            amt == null || amt <= 0            -> { _uiState.value = s.copy(errorMessage = "Enter valid amount"); return }
            s.formPaymentAccountId.isEmpty()   -> { _uiState.value = s.copy(errorMessage = "Select account"); return }
            account == null                    -> { _uiState.value = s.copy(errorMessage = "Account not found"); return }
            amt > account.balance              -> { _uiState.value = s.copy(errorMessage = "Insufficient balance"); return }
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val payment = ZakatPayment(
                cycleId     = s.activeCycle?.id ?: "",
                amount      = amt!!,
                accountId   = s.formPaymentAccountId,
                accountName = account!!.name,
                paymentDate = s.formPaymentDate,
                notes       = s.formPaymentNotes.trim()
            )
            when (val r = zakatRepo.recordPayment(userId, payment, account.balance)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(isSaving = false, showPaymentSheet = false, successMessage = "Zakat payment recorded. May Allah accept it. 🤲")
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = r.message)
                else -> Unit
            }
        }
    }

    fun saveNisabSettings() {
        val s     = _uiState.value
        val nisab = s.formNisabThreshold.toDoubleOrNull()
        if (nisab == null || nisab <= 0) { _uiState.value = s.copy(errorMessage = "Enter valid Nisab threshold"); return }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val settings = NisabSettings(
                nisabThreshold = nisab,
                silverPerGram  = s.formSilverPerGram.toDoubleOrNull() ?: 0.0,
                goldPerGram    = s.formGoldPerGram.toDoubleOrNull() ?: 0.0,
                lastUpdated    = LocalDate.now().toString()
            )
            when (val r = zakatRepo.saveNisabSettings(userId, settings)) {
                is Result.Success -> { _uiState.value = _uiState.value.copy(isSaving = false, showNisabSheet = false, nisabSettings = settings, successMessage = "Nisab settings saved") }
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = r.message)
                else -> Unit
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatScreen(
    navController: NavController,
    viewModel: ZakatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Zakat", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { IconButton(onClick = viewModel::showNisabSheet) { Icon(Icons.Default.Settings, "Nisab settings") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // ── Zakat status hero ─────────────────────────────────────────
            item { ZakatStatusHero(uiState, onStart = viewModel::showStartCycleSheet, onPay = viewModel::showPaymentSheet) }

            // ── Zakatable wealth breakdown ─────────────────────────────────
            item { WealthBreakdownCard(uiState.zakatableWealth, uiState.nisabSettings.nisabThreshold) }

            // ── Payment history ───────────────────────────────────────────
            if (uiState.payments.isNotEmpty()) {
                item { Text("Payment History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                items(uiState.payments, key = { it.id }) { payment ->
                    PaymentHistoryItem(payment)
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (uiState.showStartCycleSheet) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissSheets, sheetState = sheetState, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column {
                        Text("Start Zakat Monitoring", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Set the date your wealth crossed Nisab", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = viewModel::dismissSheets) { Icon(Icons.Default.Close, "Close") }
                }

                // Hijri preview
                val hijriPreview = remember(uiState.formStartDate) {
                    try { ZakatUtils.formatHijriDate(ZakatUtils.gregorianToHijri(uiState.formStartDate)) } catch (e: Exception) { "" }
                }

                Card(colors = CardDefaults.cardColors(containerColor = Emerald600.copy(0.1f)), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp), tint = Emerald600)
                        Column {
                            Text("Hijri date", style = MaterialTheme.typography.labelSmall, color = Emerald600)
                            Text(hijriPreview.ifBlank { "Select a valid date" }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Emerald600)
                        }
                    }
                }

                uiState.errorMessage?.let { ErrorCard(it) }
                NisabTextField(uiState.formStartDate, viewModel::onStartDateChange, "Cycle start date (yyyy-MM-dd)", leadingIcon = Icons.Default.CalendarToday)
                NisabButton("Start Monitoring", viewModel::startCycle, isLoading = uiState.isSaving)
            }
        }
    }

    if (uiState.showPaymentSheet) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissSheets, sheetState = sheetState, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column {
                        Text("Record Zakat Payment", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        if (uiState.zakatAmount > 0) {
                            Text("Due: ৳${"%,.0f".format(uiState.zakatAmount)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFEF4444))
                        }
                    }
                    IconButton(onClick = viewModel::dismissSheets) { Icon(Icons.Default.Close, "Close") }
                }
                uiState.errorMessage?.let { ErrorCard(it) }
                NisabTextField(uiState.formPaymentAmount, viewModel::onPaymentAmountChange, "Amount (৳)", leadingIcon = Icons.Default.MonetizationOn, keyboardType = KeyboardType.Decimal)
                SimpleDropdown("Pay from account", uiState.formPaymentAccountId,
                    uiState.accounts.map { it.id to "${it.name} · ৳${"%,.0f".format(it.balance)}" },
                    viewModel::onPaymentAccountChange, Icons.Default.AccountBalance)
                NisabTextField(uiState.formPaymentDate, viewModel::onPaymentDateChange, "Date (yyyy-MM-dd)", leadingIcon = Icons.Default.CalendarToday)
                NisabTextField(uiState.formPaymentNotes, viewModel::onPaymentNotesChange, "Notes (optional)", leadingIcon = Icons.Default.Notes)
                NisabButton("Record Payment", viewModel::recordPayment, isLoading = uiState.isSaving)
            }
        }
    }

    if (uiState.showNisabSheet) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissSheets, sheetState = sheetState, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column {
                        Text("Nisab Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Nisab = 595g silver or 85g gold equivalent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = viewModel::dismissSheets) { Icon(Icons.Default.Close, "Close") }
                }
                uiState.errorMessage?.let { ErrorCard(it) }
                NisabTextField(uiState.formSilverPerGram, viewModel::onSilverPerGramChange, "Silver price per gram (৳)", leadingIcon = Icons.Default.Diamond, keyboardType = KeyboardType.Decimal)
                NisabTextField(uiState.formGoldPerGram, viewModel::onGoldPerGramChange, "Gold price per gram (৳)", leadingIcon = Icons.Default.Star, keyboardType = KeyboardType.Decimal)
                NisabTextField(uiState.formNisabThreshold, viewModel::onNisabThresholdChange, "Nisab threshold (৳) — override", leadingIcon = Icons.Default.MonetizationOn, keyboardType = KeyboardType.Decimal)
                Text("Tip: Nisab (silver) = 595g × silver price per gram", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                NisabButton("Save Settings", viewModel::saveNisabSettings, isLoading = uiState.isSaving)
            }
        }
    }
}

@Composable
private fun ZakatStatusHero(uiState: ZakatUiState, onStart: () -> Unit, onPay: () -> Unit) {
    val statusColor = runCatching { Color(android.graphics.Color.parseColor(uiState.zakatStatus.colorHex)) }.getOrDefault(Emerald600)
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(0.3f))) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(statusColor.copy(0.2f)), Alignment.Center) {
                    Icon(Icons.Default.Star, null, Modifier.size(24.dp), tint = statusColor)
                }
                Column {
                    Text("Zakat Status", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(uiState.zakatStatus.label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }

            if (uiState.hijriStartDate.isNotBlank()) {
                Text("Cycle started: ${uiState.hijriStartDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (uiState.daysRemaining > 0 && uiState.zakatStatus == ZakatStatus.MONITORING) {
                Text("${uiState.daysRemaining} days remaining in Hijri year", style = MaterialTheme.typography.bodySmall, color = statusColor, fontWeight = FontWeight.Medium)
            }
            if (uiState.zakatStatus == ZakatStatus.DUE) {
                Text("Zakat due: ৳${"%,.0f".format(uiState.zakatAmount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (uiState.activeCycle == null) {
                    Button(onClick = onStart, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Start Monitoring")
                    }
                } else {
                    if (uiState.zakatStatus == ZakatStatus.DUE || uiState.zakatStatus == ZakatStatus.MONITORING) {
                        OutlinedButton(onClick = onStart, Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Adjust Date") }
                        Button(onClick = onPay, Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Pay Zakat") }
                    }
                }
            }
        }
    }
}

@Composable
private fun WealthBreakdownCard(zakatableWealth: Double, nisabThreshold: Double) {
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Zakatable Wealth", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Total zakatable wealth", style = MaterialTheme.typography.bodySmall)
                Text("৳${"%,.0f".format(zakatableWealth)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.15f))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Nisab threshold", style = MaterialTheme.typography.bodySmall)
                Text("৳${"%,.0f".format(nisabThreshold)}", style = MaterialTheme.typography.bodySmall,
                    color = if (zakatableWealth >= nisabThreshold) Emerald600 else Color(0xFFEF4444))
            }
            if (zakatableWealth >= nisabThreshold) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.15f))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Zakat (2.5%)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text("৳${"%,.0f".format(ZakatUtils.calculateZakat(zakatableWealth))}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Emerald600)
                }
            }
        }
    }
}

@Composable
private fun PaymentHistoryItem(payment: ZakatPayment) {
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.5.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, null, Modifier.size(32.dp), tint = Emerald600)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Zakat Payment", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("${payment.paymentDate} · ${payment.accountName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (payment.notes.isNotBlank()) Text(payment.notes, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("৳${"%,.0f".format(payment.amount)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Emerald600)
        }
    }
}

package com.hasan.nisabwallet.ui.screens.investments

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.model.Account
import com.hasan.nisabwallet.data.model.DividendEntry
import com.hasan.nisabwallet.data.model.Investment
import com.hasan.nisabwallet.data.repository.AccountRepository
import com.hasan.nisabwallet.data.repository.InvestmentRepository
import com.hasan.nisabwallet.ui.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class InvestmentsUiState(
    val investments: List<Investment>    = emptyList(),
    val accounts: List<Account>          = emptyList(),
    val filterType: String               = "all",
    val isLoading: Boolean               = true,
    val isSaving: Boolean                = false,
    val showFormSheet: Boolean           = false,
    val showUpdateValueSheet: Boolean    = false,
    val showDividendSheet: Boolean       = false,
    val showDeleteDialog: Boolean        = false,
    val editingInv: Investment?          = null,
    val selectedInv: Investment?         = null,
    val deletingInv: Investment?         = null,
    val errorMessage: String?            = null,
    val successMessage: String?          = null,
    // Portfolio totals
    val totalInvested: Double            = 0.0,
    val totalCurrentValue: Double        = 0.0,
    val totalReturn: Double              = 0.0,
    val totalDividends: Double           = 0.0,
    // Form
    val formName: String                 = "",
    val formType: String                 = "stock",
    val formPurchasePrice: String        = "",
    val formCurrentValue: String         = "",
    val formQuantity: String             = "1",
    val formPurchaseDate: String         = LocalDate.now().toString(),
    val formMaturityDate: String         = "",
    val formBroker: String               = "",
    val formAccountId: String            = "",
    val formNotes: String                = "",
    val formNewValue: String             = "",
    val formDividendAmount: String       = "",
    val formDividendDate: String         = LocalDate.now().toString(),
    val formDividendNotes: String        = ""
)

@HiltViewModel
class InvestmentsViewModel @Inject constructor(
    private val invRepo: InvestmentRepository,
    private val accountRepo: AccountRepository,
    private val auth: FirebaseAuth
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(InvestmentsUiState())
    val uiState = _uiState.asStateFlow()
    private val userId get() = auth.currentUser?.uid ?: ""

    init { observeInvestments(); observeAccounts() }

    private fun observeInvestments() {
        invRepo.getInvestmentsFlow(userId)
            .onEach { investments ->
                val active = investments.filter { it.status == "active" }
                _uiState.value = _uiState.value.copy(
                    investments       = filterInvestments(investments, _uiState.value.filterType),
                    totalInvested     = active.sumOf { it.totalInvested },
                    totalCurrentValue = active.sumOf { it.totalCurrentValue },
                    totalReturn       = active.sumOf { it.absoluteReturn },
                    totalDividends    = active.sumOf { it.totalDividends },
                    isLoading         = false
                )
            }
            .catch { _uiState.value = _uiState.value.copy(isLoading = false) }
            .launchIn(viewModelScope)
    }

    private fun observeAccounts() {
        accountRepo.getAccountsFlow(userId)
            .onEach { _uiState.value = _uiState.value.copy(accounts = it) }
            .launchIn(viewModelScope)
    }

    private fun filterInvestments(investments: List<Investment>, type: String) =
        if (type == "all") investments else investments.filter { it.type == type }

    fun setFilter(type: String) { _uiState.value = _uiState.value.copy(filterType = type); observeInvestments() }

    fun showAddSheet() {
        _uiState.value = _uiState.value.copy(showFormSheet = true, editingInv = null,
            formName = "", formType = "stock", formPurchasePrice = "", formCurrentValue = "",
            formQuantity = "1", formPurchaseDate = LocalDate.now().toString(),
            formMaturityDate = "", formBroker = "",
            formAccountId = _uiState.value.accounts.firstOrNull()?.id ?: "",
            formNotes = "", errorMessage = null)
    }
    fun showEditSheet(inv: Investment) {
        _uiState.value = _uiState.value.copy(showFormSheet = true, editingInv = inv,
            formName = inv.name, formType = inv.type,
            formPurchasePrice = inv.purchasePrice.toString(), formCurrentValue = inv.currentValue.toString(),
            formQuantity = inv.quantity.toString(), formPurchaseDate = inv.purchaseDate,
            formMaturityDate = inv.maturityDate, formBroker = inv.broker,
            formAccountId = inv.accountId, formNotes = inv.notes, errorMessage = null)
    }
    fun showUpdateValueSheet(inv: Investment) { _uiState.value = _uiState.value.copy(showUpdateValueSheet = true, selectedInv = inv, formNewValue = inv.currentValue.toString()) }
    fun showDividendSheet(inv: Investment)   { _uiState.value = _uiState.value.copy(showDividendSheet = true, selectedInv = inv, formDividendAmount = "", formDividendDate = LocalDate.now().toString(), formDividendNotes = "") }
    fun dismissForms() { _uiState.value = _uiState.value.copy(showFormSheet = false, showUpdateValueSheet = false, showDividendSheet = false, editingInv = null, selectedInv = null, errorMessage = null) }
    fun showDeleteDialog(inv: Investment) { _uiState.value = _uiState.value.copy(showDeleteDialog = true, deletingInv = inv) }
    fun dismissDeleteDialog() { _uiState.value = _uiState.value.copy(showDeleteDialog = false, deletingInv = null) }
    fun clearMessages() { _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null) }

    fun onNameChange(v: String)            { _uiState.value = _uiState.value.copy(formName = v) }
    fun onTypeChange(v: String)            { _uiState.value = _uiState.value.copy(formType = v) }
    fun onPurchasePriceChange(v: String)   { _uiState.value = _uiState.value.copy(formPurchasePrice = v) }
    fun onCurrentValueChange(v: String)    { _uiState.value = _uiState.value.copy(formCurrentValue = v) }
    fun onQuantityChange(v: String)        { _uiState.value = _uiState.value.copy(formQuantity = v) }
    fun onPurchaseDateChange(v: String)    { _uiState.value = _uiState.value.copy(formPurchaseDate = v) }
    fun onMaturityDateChange(v: String)    { _uiState.value = _uiState.value.copy(formMaturityDate = v) }
    fun onBrokerChange(v: String)          { _uiState.value = _uiState.value.copy(formBroker = v) }
    fun onAccountChange(v: String)         { _uiState.value = _uiState.value.copy(formAccountId = v) }
    fun onNotesChange(v: String)           { _uiState.value = _uiState.value.copy(formNotes = v) }
    fun onNewValueChange(v: String)        { _uiState.value = _uiState.value.copy(formNewValue = v) }
    fun onDividendAmountChange(v: String)  { _uiState.value = _uiState.value.copy(formDividendAmount = v) }
    fun onDividendDateChange(v: String)    { _uiState.value = _uiState.value.copy(formDividendDate = v) }
    fun onDividendNotesChange(v: String)   { _uiState.value = _uiState.value.copy(formDividendNotes = v) }

    fun saveInvestment() {
        val s = _uiState.value
        val purchasePrice = s.formPurchasePrice.toDoubleOrNull()
        val currentValue  = s.formCurrentValue.toDoubleOrNull() ?: purchasePrice
        val quantity      = s.formQuantity.toDoubleOrNull() ?: 1.0
        when {
            s.formName.isBlank()               -> { _uiState.value = s.copy(errorMessage = "Investment name required"); return }
            purchasePrice == null || purchasePrice <= 0 -> { _uiState.value = s.copy(errorMessage = "Enter valid purchase price"); return }
        }
        val account = s.accounts.find { it.id == s.formAccountId }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            val inv = Investment(
                name = s.formName.trim(), type = s.formType, purchasePrice = purchasePrice!!,
                currentValue = currentValue ?: purchasePrice, quantity = quantity,
                purchaseDate = s.formPurchaseDate, maturityDate = s.formMaturityDate,
                broker = s.formBroker, accountId = s.formAccountId,
                accountName = account?.name ?: "", notes = s.formNotes.trim()
            )
            val result = if (s.editingInv == null)
                invRepo.addInvestment(userId, inv, account?.balance ?: 0.0)
                    .let { if (it is Result.Success) Result.Success(Unit) else it as Result<Unit> }
            else invRepo.updateInvestment(userId, s.editingInv.id, inv)
            when (result) {
                is Result.Success -> _uiState.value = _uiState.value.copy(isSaving = false, showFormSheet = false, successMessage = if (s.editingInv == null) "Investment added" else "Investment updated")
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
                else -> Unit
            }
        }
    }

    fun updateCurrentValue() {
        val s   = _uiState.value
        val inv = s.selectedInv ?: return
        val v   = s.formNewValue.toDoubleOrNull()
        if (v == null || v <= 0) { _uiState.value = s.copy(errorMessage = "Enter valid value"); return }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            when (val r = invRepo.updateCurrentValue(userId, inv.id, v)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(isSaving = false, showUpdateValueSheet = false, successMessage = "Value updated")
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = r.message)
                else -> Unit
            }
        }
    }

    fun addDividend() {
        val s   = _uiState.value
        val inv = s.selectedInv ?: return
        val amt = s.formDividendAmount.toDoubleOrNull()
        if (amt == null || amt <= 0) { _uiState.value = s.copy(errorMessage = "Enter valid dividend amount"); return }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val dividend = DividendEntry(amount = amt, date = s.formDividendDate, notes = s.formDividendNotes.trim())
            when (val r = invRepo.addDividend(userId, inv.id, dividend, inv.totalDividends)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(isSaving = false, showDividendSheet = false, successMessage = "Dividend recorded")
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = r.message)
                else -> Unit
            }
        }
    }

    fun deleteInvestment() {
        val inv = _uiState.value.deletingInv ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            when (val r = invRepo.deleteInvestment(userId, inv.id)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(isSaving = false, showDeleteDialog = false, deletingInv = null, successMessage = "Investment deleted")
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = r.message)
                else -> Unit
            }
        }
    }
}

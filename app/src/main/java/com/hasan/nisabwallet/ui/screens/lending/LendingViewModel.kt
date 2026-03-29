package com.hasan.nisabwallet.ui.screens.lending

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.model.Account
import com.hasan.nisabwallet.data.model.Lending
import com.hasan.nisabwallet.data.model.LendingRepayment
import com.hasan.nisabwallet.data.repository.AccountRepository
import com.hasan.nisabwallet.data.repository.LendingRepository
import com.hasan.nisabwallet.ui.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class LendingUiState(
    val lendings: List<Lending>        = emptyList(),
    val accounts: List<Account>        = emptyList(),
    val filterStatus: String           = "active",
    val isLoading: Boolean             = true,
    val isSaving: Boolean              = false,
    val showFormSheet: Boolean         = false,
    val showRepaymentSheet: Boolean    = false,
    val showDeleteDialog: Boolean      = false,
    val editingLending: Lending?       = null,
    val selectedLending: Lending?      = null,
    val deletingLending: Lending?      = null,
    val errorMessage: String?          = null,
    val successMessage: String?        = null,
    val totalActive: Double            = 0.0,
    val totalOverdue: Double           = 0.0,
    // Form
    val formBorrowerName: String       = "",
    val formPhone: String              = "",
    val formAmount: String             = "",
    val formDueDate: String            = "",
    val formPurpose: String            = "",
    val formAccountId: String          = "",
    val formCountForZakat: Boolean     = true,
    val formNotes: String              = "",
    // Repayment form
    val repayAmount: String            = "",
    val repayDate: String              = LocalDate.now().toString(),
    val repayNotes: String             = ""
)

@HiltViewModel
class LendingViewModel @Inject constructor(
    private val lendingRepo: LendingRepository,
    private val accountRepo: AccountRepository,
    private val auth: FirebaseAuth
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(LendingUiState())
    val uiState = _uiState.asStateFlow()

    private val userId get() = auth.currentUser?.uid ?: ""

    init {
        observeLendings()
        observeAccounts()
    }

    private fun observeLendings() {
        lendingRepo.getLendingsFlow(userId)
            .onEach { lendings ->
                val filtered = filterLendings(lendings, _uiState.value.filterStatus)
                _uiState.value = _uiState.value.copy(
                    lendings     = filtered,
                    totalActive  = lendings.filter { !it.isReturned }.sumOf { it.remainingAmount },
                    totalOverdue = lendings.filter { it.isOverdue }.sumOf { it.remainingAmount },
                    isLoading    = false
                )
            }
            .catch { _uiState.value = _uiState.value.copy(isLoading = false) }
            .launchIn(viewModelScope)
    }

    private fun observeAccounts() {
        accountRepo.getAccountsFlow(userId)
            .onEach { accounts ->
                _uiState.value = _uiState.value.copy(
                    accounts      = accounts,
                    formAccountId = if (_uiState.value.formAccountId.isEmpty() && accounts.isNotEmpty())
                        accounts.first().id else _uiState.value.formAccountId
                )
            }
            .launchIn(viewModelScope)
    }

    fun setFilter(status: String) {
        _uiState.value = _uiState.value.copy(filterStatus = status)
        observeLendings()
    }

    private fun filterLendings(lendings: List<Lending>, status: String) = when (status) {
        "active"   -> lendings.filter { !it.isReturned }
        "returned" -> lendings.filter { it.isReturned }
        "overdue"  -> lendings.filter { it.isOverdue }
        else       -> lendings
    }

    fun showAddSheet() {
        _uiState.value = _uiState.value.copy(
            showFormSheet    = true, editingLending = null,
            formBorrowerName = "", formPhone = "", formAmount = "",
            formDueDate      = "", formPurpose = "",
            formAccountId    = _uiState.value.accounts.firstOrNull()?.id ?: "",
            formCountForZakat = true, formNotes = "", errorMessage = null
        )
    }

    fun showEditSheet(lending: Lending) {
        _uiState.value = _uiState.value.copy(
            showFormSheet    = true, editingLending = lending,
            formBorrowerName = lending.borrowerName,
            formPhone        = lending.phone,
            formAmount       = lending.amount.toLong().toString(),
            formDueDate      = lending.dueDate,
            formPurpose      = lending.purpose,
            formAccountId    = lending.accountId,
            formCountForZakat = lending.countForZakat,
            formNotes        = lending.notes,
            errorMessage     = null
        )
    }

    fun showRepaymentSheet(lending: Lending) {
        _uiState.value = _uiState.value.copy(
            showRepaymentSheet = true, selectedLending = lending,
            repayAmount = "", repayDate = LocalDate.now().toString(),
            repayNotes = "", errorMessage = null
        )
    }

    fun dismissForms() { _uiState.value = _uiState.value.copy(showFormSheet = false, showRepaymentSheet = false, editingLending = null, selectedLending = null, errorMessage = null) }
    fun showDeleteDialog(l: Lending) { _uiState.value = _uiState.value.copy(showDeleteDialog = true, deletingLending = l) }
    fun dismissDeleteDialog() { _uiState.value = _uiState.value.copy(showDeleteDialog = false, deletingLending = null) }
    fun clearMessages() { _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null) }

    fun onBorrowerNameChange(v: String)  { _uiState.value = _uiState.value.copy(formBorrowerName = v) }
    fun onPhoneChange(v: String)         { _uiState.value = _uiState.value.copy(formPhone = v) }
    fun onAmountChange(v: String)        { _uiState.value = _uiState.value.copy(formAmount = v) }
    fun onDueDateChange(v: String)       { _uiState.value = _uiState.value.copy(formDueDate = v) }
    fun onPurposeChange(v: String)       { _uiState.value = _uiState.value.copy(formPurpose = v) }
    fun onAccountChange(v: String)       { _uiState.value = _uiState.value.copy(formAccountId = v) }
    fun onZakatChange(v: Boolean)        { _uiState.value = _uiState.value.copy(formCountForZakat = v) }
    fun onNotesChange(v: String)         { _uiState.value = _uiState.value.copy(formNotes = v) }
    fun onRepayAmountChange(v: String)   { _uiState.value = _uiState.value.copy(repayAmount = v) }
    fun onRepayDateChange(v: String)     { _uiState.value = _uiState.value.copy(repayDate = v) }
    fun onRepayNotesChange(v: String)    { _uiState.value = _uiState.value.copy(repayNotes = v) }

    fun saveLending() {
        val s = _uiState.value
        val amount = s.formAmount.toDoubleOrNull()
        when {
            s.formBorrowerName.isBlank() -> { _uiState.value = s.copy(errorMessage = "Borrower name required"); return }
            amount == null || amount <= 0 -> { _uiState.value = s.copy(errorMessage = "Enter valid amount"); return }
            s.formAccountId.isEmpty()    -> { _uiState.value = s.copy(errorMessage = "Select an account"); return }
        }
        val account = s.accounts.find { it.id == s.formAccountId } ?: return
        if (s.editingLending == null && amount!! > account.balance) {
            _uiState.value = s.copy(errorMessage = "Insufficient balance in ${account.name}"); return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            val lending = Lending(
                borrowerName  = s.formBorrowerName.trim(),
                phone         = s.formPhone.trim(),
                amount        = amount!!,
                dueDate       = s.formDueDate,
                purpose       = s.formPurpose.trim(),
                accountId     = s.formAccountId,
                accountName   = account.name,
                countForZakat = s.formCountForZakat,
                notes         = s.formNotes.trim()
            )
            val result = if (s.editingLending == null)
                lendingRepo.addLending(userId, lending, account.balance)
                    .let { if (it is Result.Success) Result.Success(Unit) else it as Result<Unit> }
            else
                lendingRepo.updateLending(userId, s.editingLending.id, lending)

            when (result) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSaving = false, showFormSheet = false,
                    successMessage = if (s.editingLending == null) "Lending recorded" else "Lending updated"
                )
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
                else -> Unit
            }
        }
    }

    fun recordRepayment() {
        val s       = _uiState.value
        val lending = s.selectedLending ?: return
        val amount  = s.repayAmount.toDoubleOrNull()
        if (amount == null || amount <= 0) { _uiState.value = s.copy(errorMessage = "Enter valid amount"); return }
        val account = s.accounts.find { it.id == lending.accountId } ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val repayment = LendingRepayment(amount = amount, date = s.repayDate, notes = s.repayNotes.trim())
            when (val r = lendingRepo.recordRepayment(userId, lending.id, repayment, lending, account.balance)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSaving = false, showRepaymentSheet = false,
                    successMessage = "Repayment of ৳${"%,.0f".format(amount)} recorded"
                )
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = r.message)
                else -> Unit
            }
        }
    }

    fun deleteLending() {
        val lending = _uiState.value.deletingLending ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            when (val r = lendingRepo.deleteLending(userId, lending.id)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSaving = false, showDeleteDialog = false, deletingLending = null,
                    successMessage = "Lending deleted"
                )
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = r.message)
                else -> Unit
            }
        }
    }
}

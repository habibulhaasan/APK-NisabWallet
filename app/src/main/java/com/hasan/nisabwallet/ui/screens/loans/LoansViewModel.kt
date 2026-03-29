package com.hasan.nisabwallet.ui.screens.loans

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.model.Account
import com.hasan.nisabwallet.data.model.Loan
import com.hasan.nisabwallet.data.model.LoanPayment
import com.hasan.nisabwallet.data.repository.AccountRepository
import com.hasan.nisabwallet.data.repository.LoanRepository
import com.hasan.nisabwallet.ui.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class LoansUiState(
    val loans: List<Loan>             = emptyList(),
    val accounts: List<Account>       = emptyList(),
    val filterStatus: String          = "active",  // "all" | "active" | "paid"
    val isLoading: Boolean            = true,
    val isSaving: Boolean             = false,
    val showFormSheet: Boolean        = false,
    val showPaymentSheet: Boolean     = false,
    val showDeleteDialog: Boolean     = false,
    val editingLoan: Loan?            = null,
    val selectedLoan: Loan?           = null,
    val deletingLoan: Loan?           = null,
    val errorMessage: String?         = null,
    val successMessage: String?       = null,
    // Totals
    val totalActiveLoans: Double      = 0.0,
    // Loan form
    val formLenderName: String        = "",
    val formLoanType: String          = "qard-hasan",
    val formPrincipal: String         = "",
    val formInterestRate: String      = "0",
    val formMonthlyPayment: String    = "",
    val formTotalMonths: String       = "",
    val formStartDate: String         = LocalDate.now().toString(),
    val formAccountId: String         = "",
    val formNotes: String             = "",
    val formReminders: Boolean        = true,
    // Payment form
    val paymentAmount: String         = "",
    val paymentDate: String           = LocalDate.now().toString(),
    val paymentNotes: String          = ""
)

@HiltViewModel
class LoansViewModel @Inject constructor(
    private val loanRepo: LoanRepository,
    private val accountRepo: AccountRepository,
    private val auth: FirebaseAuth
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(LoansUiState())
    val uiState = _uiState.asStateFlow()

    private val userId get() = auth.currentUser?.uid ?: ""

    init {
        observeLoans()
        observeAccounts()
    }

    private fun observeLoans() {
        loanRepo.getLoansFlow(userId)
            .onEach { loans ->
                val filtered = filterLoans(loans, _uiState.value.filterStatus)
                _uiState.value = _uiState.value.copy(
                    loans            = filtered,
                    totalActiveLoans = loans.filter { it.status == "active" }.sumOf { it.remainingBalance },
                    isLoading        = false
                )
            }
            .catch { _uiState.value = _uiState.value.copy(isLoading = false) }
            .launchIn(viewModelScope)
    }

    private fun observeAccounts() {
        accountRepo.getAccountsFlow(userId)
            .onEach { accounts ->
                _uiState.value = _uiState.value.copy(
                    accounts     = accounts,
                    formAccountId = if (_uiState.value.formAccountId.isEmpty() && accounts.isNotEmpty())
                        accounts.first().id else _uiState.value.formAccountId
                )
            }
            .launchIn(viewModelScope)
    }

    fun setFilter(status: String) {
        _uiState.value = _uiState.value.copy(filterStatus = status)
        observeLoans()
    }

    private fun filterLoans(loans: List<Loan>, status: String) = when (status) {
        "active" -> loans.filter { !it.isPaid }
        "paid"   -> loans.filter { it.isPaid }
        else     -> loans
    }

    // ── Sheet control ─────────────────────────────────────────────────────────
    fun showAddSheet() {
        val accounts = _uiState.value.accounts
        _uiState.value = _uiState.value.copy(
            showFormSheet   = true, editingLoan = null,
            formLenderName  = "", formLoanType = "qard-hasan",
            formPrincipal   = "", formInterestRate = "0",
            formMonthlyPayment = "", formTotalMonths = "",
            formStartDate   = LocalDate.now().toString(),
            formAccountId   = accounts.firstOrNull()?.id ?: "",
            formNotes = "", formReminders = true, errorMessage = null
        )
    }

    fun showEditSheet(loan: Loan) {
        _uiState.value = _uiState.value.copy(
            showFormSheet      = true, editingLoan = loan,
            formLenderName     = loan.lenderName,
            formLoanType       = loan.loanType,
            formPrincipal      = loan.principalAmount.toLong().toString(),
            formInterestRate   = loan.interestRate.toString(),
            formMonthlyPayment = loan.monthlyPayment.toLong().toString(),
            formTotalMonths    = loan.totalMonths.toString(),
            formStartDate      = loan.startDate,
            formAccountId      = loan.accountId,
            formNotes          = loan.notes,
            formReminders      = loan.enableReminders,
            errorMessage       = null
        )
    }

    fun showPaymentSheet(loan: Loan) {
        _uiState.value = _uiState.value.copy(
            showPaymentSheet = true, selectedLoan = loan,
            paymentAmount    = loan.monthlyPayment.toLong().let { if (it > 0) it.toString() else "" },
            paymentDate      = LocalDate.now().toString(),
            paymentNotes     = "", errorMessage = null
        )
    }

    fun dismissForms() {
        _uiState.value = _uiState.value.copy(
            showFormSheet    = false, showPaymentSheet = false,
            editingLoan      = null, selectedLoan = null, errorMessage = null
        )
    }

    fun showDeleteDialog(loan: Loan) { _uiState.value = _uiState.value.copy(showDeleteDialog = true, deletingLoan = loan) }
    fun dismissDeleteDialog()        { _uiState.value = _uiState.value.copy(showDeleteDialog = false, deletingLoan = null) }
    fun clearMessages()              { _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null) }

    fun onLenderNameChange(v: String)     { _uiState.value = _uiState.value.copy(formLenderName = v) }
    fun onLoanTypeChange(v: String)       { _uiState.value = _uiState.value.copy(formLoanType = v) }
    fun onPrincipalChange(v: String)      { _uiState.value = _uiState.value.copy(formPrincipal = v) }
    fun onInterestRateChange(v: String)   { _uiState.value = _uiState.value.copy(formInterestRate = v) }
    fun onMonthlyPaymentChange(v: String) { _uiState.value = _uiState.value.copy(formMonthlyPayment = v) }
    fun onTotalMonthsChange(v: String)    { _uiState.value = _uiState.value.copy(formTotalMonths = v) }
    fun onStartDateChange(v: String)      { _uiState.value = _uiState.value.copy(formStartDate = v) }
    fun onAccountChange(v: String)        { _uiState.value = _uiState.value.copy(formAccountId = v) }
    fun onNotesChange(v: String)          { _uiState.value = _uiState.value.copy(formNotes = v) }
    fun onRemindersChange(v: Boolean)     { _uiState.value = _uiState.value.copy(formReminders = v) }
    fun onPaymentAmountChange(v: String)  { _uiState.value = _uiState.value.copy(paymentAmount = v) }
    fun onPaymentDateChange(v: String)    { _uiState.value = _uiState.value.copy(paymentDate = v) }
    fun onPaymentNotesChange(v: String)   { _uiState.value = _uiState.value.copy(paymentNotes = v) }

    // ── CRUD ──────────────────────────────────────────────────────────────────
    fun saveLoan() {
        val s = _uiState.value
        val principal = s.formPrincipal.toDoubleOrNull()
        when {
            s.formLenderName.isBlank() -> { _uiState.value = s.copy(errorMessage = "Lender name required"); return }
            principal == null || principal <= 0 -> { _uiState.value = s.copy(errorMessage = "Enter valid principal"); return }
            s.formAccountId.isEmpty()  -> { _uiState.value = s.copy(errorMessage = "Select an account"); return }
        }
        val account = s.accounts.find { it.id == s.formAccountId } ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            val loan = Loan(
                lenderName      = s.formLenderName.trim(),
                loanType        = s.formLoanType,
                principalAmount = principal!!,
                interestRate    = s.formInterestRate.toDoubleOrNull() ?: 0.0,
                monthlyPayment  = s.formMonthlyPayment.toDoubleOrNull() ?: 0.0,
                totalMonths     = s.formTotalMonths.toIntOrNull() ?: 0,
                startDate       = s.formStartDate,
                accountId       = s.formAccountId,
                accountName     = account.name,
                notes           = s.formNotes.trim(),
                enableReminders = s.formReminders
            )

            val result = if (s.editingLoan == null)
                loanRepo.addLoan(userId, loan, account.balance)
                    .let { if (it is Result.Success) Result.Success(Unit) else it as Result<Unit> }
            else
                loanRepo.updateLoan(userId, s.editingLoan.id, loan)

            when (result) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSaving = false, showFormSheet = false,
                    successMessage = if (s.editingLoan == null) "Loan added" else "Loan updated"
                )
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
                else -> Unit
            }
        }
    }

    fun recordPayment() {
        val s    = _uiState.value
        val loan = s.selectedLoan ?: return
        val amt  = s.paymentAmount.toDoubleOrNull()
        if (amt == null || amt <= 0) { _uiState.value = s.copy(errorMessage = "Enter valid amount"); return }
        val account = s.accounts.find { it.id == loan.accountId } ?: return
        if (amt > account.balance) { _uiState.value = s.copy(errorMessage = "Insufficient balance"); return }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val payment = LoanPayment(amount = amt, date = s.paymentDate, notes = s.paymentNotes.trim())
            when (val r = loanRepo.recordPayment(userId, loan.id, payment, loan, account.balance)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSaving = false, showPaymentSheet = false,
                    successMessage = "Payment of ৳${"%,.0f".format(amt)} recorded"
                )
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = r.message)
                else -> Unit
            }
        }
    }

    fun deleteLoan() {
        val loan = _uiState.value.deletingLoan ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            when (val r = loanRepo.deleteLoan(userId, loan.id)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSaving = false, showDeleteDialog = false, deletingLoan = null,
                    successMessage = "Loan deleted"
                )
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = r.message)
                else -> Unit
            }
        }
    }
}

package com.hasan.nisabwallet.ui.screens.transactions

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.model.Account
import com.hasan.nisabwallet.data.model.Category
import com.hasan.nisabwallet.data.model.Transaction
import com.hasan.nisabwallet.data.repository.AccountRepository
import com.hasan.nisabwallet.data.repository.CategoryRepository
import com.hasan.nisabwallet.data.repository.TransactionRepository
import com.hasan.nisabwallet.ui.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class TransactionsUiState(
    val allTransactions: List<Transaction>  = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val accounts: List<Account>             = emptyList(),
    val categories: List<Category>          = emptyList(),
    val filter: TransactionFilter           = TransactionFilter(),
    val isLoading: Boolean                  = true,
    val isSaving: Boolean                   = false,
    val showFormSheet: Boolean              = false,
    val showDeleteDialog: Boolean           = false,
    val editingTransaction: Transaction?    = null,
    val deletingTransaction: Transaction?   = null,
    val errorMessage: String?               = null,
    val successMessage: String?             = null,
    // Summary
    val totalIncome: Double                 = 0.0,
    val totalExpense: Double                = 0.0,
    // Form fields
    val formType: String                    = "Expense",
    val formAmount: String                  = "",
    val formAccountId: String               = "",
    val formCategoryId: String              = "",
    val formDescription: String             = "",
    val formDate: String                    = LocalDate.now().toString()
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val accountRepo: AccountRepository,
    private val categoryRepo: CategoryRepository,
    private val auth: FirebaseAuth
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState = _uiState.asStateFlow()

    private val userId get() = auth.currentUser?.uid ?: ""

    init {
        observeTransactions()
        observeAccounts()
        observeCategories()
    }

    // ── Real-time observers ───────────────────────────────────────────────────

    private fun observeTransactions() {
        transactionRepo.getTransactionsFlow(userId)
            .onEach { txns ->
                val filtered = applyFilter(txns, _uiState.value.filter)
                _uiState.value = _uiState.value.copy(
                    allTransactions      = txns,
                    filteredTransactions = filtered,
                    totalIncome          = filtered.filter { it.isIncome  }.sumOf { it.amount },
                    totalExpense         = filtered.filter { it.isExpense }.sumOf { it.amount },
                    isLoading            = false
                )
            }
            .catch { e -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message) }
            .launchIn(viewModelScope)
    }

    private fun observeAccounts() {
        accountRepo.getAccountsFlow(userId)
            .onEach { accounts ->
                val current = _uiState.value
                _uiState.value = current.copy(
                    accounts      = accounts,
                    formAccountId = if (current.formAccountId.isEmpty() && accounts.isNotEmpty())
                        accounts.first().id else current.formAccountId
                )
            }
            .launchIn(viewModelScope)
    }

    private fun observeCategories() {
        categoryRepo.getCategoriesFlow(userId)
            .onEach { cats -> _uiState.value = _uiState.value.copy(categories = cats) }
            .launchIn(viewModelScope)
    }

    // ── Filter ────────────────────────────────────────────────────────────────

    fun updateFilter(filter: TransactionFilter) {
        val filtered = applyFilter(_uiState.value.allTransactions, filter)
        _uiState.value = _uiState.value.copy(
            filter               = filter,
            filteredTransactions = filtered,
            totalIncome          = filtered.filter { it.isIncome  }.sumOf { it.amount },
            totalExpense         = filtered.filter { it.isExpense }.sumOf { it.amount }
        )
    }

    fun clearFilter() = updateFilter(TransactionFilter())

    private fun applyFilter(
        txns: List<Transaction>,
        filter: TransactionFilter
    ): List<Transaction> = txns.filter { t ->
        (filter.type       == "All" || t.type       == filter.type) &&
        (filter.categoryId.isEmpty() || t.categoryId == filter.categoryId) &&
        (filter.accountId.isEmpty()  || t.accountId  == filter.accountId) &&
        (filter.startDate.isEmpty()  || t.date >= filter.startDate) &&
        (filter.endDate.isEmpty()    || t.date <= filter.endDate) &&
        (filter.searchQuery.isEmpty() ||
            t.description.contains(filter.searchQuery, ignoreCase = true) ||
            t.categoryName.contains(filter.searchQuery, ignoreCase = true) ||
            t.accountName.contains(filter.searchQuery, ignoreCase = true))
    }

    // ── Form state ────────────────────────────────────────────────────────────

    fun showAddForm(type: String = "Expense") {
        val accounts = _uiState.value.accounts
        _uiState.value = _uiState.value.copy(
            showFormSheet      = true,
            editingTransaction = null,
            formType           = type,
            formAmount         = "",
            formAccountId      = accounts.firstOrNull()?.id ?: "",
            formCategoryId     = "",
            formDescription    = "",
            formDate           = LocalDate.now().toString(),
            errorMessage       = null
        )
    }

    fun showEditForm(transaction: Transaction) {
        _uiState.value = _uiState.value.copy(
            showFormSheet      = true,
            editingTransaction = transaction,
            formType           = transaction.type,
            formAmount         = transaction.amount.toLong().toString(),
            formAccountId      = transaction.accountId,
            formCategoryId     = transaction.categoryId,
            formDescription    = transaction.description,
            formDate           = transaction.date,
            errorMessage       = null
        )
    }

    fun dismissForm() {
        _uiState.value = _uiState.value.copy(showFormSheet = false, editingTransaction = null, errorMessage = null)
    }

    fun showDeleteDialog(transaction: Transaction) {
        _uiState.value = _uiState.value.copy(showDeleteDialog = true, deletingTransaction = transaction)
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = false, deletingTransaction = null)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    // Form field setters
    fun onTypeChange(v: String)        { _uiState.value = _uiState.value.copy(formType = v, formCategoryId = "") }
    fun onAmountChange(v: String)      { _uiState.value = _uiState.value.copy(formAmount = v) }
    fun onAccountChange(v: String)     { _uiState.value = _uiState.value.copy(formAccountId = v) }
    fun onCategoryChange(v: String)    { _uiState.value = _uiState.value.copy(formCategoryId = v) }
    fun onDescriptionChange(v: String) { _uiState.value = _uiState.value.copy(formDescription = v) }
    fun onDateChange(v: String)        { _uiState.value = _uiState.value.copy(formDate = v) }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    fun saveTransaction() {
        val s = _uiState.value
        val amount = s.formAmount.toDoubleOrNull()
        when {
            amount == null || amount <= 0 -> { _uiState.value = s.copy(errorMessage = "Enter a valid amount"); return }
            s.formAccountId.isEmpty()     -> { _uiState.value = s.copy(errorMessage = "Select an account"); return }
            s.formCategoryId.isEmpty()    -> { _uiState.value = s.copy(errorMessage = "Select a category"); return }
            s.formDate.isEmpty()          -> { _uiState.value = s.copy(errorMessage = "Select a date"); return }
        }

        val account  = s.accounts.find { it.id == s.formAccountId } ?: return
        val category = s.categories.find { it.id == s.formCategoryId } ?: return

        // Guard: prevent overdraft below 0 for expense
        if (s.editingTransaction == null && s.formType == "Expense" && amount!! > account.balance) {
            _uiState.value = s.copy(errorMessage = "Insufficient balance in ${account.name}")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)

            val txn = Transaction(
                id           = s.editingTransaction?.id ?: "",
                type         = s.formType,
                amount       = amount!!,
                accountId    = s.formAccountId,
                accountName  = account.name,
                categoryId   = s.formCategoryId,
                categoryName = category.name,
                categoryColor = category.color,
                description  = s.formDescription.trim(),
                date         = s.formDate,
                isRiba       = category.isRiba
            )

            val result = if (s.editingTransaction == null) {
                transactionRepo.addTransaction(userId, txn, account.balance, category.isRiba)
                    .let { if (it is Result.Success) Result.Success(Unit) else it as Result<Unit> }
            } else {
                transactionRepo.updateTransaction(userId, s.editingTransaction.id, txn, s.editingTransaction, account.balance)
            }

            when (result) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSaving       = false,
                    showFormSheet  = false,
                    successMessage = if (s.editingTransaction == null) "Transaction added" else "Transaction updated"
                )
                is Result.Error   -> _uiState.value = _uiState.value.copy(
                    isSaving     = false,
                    errorMessage = result.message
                )
                else -> Unit
            }
        }
    }

    fun deleteTransaction() {
        val txn     = _uiState.value.deletingTransaction ?: return
        val account = _uiState.value.accounts.find { it.id == txn.accountId } ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            when (val r = transactionRepo.deleteTransaction(userId, txn.id, txn, account.balance)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSaving         = false,
                    showDeleteDialog = false,
                    deletingTransaction = null,
                    successMessage   = "Transaction deleted"
                )
                is Result.Error   -> _uiState.value = _uiState.value.copy(
                    isSaving     = false,
                    errorMessage = r.message
                )
                else -> Unit
            }
        }
    }
}

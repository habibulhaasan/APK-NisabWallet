package com.hasan.nisabwallet.ui.screens.accounts

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.model.Account
import com.hasan.nisabwallet.data.model.AccountType
import com.hasan.nisabwallet.data.repository.AccountRepository
import com.hasan.nisabwallet.ui.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountsUiState(
    val accounts: List<Account>      = emptyList(),
    val isLoading: Boolean           = true,
    val totalBalance: Double         = 0.0,
    val showAddSheet: Boolean        = false,
    val showEditSheet: Boolean       = false,
    val showDeleteDialog: Boolean    = false,
    val editingAccount: Account?     = null,
    val deletingAccount: Account?    = null,
    val isSaving: Boolean            = false,
    val errorMessage: String?        = null,
    val successMessage: String?      = null,
    // Form fields
    val formName: String             = "",
    val formType: String             = AccountType.CASH.key,
    val formBalance: String          = "0",
    val formColor: String            = "#10B981",
    val formDescription: String      = ""
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val auth: FirebaseAuth
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState = _uiState.asStateFlow()

    private val userId get() = auth.currentUser?.uid ?: ""

    init {
        observeAccounts()
    }

    // ── Real-time observation ─────────────────────────────────────────────────

    private fun observeAccounts() {
        accountRepository.getAccountsFlow(userId)
            .onEach { accounts ->
                _uiState.value = _uiState.value.copy(
                    accounts     = accounts,
                    totalBalance = accounts.sumOf { it.balance },
                    isLoading    = false
                )
            }
            .catch { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading    = false,
                    errorMessage = e.message
                )
            }
            .launchIn(viewModelScope)
    }

    // ── Form field updates ────────────────────────────────────────────────────

    fun onFormNameChange(v: String)        { _uiState.value = _uiState.value.copy(formName = v) }
    fun onFormTypeChange(v: String)        {
        val defaultColor = AccountType.fromKey(v).color
        _uiState.value = _uiState.value.copy(formType = v, formColor = defaultColor)
    }
    fun onFormBalanceChange(v: String)     { _uiState.value = _uiState.value.copy(formBalance = v) }
    fun onFormColorChange(v: String)       { _uiState.value = _uiState.value.copy(formColor = v) }
    fun onFormDescriptionChange(v: String) { _uiState.value = _uiState.value.copy(formDescription = v) }

    // ── Sheet control ─────────────────────────────────────────────────────────

    fun showAddSheet() {
        _uiState.value = _uiState.value.copy(
            showAddSheet    = true,
            editingAccount  = null,
            formName        = "",
            formType        = AccountType.CASH.key,
            formBalance     = "0",
            formColor       = AccountType.CASH.color,
            formDescription = ""
        )
    }

    fun showEditSheet(account: Account) {
        _uiState.value = _uiState.value.copy(
            showEditSheet   = true,
            editingAccount  = account,
            formName        = account.name,
            formType        = account.type,
            formBalance     = account.balance.toLong().toString(),
            formColor       = account.color,
            formDescription = account.description
        )
    }

    fun dismissSheets() {
        _uiState.value = _uiState.value.copy(
            showAddSheet  = false,
            showEditSheet = false,
            errorMessage  = null
        )
    }

    fun showDeleteDialog(account: Account) {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = true,
            deletingAccount  = account
        )
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = false, deletingAccount = null)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    // ── CRUD operations ───────────────────────────────────────────────────────

    fun addAccount() {
        val s = _uiState.value
        if (s.formName.isBlank()) {
            _uiState.value = s.copy(errorMessage = "Account name is required")
            return
        }
        val balance = s.formBalance.toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val account = Account(
                name        = s.formName.trim(),
                type        = s.formType,
                balance     = balance,
                color       = s.formColor,
                description = s.formDescription.trim()
            )
            when (val result = accountRepository.addAccount(userId, account)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSaving       = false,
                    showAddSheet   = false,
                    successMessage = "Account added successfully"
                )
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isSaving     = false,
                    errorMessage = result.message
                )
                else -> Unit
            }
        }
    }

    fun updateAccount() {
        val s       = _uiState.value
        val editing = s.editingAccount ?: return
        if (s.formName.isBlank()) {
            _uiState.value = s.copy(errorMessage = "Account name is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val updated = editing.copy(
                name        = s.formName.trim(),
                type        = s.formType,
                color       = s.formColor,
                description = s.formDescription.trim()
            )
            when (val result = accountRepository.updateAccount(userId, editing.id, updated)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSaving       = false,
                    showEditSheet  = false,
                    successMessage = "Account updated"
                )
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isSaving     = false,
                    errorMessage = result.message
                )
                else -> Unit
            }
        }
    }

    fun deleteAccount() {
        val account = _uiState.value.deletingAccount ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            when (val result = accountRepository.deleteAccount(userId, account.id)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSaving         = false,
                    showDeleteDialog = false,
                    deletingAccount  = null,
                    successMessage   = "${account.name} deleted"
                )
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isSaving     = false,
                    errorMessage = result.message
                )
                else -> Unit
            }
        }
    }
}

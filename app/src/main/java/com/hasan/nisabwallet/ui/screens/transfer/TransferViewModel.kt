package com.hasan.nisabwallet.ui.screens.transfer

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.model.Account
import com.hasan.nisabwallet.data.model.Transfer
import com.hasan.nisabwallet.data.repository.AccountRepository
import com.hasan.nisabwallet.data.repository.TransferRepository
import com.hasan.nisabwallet.ui.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TransferUiState(
    val accounts: List<Account>      = emptyList(),
    val transfers: List<Transfer>    = emptyList(),
    val isLoading: Boolean           = true,
    val isSaving: Boolean            = false,
    val showDeleteDialog: Boolean    = false,
    val deletingTransfer: Transfer?  = null,
    val successMessage: String?      = null,
    val errorMessage: String?        = null,
    // Form
    val fromAccountId: String        = "",
    val toAccountId: String          = "",
    val amount: String               = "",
    val description: String          = "",
    val date: String                 = LocalDate.now().toString()
)

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val transferRepo: TransferRepository,
    private val accountRepo: AccountRepository,
    private val auth: FirebaseAuth
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState = _uiState.asStateFlow()

    private val userId get() = auth.currentUser?.uid ?: ""

    init {
        observeAccounts()
        observeTransfers()
    }

    private fun observeAccounts() {
        accountRepo.getAccountsFlow(userId)
            .onEach { accounts ->
                val s = _uiState.value
                _uiState.value = s.copy(
                    accounts      = accounts,
                    fromAccountId = if (s.fromAccountId.isEmpty() && accounts.isNotEmpty())
                        accounts[0].id else s.fromAccountId,
                    toAccountId   = if (s.toAccountId.isEmpty() && accounts.size > 1)
                        accounts[1].id else s.toAccountId,
                    isLoading     = false
                )
            }
            .launchIn(viewModelScope)
    }

    private fun observeTransfers() {
        transferRepo.getTransfersFlow(userId)
            .onEach { transfers -> _uiState.value = _uiState.value.copy(transfers = transfers) }
            .catch { }
            .launchIn(viewModelScope)
    }

    fun onFromAccountChange(id: String)   { _uiState.value = _uiState.value.copy(fromAccountId = id, errorMessage = null) }
    fun onToAccountChange(id: String)     { _uiState.value = _uiState.value.copy(toAccountId = id,   errorMessage = null) }
    fun onAmountChange(v: String)         { _uiState.value = _uiState.value.copy(amount = v,          errorMessage = null) }
    fun onDescriptionChange(v: String)    { _uiState.value = _uiState.value.copy(description = v) }
    fun onDateChange(v: String)           { _uiState.value = _uiState.value.copy(date = v) }
    fun clearMessages()                   { _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null) }
    fun showDeleteDialog(t: Transfer)     { _uiState.value = _uiState.value.copy(showDeleteDialog = true, deletingTransfer = t) }
    fun dismissDeleteDialog()             { _uiState.value = _uiState.value.copy(showDeleteDialog = false, deletingTransfer = null) }

    fun transfer() {
        val s = _uiState.value
        val amount = s.amount.toDoubleOrNull()

        when {
            amount == null || amount <= 0 -> { _uiState.value = s.copy(errorMessage = "Enter a valid amount"); return }
            s.fromAccountId.isEmpty()     -> { _uiState.value = s.copy(errorMessage = "Select source account"); return }
            s.toAccountId.isEmpty()       -> { _uiState.value = s.copy(errorMessage = "Select destination account"); return }
            s.fromAccountId == s.toAccountId -> { _uiState.value = s.copy(errorMessage = "Source and destination must differ"); return }
        }

        val fromAcc = s.accounts.find { it.id == s.fromAccountId } ?: return
        val toAcc   = s.accounts.find { it.id == s.toAccountId   } ?: return

        if (amount!! > fromAcc.balance) {
            _uiState.value = s.copy(errorMessage = "Insufficient balance in ${fromAcc.name}")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)

            val transfer = Transfer(
                fromAccountId   = fromAcc.id,
                fromAccountName = fromAcc.name,
                toAccountId     = toAcc.id,
                toAccountName   = toAcc.name,
                amount          = amount,
                description     = s.description.trim(),
                date            = s.date
            )

            when (val r = transferRepo.addTransfer(userId, transfer, fromAcc.balance, toAcc.balance)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSaving       = false,
                    amount         = "",
                    description    = "",
                    successMessage = "৳${"%,.0f".format(amount)} transferred from ${fromAcc.name} to ${toAcc.name}"
                )
                is Result.Error   -> _uiState.value = _uiState.value.copy(
                    isSaving     = false,
                    errorMessage = r.message
                )
                else -> Unit
            }
        }
    }

    fun deleteTransfer() {
        val t       = _uiState.value.deletingTransfer ?: return
        val fromAcc = _uiState.value.accounts.find { it.id == t.fromAccountId } ?: return
        val toAcc   = _uiState.value.accounts.find { it.id == t.toAccountId   } ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            when (val r = transferRepo.deleteTransfer(userId, t, fromAcc.balance, toAcc.balance)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSaving         = false,
                    showDeleteDialog = false,
                    deletingTransfer = null,
                    successMessage   = "Transfer deleted and balances reversed"
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

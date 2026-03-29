package com.hasan.nisabwallet.ui.screens.goals

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.model.Account
import com.hasan.nisabwallet.data.model.Goal
import com.hasan.nisabwallet.data.repository.AccountRepository
import com.hasan.nisabwallet.data.repository.GoalRepository
import com.hasan.nisabwallet.ui.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class GoalsUiState(
    val goals: List<Goal>             = emptyList(),
    val accounts: List<Account>       = emptyList(),
    val availableBalances: Map<String, Double> = emptyMap(),
    val filterStatus: String          = "active",
    val isLoading: Boolean            = true,
    val isSaving: Boolean             = false,
    val showFormSheet: Boolean        = false,
    val showDepositSheet: Boolean     = false,
    val showWithdrawSheet: Boolean    = false,
    val showDeleteDialog: Boolean     = false,
    val editingGoal: Goal?            = null,
    val selectedGoal: Goal?           = null,
    val deletingGoal: Goal?           = null,
    val errorMessage: String?         = null,
    val successMessage: String?       = null,
    val totalTargeted: Double         = 0.0,
    val totalSaved: Double            = 0.0,
    // Form
    val formGoalName: String          = "",
    val formCategory: String          = "other",
    val formTargetAmount: String      = "",
    val formCurrentAmount: String     = "0",
    val formTargetDate: String        = "",
    val formMonthly: String           = "",
    val formAccountId: String         = "",
    val formPriority: String          = "medium",
    val formDescription: String       = "",
    val formNotifications: Boolean    = true,
    // Transaction amount
    val transactionAmount: String     = ""
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepo: GoalRepository,
    private val accountRepo: AccountRepository,
    private val auth: FirebaseAuth
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState = _uiState.asStateFlow()

    private val userId get() = auth.currentUser?.uid ?: ""

    init {
        observeGoals()
        observeAccounts()
    }

    private fun observeGoals() {
        goalRepo.getGoalsFlow(userId)
            .onEach { goals ->
                val filtered = filterGoals(goals, _uiState.value.filterStatus)
                _uiState.value = _uiState.value.copy(
                    goals         = filtered,
                    totalTargeted = goals.filter { it.status == "active" }.sumOf { it.targetAmount },
                    totalSaved    = goals.filter { it.status == "active" }.sumOf { it.currentAmount },
                    isLoading     = false
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
                loadAvailableBalances(accounts)
            }
            .launchIn(viewModelScope)
    }

    private fun loadAvailableBalances(accounts: List<Account>) {
        viewModelScope.launch {
            val map = accounts.associate { acc ->
                val allocated = goalRepo.getTotalAllocated(userId, acc.id)
                acc.id to maxOf(0.0, acc.balance - allocated)
            }
            _uiState.value = _uiState.value.copy(availableBalances = map)
        }
    }

    fun setFilter(status: String) {
        _uiState.value = _uiState.value.copy(filterStatus = status)
        observeGoals()
    }

    private fun filterGoals(goals: List<Goal>, status: String) = when (status) {
        "active"    -> goals.filter { it.status == "active" }
        "completed" -> goals.filter { it.isCompleted }
        else        -> goals
    }

    fun showAddSheet() {
        _uiState.value = _uiState.value.copy(
            showFormSheet = true, editingGoal = null,
            formGoalName = "", formCategory = "other",
            formTargetAmount = "", formCurrentAmount = "0",
            formTargetDate = "", formMonthly = "",
            formAccountId = _uiState.value.accounts.firstOrNull()?.id ?: "",
            formPriority = "medium", formDescription = "", formNotifications = true, errorMessage = null
        )
    }

    fun showEditSheet(goal: Goal) {
        _uiState.value = _uiState.value.copy(
            showFormSheet    = true, editingGoal = goal,
            formGoalName     = goal.goalName, formCategory = goal.category,
            formTargetAmount = goal.targetAmount.toLong().toString(),
            formCurrentAmount = goal.currentAmount.toLong().toString(),
            formTargetDate   = goal.targetDate,
            formMonthly      = if (goal.monthlyContribution > 0) goal.monthlyContribution.toLong().toString() else "",
            formAccountId    = goal.linkedAccountId,
            formPriority     = goal.priority, formDescription = goal.description,
            formNotifications = goal.enableNotifications, errorMessage = null
        )
    }

    fun showDepositSheet(goal: Goal) {
        _uiState.value = _uiState.value.copy(showDepositSheet = true, selectedGoal = goal, transactionAmount = "", errorMessage = null)
    }

    fun showWithdrawSheet(goal: Goal) {
        _uiState.value = _uiState.value.copy(showWithdrawSheet = true, selectedGoal = goal, transactionAmount = "", errorMessage = null)
    }

    fun dismissForms() {
        _uiState.value = _uiState.value.copy(
            showFormSheet = false, showDepositSheet = false, showWithdrawSheet = false,
            editingGoal = null, selectedGoal = null, errorMessage = null
        )
    }

    fun showDeleteDialog(g: Goal) { _uiState.value = _uiState.value.copy(showDeleteDialog = true, deletingGoal = g) }
    fun dismissDeleteDialog()     { _uiState.value = _uiState.value.copy(showDeleteDialog = false, deletingGoal = null) }
    fun clearMessages()           { _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null) }

    fun onGoalNameChange(v: String)    { _uiState.value = _uiState.value.copy(formGoalName = v) }
    fun onCategoryChange(v: String)    { _uiState.value = _uiState.value.copy(formCategory = v) }
    fun onTargetAmountChange(v: String){ _uiState.value = _uiState.value.copy(formTargetAmount = v) }
    fun onTargetDateChange(v: String)  { _uiState.value = _uiState.value.copy(formTargetDate = v) }
    fun onMonthlyChange(v: String)     { _uiState.value = _uiState.value.copy(formMonthly = v) }
    fun onAccountChange(v: String)     { _uiState.value = _uiState.value.copy(formAccountId = v) }
    fun onPriorityChange(v: String)    { _uiState.value = _uiState.value.copy(formPriority = v) }
    fun onDescriptionChange(v: String) { _uiState.value = _uiState.value.copy(formDescription = v) }
    fun onNotificationsChange(v: Boolean){ _uiState.value = _uiState.value.copy(formNotifications = v) }
    fun onTransactionAmountChange(v: String){ _uiState.value = _uiState.value.copy(transactionAmount = v) }

    fun saveGoal() {
        val s = _uiState.value
        val target = s.formTargetAmount.toDoubleOrNull()
        when {
            s.formGoalName.isBlank()         -> { _uiState.value = s.copy(errorMessage = "Goal name required"); return }
            target == null || target <= 0    -> { _uiState.value = s.copy(errorMessage = "Enter valid target amount"); return }
            s.formAccountId.isEmpty()        -> { _uiState.value = s.copy(errorMessage = "Select a linked account"); return }
        }
        val account = s.accounts.find { it.id == s.formAccountId } ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            val goal = Goal(
                goalName            = s.formGoalName.trim(),
                category            = s.formCategory,
                targetAmount        = target!!,
                currentAmount       = s.formCurrentAmount.toDoubleOrNull() ?: 0.0,
                targetDate          = s.formTargetDate,
                monthlyContribution = s.formMonthly.toDoubleOrNull() ?: 0.0,
                linkedAccountId     = s.formAccountId,
                linkedAccountName   = account.name,
                priority            = s.formPriority,
                description         = s.formDescription.trim(),
                enableNotifications = s.formNotifications
            )
            val result = if (s.editingGoal == null)
                goalRepo.addGoal(userId, goal)
                    .let { if (it is Result.Success) Result.Success(Unit) else it as Result<Unit> }
            else
                goalRepo.updateGoal(userId, s.editingGoal.id, goal)

            when (result) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSaving = false, showFormSheet = false,
                    successMessage = if (s.editingGoal == null) "Goal created" else "Goal updated"
                )
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
                else -> Unit
            }
        }
    }

    fun deposit() {
        val s    = _uiState.value
        val goal = s.selectedGoal ?: return
        val amt  = s.transactionAmount.toDoubleOrNull()
        val avail = s.availableBalances[goal.linkedAccountId] ?: 0.0
        when {
            amt == null || amt <= 0 -> { _uiState.value = s.copy(errorMessage = "Enter valid amount"); return }
            amt > avail             -> { _uiState.value = s.copy(errorMessage = "Only ৳${"%,.0f".format(avail)} available in linked account"); return }
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            when (val r = goalRepo.deposit(userId, goal.id, amt!!, goal.currentAmount)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSaving = false, showDepositSheet = false,
                    successMessage = "৳${"%,.0f".format(amt)} added to ${goal.goalName}"
                )
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = r.message)
                else -> Unit
            }
        }
    }

    fun withdraw() {
        val s    = _uiState.value
        val goal = s.selectedGoal ?: return
        val amt  = s.transactionAmount.toDoubleOrNull()
        when {
            amt == null || amt <= 0    -> { _uiState.value = s.copy(errorMessage = "Enter valid amount"); return }
            amt > goal.currentAmount   -> { _uiState.value = s.copy(errorMessage = "Cannot withdraw more than ৳${"%,.0f".format(goal.currentAmount)}"); return }
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            when (val r = goalRepo.withdraw(userId, goal.id, amt!!, goal.currentAmount)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSaving = false, showWithdrawSheet = false,
                    successMessage = "৳${"%,.0f".format(amt)} withdrawn from ${goal.goalName}"
                )
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = r.message)
                else -> Unit
            }
        }
    }

    fun deleteGoal() {
        val goal = _uiState.value.deletingGoal ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            when (val r = goalRepo.deleteGoal(userId, goal.id)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSaving = false, showDeleteDialog = false, deletingGoal = null,
                    successMessage = "${goal.goalName} deleted"
                )
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = r.message)
                else -> Unit
            }
        }
    }
}

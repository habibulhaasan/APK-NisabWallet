package com.hasan.nisabwallet.ui.screens.budgets

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.model.Budget
import com.hasan.nisabwallet.data.model.Category
import com.hasan.nisabwallet.data.repository.BudgetRepository
import com.hasan.nisabwallet.data.repository.CategoryRepository
import com.hasan.nisabwallet.ui.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BudgetsUiState(
    val budgets: List<Budget>            = emptyList(),
    val categories: List<Category>       = emptyList(),
    val spentMap: Map<String, Double>    = emptyMap(),
    val isLoading: Boolean               = true,
    val isSaving: Boolean                = false,
    val isCopying: Boolean               = false,
    val showFormSheet: Boolean           = false,
    val showDeleteDialog: Boolean        = false,
    val showCopyDialog: Boolean          = false,
    val editingBudget: Budget?           = null,
    val deletingBudget: Budget?          = null,
    val selectedYear: Int                = LocalDate.now().year,
    val selectedMonth: Int               = LocalDate.now().monthValue,
    val errorMessage: String?            = null,
    val successMessage: String?          = null,
    // Form
    val formCategoryId: String           = "",
    val formAmount: String               = "",
    val formRollover: Boolean            = false,
    val formNotes: String                = "",
    // Totals
    val totalBudgeted: Double            = 0.0,
    val totalSpent: Double               = 0.0
)

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepo: BudgetRepository,
    private val categoryRepo: CategoryRepository,
    private val auth: FirebaseAuth
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(BudgetsUiState())
    val uiState = _uiState.asStateFlow()
    private val userId get() = auth.currentUser?.uid ?: ""

    // Track the active budget observation job so we can cancel it before starting a new one
    private var budgetObserverJob: Job? = null

    init {
        observeCategories()
        observeBudgets()
        loadSpent()
    }

    private fun observeCategories() {
        categoryRepo.getCategoriesFlow(userId)
            .onEach { cats ->
                _uiState.value = _uiState.value.copy(
                    categories = cats.filter { it.type == "Expense" }
                )
            }
            .launchIn(viewModelScope)
    }

    private fun observeBudgets() {
        // Cancel the previous collector before starting a new one
        budgetObserverJob?.cancel()

        val s = _uiState.value
        budgetObserverJob = budgetRepo.getBudgetsFlow(userId, s.selectedYear, s.selectedMonth)
            .onEach { budgets ->
                val spent    = _uiState.value.spentMap
                val enriched = budgets.map { b -> b.copy(spent = spent[b.categoryId] ?: 0.0) }
                _uiState.value = _uiState.value.copy(
                    budgets       = enriched,
                    totalBudgeted = enriched.sumOf { it.amount },
                    totalSpent    = enriched.sumOf { it.spent },
                    isLoading     = false
                )
            }
            .catch { _uiState.value = _uiState.value.copy(isLoading = false) }
            .launchIn(viewModelScope)
    }

    private fun loadSpent() {
        viewModelScope.launch {
            val s     = _uiState.value
            val spent = budgetRepo.getSpentPerCategory(userId, s.selectedYear, s.selectedMonth)
            val enriched = _uiState.value.budgets.map { b ->
                b.copy(spent = spent[b.categoryId] ?: 0.0)
            }
            _uiState.value = _uiState.value.copy(
                spentMap      = spent,
                budgets       = enriched,
                totalSpent    = enriched.sumOf { it.spent }
            )
        }
    }

    fun prevMonth() = changeMonth(-1)
    fun nextMonth() = changeMonth(1)

    private fun changeMonth(delta: Int) {
        val s = _uiState.value
        val d = LocalDate.of(s.selectedYear, s.selectedMonth, 1).plusMonths(delta.toLong())
        _uiState.value = s.copy(
            selectedYear  = d.year,
            selectedMonth = d.monthValue,
            isLoading     = true,
            budgets       = emptyList() // Clear stale data immediately
        )
        // observeBudgets() now safely cancels the previous Job before starting the new one
        observeBudgets()
        loadSpent()
    }

}


    // ── Month navigation ──────────────────────────────────────────────────────

    fun prevMonth() = changeMonth(-1)
    fun nextMonth() = changeMonth(1)

    private fun changeMonth(delta: Int) {
        val s   = _uiState.value
        val d   = LocalDate.of(s.selectedYear, s.selectedMonth, 1).plusMonths(delta.toLong())
        _uiState.value = s.copy(
            selectedYear  = d.year,
            selectedMonth = d.monthValue,
            isLoading     = true
        )
        observeBudgets()
        loadSpent()
    }

    // ── Sheet control ─────────────────────────────────────────────────────────

    fun showAddSheet() {
        _uiState.value = _uiState.value.copy(
            showFormSheet  = true,
            editingBudget  = null,
            formCategoryId = "",
            formAmount     = "",
            formRollover   = false,
            formNotes      = "",
            errorMessage   = null
        )
    }

    fun showEditSheet(budget: Budget) {
        _uiState.value = _uiState.value.copy(
            showFormSheet  = true,
            editingBudget  = budget,
            formCategoryId = budget.categoryId,
            formAmount     = budget.amount.toLong().toString(),
            formRollover   = budget.rollover,
            formNotes      = budget.notes,
            errorMessage   = null
        )
    }

    fun dismissSheet()       { _uiState.value = _uiState.value.copy(showFormSheet = false, editingBudget = null) }
    fun showDeleteDialog(b: Budget) { _uiState.value = _uiState.value.copy(showDeleteDialog = true, deletingBudget = b) }
    fun dismissDeleteDialog(){ _uiState.value = _uiState.value.copy(showDeleteDialog = false, deletingBudget = null) }
    fun showCopyDialog()     { _uiState.value = _uiState.value.copy(showCopyDialog = true) }
    fun dismissCopyDialog()  { _uiState.value = _uiState.value.copy(showCopyDialog = false) }
    fun clearMessages()      { _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null) }
    fun onCategoryChange(v: String) { _uiState.value = _uiState.value.copy(formCategoryId = v) }
    fun onAmountChange(v: String)   { _uiState.value = _uiState.value.copy(formAmount = v) }
    fun onRolloverChange(v: Boolean){ _uiState.value = _uiState.value.copy(formRollover = v) }
    fun onNotesChange(v: String)    { _uiState.value = _uiState.value.copy(formNotes = v) }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    fun saveBudget() {
        val s      = _uiState.value
        val amount = s.formAmount.toDoubleOrNull()
        when {
            s.formCategoryId.isEmpty() -> { _uiState.value = s.copy(errorMessage = "Select a category"); return }
            amount == null || amount <= 0 -> { _uiState.value = s.copy(errorMessage = "Enter a valid amount"); return }
        }
        val cat = s.categories.find { it.id == s.formCategoryId } ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            val budget = Budget(
                categoryId    = s.formCategoryId,
                categoryName  = cat.name,
                categoryColor = cat.color,
                amount        = amount!!,
                year          = s.selectedYear,
                month         = s.selectedMonth,
                rollover      = s.formRollover,
                notes         = s.formNotes.trim()
            )
            val result = if (s.editingBudget == null)
                budgetRepo.addBudget(userId, budget)
                    .let { if (it is Result.Success) Result.Success(Unit) else it as Result<Unit> }
            else
                budgetRepo.updateBudget(userId, s.editingBudget.id, budget)

            when (result) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSaving       = false,
                    showFormSheet  = false,
                    successMessage = if (s.editingBudget == null) "Budget added" else "Budget updated"
                )
                is Result.Error   -> _uiState.value = _uiState.value.copy(
                    isSaving     = false, errorMessage = result.message
                )
                else -> Unit
            }
        }
    }

    fun deleteBudget() {
        val b = _uiState.value.deletingBudget ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            when (val r = budgetRepo.deleteBudget(userId, b.id)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSaving = false, showDeleteDialog = false, deletingBudget = null,
                    successMessage = "${b.categoryName} budget deleted"
                )
                is Result.Error   -> _uiState.value = _uiState.value.copy(
                    isSaving = false, errorMessage = r.message
                )
                else -> Unit
            }
        }
    }

    fun copyToNextMonth() {
        val s     = _uiState.value
        val nextD = LocalDate.of(s.selectedYear, s.selectedMonth, 1).plusMonths(1)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCopying = true, showCopyDialog = false)
            when (val r = budgetRepo.copyBudgetsToNextMonth(
                userId, s.selectedYear, s.selectedMonth, nextD.year, nextD.monthValue
            )) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isCopying      = false,
                    successMessage = "${r.data} budgets copied to ${nextD.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${nextD.year}"
                )
                is Result.Error   -> _uiState.value = _uiState.value.copy(
                    isCopying = false, errorMessage = r.message
                )
                else -> Unit
            }
        }
    }
}

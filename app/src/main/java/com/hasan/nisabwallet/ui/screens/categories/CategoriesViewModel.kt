package com.hasan.nisabwallet.ui.screens.categories

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.model.Category
import com.hasan.nisabwallet.data.repository.CategoryRepository
import com.hasan.nisabwallet.ui.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val allCategories: List<Category>    = emptyList(),
    val incomeCategories: List<Category> = emptyList(),
    val expenseCategories: List<Category> = emptyList(),
    val isLoading: Boolean               = true,
    val isSaving: Boolean                = false,
    val showFormSheet: Boolean           = false,
    val showDeleteDialog: Boolean        = false,
    val editingCategory: Category?       = null,
    val deletingCategory: Category?      = null,
    val selectedTab: Int                 = 0,          // 0 = Income, 1 = Expense
    val errorMessage: String?            = null,
    val successMessage: String?          = null,
    // Form fields
    val formName: String                 = "",
    val formType: String                 = "Expense",
    val formColor: String                = "#6B7280"
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val auth: FirebaseAuth
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState = _uiState.asStateFlow()

    private val userId get() = auth.currentUser?.uid ?: ""

    init { observeCategories() }

    private fun observeCategories() {
        categoryRepository.getCategoriesFlow(userId)
            .onEach { cats ->
                _uiState.value = _uiState.value.copy(
                    allCategories     = cats,
                    incomeCategories  = cats.filter { it.type == "Income" },
                    expenseCategories = cats.filter { it.type == "Expense" },
                    isLoading         = false
                )
            }
            .catch { e -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message) }
            .launchIn(viewModelScope)
    }

    fun onTabSelected(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    // ── Form control ──────────────────────────────────────────────────────────

    fun showAddSheet(type: String = if (_uiState.value.selectedTab == 0) "Income" else "Expense") {
        _uiState.value = _uiState.value.copy(
            showFormSheet    = true,
            editingCategory  = null,
            formName         = "",
            formType         = type,
            formColor        = "#6B7280",
            errorMessage     = null
        )
    }

    fun showEditSheet(category: Category) {
        _uiState.value = _uiState.value.copy(
            showFormSheet    = true,
            editingCategory  = category,
            formName         = category.name,
            formType         = category.type,
            formColor        = category.color,
            errorMessage     = null
        )
    }

    fun dismissSheet() {
        _uiState.value = _uiState.value.copy(showFormSheet = false, editingCategory = null, errorMessage = null)
    }

    fun showDeleteDialog(category: Category) {
        _uiState.value = _uiState.value.copy(showDeleteDialog = true, deletingCategory = category)
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = false, deletingCategory = null)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun onNameChange(v: String)  { _uiState.value = _uiState.value.copy(formName = v,  errorMessage = null) }
    fun onColorChange(v: String) { _uiState.value = _uiState.value.copy(formColor = v) }
    fun onTypeChange(v: String)  { _uiState.value = _uiState.value.copy(formType = v)  }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    fun saveCategory() {
        val s = _uiState.value
        if (s.formName.isBlank()) {
            _uiState.value = s.copy(errorMessage = "Category name is required")
            return
        }
        // Check duplicate name within same type
        val duplicate = s.allCategories.any {
            it.name.equals(s.formName.trim(), ignoreCase = true) &&
            it.type == s.formType &&
            it.id != (s.editingCategory?.id ?: "")
        }
        if (duplicate) {
            _uiState.value = s.copy(errorMessage = "A ${s.formType} category named \"${s.formName.trim()}\" already exists")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)

            val result = if (s.editingCategory == null) {
                categoryRepository.addCategory(
                    userId,
                    Category(name = s.formName.trim(), type = s.formType, color = s.formColor)
                )
            } else {
                categoryRepository.updateCategory(
                    userId,
                    s.editingCategory.id,
                    s.editingCategory.copy(name = s.formName.trim(), color = s.formColor)
                )
            }

            when (result) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSaving       = false,
                    showFormSheet  = false,
                    successMessage = if (s.editingCategory == null) "Category added" else "Category updated"
                )
                is Result.Error   -> _uiState.value = _uiState.value.copy(
                    isSaving     = false,
                    errorMessage = result.message
                )
                else -> Unit
            }
        }
    }

    fun deleteCategory() {
        val cat = _uiState.value.deletingCategory ?: return
        if (cat.isSystem) {
            _uiState.value = _uiState.value.copy(
                showDeleteDialog = false,
                errorMessage     = "System categories cannot be deleted"
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            when (val r = categoryRepository.deleteCategory(userId, cat.id)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSaving         = false,
                    showDeleteDialog = false,
                    deletingCategory = null,
                    successMessage   = "${cat.name} deleted"
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

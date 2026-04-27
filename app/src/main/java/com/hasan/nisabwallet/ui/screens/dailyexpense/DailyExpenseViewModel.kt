// app/src/main/java/com/hasan/nisabwallet/ui/screens/dailyexpense/DailyExpenseViewModel.kt
package com.hasan.nisabwallet.ui.screens.dailyexpense

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hasan.nisabwallet.data.firebase.FirestorePaths
import com.hasan.nisabwallet.data.firebase.snapshotFlow
import com.hasan.nisabwallet.data.model.*
import com.hasan.nisabwallet.data.repository.AccountRepository
import com.hasan.nisabwallet.data.repository.CategoryRepository
import com.hasan.nisabwallet.data.safeCall
import com.hasan.nisabwallet.ui.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

enum class ExpenseViewMode { MONTHLY, YEARLY, HISTORY }

data class DailyExpenseUiState(
    // Data
    val tabs: List<ExpenseTrackerTab>              = emptyList(),
    val monthDataMap: Map<String, ExpenseTrackerMonthData> = emptyMap(),
    val yearDataMap: Map<String, ExpenseTrackerYearData>   = emptyMap(),
    val accounts: List<Account>                    = emptyList(),
    val categories: List<Category>                 = emptyList(),
    // Navigation
    val viewMode: ExpenseViewMode                  = ExpenseViewMode.MONTHLY,
    val currentYear: Int                           = LocalDate.now().year,
    val currentMonth: Int                          = LocalDate.now().monthValue, // 1-12
    val activeTabId: String                        = "",
    // UI
    val isLoading: Boolean                         = true,
    val isSaving: Boolean                          = false,
    val showTabSheet: Boolean                      = false,
    val showConfirmSheet: Boolean                  = false,
    val editingTab: ExpenseTrackerTab?             = null,
    val errorMessage: String?                      = null,
    val successMessage: String?                    = null,
    // Tab form fields
    val formTabTitle: String                       = "",
    val formTabCategoryId: String                  = "",
    val formTabColor: String                       = "emerald",
    val formTabDefaultAmount: String               = "0",
    val formTabUnit: String                        = "",
    // Confirm form
    val confirmAccountId: String                   = "",
    val confirmNote: String                        = ""
) {
    val currentYM: String get() = "%04d-%02d".format(currentYear, currentMonth)
    val activeTab: ExpenseTrackerTab? get() = tabs.find { it.id == activeTabId }
    val currentMonthData: ExpenseTrackerMonthData? get() = monthDataMap[currentYM]
    val currentYearData: ExpenseTrackerYearData? get() = yearDataMap[currentYear.toString()]
    val isCurrentPeriodConfirmed: Boolean get() = when (viewMode) {
        ExpenseViewMode.MONTHLY -> currentMonthData?.confirmedAt != null
        ExpenseViewMode.YEARLY  -> currentYearData?.confirmedAt != null
        else -> false
    }
    val grandTotal: Double get() {
        val dataDoc = when (viewMode) {
            ExpenseViewMode.MONTHLY -> currentMonthData
            ExpenseViewMode.YEARLY  -> currentYearData?.let {
                ExpenseTrackerMonthData(tabs = it.tabs)
            }
            else -> null
        }
        return tabs.sumOf { tab ->
            (dataDoc?.tabs?.get(tab.id) ?: emptyMap()).values.sumOf { it }
        }
    }

    // Get the day amounts for the active tab in monthly view
    fun getActiveTabMonthlyData(): Map<String, Double> =
        currentMonthData?.tabs?.get(activeTabId) ?: emptyMap()

    // Get the month amounts for the active tab in yearly view
    fun getActiveTabYearlyData(): Map<String, Double> {
        val yearTab = currentYearData?.tabs?.get(activeTabId) ?: emptyMap()
        return yearTab.mapKeys { it.key }
    }

    // Auto-sum monthly→yearly for a given tab+month
    fun getMonthlyAutoSum(tabId: String, year: Int, monthIdx: Int): Double? {
        val ym = "%04d-%02d".format(year, monthIdx + 1)
        val md = monthDataMap[ym]?.tabs?.get(tabId) ?: return null
        val sum = md.values.sumOf { it }
        return if (sum > 0) sum else null
    }
}

@HiltViewModel
class DailyExpenseViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val accountRepo: AccountRepository,
    private val categoryRepo: CategoryRepository,
    private val auth: FirebaseAuth
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(DailyExpenseUiState())
    val uiState = _uiState.asStateFlow()
    private val userId get() = auth.currentUser?.uid ?: ""

    // Debounce timer for cell saves
    private var saveDebounceJob: kotlinx.coroutines.Job? = null

    init {
        loadAll()
    }

    private fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            loadTabs()
            loadAllTrackerData()
            loadAccounts()
            loadCategories()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private suspend fun loadTabs() {
        val snap = FirestorePaths.expenseTrackerTabs(db, userId).get().await()
        val loaded = snap.documents
            .mapNotNull { it.toObject(ExpenseTrackerTab::class.java)?.copy(id = it.id) }
            .sortedBy { it.order }
        _uiState.value = _uiState.value.copy(
            tabs        = loaded,
            activeTabId = if (loaded.isNotEmpty() && _uiState.value.activeTabId.isEmpty())
                loaded.first().id else _uiState.value.activeTabId
        )
    }

    private suspend fun loadAllTrackerData() {
        val snap = FirestorePaths.expenseTrackerData(db, userId).get().await()
        val monthMap = mutableMapOf<String, ExpenseTrackerMonthData>()
        val yearMap  = mutableMapOf<String, ExpenseTrackerYearData>()
        snap.documents.forEach { doc ->
            when (doc.id.length) {
                7 -> {
                    // YYYY-MM
                    @Suppress("UNCHECKED_CAST")
                    val rawTabs = doc.get("tabs") as? Map<String, Map<String, Any>> ?: emptyMap()
                    val tabs = rawTabs.mapValues { (_, dayMap) ->
                        dayMap.mapValues { (_, v) -> (v as? Number)?.toDouble() ?: 0.0 }
                            .mapKeys { (k, _) -> k }
                    }
                    monthMap[doc.id] = ExpenseTrackerMonthData(
                        id       = doc.id,
                        month    = doc.id,
                        tabs     = tabs,
                        confirmedAt = doc.getDate("confirmedAt"),
                        totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                        accountId   = doc.getString("accountId") ?: "",
                        note        = doc.getString("note") ?: ""
                    )
                }
                4 -> {
                    // YYYY
                    @Suppress("UNCHECKED_CAST")
                    val rawTabs = doc.get("tabs") as? Map<String, Map<String, Any>> ?: emptyMap()
                    val tabs = rawTabs.mapValues { (_, mMap) ->
                        mMap.mapValues { (_, v) -> (v as? Number)?.toDouble() ?: 0.0 }
                            .mapKeys { (k, _) -> k }
                    }
                    yearMap[doc.id] = ExpenseTrackerYearData(
                        id   = doc.id,
                        year = doc.id,
                        tabs = tabs,
                        confirmedAt = doc.getDate("confirmedAt"),
                        totalAmount = doc.getDouble("totalAmount") ?: 0.0
                    )
                }
            }
        }
        _uiState.value = _uiState.value.copy(
            monthDataMap = monthMap,
            yearDataMap  = yearMap
        )
    }

    private suspend fun loadAccounts() {
        accountRepo.getAccountsFlow(userId).first().also { accounts ->
            _uiState.value = _uiState.value.copy(
                accounts = accounts.filter { it.balance > 0 }
            )
        }
    }

    private suspend fun loadCategories() {
        categoryRepo.getCategoriesFlow(userId).first().also { cats ->
            _uiState.value = _uiState.value.copy(
                categories = cats.filter { it.type == "Expense" }
            )
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    fun setViewMode(mode: ExpenseViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
    }

    fun setActiveTab(tabId: String) {
        _uiState.value = _uiState.value.copy(activeTabId = tabId)
    }

    fun prevPeriod() {
        val s = _uiState.value
        if (s.viewMode == ExpenseViewMode.MONTHLY) {
            val d = LocalDate.of(s.currentYear, s.currentMonth, 1).minusMonths(1)
            _uiState.value = s.copy(currentYear = d.year, currentMonth = d.monthValue)
        } else {
            _uiState.value = s.copy(currentYear = s.currentYear - 1)
        }
    }

    fun nextPeriod() {
        val s = _uiState.value
        if (s.viewMode == ExpenseViewMode.MONTHLY) {
            val d = LocalDate.of(s.currentYear, s.currentMonth, 1).plusMonths(1)
            _uiState.value = s.copy(currentYear = d.year, currentMonth = d.monthValue)
        } else {
            _uiState.value = s.copy(currentYear = s.currentYear + 1)
        }
    }

    // ── Cell editing ──────────────────────────────────────────────────────────

    fun onMonthCellChange(tabId: String, day: Int, value: String) {
        val s = _uiState.value
        val curData = s.monthDataMap[s.currentYM] ?: ExpenseTrackerMonthData(month = s.currentYM)
        val tabData = (curData.tabs[tabId] ?: emptyMap()).toMutableMap()
        val dayKey  = day.toString()
        if (value.isBlank() || value.toDoubleOrNull() == null) tabData.remove(dayKey)
        else tabData[dayKey] = value.toDouble()

        val newTabs = curData.tabs.toMutableMap().apply { put(tabId, tabData) }
        val newData = curData.copy(tabs = newTabs)
        val newMap  = s.monthDataMap.toMutableMap().apply { put(s.currentYM, newData) }
        _uiState.value = s.copy(monthDataMap = newMap)

        // Debounced Firestore save
        saveDebounceJob?.cancel()
        saveDebounceJob = viewModelScope.launch {
            kotlinx.coroutines.delay(800)
            saveMonthDataToFirestore(newData)
        }
    }

    fun onYearCellChange(tabId: String, monthIdx: Int, value: String) {
        val s = _uiState.value
        val yearKey = s.currentYear.toString()
        val curData = s.yearDataMap[yearKey] ?: ExpenseTrackerYearData(year = yearKey)
        val tabData = (curData.tabs[tabId] ?: emptyMap()).toMutableMap()
        val key = monthIdx.toString()
        if (value.isBlank()) tabData.remove(key) else tabData[key] = value.toDoubleOrNull() ?: 0.0
        val newTabs = curData.tabs.toMutableMap().apply { put(tabId, tabData) }
        val newData = curData.copy(tabs = newTabs)
        val newMap  = s.yearDataMap.toMutableMap().apply { put(yearKey, newData) }
        _uiState.value = s.copy(yearDataMap = newMap)

        saveDebounceJob?.cancel()
        saveDebounceJob = viewModelScope.launch {
            kotlinx.coroutines.delay(800)
            saveYearDataToFirestore(newData)
        }
    }

    private suspend fun saveMonthDataToFirestore(data: ExpenseTrackerMonthData) {
        val docId = data.month
        FirestorePaths.expenseTrackerData(db, userId).document(docId).set(
            mapOf("month" to docId, "tabs" to data.tabs, "updatedAt" to FieldValue.serverTimestamp()),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    private suspend fun saveYearDataToFirestore(data: ExpenseTrackerYearData) {
        val docId = data.year
        FirestorePaths.expenseTrackerData(db, userId).document(docId).set(
            mapOf("year" to docId, "tabs" to data.tabs, "updatedAt" to FieldValue.serverTimestamp()),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    // ── Tab CRUD ──────────────────────────────────────────────────────────────

    fun showAddTabSheet() {
        _uiState.value = _uiState.value.copy(
            showTabSheet      = true, editingTab = null,
            formTabTitle      = "", formTabCategoryId = "",
            formTabColor      = "emerald", formTabDefaultAmount = "0",
            formTabUnit       = "", errorMessage = null
        )
    }

    fun showEditTabSheet(tab: ExpenseTrackerTab) {
        _uiState.value = _uiState.value.copy(
            showTabSheet         = true, editingTab = tab,
            formTabTitle         = tab.title,
            formTabCategoryId    = tab.categoryId,
            formTabColor         = tab.color,
            formTabDefaultAmount = tab.defaultAmount.toString(),
            formTabUnit          = tab.unit, errorMessage = null
        )
    }

    fun dismissTabSheet() {
        _uiState.value = _uiState.value.copy(showTabSheet = false, editingTab = null, errorMessage = null)
    }

    fun onTabTitleChange(v: String)       { _uiState.value = _uiState.value.copy(formTabTitle = v) }
    fun onTabCategoryChange(v: String)    { _uiState.value = _uiState.value.copy(formTabCategoryId = v) }
    fun onTabColorChange(v: String)       { _uiState.value = _uiState.value.copy(formTabColor = v) }
    fun onTabDefaultAmountChange(v: String) { _uiState.value = _uiState.value.copy(formTabDefaultAmount = v) }
    fun onTabUnitChange(v: String)        { _uiState.value = _uiState.value.copy(formTabUnit = v) }

    fun saveTab() {
        val s = _uiState.value
        if (s.formTabTitle.isBlank()) { _uiState.value = s.copy(errorMessage = "Title is required"); return }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            val data = mapOf(
                "title"         to s.formTabTitle.trim(),
                "categoryId"    to s.formTabCategoryId,
                "color"         to s.formTabColor,
                "defaultAmount" to (s.formTabDefaultAmount.toDoubleOrNull() ?: 0.0),
                "unit"          to s.formTabUnit.trim(),
                "order"         to (s.editingTab?.order ?: s.tabs.size)
            )
            if (s.editingTab != null) {
                FirestorePaths.expenseTrackerTabs(db, userId).document(s.editingTab.id).set(data,
                    com.google.firebase.firestore.SetOptions.merge()).await()
            } else {
                FirestorePaths.expenseTrackerTabs(db, userId).add(data).await()
            }
            loadTabs()
            _uiState.value = _uiState.value.copy(isSaving = false, showTabSheet = false,
                successMessage = if (s.editingTab != null) "Tab updated" else "Tab added")
        }
    }

    fun deleteTab(tabId: String) {
        viewModelScope.launch {
            FirestorePaths.expenseTrackerTabs(db, userId).document(tabId).delete().await()
            loadTabs()
            _uiState.value = _uiState.value.copy(successMessage = "Tab deleted")
        }
    }

    // ── Confirm & Record ──────────────────────────────────────────────────────

    fun showConfirmSheet()   { _uiState.value = _uiState.value.copy(showConfirmSheet = true, confirmAccountId = "", confirmNote = "", errorMessage = null) }
    fun dismissConfirmSheet(){ _uiState.value = _uiState.value.copy(showConfirmSheet = false) }
    fun onConfirmAccountChange(v: String) { _uiState.value = _uiState.value.copy(confirmAccountId = v) }
    fun onConfirmNoteChange(v: String)    { _uiState.value = _uiState.value.copy(confirmNote = v) }
    fun clearMessages() { _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null) }

    fun recordExpense() {
        val s = _uiState.value
        if (s.confirmAccountId.isEmpty()) { _uiState.value = s.copy(errorMessage = "Select an account"); return }
        val acc = s.accounts.find { it.id == s.confirmAccountId } ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val isMonthly = s.viewMode == ExpenseViewMode.MONTHLY
            val dataDoc   = if (isMonthly) s.currentMonthData else s.currentYearData?.let {
                ExpenseTrackerMonthData(tabs = it.tabs)
            }
            val label = if (isMonthly) {
                val months = listOf("January","February","March","April","May","June",
                    "July","August","September","October","November","December")
                "${months[s.currentMonth - 1]} ${s.currentYear}"
            } else s.currentYear.toString()

            val txIds = mutableListOf<String>()
            var grandTotal = 0.0

            for (tab in s.tabs) {
                val tabData  = dataDoc?.tabs?.get(tab.id) ?: emptyMap()
                val tabTotal = tabData.values.sumOf { it }
                if (tabTotal <= 0) continue
                grandTotal += tabTotal

                val txnRef = FirestorePaths.transactions(db, userId).document()
                txnRef.set(mapOf(
                    "transactionId"  to UUID.randomUUID().toString(),
                    "type"           to "Expense",
                    "amount"         to tabTotal,
                    "accountId"      to s.confirmAccountId,
                    "accountName"    to acc.name,
                    "categoryId"     to tab.categoryId,
                    "categoryName"   to (s.categories.find { it.id == tab.categoryId }?.name ?: "Expense"),
                    "description"    to "${if (s.confirmNote.isNotBlank()) "${s.confirmNote} — " else ""}${tab.title} — $label",
                    "date"           to LocalDate.now().toString(),
                    "isRiba"         to false,
                    "isDailyTracker" to true,
                    "trackerPeriod"  to (if (isMonthly) s.currentYM else s.currentYear.toString()),
                    "createdAt"      to FieldValue.serverTimestamp()
                )).await()
                txIds.add(txnRef.id)
            }

            if (grandTotal <= 0) {
                _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = "No amounts to record")
                return@launch
            }

            // Deduct from account
            FirestorePaths.accounts(db, userId).document(s.confirmAccountId)
                .update("balance", acc.balance - grandTotal, "updatedAt", FieldValue.serverTimestamp()).await()

            // Mark confirmed
            val periodKey = if (isMonthly) s.currentYM else s.currentYear.toString()
            FirestorePaths.expenseTrackerData(db, userId).document(periodKey).set(
                mapOf("confirmedAt" to FieldValue.serverTimestamp(),
                    "transactionIds" to txIds, "totalAmount" to grandTotal,
                    "accountId" to s.confirmAccountId, "note" to s.confirmNote),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()

            loadAllTrackerData()
            _uiState.value = _uiState.value.copy(isSaving = false, showConfirmSheet = false,
                successMessage = "৳${"%,.0f".format(grandTotal)} recorded across ${txIds.size} transaction(s)!")
        }
    }
}
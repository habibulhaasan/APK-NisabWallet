// app/src/main/java/com/hasan/nisabwallet/ui/screens/grocery/GroceryViewModel.kt
package com.hasan.nisabwallet.ui.screens.grocery

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hasan.nisabwallet.data.firebase.FirestorePaths
import com.hasan.nisabwallet.data.model.*
import com.hasan.nisabwallet.data.repository.AccountRepository
import com.hasan.nisabwallet.data.repository.CategoryRepository
import com.hasan.nisabwallet.ui.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

// A row shown in the planner table
data class GroceryRow(
    val itemId: String,
    val name: String,
    val unit: String,
    val category: String,
    val archived: Boolean,
    // Current month
    val curQty: Double,
    val curUnitPrice: Double,
    val curBought: Boolean,
    val curBoughtPrice: Double?,
    val curRecorded: Boolean,
    // Previous month reference
    val prevQty: Double?,
    val prevUnitPrice: Double?
) {
    val curTotal: Double get() = curQty * curUnitPrice
    val prevTotal: Double? get() = if (prevQty != null) prevQty * (prevUnitPrice ?: 0.0) else null
    val effectivePrice: Double get() = if (curBought) (curBoughtPrice ?: curTotal) else curTotal
}

data class GroceryUiState(
    val masterItems: List<GroceryItem>   = emptyList(),
    val monthDataMap: Map<String, GroceryMonthData> = emptyMap(),
    val accounts: List<Account>          = emptyList(),
    val categories: List<Category>       = emptyList(),
    val rows: List<GroceryRow>           = emptyList(),
    val currentYear: Int                 = LocalDate.now().year,
    val currentMonth: Int                = LocalDate.now().monthValue,
    val activeTab: String                = "planner",  // planner | history
    val isLoading: Boolean               = true,
    val isSaving: Boolean                = false,
    val showAddItemSheet: Boolean        = false,
    val showConfirmSheet: Boolean        = false,
    val editingItem: GroceryItem?        = null,
    val searchQuery: String              = "",
    val filterBought: String             = "all",       // all | bought | pending
    val filterCategory: String           = "all",
    val showArchived: Boolean            = false,
    val errorMessage: String?            = null,
    val successMessage: String?          = null,
    // Add item form
    val formName: String                 = "",
    val formUnit: String                 = "pcs",
    val formDefaultQty: String           = "1",
    val formDefaultUnitPrice: String     = "0",
    val formCategory: String             = "",
    // Confirm form
    val confirmAccountId: String         = "",
    val confirmNote: String              = "",
    val confirmCategoryId: String        = ""
) {
    val currentYM: String get() = "%04d-%02d".format(currentYear, currentMonth)
    val prevYM: String get() {
        val d = LocalDate.of(currentYear, currentMonth, 1).minusMonths(1)
        return "%04d-%02d".format(d.year, d.monthValue)
    }
    val currentMonthData: GroceryMonthData? get() = monthDataMap[currentYM]
    val isConfirmed: Boolean get() = currentMonthData?.confirmedAt != null

    val boughtRows: List<GroceryRow>       get() = rows.filter { it.curBought }
    val recordableBought: List<GroceryRow> get() = boughtRows.filter { !it.curRecorded }
    val totalBought: Double get() = recordableBought.sumOf { it.effectivePrice }
    val pendingRows: List<GroceryRow>      get() = rows.filter { !it.curBought && it.curQty > 0 && it.curUnitPrice > 0 }
    val totalPending: Double               get() = pendingRows.sumOf { it.curTotal }

    val filteredRows: List<GroceryRow> get() = rows.filter { row ->
        (!row.archived || showArchived) &&
        (searchQuery.isEmpty() || row.name.contains(searchQuery, ignoreCase = true)) &&
        (filterBought == "all" || (filterBought == "bought" && row.curBought) || (filterBought == "pending" && !row.curBought)) &&
        (filterCategory == "all" || row.category == filterCategory)
    }

    val archivedCount: Int get() = rows.count { it.archived }
}

@HiltViewModel
class GroceryViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val accountRepo: AccountRepository,
    private val categoryRepo: CategoryRepository,
    private val auth: FirebaseAuth
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(GroceryUiState())
    val uiState = _uiState.asStateFlow()
    private val userId get() = auth.currentUser?.uid ?: ""
    private var saveDebounceJob: kotlinx.coroutines.Job? = null

    init { loadAll() }

    private fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            loadItems()
            loadMonthData()
            loadAccounts()
            loadCategories()
            buildRows()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private suspend fun loadItems() {
        val snap = FirestorePaths.groceryItems(db, userId)
            .orderBy("name").get().await()
        val items = snap.documents.mapNotNull { it.toObject(GroceryItem::class.java)?.copy(id = it.id) }
        _uiState.value = _uiState.value.copy(masterItems = items)
    }

    private suspend fun loadMonthData() {
        val snap = FirestorePaths.groceryMonths(db, userId).get().await()
        val map  = mutableMapOf<String, GroceryMonthData>()
        snap.documents.forEach { doc ->
            @Suppress("UNCHECKED_CAST")
            val rawItems = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
            val entries = rawItems.map { m ->
                GroceryMonthEntry(
                    itemId      = m["itemId"] as? String ?: "",
                    qty         = (m["qty"] as? Number)?.toDouble() ?: 1.0,
                    unitPrice   = (m["unitPrice"] as? Number)?.toDouble() ?: 0.0,
                    bought      = m["bought"] as? Boolean ?: false,
                    boughtPrice = (m["boughtPrice"] as? Number)?.toDouble()
                )
            }
            @Suppress("UNCHECKED_CAST")
            val recordedIds = doc.get("recordedItemIds") as? List<String> ?: emptyList()
            map[doc.id] = GroceryMonthData(
                id              = doc.id,
                month           = doc.id,
                items           = entries,
                confirmedAt     = doc.getDate("confirmedAt"),
                totalAmount     = doc.getDouble("totalAmount") ?: 0.0,
                accountId       = doc.getString("accountId") ?: "",
                recordedItemIds = recordedIds
            )
        }
        _uiState.value = _uiState.value.copy(monthDataMap = map)
    }

    private suspend fun loadAccounts() {
        val accounts = accountRepo.getAccountsFlow(userId).first()
        _uiState.value = _uiState.value.copy(accounts = accounts.filter { it.balance > 0 })
    }

    private suspend fun loadCategories() {
        val cats = categoryRepo.getCategoriesFlow(userId).first()
        _uiState.value = _uiState.value.copy(categories = cats.filter { it.type == "Expense" })
    }

    private fun buildRows() {
        val s = _uiState.value
        val curMonthItems  = s.currentMonthData?.items ?: emptyList()
        val prevMonthItems = s.monthDataMap[s.prevYM]?.items ?: emptyList()
        val recordedIds    = s.currentMonthData?.recordedItemIds ?: emptyList()

        val rows = s.masterItems.map { item ->
            val cur  = curMonthItems.find  { it.itemId == item.id }
            val prev = prevMonthItems.find { it.itemId == item.id }
            GroceryRow(
                itemId        = item.id,
                name          = item.name,
                unit          = item.unit,
                category      = item.category,
                archived      = item.archived,
                curQty        = cur?.qty        ?: item.defaultQty,
                curUnitPrice  = cur?.unitPrice  ?: item.defaultUnitPrice,
                curBought     = cur?.bought     ?: false,
                curBoughtPrice = cur?.boughtPrice,
                curRecorded   = recordedIds.contains(item.id),
                prevQty       = prev?.qty,
                prevUnitPrice = prev?.unitPrice
            )
        }
        _uiState.value = s.copy(rows = rows)
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    fun prevMonth() {
        val d = LocalDate.of(_uiState.value.currentYear, _uiState.value.currentMonth, 1).minusMonths(1)
        _uiState.value = _uiState.value.copy(currentYear = d.year, currentMonth = d.monthValue)
        buildRows()
    }

    fun nextMonth() {
        val d = LocalDate.of(_uiState.value.currentYear, _uiState.value.currentMonth, 1).plusMonths(1)
        _uiState.value = _uiState.value.copy(currentYear = d.year, currentMonth = d.monthValue)
        buildRows()
    }

    fun setActiveTab(tab: String) { _uiState.value = _uiState.value.copy(activeTab = tab) }
    fun setSearch(q: String)      { _uiState.value = _uiState.value.copy(searchQuery = q) }
    fun setFilterBought(f: String){ _uiState.value = _uiState.value.copy(filterBought = f) }
    fun setFilterCategory(f: String) { _uiState.value = _uiState.value.copy(filterCategory = f) }
    fun toggleShowArchived()      { _uiState.value = _uiState.value.copy(showArchived = !_uiState.value.showArchived) }
    fun clearMessages()           { _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null) }

    // ── Cell editing ──────────────────────────────────────────────────────────

    private fun updateRowAndScheduleSave(itemId: String, transform: (GroceryRow) -> GroceryRow) {
        _uiState.value = _uiState.value.copy(
            rows = _uiState.value.rows.map { if (it.itemId == itemId) transform(it) else it }
        )
        saveDebounceJob?.cancel()
        saveDebounceJob = viewModelScope.launch {
            kotlinx.coroutines.delay(800)
            persistRows()
        }
    }

    fun onQtyChange(itemId: String, value: String) {
        updateRowAndScheduleSave(itemId) { it.copy(curQty = value.toDoubleOrNull() ?: it.curQty) }
    }

    fun onUnitPriceChange(itemId: String, value: String) {
        updateRowAndScheduleSave(itemId) { it.copy(curUnitPrice = value.toDoubleOrNull() ?: it.curUnitPrice) }
    }

    fun onBoughtPriceChange(itemId: String, value: String) {
        updateRowAndScheduleSave(itemId) { it.copy(curBoughtPrice = value.toDoubleOrNull()) }
    }

    fun toggleBought(itemId: String) {
        val row = _uiState.value.rows.find { it.itemId == itemId } ?: return
        val checked = !row.curBought
        updateRowAndScheduleSave(itemId) {
            it.copy(curBought = checked,
                curBoughtPrice = if (checked) (it.curBoughtPrice ?: it.curTotal) else null)
        }
    }

    private suspend fun persistRows() {
        val s     = _uiState.value
        val items = s.rows.map { r ->
            mapOf("itemId" to r.itemId, "qty" to r.curQty, "unitPrice" to r.curUnitPrice,
                "bought" to r.curBought, "boughtPrice" to r.curBoughtPrice)
        }
        FirestorePaths.groceryMonths(db, userId).document(s.currentYM).set(
            mapOf("month" to s.currentYM, "items" to items, "updatedAt" to FieldValue.serverTimestamp()),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()

        // Update local state so recordedItemIds are preserved
        val existing = s.monthDataMap[s.currentYM]
        val entries  = s.rows.map { r ->
            GroceryMonthEntry(r.itemId, r.curQty, r.curUnitPrice, r.curBought, r.curBoughtPrice)
        }
        val updated = (existing ?: GroceryMonthData(id = s.currentYM, month = s.currentYM)).copy(items = entries)
        _uiState.value = _uiState.value.copy(
            monthDataMap = _uiState.value.monthDataMap.toMutableMap().apply { put(s.currentYM, updated) }
        )
    }

    // ── Master item CRUD ──────────────────────────────────────────────────────

    fun showAddItemSheet() {
        _uiState.value = _uiState.value.copy(
            showAddItemSheet = true, editingItem = null,
            formName = "", formUnit = "pcs", formDefaultQty = "1",
            formDefaultUnitPrice = "0", formCategory = "", errorMessage = null
        )
    }

    fun showEditItemSheet(item: GroceryItem) {
        _uiState.value = _uiState.value.copy(
            showAddItemSheet  = true, editingItem = item,
            formName          = item.name, formUnit = item.unit,
            formDefaultQty    = item.defaultQty.toString(),
            formDefaultUnitPrice = item.defaultUnitPrice.toString(),
            formCategory      = item.category, errorMessage = null
        )
    }

    fun dismissItemSheet() {
        _uiState.value = _uiState.value.copy(showAddItemSheet = false, editingItem = null, errorMessage = null)
    }

    fun onFormNameChange(v: String)         { _uiState.value = _uiState.value.copy(formName = v) }
    fun onFormUnitChange(v: String)         { _uiState.value = _uiState.value.copy(formUnit = v) }
    fun onFormDefaultQtyChange(v: String)   { _uiState.value = _uiState.value.copy(formDefaultQty = v) }
    fun onFormDefaultPriceChange(v: String) { _uiState.value = _uiState.value.copy(formDefaultUnitPrice = v) }
    fun onFormCategoryChange(v: String)     { _uiState.value = _uiState.value.copy(formCategory = v) }
    fun onConfirmAccountChange(v: String)   { _uiState.value = _uiState.value.copy(confirmAccountId = v) }
    fun onConfirmNoteChange(v: String)      { _uiState.value = _uiState.value.copy(confirmNote = v) }
    fun onConfirmCategoryChange(v: String)  { _uiState.value = _uiState.value.copy(confirmCategoryId = v) }

    fun saveItem() {
        val s = _uiState.value
        if (s.formName.isBlank()) { _uiState.value = s.copy(errorMessage = "Item name is required"); return }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            val data = mapOf(
                "name"             to s.formName.trim(),
                "unit"             to s.formUnit,
                "defaultQty"       to (s.formDefaultQty.toDoubleOrNull() ?: 1.0),
                "defaultUnitPrice" to (s.formDefaultUnitPrice.toDoubleOrNull() ?: 0.0),
                "category"         to s.formCategory
            )
            if (s.editingItem != null) {
                FirestorePaths.groceryItems(db, userId).document(s.editingItem.id).update(data).await()
            } else {
                FirestorePaths.groceryItems(db, userId).add(data + mapOf("createdAt" to FieldValue.serverTimestamp())).await()
            }
            loadItems()
            buildRows()
            _uiState.value = _uiState.value.copy(isSaving = false, showAddItemSheet = false,
                successMessage = if (s.editingItem != null) "Item updated" else "Item added!")
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            FirestorePaths.groceryItems(db, userId).document(itemId).delete().await()
            loadItems(); buildRows()
            _uiState.value = _uiState.value.copy(successMessage = "Item deleted")
        }
    }

    fun archiveItem(itemId: String, archive: Boolean) {
        viewModelScope.launch {
            FirestorePaths.groceryItems(db, userId).document(itemId)
                .update("archived", archive).await()
            loadItems(); buildRows()
        }
    }

    // ── Confirm & record ──────────────────────────────────────────────────────

    fun showConfirmSheet() { _uiState.value = _uiState.value.copy(showConfirmSheet = true, errorMessage = null) }
    fun dismissConfirmSheet() { _uiState.value = _uiState.value.copy(showConfirmSheet = false) }

    fun recordPurchase() {
        val s       = _uiState.value
        if (s.confirmAccountId.isEmpty()) { _uiState.value = s.copy(errorMessage = "Select an account"); return }
        val toRecord = s.recordableBought.filter { it.effectivePrice > 0 }
        if (toRecord.isEmpty()) { _uiState.value = s.copy(errorMessage = "No bought items to record"); return }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val acc       = s.accounts.find { it.id == s.confirmAccountId }!!
            val total     = toRecord.sumOf { it.effectivePrice }
            val today     = LocalDate.now().toString()
            val monthLabel = "%s %d".format(listOf("January","February","March","April","May","June",
                "July","August","September","October","November","December")[s.currentMonth - 1], s.currentYear)
            val notePrefix = if (s.confirmNote.isNotBlank()) "${s.confirmNote} — " else ""
            val txIds      = mutableListOf<String>()

            // Group by category — one transaction per category
            val byCategory = toRecord.groupBy { it.category.ifEmpty { "__none__" } }
            for ((catId, catItems) in byCategory) {
                val catTotal = catItems.sumOf { it.effectivePrice }
                val txCatId  = if (catId == "__none__") s.confirmCategoryId else catId
                val catName  = if (catId == "__none__") "Groceries" else
                    (s.categories.find { it.id == catId }?.name ?: catId)
                val itemList = catItems.joinToString(", ") { "${it.name} ×${it.curQty.toInt()}" }
                val txnRef   = FirestorePaths.transactions(db, userId).document()
                txnRef.set(mapOf(
                    "transactionId" to UUID.randomUUID().toString(),
                    "type"          to "Expense",
                    "amount"        to catTotal,
                    "accountId"     to s.confirmAccountId,
                    "accountName"   to acc.name,
                    "categoryId"    to txCatId,
                    "categoryName"  to catName,
                    "description"   to "${notePrefix}Groceries $monthLabel [$catName]: $itemList",
                    "date"          to today,
                    "isRiba"        to false,
                    "isGrocery"     to true,
                    "groceryMonth"  to s.currentYM,
                    "createdAt"     to FieldValue.serverTimestamp()
                )).await()
                txIds.add(txnRef.id)
            }

            // Deduct from account
            FirestorePaths.accounts(db, userId).document(s.confirmAccountId)
                .update("balance", acc.balance - total, "updatedAt", FieldValue.serverTimestamp()).await()

            // Update recorded item ids
            val prevRecorded = s.currentMonthData?.recordedItemIds ?: emptyList()
            val newRecorded  = (prevRecorded + toRecord.map { it.itemId }).distinct()

            FirestorePaths.groceryMonths(db, userId).document(s.currentYM).set(
                mapOf("confirmedAt" to FieldValue.serverTimestamp(),
                    "transactionIds" to txIds, "totalAmount" to total,
                    "accountId" to s.confirmAccountId, "recordedItemIds" to newRecorded),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()

            loadMonthData(); loadItems(); buildRows()
            _uiState.value = _uiState.value.copy(isSaving = false, showConfirmSheet = false,
                successMessage = "৳${"%,.0f".format(total)} recorded across ${txIds.size} transaction(s)!")
        }
    }
}
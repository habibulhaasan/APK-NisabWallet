package com.hasan.nisabwallet.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hasan.nisabwallet.data.firebase.FirestorePaths
import com.hasan.nisabwallet.data.firebase.snapshotFlow
import com.hasan.nisabwallet.data.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

// --- Data Models for UI State ---

data class CategoryStat(val name: String, val color: String, val amount: Double, val percent: Float)
data class MonthlyBar(val label: String, val income: Double, val expense: Double)

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val selectedRange: String = "month",
    val selectedTab: Int = 0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netFlow: Double = 0.0,
    val topExpenseCategories: List<CategoryStat> = emptyList(),
    val topIncomeCategories: List<CategoryStat> = emptyList(),
    val monthlyBars: List<MonthlyBar> = emptyList()
)

// --- ViewModel Implementation ---

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState = _uiState.asStateFlow()

    private val userId get() = auth.currentUser?.uid ?: ""

    // Internal flows to trigger recalculations
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    private val _selectedRange = MutableStateFlow("month")

    init {
        // 1. Observe Firestore Transactions
        FirestorePaths.transactions(db, userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(500) // Optimization: Only fetch recent 500 for analytics
            .snapshotFlow()
            .map { snap ->
                // Map documents to Transaction objects on a background thread
                withContext(Dispatchers.Default) {
                    snap.documents.mapNotNull { it.toObject(Transaction::class.java)?.copy(id = it.id) }
                }
            }
            .onEach { allTransactions ->
                _transactions.value = allTransactions
                _uiState.update { it.copy(isLoading = false) }
            }
            .catch { _uiState.update { it.copy(isLoading = false) } }
            .launchIn(viewModelScope)

        // 2. Reactively recompute analytics whenever data or range changes
        combine(_transactions, _selectedRange) { txns, range -> txns to range }
            .debounce(200L) // Prevent flickering if multiple updates happen fast
            .map { (txns, range) ->
                withContext(Dispatchers.Default) { computeAnalytics(txns, range) }
            }
            .onEach { result ->
                _uiState.update { state ->
                    state.copy(
                        totalIncome = result.income,
                        totalExpense = result.expense,
                        netFlow = result.income - result.expense,
                        topExpenseCategories = result.expenseCats,
                        topIncomeCategories = result.incomeCats,
                        monthlyBars = result.bars
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun setRange(range: String) {
        _selectedRange.value = range
        _uiState.update { it.copy(selectedRange = range) }
    }

    fun setTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    // Result wrapper for the computation logic
    private data class AnalyticsResult(
        val income: Double,
        val expense: Double,
        val expenseCats: List<CategoryStat>,
        val incomeCats: List<CategoryStat>,
        val bars: List<MonthlyBar>
    )

    /**
     * Pure logic function to calculate stats without touching UI State directly.
     * Running this on Dispatchers.Default ensures the UI stays smooth.
     */
    private fun computeAnalytics(transactions: List<Transaction>, range: String): AnalyticsResult {
        val today = LocalDate.now()
        val start = when (range) {
            "week"    -> today.minusWeeks(1)
            "month"   -> today.withDayOfMonth(1)
            "3months" -> today.minusMonths(3).withDayOfMonth(1)
            else      -> today.withDayOfYear(1)
        }

        val filtered = transactions.filter {
            runCatching { !LocalDate.parse(it.date).isBefore(start) }.getOrDefault(false)
        }

        val income = filtered.filter { it.isIncome }.sumOf { it.amount }
        val expense = filtered.filter { it.isExpense }.sumOf { it.amount }

        fun catStats(type: String, total: Double): List<CategoryStat> =
            filtered.filter { it.type == type }
                .groupBy { it.categoryName }
                .mapValues { (_, t) -> t.first().categoryColor to t.sumOf { it.amount } }
                .entries.sortedByDescending { it.value.second }.take(6)
                .map { (name, pair) ->
                    CategoryStat(
                        name = name,
                        color = pair.first,
                        amount = pair.second,
                        percent = if (total > 0) (pair.second / total).toFloat() else 0f
                    )
                }

        val bars = (5 downTo 0).map { ago ->
            val targetDate = today.minusMonths(ago.toLong())
            val monthlyTxns = transactions.filter {
                runCatching {
                    LocalDate.parse(it.date).let { ld -> 
                        ld.month == targetDate.month && ld.year == targetDate.year 
                    }
                }.getOrDefault(false)
            }
            MonthlyBar(
                label = targetDate.month.name.take(3),
                income = monthlyTxns.filter { it.isIncome }.sumOf { it.amount },
                expense = monthlyTxns.filter { it.isExpense }.sumOf { it.amount }
            )
        }

        return AnalyticsResult(
            income, 
            expense, 
            catStats("Expense", expense), 
            catStats("Income", income), 
            bars
        )
    }
}
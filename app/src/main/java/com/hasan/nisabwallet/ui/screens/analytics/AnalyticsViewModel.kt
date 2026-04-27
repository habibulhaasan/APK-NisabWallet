

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState = _uiState.asStateFlow()
    private val userId get() = auth.currentUser?.uid ?: ""

    // Keep raw transactions separately so recalculate() can access them without re-fetching
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    private val _selectedRange = MutableStateFlow("month")

    init {
        // Observe transactions
        FirestorePaths.transactions(db, userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(500) // FIX: Cap at 500 — analytics doesn't need all-time history on first load
            .snapshotFlow()
            .map { snap ->
                withContext(Dispatchers.Default) {
                    snap.documents.mapNotNull { it.toObject(Transaction::class.java)?.copy(id = it.id) }
                }
            }
            .onEach { all ->
                _transactions.value = all
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
            .catch { _uiState.value = _uiState.value.copy(isLoading = false) }
            .launchIn(viewModelScope)

        // Recompute analytics whenever transactions OR range changes, with debounce
        combine(_transactions, _selectedRange) { txns, range -> txns to range }
            .debounce(200L)
            .map { (txns, range) ->
                withContext(Dispatchers.Default) { computeAnalytics(txns, range) }
            }
            .onEach { result -> _uiState.value = _uiState.value.copy(
                totalIncome          = result.income,
                totalExpense         = result.expense,
                netFlow              = result.income - result.expense,
                topExpenseCategories = result.expenseCats,
                topIncomeCategories  = result.incomeCats,
                monthlyBars          = result.bars
            )}
            .launchIn(viewModelScope)
    }

    fun setRange(r: String) {
        _selectedRange.value   = r
        _uiState.value = _uiState.value.copy(selectedRange = r)
    }

    fun setTab(t: Int) { _uiState.value = _uiState.value.copy(selectedTab = t) }

    private data class AnalyticsResult(
        val income: Double,
        val expense: Double,
        val expenseCats: List<CategoryStat>,
        val incomeCats: List<CategoryStat>,
        val bars: List<MonthlyBar>
    )

    // Pure function — safe to run on Dispatchers.Default
    private fun computeAnalytics(transactions: List<Transaction>, range: String): AnalyticsResult {
        val today = LocalDate.now()
        val start = when (range) {
            "week"    -> today.minusWeeks(1)
            "month"   -> today.withDayOfMonth(1)
            "3months" -> today.minusMonths(3).withDayOfMonth(1)
            else      -> today.withDayOfYear(1)
        }
        val filtered = transactions.filter {
            try { !LocalDate.parse(it.date).isBefore(start) } catch (e: Exception) { false }
        }
        val income  = filtered.filter { it.isIncome  }.sumOf { it.amount }
        val expense = filtered.filter { it.isExpense }.sumOf { it.amount }

        fun catStats(type: String, total: Double): List<CategoryStat> =
            filtered.filter { it.type == type }
                .groupBy { it.categoryName }
                .mapValues { (_, t) -> t.first().categoryColor to t.sumOf { it.amount } }
                .entries.sortedByDescending { it.value.second }.take(6)
                .map { (n, p) -> CategoryStat(n, p.first, p.second,
                    if (total > 0) (p.second / total).toFloat() else 0f) }

        val bars = (5 downTo 0).map { ago ->
            val d    = today.minusMonths(ago.toLong())
            val txns = transactions.filter {
                try { LocalDate.parse(it.date).let { ld -> ld.month == d.month && ld.year == d.year } }
                catch (e: Exception) { false }
            }
            MonthlyBar(
                d.month.name.take(3),
                txns.filter { it.isIncome  }.sumOf { it.amount },
                txns.filter { it.isExpense }.sumOf { it.amount }
            )
        }

        return AnalyticsResult(income, expense, catStats("Expense", expense), catStats("Income", income), bars)
    }
}
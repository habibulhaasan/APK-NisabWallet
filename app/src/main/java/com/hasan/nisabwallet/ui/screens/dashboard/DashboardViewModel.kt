package com.hasan.nisabwallet.ui.screens.dashboard

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hasan.nisabwallet.data.model.Account
import com.hasan.nisabwallet.data.model.Transaction
import com.hasan.nisabwallet.data.repository.DashboardRepository
import com.hasan.nisabwallet.data.repository.ZakatCycleSummary
import com.hasan.nisabwallet.ui.viewmodel.BaseViewModel
import com.hasan.nisabwallet.utils.ZakatStatus
import com.hasan.nisabwallet.utils.ZakatUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DashboardUiState(
    // Accounts
    val accounts: List<Account>              = emptyList(),
    val totalBalance: Double                 = 0.0,
    val totalPurifiedBalance: Double         = 0.0,

    // Monthly summary
    val thisMonthIncome: Double              = 0.0,
    val thisMonthExpense: Double             = 0.0,
    val currentMonth: String                 = "",

    // Recent transactions
    val recentTransactions: List<Transaction> = emptyList(),

    // Feature totals for net worth
    val totalLoans: Double                   = 0.0,
    val totalLendings: Double                = 0.0,
    val totalGoals: Double                   = 0.0,
    val totalInvestments: Double             = 0.0,
    val totalJewellery: Double               = 0.0,

    // Net worth = accounts + investments + jewellery + lendings - loans
    val netWorth: Double                     = 0.0,

    // Zakat
    val nisabThreshold: Double               = 0.0,
    val zakatStatus: ZakatStatus             = ZakatStatus.NOT_MANDATORY,
    val activeCycle: ZakatCycleSummary?      = null,
    val daysUntilZakat: Long                 = 0L,
    val zakatAmount: Double                  = 0.0,
    val hijriStartDate: String               = "",

    // UI state
    val isLoading: Boolean                   = true,
    val isRefreshing: Boolean                = false,
    val userName: String                     = ""
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepo: DashboardRepository,
    private val auth: FirebaseAuth
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()
    private val userId get() = auth.currentUser?.uid ?: ""

    // Track when one-time data was last loaded to avoid redundant fetches
    private var lastOneTimeLoadMs = 0L
    private val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes

    init {
        val user = auth.currentUser
        _uiState.value = _uiState.value.copy(
            userName     = user?.displayName ?: user?.email?.substringBefore("@") ?: "User",
            currentMonth = LocalDate.now().month.name.lowercase().replaceFirstChar { it.uppercase() }
        )
        observeRealTimeData()
        loadOneTimeData()
    }

    private fun observeRealTimeData() {
        combine(
            dashboardRepo.getAccountsFlow(userId),
            dashboardRepo.getRecentTransactionsFlow(userId, limit = 8)
        ) { accounts, recentTxns ->
            val totalBal    = accounts.sumOf { it.balance }
            val purifiedBal = accounts.sumOf { it.purifiedBalance }
            _uiState.value = _uiState.value.copy(
                accounts             = accounts,
                totalBalance         = totalBal,
                totalPurifiedBalance = purifiedBal,
                recentTransactions   = recentTxns,
                isLoading            = false
            )
            recalculateNetWorth()
            recalculateZakat()
        }
        .catch { _uiState.value = _uiState.value.copy(isLoading = false) }
        .launchIn(viewModelScope)
    }

    fun loadOneTimeData(forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()
        // Skip if data is still fresh and this isn't a manual refresh
        if (!forceRefresh && (now - lastOneTimeLoadMs) < CACHE_TTL_MS) return

        viewModelScope.launch {
            val now2 = LocalDate.now()
            // All 9 run in parallel — no change here, but now gated by TTL cache
            val incomeDeferred   = async { dashboardRepo.getMonthlyIncome(userId, now2.year, now2.monthValue) }
            val expenseDeferred  = async { dashboardRepo.getMonthlyExpense(userId, now2.year, now2.monthValue) }
            val loansDeferred    = async { dashboardRepo.getActiveLoansTotal(userId) }
            val lendingsDeferred = async { dashboardRepo.getActiveLendingsTotal(userId) }
            val goalsDeferred    = async { dashboardRepo.getGoalsTotal(userId) }
            val investDeferred   = async { dashboardRepo.getInvestmentsTotal(userId) }
            val jewellDeferred   = async { dashboardRepo.getJewelleryTotal(userId) }
            val nisabDeferred    = async { dashboardRepo.getNisabThreshold(userId) }
            val cycleDeferred    = async { dashboardRepo.getActiveZakatCycle(userId) }

            _uiState.value = _uiState.value.copy(
                thisMonthIncome  = incomeDeferred.await(),
                thisMonthExpense = expenseDeferred.await(),
                totalLoans       = loansDeferred.await(),
                totalLendings    = lendingsDeferred.await(),
                totalGoals       = goalsDeferred.await(),
                totalInvestments = investDeferred.await(),
                totalJewellery   = jewellDeferred.await(),
                nisabThreshold   = nisabDeferred.await(),
                activeCycle      = cycleDeferred.await()
            )
            lastOneTimeLoadMs = System.currentTimeMillis()
            recalculateNetWorth()
            recalculateZakat()
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        viewModelScope.launch {
            loadOneTimeData(forceRefresh = true)
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    private fun recalculateNetWorth() {
        val s = _uiState.value
        _uiState.value = s.copy(
            netWorth = s.totalBalance + s.totalInvestments + s.totalJewellery + s.totalLendings - s.totalLoans
        )
    }

    private fun recalculateZakat() {
        val s     = _uiState.value
        val cycle = s.activeCycle
        val status = ZakatUtils.determineZakatStatus(
            totalWealth          = s.totalPurifiedBalance + s.totalInvestments + s.totalJewellery + s.totalLendings,
            nisabThreshold       = s.nisabThreshold,
            activeCycleStartDate = cycle?.startDate,
            isCyclePaid          = cycle?.status == "paid"
        )
        val daysLeft    = if (cycle != null) ZakatUtils.daysUntilHijriAnniversary(cycle.startDate) else 0L
        val zakatAmount = if (status == ZakatStatus.DUE)
            ZakatUtils.calculateZakat(s.totalPurifiedBalance + s.totalInvestments + s.totalJewellery) else 0.0
        val hijriStr = if (cycle != null) {
            try { ZakatUtils.formatHijriDate(ZakatUtils.gregorianToHijri(cycle.startDate)) }
            catch (e: Exception) { "" }
        } else ""

        _uiState.value = _uiState.value.copy(
            zakatStatus    = status,
            daysUntilZakat = daysLeft,
            zakatAmount    = zakatAmount,
            hijriStartDate = hijriStr
        )
    }
}
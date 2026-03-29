package com.hasan.nisabwallet.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hasan.nisabwallet.data.firebase.FirestorePaths
import com.hasan.nisabwallet.data.firebase.snapshotFlow
import com.hasan.nisabwallet.data.model.Transaction
import com.hasan.nisabwallet.ui.theme.Emerald600
import com.hasan.nisabwallet.ui.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

data class CategoryStat(val name: String, val color: String, val amount: Double, val percent: Float)
data class MonthlyBar(val label: String, val income: Double, val expense: Double)
data class AnalyticsUiState(
    val transactions: List<Transaction>      = emptyList(),
    val isLoading: Boolean                   = true,
    val selectedRange: String                = "month",
    val selectedTab: Int                     = 0,
    val totalIncome: Double                  = 0.0,
    val totalExpense: Double                 = 0.0,
    val netFlow: Double                      = 0.0,
    val topExpenseCategories: List<CategoryStat> = emptyList(),
    val topIncomeCategories: List<CategoryStat>  = emptyList(),
    val monthlyBars: List<MonthlyBar>        = emptyList()
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState = _uiState.asStateFlow()
    private val userId get() = auth.currentUser?.uid ?: ""

    init {
        FirestorePaths.transactions(db, userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .snapshotFlow()
            .onEach { snap ->
                val all = snap.documents.mapNotNull { it.toObject(Transaction::class.java)?.copy(id = it.id) }
                _uiState.value = _uiState.value.copy(transactions = all, isLoading = false)
                recalculate()
            }
            .catch { _uiState.value = _uiState.value.copy(isLoading = false) }
            .launchIn(viewModelScope)
    }

    fun setRange(r: String) { _uiState.value = _uiState.value.copy(selectedRange = r); recalculate() }
    fun setTab(t: Int)      { _uiState.value = _uiState.value.copy(selectedTab = t) }

    private fun recalculate() {
        val s     = _uiState.value
        val today = LocalDate.now()
        val start = when (s.selectedRange) {
            "week"    -> today.minusWeeks(1)
            "month"   -> today.withDayOfMonth(1)
            "3months" -> today.minusMonths(3).withDayOfMonth(1)
            else      -> today.withDayOfYear(1)
        }
        val filtered = s.transactions.filter {
            try { !LocalDate.parse(it.date).isBefore(start) } catch (e: Exception) { false }
        }
        val income  = filtered.filter { it.isIncome  }.sumOf { it.amount }
        val expense = filtered.filter { it.isExpense }.sumOf { it.amount }

        fun catStats(type: String, total: Double) = filtered.filter { it.type == type }
            .groupBy { it.categoryName }
            .mapValues { (_, t) -> t.first().categoryColor to t.sumOf { it.amount } }
            .entries.sortedByDescending { it.value.second }.take(6)
            .map { (n, p) -> CategoryStat(n, p.first, p.second, if (total > 0) (p.second / total).toFloat() else 0f) }

        val bars = (5 downTo 0).map { ago ->
            val d  = today.minusMonths(ago.toLong())
            val txns = s.transactions.filter {
                try { LocalDate.parse(it.date).let { ld -> ld.month == d.month && ld.year == d.year } }
                catch (e: Exception) { false }
            }
            MonthlyBar(d.month.name.take(3), txns.filter { it.isIncome }.sumOf { it.amount }, txns.filter { it.isExpense }.sumOf { it.amount })
        }

        _uiState.value = s.copy(
            totalIncome = income, totalExpense = expense, netFlow = income - expense,
            topExpenseCategories = catStats("Expense", expense),
            topIncomeCategories  = catStats("Income",  income),
            monthlyBars          = bars
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    navController: NavController,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Analytics", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("week" to "This Week", "month" to "This Month", "3months" to "3 Months", "year" to "This Year")) { (k, l) ->
                        FilterChip(selected = uiState.selectedRange == k, onClick = { viewModel.setRange(k) },
                            label = { Text(l, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary))
                    }
                }
            }

            // Summary
            item {
                Card(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp)) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), Arrangement.SpaceAround) {
                        NetStat("Income", uiState.totalIncome, Color(0xFF10B981), Icons.Default.TrendingUp)
                        NetStat("Expense", uiState.totalExpense, Color(0xFFEF4444), Icons.Default.TrendingDown)
                        NetStat(if (uiState.netFlow >= 0) "Saved" else "Deficit",
                            kotlin.math.abs(uiState.netFlow),
                            if (uiState.netFlow >= 0) MaterialTheme.colorScheme.primary else Color(0xFFEF4444),
                            if (uiState.netFlow >= 0) Icons.Default.Savings else Icons.Default.Warning)
                    }
                }
            }

            // Monthly bar chart
            item {
                Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Monthly trend", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        val maxVal = uiState.monthlyBars.maxOfOrNull { maxOf(it.income, it.expense) }?.takeIf { it > 0 } ?: 1.0
                        Row(Modifier.fillMaxWidth().height(100.dp), Arrangement.SpaceAround, Alignment.Bottom) {
                            uiState.monthlyBars.forEach { bar ->
                                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Box(Modifier.width(9.dp).fillMaxHeight((bar.income / maxVal).toFloat().coerceAtLeast(0.02f))
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(Color(0xFF10B981).copy(0.8f)))
                                    Box(Modifier.width(9.dp).fillMaxHeight((bar.expense / maxVal).toFloat().coerceAtLeast(0.02f))
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(Color(0xFFEF4444).copy(0.8f)))
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceAround) {
                            uiState.monthlyBars.forEach { bar ->
                                Text(bar.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981))); Text("Income", style = MaterialTheme.typography.labelSmall) }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF4444))); Text("Expense", style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            }

            // Category breakdown
            item {
                TabRow(selectedTabIndex = uiState.selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
                    listOf("Expenses" to Color(0xFFEF4444), "Income" to Color(0xFF10B981)).forEachIndexed { i, (l, c) ->
                        Tab(selected = uiState.selectedTab == i, onClick = { viewModel.setTab(i) }, text = {
                            Text(l, color = if (uiState.selectedTab == i) c else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (uiState.selectedTab == i) FontWeight.SemiBold else FontWeight.Normal)
                        })
                    }
                }
            }

            val cats = if (uiState.selectedTab == 0) uiState.topExpenseCategories else uiState.topIncomeCategories
            if (cats.isEmpty()) {
                item { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { Text("No data for this period", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            } else {
                items(cats) { stat ->
                    val c = runCatching { Color(android.graphics.Color.parseColor(stat.color)) }.getOrDefault(Emerald600)
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(c))
                        Spacer(Modifier.width(10.dp))
                        Text(stat.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Text("৳${"%,.0f".format(stat.amount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(8.dp))
                        LinearProgressIndicator(progress = { stat.percent }, modifier = Modifier.width(60.dp).height(4.dp).clip(RoundedCornerShape(2.dp)), color = c, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun NetStat(label: String, amount: Double, color: Color, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, Modifier.size(20.dp), tint = color)
        Text("৳${"%,.0f".format(amount)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// app/src/main/java/com/hasan/nisabwallet/ui/screens/dailyexpense/DailyExpenseScreen.kt
package com.hasan.nisabwallet.ui.screens.dailyexpense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hasan.nisabwallet.ui.components.*
import com.hasan.nisabwallet.ui.theme.Emerald600
import com.hasan.nisabwallet.data.model.ExpenseTrackerMonthData
import com.hasan.nisabwallet.ui.components.SimpleDropdown
import java.time.LocalDate
import java.time.YearMonth

private val TAB_COLORS = mapOf(
    "emerald" to Color(0xFF10B981),
    "blue"    to Color(0xFF3B82F6),
    "violet"  to Color(0xFF8B5CF6),
    "amber"   to Color(0xFFF59E0B),
    "rose"    to Color(0xFFF43F5E),
    "cyan"    to Color(0xFF06B6D4),
    "orange"  to Color(0xFFF97316),
    "pink"    to Color(0xFFEC4899),
    "teal"    to Color(0xFF14B8A6),
    "indigo"  to Color(0xFF6366F1)
)

private val MONTH_NAMES = listOf("January","February","March","April","May","June",
    "July","August","September","October","November","December")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyExpenseScreen(
    navController: NavController,
    viewModel: DailyExpenseViewModel = hiltViewModel()
) {
    val uiState       by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState    = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHost  = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { snackbarHost.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Daily Expense Tracker", fontWeight = FontWeight.SemiBold)
                        Text("Admin Feature", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::showAddTabSheet) {
                        Icon(Icons.Default.Settings, "Manage Tabs")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        if (uiState.tabs.isEmpty()) {
            // Empty state
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.BarChart, null, Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
                    Text("No expense types configured", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text("Tap ⚙ to add your first expense tab",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    NisabButton("Add First Tab", viewModel::showAddTabSheet,
                        modifier = Modifier.width(200.dp))
                }
            }
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().padding(padding)) {

            // ── View mode toggle ──────────────────────────────────────────────
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    ExpenseViewMode.MONTHLY  to "Monthly",
                    ExpenseViewMode.YEARLY   to "Yearly",
                    ExpenseViewMode.HISTORY  to "History"
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = uiState.viewMode == mode,
                        onClick  = { viewModel.setViewMode(mode) },
                        label    = { Text(label, fontSize = 13.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor     = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // ── Period navigator (hidden in history) ──────────────────────────
            if (uiState.viewMode != ExpenseViewMode.HISTORY) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    IconButton(onClick = viewModel::prevPeriod) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous")
                    }
                    Text(
                        if (uiState.viewMode == ExpenseViewMode.MONTHLY)
                            "${MONTH_NAMES[uiState.currentMonth - 1]} ${uiState.currentYear}"
                        else uiState.currentYear.toString(),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = viewModel::nextPeriod) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next")
                    }
                }
            }

            // ── Summary pills row ─────────────────────────────────────────────
            if (uiState.viewMode != ExpenseViewMode.HISTORY) {
                Row(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Per-tab totals
                    uiState.tabs.forEach { tab ->
                        val tabData = uiState.currentMonthData?.tabs?.get(tab.id) ?: emptyMap()
                        val total   = tabData.values.sumOf { it }
                        if (total > 0) {
                            val color = TAB_COLORS[tab.color] ?: Emerald600
                            Box(Modifier.clip(RoundedCornerShape(8.dp))
                                .background(color.copy(0.12f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)) {
                                Text("${tab.title}: ৳${"%,.0f".format(total)}",
                                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    color = color)
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (uiState.grandTotal > 0 && !uiState.isCurrentPeriodConfirmed) {
                        Button(
                            onClick = viewModel::showConfirmSheet,
                            shape   = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.CreditCard, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Record", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (uiState.isCurrentPeriodConfirmed) {
                        Box(Modifier.clip(RoundedCornerShape(8.dp))
                            .background(Emerald600.copy(0.12f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.CheckCircle, null, Modifier.size(14.dp),
                                    tint = Emerald600)
                                Text("Recorded", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    color = Emerald600)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // ── Tab selector pills ────────────────────────────────────────────
            if (uiState.viewMode != ExpenseViewMode.HISTORY) {
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.padding(vertical = 4.dp)
                ) {
                    items(uiState.tabs) { tab ->
                        val selected = tab.id == uiState.activeTabId
                        val color    = TAB_COLORS[tab.color] ?: Emerald600
                        FilterChip(
                            selected = selected,
                            onClick  = { viewModel.setActiveTab(tab.id) },
                            label    = { Text(tab.title, fontSize = 13.sp) },
                            leadingIcon = {
                                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp))
                                    .background(color))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(0.15f),
                                selectedLabelColor     = color
                            )
                        )
                    }
                }
            }

            // ── Main content ──────────────────────────────────────────────────
            when (uiState.viewMode) {
                ExpenseViewMode.MONTHLY  -> MonthlyGridContent(uiState, viewModel)
                ExpenseViewMode.YEARLY   -> YearlyGridContent(uiState, viewModel)
                ExpenseViewMode.HISTORY  -> HistoryContent(uiState)
            }
        }
    }

    // ── Tab management sheet ──────────────────────────────────────────────────
    if (uiState.showTabSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissTabSheet,
            sheetState       = sheetState,
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            TabManagementSheet(uiState, viewModel)
        }
    }

    // ── Confirm sheet ─────────────────────────────────────────────────────────
    if (uiState.showConfirmSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissConfirmSheet,
            sheetState       = sheetState,
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            ConfirmExpenseSheet(uiState, viewModel)
        }
    }
}

// ── Monthly Grid ──────────────────────────────────────────────────────────────

@Composable
private fun MonthlyGridContent(uiState: DailyExpenseUiState, viewModel: DailyExpenseViewModel) {
    val tab = uiState.activeTab ?: return
    val color = TAB_COLORS[tab.color] ?: Emerald600
    val ym = YearMonth.of(uiState.currentYear, uiState.currentMonth)
    val daysInMonth = ym.lengthOfMonth()
    val data = uiState.getActiveTabMonthlyData()
    val total = data.values.sumOf { it }
    val today = LocalDate.now()

    // Cell edit state — local to avoid recompose lag
    val cellValues = remember(data, uiState.currentYM, uiState.activeTabId) {
        (1..daysInMonth).associate { day ->
            day to (data[day.toString()]?.let { if (it == 0.0) "" else it.toLong().toString() } ?: "")
        }.toMutableMap()
    }

    Column(Modifier.fillMaxSize()) {
        // Header card
        Card(Modifier.fillMaxWidth().padding(16.dp), RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = color.copy(0.08f))) {
            Row(Modifier.padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text(tab.title, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold, color = color)
                    Text("${data.count { it.value > 0 }} days recorded",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("৳${"%,.0f".format(total)}", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = color)
                    if (data.isNotEmpty()) {
                        val daysWithData = data.count { it.value > 0 }
                        if (daysWithData > 0)
                            Text("avg ৳${"%,.0f".format(total / daysWithData)}/day",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Day rows in scrollable column
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items((1..daysInMonth).toList()) { day ->
                val dayDate   = LocalDate.of(uiState.currentYear, uiState.currentMonth, day)
                val dayName   = listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")[dayDate.dayOfWeek.value % 7]
                val isWeekend = dayDate.dayOfWeek.value == 7 || dayDate.dayOfWeek.value == 6
                val isToday   = dayDate == today
                val val0      = data[day.toString()] ?: 0.0
                val running   = (1..day).sumOf { d -> data[d.toString()] ?: 0.0 }

                var cellText by remember(day, uiState.currentYM, uiState.activeTabId) {
                    mutableStateOf(if (val0 > 0) val0.toLong().toString() else "")
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = when {
                            isToday   -> MaterialTheme.colorScheme.primaryContainer
                            val0 > 0  -> color.copy(0.06f)
                            isWeekend -> MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
                            else      -> MaterialTheme.colorScheme.surface
                        }
                    ),
                    elevation = CardDefaults.cardElevation(if (isToday) 2.dp else 0.5.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Day number
                        Text(
                            "$day",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color      = if (isToday) MaterialTheme.colorScheme.primary
                                         else if (isWeekend) Color(0xFFF59E0B)
                                         else MaterialTheme.colorScheme.onSurface,
                            modifier   = Modifier.width(28.dp)
                        )
                        // Day name
                        Text(dayName, style = MaterialTheme.typography.labelSmall,
                            color = if (isWeekend) Color(0xFFF59E0B)
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(36.dp))
                        // Amount input
                        OutlinedTextField(
                            value         = cellText,
                            onValueChange = {
                                cellText = it
                                viewModel.onMonthCellChange(tab.id, day, it)
                            },
                            placeholder   = { Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine    = true,
                            enabled       = !uiState.isCurrentPeriodConfirmed,
                            textStyle     = LocalTextStyle.current.copy(
                                textAlign  = TextAlign.End,
                                fontSize   = 14.sp,
                                fontWeight = if (val0 > 0) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            shape  = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = color,
                                unfocusedBorderColor = if (val0 > 0) color.copy(0.4f)
                                                       else MaterialTheme.colorScheme.outline.copy(0.3f)
                            ),
                            modifier = Modifier.weight(1f).height(44.dp)
                        )
                        // Running total
                        Text(
                            if (running > 0) "৳${"%,.0f".format(running)}" else "—",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = if (running > 0) MaterialTheme.colorScheme.onSurfaceVariant
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.2f),
                            modifier = Modifier.width(72.dp).padding(start = 8.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
            // Total row
            item {
                Card(Modifier.fillMaxWidth(), RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Total", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("৳${"%,.0f".format(total)}", color = Color(0xFF34D399),
                            fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// ── Yearly Grid ───────────────────────────────────────────────────────────────

@Composable
private fun YearlyGridContent(uiState: DailyExpenseUiState, viewModel: DailyExpenseViewModel) {
    val tab = uiState.activeTab ?: return
    val color = TAB_COLORS[tab.color] ?: Emerald600
    val currentMonthIdx = LocalDate.now().monthValue - 1

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(MONTH_NAMES.indices.toList()) { monthIdx ->
            val autoSum = uiState.getMonthlyAutoSum(tab.id, uiState.currentYear, monthIdx)
            val manualVal = uiState.getActiveTabYearlyData()[monthIdx.toString()] ?: 0.0
            val effective = autoSum ?: manualVal
            val isCurrentMonth = monthIdx == currentMonthIdx && uiState.currentYear == LocalDate.now().year
            val isAutoFilled = autoSum != null

            var cellText by remember(monthIdx, uiState.currentYear, uiState.activeTabId) {
                mutableStateOf(if (manualVal > 0) manualVal.toLong().toString() else "")
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(10.dp),
                colors   = CardDefaults.cardColors(
                    containerColor = when {
                        isCurrentMonth -> MaterialTheme.colorScheme.primaryContainer
                        effective > 0  -> color.copy(0.06f)
                        else           -> MaterialTheme.colorScheme.surface
                    }
                ),
                elevation = CardDefaults.cardElevation(if (isCurrentMonth) 2.dp else 0.5.dp)
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(MONTH_NAMES[monthIdx].take(3),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isCurrentMonth) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrentMonth) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(40.dp))
                    // Auto/Manual badge
                    Box(Modifier.clip(RoundedCornerShape(4.dp))
                        .background(if (isAutoFilled) Color(0xFF3B82F6).copy(0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(if (isAutoFilled) "Auto" else "Manual",
                            fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                            color = if (isAutoFilled) Color(0xFF3B82F6)
                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    if (isAutoFilled) {
                        // Read-only auto sum
                        Text("৳${"%,.0f".format(effective)}", style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold, color = color,
                            modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    } else {
                        // Manual input
                        OutlinedTextField(
                            value = cellText,
                            onValueChange = {
                                cellText = it
                                viewModel.onYearCellChange(tab.id, monthIdx, it)
                            },
                            placeholder   = { Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine    = true,
                            enabled       = !uiState.isCurrentPeriodConfirmed,
                            textStyle     = LocalTextStyle.current.copy(textAlign = TextAlign.End, fontSize = 14.sp),
                            shape  = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        )
                    }
                }
            }
        }
        item {
            val total = MONTH_NAMES.indices.sumOf { monthIdx ->
                uiState.getMonthlyAutoSum(tab.id, uiState.currentYear, monthIdx)
                    ?: (uiState.getActiveTabYearlyData()[monthIdx.toString()] ?: 0.0)
            }
            Card(Modifier.fillMaxWidth(), RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black)) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Total ${uiState.currentYear}", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("৳${"%,.0f".format(total)}", color = Color(0xFF34D399),
                        fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

// ── History view ──────────────────────────────────────────────────────────────

@Composable
private fun HistoryContent(uiState: DailyExpenseUiState) {
    val months = uiState.monthDataMap.keys.filter { it.length == 7 }.sortedDescending()
    if (months.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.History, null, Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f))
                Text("No history yet", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(months) { ym ->
            val data      = uiState.monthDataMap[ym]!!
            val confirmed = data.confirmedAt != null
            val total     = data.tabs.values.sumOf { it.values.sumOf { v -> v } }
            val year      = ym.split("-")[0].toInt()
            val monthIdx  = ym.split("-")[1].toInt() - 1

            Card(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = if (confirmed)
                    androidx.compose.foundation.BorderStroke(1.dp, Emerald600.copy(0.3f))
                else null,
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${MONTH_NAMES[monthIdx]} $year",
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        uiState.tabs.forEach { tab ->
                            val tabTotal = data.tabs[tab.id]?.values?.sumOf { it } ?: 0.0
                            if (tabTotal > 0) {
                                val color = TAB_COLORS[tab.color] ?: Emerald600
                                Text("${tab.title}: ৳${"%,.0f".format(tabTotal)}",
                                    fontSize = 12.sp, color = color,
                                    fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("৳${"%,.0f".format(total)}", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall)
                        Box(Modifier.clip(RoundedCornerShape(6.dp))
                            .background(if (confirmed) Emerald600.copy(0.12f)
                                        else Color(0xFFF59E0B).copy(0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(if (confirmed) "✓ Recorded" else "Draft",
                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                color = if (confirmed) Emerald600 else Color(0xFFF59E0B))
                        }
                    }
                }
            }
        }
    }
}

// ── Tab management sheet ──────────────────────────────────────────────────────

@Composable
private fun TabManagementSheet(uiState: DailyExpenseUiState, viewModel: DailyExpenseViewModel) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Manage Expense Tabs", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
            IconButton(onClick = viewModel::dismissTabSheet) {
                Icon(Icons.Default.Close, "Close")
            }
        }
        uiState.errorMessage?.let { ErrorCard(it) }

        // Existing tabs
        uiState.tabs.forEach { tab ->
            val color = TAB_COLORS[tab.color] ?: Emerald600
            val isEditing = uiState.editingTab?.id == tab.id
            Card(Modifier.fillMaxWidth(), RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isEditing) MaterialTheme.colorScheme.primaryContainer
                                     else MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    if (!isEditing) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp))
                                .background(color))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tab.title, style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold)
                                val catName = uiState.categories.find { it.id == tab.categoryId }?.name
                                Text(buildString {
                                    if (catName != null) append(catName)
                                    if (tab.unit.isNotBlank()) append(" · ${tab.unit}/day")
                                    if (tab.defaultAmount > 0) append(" · default: ${tab.defaultAmount.toInt()}")
                                }, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.showEditTabSheet(tab) },
                                modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, "Edit", Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.deleteTab(tab.id) },
                                modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, "Delete", Modifier.size(16.dp),
                                    tint = Color(0xFFEF4444))
                            }
                        }
                    } else {
                        TabFormContent(uiState, viewModel, isEditing = true)
                    }
                }
            }
        }

        // Add new tab form
        HorizontalDivider()
        Text("Add New Tab", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (uiState.editingTab == null) {
            TabFormContent(uiState, viewModel, isEditing = false)
        }
    }
}

@Composable
private fun TabFormContent(
    uiState: DailyExpenseUiState,
    viewModel: DailyExpenseViewModel,
    isEditing: Boolean
) {
    val colors = listOf("emerald","blue","violet","amber","rose","cyan","orange","pink","teal","indigo")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NisabTextField(uiState.formTabTitle, viewModel::onTabTitleChange, "Title *",
                modifier = Modifier.weight(1f))
            NisabTextField(uiState.formTabUnit, viewModel::onTabUnitChange, "Unit",
                modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SimpleDropdown("Category", uiState.formTabCategoryId,
                listOf("" to "No category") + uiState.categories.map { it.id to it.name },
                viewModel::onTabCategoryChange, Icons.Default.Category,
                modifier = Modifier.weight(1f))
            NisabTextField(uiState.formTabDefaultAmount, viewModel::onTabDefaultAmountChange,
                "Default", keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f))
        }
        // Color picker
        Text("Color", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colors.forEach { colorName ->
                val c       = TAB_COLORS[colorName] ?: Emerald600
                val selected = uiState.formTabColor == colorName
                Box(
                    modifier = Modifier.size(if (selected) 32.dp else 28.dp)
                        .clip(RoundedCornerShape(if (selected) 10.dp else 8.dp))
                        .background(c)
                        .clickable { viewModel.onTabColorChange(colorName) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) Icon(Icons.Default.Check, null, Modifier.size(16.dp),
                        tint = Color.White)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isEditing) {
                OutlinedButton(onClick = viewModel::dismissTabSheet, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)) { Text("Cancel") }
            }
            NisabButton(
                text      = if (isEditing) "Update Tab" else "Add Tab",
                onClick   = viewModel::saveTab,
                isLoading = uiState.isSaving,
                modifier  = Modifier.weight(1f)
            )
        }
    }
}

// ── Confirm expense sheet ─────────────────────────────────────────────────────

@Composable
private fun ConfirmExpenseSheet(uiState: DailyExpenseUiState, viewModel: DailyExpenseViewModel) {
    val isMonthly = uiState.viewMode == ExpenseViewMode.MONTHLY
    val label     = if (isMonthly)
        "${MONTH_NAMES[uiState.currentMonth - 1]} ${uiState.currentYear}"
    else uiState.currentYear.toString()

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Record Expense", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
            IconButton(onClick = viewModel::dismissConfirmSheet) {
                Icon(Icons.Default.Close, "Close")
            }
        }

        // Summary
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(16.dp)) {
                Text("$label — Total Expenses", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("৳${"%,.0f".format(uiState.grandTotal)}",
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }

        // Per-tab breakdown
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Breakdown", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                uiState.tabs.forEach { tab ->
                    val dataDoc = if (isMonthly) uiState.currentMonthData else uiState.currentYearData?.let {
                        ExpenseTrackerMonthData(tabs = it.tabs)
                    }
                    val tabTotal = (dataDoc?.tabs?.get(tab.id) ?: emptyMap()).values.sumOf { it }
                    if (tabTotal > 0) {
                        val color = TAB_COLORS[tab.color] ?: Emerald600
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
                                Text(tab.title, style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium)
                            }
                            Text("৳${"%,.0f".format(tabTotal)}", style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        uiState.errorMessage?.let { ErrorCard(it) }

        // Account selector
        Text("Deduct from Account *", style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium)
        uiState.accounts.forEach { acc ->
            val selected = uiState.confirmAccountId == acc.id
            Card(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.onConfirmAccountChange(acc.id) },
                shape    = RoundedCornerShape(12.dp),
                border   = androidx.compose.foundation.BorderStroke(
                    if (selected) 2.dp else 1.dp,
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(0.3f)
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                     else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(Modifier.padding(12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column {
                        Text(acc.name, style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text("Balance: ৳${"%,.0f".format(acc.balance)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (selected) Icon(Icons.Default.CheckCircle, null,
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        NisabTextField(uiState.confirmNote, viewModel::onConfirmNoteChange,
            "Note (optional)", leadingIcon = Icons.Default.Notes)

        NisabButton(
            text      = "Record ৳${"%,.0f".format(uiState.grandTotal)}",
            onClick   = viewModel::recordExpense,
            isLoading = uiState.isSaving,
            enabled   = uiState.confirmAccountId.isNotEmpty()
        )
    }
}
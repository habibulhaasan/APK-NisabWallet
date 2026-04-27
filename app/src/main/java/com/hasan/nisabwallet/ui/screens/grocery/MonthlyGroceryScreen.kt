// app/src/main/java/com/hasan/nisabwallet/ui/screens/grocery/MonthlyGroceryScreen.kt
package com.hasan.nisabwallet.ui.screens.grocery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hasan.nisabwallet.ui.components.*
import com.hasan.nisabwallet.ui.screens.transactions.SimpleDropdown
import com.hasan.nisabwallet.ui.theme.Emerald600

private val MONTHS = listOf("January","February","March","April","May","June",
    "July","August","September","October","November","December")
private val UNITS  = listOf("pcs","kg","g","litre","ml","pack","dozen","bag","bottle","box","can","bunch")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyGroceryScreen(
    navController: NavController,
    viewModel: GroceryViewModel = hiltViewModel()
) {
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState   = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { snackbarHost.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monthly Grocery Planner", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::showAddItemSheet) {
                        Icon(Icons.Default.Add, "Add Item")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().padding(padding)) {

            // ── Tab toggle ────────────────────────────────────────────────────
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("planner" to "Planner", "history" to "History").forEach { (tab, label) ->
                    FilterChip(
                        selected = uiState.activeTab == tab,
                        onClick  = { viewModel.setActiveTab(tab) },
                        label    = { Text(label, fontSize = 13.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor     = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            when (uiState.activeTab) {
                "planner" -> PlannerContent(uiState, viewModel)
                "history" -> HistoryTabContent(uiState)
            }
        }
    }

    // ── Add / edit item sheet ─────────────────────────────────────────────────
    if (uiState.showAddItemSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissItemSheet,
            sheetState       = sheetState,
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            AddEditItemSheet(uiState, viewModel)
        }
    }

    // ── Confirm sheet ─────────────────────────────────────────────────────────
    if (uiState.showConfirmSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissConfirmSheet,
            sheetState       = sheetState,
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            ConfirmPurchaseSheet(uiState, viewModel)
        }
    }
}

// ── Planner Tab ───────────────────────────────────────────────────────────────

@Composable
private fun PlannerContent(uiState: GroceryUiState, viewModel: GroceryViewModel) {
    Column(Modifier.fillMaxSize()) {

        // ── Sticky month navigator + summary ──────────────────────────────────
        Card(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            RoundedCornerShape(14.dp),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Month nav row
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    IconButton(onClick = viewModel::prevMonth, Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous", Modifier.size(18.dp))
                    }
                    Text("${MONTHS[uiState.currentMonth - 1]} ${uiState.currentYear}",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = viewModel::nextMonth, Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next", Modifier.size(18.dp))
                    }
                }
                // Summary pills row
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    // Bought
                    Box(Modifier.clip(RoundedCornerShape(8.dp))
                        .background(Emerald600.copy(0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("✓ ${uiState.boughtRows.size} · ৳${"%,.0f".format(
                            uiState.boughtRows.sumOf { it.effectivePrice })}",
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Emerald600)
                    }
                    // Pending
                    Box(Modifier.clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF59E0B).copy(0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("… ${uiState.pendingRows.size} · ৳${"%,.0f".format(uiState.totalPending)}",
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF59E0B))
                    }
                    Spacer(Modifier.weight(1f))
                    if (uiState.recordableBought.isNotEmpty()) {
                        Button(onClick = viewModel::showConfirmSheet,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                            Icon(Icons.Default.CreditCard, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Record", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── Search & filter bar ───────────────────────────────────────────────
        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::setSearch,
                placeholder = { Text("Search…") },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                singleLine  = true,
                shape       = RoundedCornerShape(12.dp),
                modifier    = Modifier.weight(1f).height(48.dp)
            )
            // Bought filter
            listOf("all" to "All", "bought" to "✓", "pending" to "…").forEach { (f, l) ->
                val sel = uiState.filterBought == f
                Box(Modifier.clip(RoundedCornerShape(8.dp))
                    .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { viewModel.setFilterBought(f) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Text(l, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = if (sel) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (uiState.masterItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.ShoppingCart, null, Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f))
                    Text("No grocery items yet", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    NisabButton("Add First Item", viewModel::showAddItemSheet,
                        modifier = Modifier.width(200.dp))
                }
            }
            return
        }

        // ── Grocery item list ─────────────────────────────────────────────────
        val grouped = uiState.filteredRows
            .groupBy { it.category.ifEmpty { "__none__" } }
            .entries.sortedWith(compareBy { if (it.key == "__none__") "zzz" else it.key })

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            grouped.forEach { (catId, catRows) ->
                val catName = if (catId == "__none__") "Uncategorised"
                              else uiState.categories.find { it.id == catId }?.name ?: catId
                val catBoughtTotal = catRows.filter { it.curBought && !it.curRecorded }.sumOf { it.effectivePrice }
                val catPlanTotal   = catRows.sumOf { it.curTotal }

                item(key = "header_$catId") {
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text(catName.uppercase(), style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("৳${"%,.0f".format(catBoughtTotal)} / ৳${"%,.0f".format(catPlanTotal)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                items(catRows, key = { it.itemId }) { row ->
                    GroceryRowCard(row = row, viewModel = viewModel, uiState = uiState)
                }
            }

            // Bottom totals
            item {
                Card(Modifier.fillMaxWidth(), RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Total", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Plan: ৳${"%,.0f".format(uiState.filteredRows.sumOf { it.curTotal })}",
                                color = Color(0xFF9CA3AF), fontSize = 12.sp)
                            Text("Bought: ৳${"%,.0f".format(uiState.filteredRows.filter { it.curBought }.sumOf { it.effectivePrice })}",
                                color = Color(0xFF34D399), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroceryRowCard(row: GroceryRow, viewModel: GroceryViewModel, uiState: GroceryUiState) {
    var showActions by remember { mutableStateOf(false) }
    var qtyText     by remember(row.itemId, uiState.currentYM) { mutableStateOf(row.curQty.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() }) }
    var priceText   by remember(row.itemId, uiState.currentYM) { mutableStateOf(row.curUnitPrice.let { if (it == 0.0) "" else it.toLong().toString() }) }
    var boughtText  by remember(row.itemId, uiState.currentYM) { mutableStateOf(row.curBoughtPrice?.let { it.toLong().toString() } ?: "") }

    Card(
        Modifier.fillMaxWidth(),
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                row.curRecorded -> Emerald600.copy(0.04f)
                row.curBought   -> Emerald600.copy(0.08f)
                else            -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(0.5.dp)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Checkbox
                Checkbox(
                    checked  = row.curBought,
                    onCheckedChange = { viewModel.toggleBought(row.itemId) },
                    colors   = CheckboxDefaults.colors(checkedColor = Emerald600),
                    modifier = Modifier.size(24.dp)
                )
                // Name
                Text(
                    row.name,
                    style  = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color  = if (row.curBought) Emerald600 else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (row.curRecorded) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.weight(1f)
                )
                // Qty input with unit
                Row(Modifier.width(96.dp).height(36.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it; viewModel.onQtyChange(row.itemId, it) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape      = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                        modifier   = Modifier.weight(1f).fillMaxHeight(),
                        textStyle  = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 13.sp)
                    )
                    Box(Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        .fillMaxHeight().padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center) {
                        Text(row.unit, fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                // ⋮ actions toggle
                IconButton(onClick = { showActions = !showActions }, Modifier.size(28.dp)) {
                    Icon(Icons.Default.MoreVert, "Actions", Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Row 2: price inputs + totals
            Row(Modifier.padding(start = 32.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                // Unit price
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it; viewModel.onUnitPriceChange(row.itemId, it) },
                    placeholder = { Text("Unit ৳", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape      = RoundedCornerShape(8.dp),
                    modifier   = Modifier.width(80.dp).height(36.dp),
                    textStyle  = LocalTextStyle.current.copy(textAlign = TextAlign.End, fontSize = 12.sp)
                )
                // Planned total
                Text("= ৳${"%,.0f".format(row.curTotal)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (row.curBought) MaterialTheme.colorScheme.onSurface.copy(0.4f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = if (row.curBought) TextDecoration.LineThrough else TextDecoration.None)
                Spacer(Modifier.weight(1f))
                // Bought price (shown only when bought)
                if (row.curBought) {
                    OutlinedTextField(
                        value = boughtText,
                        onValueChange = { boughtText = it; viewModel.onBoughtPriceChange(row.itemId, it) },
                        placeholder = { Text("Actual ৳", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape  = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Emerald600,
                            unfocusedBorderColor = Emerald600.copy(0.5f)
                        ),
                        modifier   = Modifier.width(88.dp).height(36.dp),
                        textStyle  = LocalTextStyle.current.copy(textAlign = TextAlign.End, fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold, color = Emerald600)
                    )
                }
                // Prev month reference
                if (row.prevTotal != null) {
                    Text("prev ৳${"%,.0f".format(row.prevTotal)}",
                        fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
                }
            }

            // Actions row (shown when ⋮ tapped)
            if (showActions) {
                Row(Modifier.padding(start = 32.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        viewModel.showEditItemSheet(
                            com.hasan.nisabwallet.data.model.GroceryItem(
                                id = row.itemId, name = row.name, unit = row.unit, category = row.category
                            )
                        )
                        showActions = false
                    }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                        Icon(Icons.Default.Edit, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit", fontSize = 12.sp)
                    }
                    TextButton(onClick = { viewModel.archiveItem(row.itemId, !row.archived); showActions = false },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                        Icon(if (row.archived) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (row.archived) "Restore" else "Archive", fontSize = 12.sp)
                    }
                    TextButton(onClick = { viewModel.deleteItem(row.itemId); showActions = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                        Icon(Icons.Default.Delete, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ── Add/Edit item sheet ───────────────────────────────────────────────────────

@Composable
private fun AddEditItemSheet(uiState: GroceryUiState, viewModel: GroceryViewModel) {
    val isEdit = uiState.editingItem != null
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(if (isEdit) "Edit Item" else "Add Item",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = viewModel::dismissItemSheet) { Icon(Icons.Default.Close, "Close") }
        }
        uiState.errorMessage?.let { ErrorCard(it) }

        NisabTextField(uiState.formName, viewModel::onFormNameChange, "Item name *",
            leadingIcon = Icons.Default.ShoppingCart)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SimpleDropdown("Unit", uiState.formUnit,
                UNITS.map { it to it }, viewModel::onFormUnitChange,
                Icons.Default.Scale, modifier = Modifier.weight(1f))
            NisabTextField(uiState.formDefaultQty, viewModel::onFormDefaultQtyChange, "Default Qty",
                keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
        }

        NisabTextField(uiState.formDefaultUnitPrice, viewModel::onFormDefaultPriceChange,
            "Default Unit Price (৳)", leadingIcon = Icons.Default.MonetizationOn,
            keyboardType = KeyboardType.Number)

        SimpleDropdown("Category", uiState.formCategory,
            listOf("" to "No category") + uiState.categories.map { it.id to it.name },
            viewModel::onFormCategoryChange, Icons.Default.Category)

        NisabButton(if (isEdit) "Update Item" else "Add Item", viewModel::saveItem,
            isLoading = uiState.isSaving)
    }
}

// ── Confirm purchase sheet ────────────────────────────────────────────────────

@Composable
private fun ConfirmPurchaseSheet(uiState: GroceryUiState, viewModel: GroceryViewModel) {
    val monthLabel = "${MONTHS[uiState.currentMonth - 1]} ${uiState.currentYear}"
    val toRecord   = uiState.recordableBought.filter { it.effectivePrice > 0 }
    val hasUncategorised = toRecord.any { it.category.isEmpty() }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Record Grocery Expense", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
            IconButton(onClick = viewModel::dismissConfirmSheet) { Icon(Icons.Default.Close, "Close") }
        }

        // Summary hero
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Emerald600.copy(0.1f))) {
            Column(Modifier.padding(16.dp)) {
                Text("$monthLabel — New Items to Record",
                    style = MaterialTheme.typography.labelSmall, color = Emerald600)
                Text("৳${"%,.0f".format(uiState.totalBought)}",
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("${toRecord.size} item(s) to record",
                    style = MaterialTheme.typography.bodySmall, color = Emerald600)
            }
        }

        // Items list
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Items to Record", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                toRecord.sortedBy { it.name }.forEach { row ->
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Column {
                            Text("${row.name} ×${row.curQty.toInt()}",
                                style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            val catName = uiState.categories.find { it.id == row.category }?.name
                            if (catName != null) Text(catName, fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("৳${"%,.0f".format(row.effectivePrice)}",
                            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Fallback category for uncategorised
        if (hasUncategorised) {
            SimpleDropdown("Fallback Category (for uncategorised items)", uiState.confirmCategoryId,
                listOf("" to "Select…") + uiState.categories.map { it.id to it.name },
                viewModel::onConfirmCategoryChange, Icons.Default.Category)
        }

        uiState.errorMessage?.let { ErrorCard(it) }

        // Account selector
        Text("Deduct from Account *", style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium)
        uiState.accounts.forEach { acc ->
            val selected   = uiState.confirmAccountId == acc.id
            val sufficient = acc.balance >= uiState.totalBought
            Card(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.onConfirmAccountChange(acc.id) },
                shape    = RoundedCornerShape(12.dp),
                border   = androidx.compose.foundation.BorderStroke(
                    if (selected) 2.dp else 1.dp,
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(0.3f)
                ),
                colors   = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                     else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(Modifier.padding(12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column {
                        Text(acc.name, fontWeight = FontWeight.SemiBold)
                        Text("Balance: ৳${"%,.0f".format(acc.balance)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (sufficient) Emerald600 else Color(0xFFF97316))
                        if (!sufficient) Text("May be insufficient",
                            fontSize = 11.sp, color = Color(0xFFF97316))
                    }
                    if (selected) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        NisabTextField(uiState.confirmNote, viewModel::onConfirmNoteChange,
            "Note (optional)", leadingIcon = Icons.Default.Notes)

        NisabButton(
            text      = "Record ${toRecord.size} item(s) · ৳${"%,.0f".format(uiState.totalBought)}",
            onClick   = viewModel::recordPurchase,
            isLoading = uiState.isSaving,
            enabled   = uiState.confirmAccountId.isNotEmpty() &&
                (!hasUncategorised || uiState.confirmCategoryId.isNotEmpty())
        )
    }
}

// ── History Tab ───────────────────────────────────────────────────────────────

@Composable
private fun HistoryTabContent(uiState: GroceryUiState) {
    val months = uiState.monthDataMap.keys.sortedDescending()
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

    // Summary stats
    val totalSpent    = uiState.monthDataMap.values.sumOf { it.totalAmount }
    val recordedCount = uiState.monthDataMap.values.count { it.confirmedAt != null }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    "Months" to months.size.toString(),
                    "Recorded" to recordedCount.toString(),
                    "Total Spent" to "৳${"%,.0f".format(totalSpent)}"
                ).forEach { (label, value) ->
                    Card(Modifier.weight(1f), RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(10.dp)) {
                            Text(label, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(value, style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        items(months) { ym ->
            val data       = uiState.monthDataMap[ym]!!
            val confirmed  = data.confirmedAt != null
            val boughtItems = data.items.filter { it.bought }
            val total      = boughtItems.sumOf { it.boughtPrice ?: it.qty * it.unitPrice }
            val yearParts  = ym.split("-")
            val monthLabel = "${MONTHS[yearParts[1].toInt() - 1]} ${yearParts[0]}"

            Card(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = if (confirmed)
                    androidx.compose.foundation.BorderStroke(1.dp, Emerald600.copy(0.3f)) else null,
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (confirmed) Emerald600.copy(0.12f)
                                        else Color(0xFFF59E0B).copy(0.12f)),
                        Alignment.Center
                    ) {
                        Icon(if (confirmed) Icons.Default.CheckCircle else Icons.Default.ShoppingCart,
                            null, Modifier.size(22.dp),
                            tint = if (confirmed) Emerald600 else Color(0xFFF59E0B))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(monthLabel, style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        Text("${boughtItems.size} items bought",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
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
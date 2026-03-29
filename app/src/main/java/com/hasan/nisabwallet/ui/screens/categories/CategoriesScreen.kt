package com.hasan.nisabwallet.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hasan.nisabwallet.data.model.Category
import com.hasan.nisabwallet.ui.components.*
import com.hasan.nisabwallet.ui.theme.Emerald600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    navController: NavController,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            if (!uiState.showFormSheet) snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = viewModel::showAddSheet,
                containerColor = MaterialTheme.colorScheme.primary,
                shape          = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Default.Add, "Add category", tint = MaterialTheme.colorScheme.onPrimary) }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(Modifier.fillMaxSize().padding(padding)) {

            // ── Tab row ───────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor   = MaterialTheme.colorScheme.surface,
                contentColor     = MaterialTheme.colorScheme.primary
            ) {
                listOf("Income" to Color(0xFF10B981), "Expense" to Color(0xFFEF4444))
                    .forEachIndexed { index, (label, color) ->
                        Tab(
                            selected = uiState.selectedTab == index,
                            onClick  = { viewModel.onTabSelected(index) },
                            text = {
                                Text(
                                    label,
                                    fontWeight = if (uiState.selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                    color      = if (uiState.selectedTab == index) color
                                                 else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            val displayList = if (uiState.selectedTab == 0)
                uiState.incomeCategories else uiState.expenseCategories

            if (displayList.isEmpty()) {
                EmptyState(
                    icon     = Icons.Default.Category,
                    title    = "No categories yet",
                    subtitle = "Tap + to add your first ${if (uiState.selectedTab == 0) "income" else "expense"} category",
                    modifier = Modifier.weight(1f)
                )
            } else {
                // ── Stats strip ───────────────────────────────────────────
                val systemCount = displayList.count { it.isSystem }
                val customCount = displayList.count { !it.isSystem }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBadge("${displayList.size} total",    MaterialTheme.colorScheme.primary)
                    StatBadge("$systemCount system",          Color(0xFF6B7280))
                    StatBadge("$customCount custom",          Emerald600)
                }

                LazyColumn(
                    modifier       = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayList, key = { it.id }) { category ->
                        CategoryItem(
                            category = category,
                            onEdit   = { viewModel.showEditSheet(category) },
                            onDelete = {
                                if (category.isSystem)
                                    viewModel.showDeleteDialog(category) // will show error
                                else
                                    viewModel.showDeleteDialog(category)
                            }
                        )
                    }
                }
            }
        }
    }

    // ── Form sheet ────────────────────────────────────────────────────────────
    if (uiState.showFormSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissSheet,
            sheetState       = sheetState,
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            CategoryFormSheet(
                uiState   = uiState,
                viewModel = viewModel,
                onCancel  = viewModel::dismissSheet
            )
        }
    }

    // ── Delete dialog ─────────────────────────────────────────────────────────
    if (uiState.showDeleteDialog) {
        val cat = uiState.deletingCategory
        if (cat?.isSystem == true) {
            AlertDialog(
                onDismissRequest = viewModel::dismissDeleteDialog,
                title   = { Text("Cannot Delete", fontWeight = FontWeight.SemiBold) },
                text    = { Text("\"${cat.name}\" is a system category required for app features. It cannot be deleted.") },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissDeleteDialog) { Text("OK") }
                },
                shape = RoundedCornerShape(16.dp)
            )
        } else {
            ConfirmDialog(
                title         = "Delete Category",
                message       = "Delete \"${cat?.name}\"? Existing transactions will keep this category label.",
                confirmText   = "Delete",
                isDestructive = true,
                onConfirm     = viewModel::deleteCategory,
                onDismiss     = viewModel::dismissDeleteDialog
            )
        }
    }
}

// ── Category list item ────────────────────────────────────────────────────────
@Composable
private fun CategoryItem(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = runCatching {
        Color(android.graphics.Color.parseColor(category.color))
    }.getOrDefault(Emerald600)

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colour circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment      = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        category.name,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (category.isSystem) {
                        SystemBadge()
                    }
                    if (category.isRiba) {
                        RibaBadge()
                    }
                }
                Text(
                    category.type,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Actions — edit always allowed; delete greyed for system
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit, "Edit",
                    modifier = Modifier.size(16.dp),
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick  = onDelete,
                modifier = Modifier.size(32.dp),
                enabled  = !category.isSystem
            ) {
                Icon(
                    Icons.Default.Delete, "Delete",
                    modifier = Modifier.size(16.dp),
                    tint     = if (category.isSystem)
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    else Color(0xFFEF4444)
                )
            }
        }
    }
}

// ── Badges ────────────────────────────────────────────────────────────────────
@Composable
private fun SystemBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            "SYSTEM",
            style      = MaterialTheme.typography.labelSmall,
            fontSize   = 9.sp,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RibaBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            "RIBA",
            style      = MaterialTheme.typography.labelSmall,
            fontSize   = 9.sp,
            fontWeight = FontWeight.Bold,
            color      = Color(0xFFF59E0B)
        )
    }
}

@Composable
private fun StatBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = color, fontWeight = FontWeight.SemiBold)
    }
}

// ── Category form sheet ───────────────────────────────────────────────────────
@Composable
private fun CategoryFormSheet(
    uiState: CategoriesUiState,
    viewModel: CategoriesViewModel,
    onCancel: () -> Unit
) {
    val isEdit = uiState.editingCategory != null
    val isSystemEdit = uiState.editingCategory?.isSystem == true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ) {
            Text(
                if (isEdit) "Edit Category" else "New Category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Close") }
        }

        if (isSystemEdit) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, null, Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "System category — only the colour can be changed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        uiState.errorMessage?.let { ErrorCard(it) }

        // Name field
        NisabTextField(
            value         = uiState.formName,
            onValueChange = viewModel::onNameChange,
            label         = "Category name",
            leadingIcon   = Icons.Default.Label,
            readOnly      = isSystemEdit,
            isError       = uiState.errorMessage != null && uiState.formName.isBlank()
        )

        // Type selector — hidden for edit
        if (!isEdit) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Income" to Color(0xFF10B981), "Expense" to Color(0xFFEF4444))
                    .forEach { (type, color) ->
                        val selected = uiState.formType == type
                        OutlinedButton(
                            onClick  = { viewModel.onTypeChange(type) },
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) color.copy(0.1f) else Color.Transparent
                            ),
                            border   = androidx.compose.foundation.BorderStroke(
                                1.5.dp, if (selected) color else MaterialTheme.colorScheme.outline
                            ),
                            shape    = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                type,
                                color      = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
            }
        }

        // Colour picker
        CategoryColorPicker(
            selectedColor   = uiState.formColor,
            onColorSelected = viewModel::onColorChange
        )

        // Preview
        CategoryPreview(name = uiState.formName, color = uiState.formColor)

        NisabButton(
            text      = if (isEdit) "Update Category" else "Add Category",
            onClick   = viewModel::saveCategory,
            isLoading = uiState.isSaving
        )
    }
}

@Composable
private fun CategoryColorPicker(selectedColor: String, onColorSelected: (String) -> Unit) {
    val presets = listOf(
        "#10B981","#3B82F6","#8B5CF6","#F59E0B","#EF4444",
        "#06B6D4","#EC4899","#84CC16","#F97316","#6366F1",
        "#14B8A6","#6B7280","#1D4ED8","#7C3AED","#DB2777"
    )

    Column {
        Text(
            "Colour",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(presets) { hex ->
                val color = runCatching {
                    Color(android.graphics.Color.parseColor(hex))
                }.getOrDefault(Emerald600)

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (selectedColor == hex)
                                Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            else Modifier
                        )
                        .clickable { onColorSelected(hex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedColor == hex) {
                        Icon(
                            Icons.Default.Check, null,
                            modifier = Modifier.size(16.dp),
                            tint     = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPreview(name: String, color: String) {
    val accent = runCatching {
        Color(android.graphics.Color.parseColor(color))
    }.getOrDefault(Emerald600)

    if (name.isBlank()) return

    Column {
        Text(
            "Preview",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.take(1).uppercase(),
                    color      = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp
                )
            }
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

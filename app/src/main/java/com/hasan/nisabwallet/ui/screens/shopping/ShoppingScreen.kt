package com.hasan.nisabwallet.ui.screens.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.model.ShoppingCart
import com.hasan.nisabwallet.data.model.ShoppingItem
import com.hasan.nisabwallet.data.repository.ShoppingRepository
import com.hasan.nisabwallet.ui.components.*
import com.hasan.nisabwallet.ui.theme.Emerald600
import com.hasan.nisabwallet.ui.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────
data class ShoppingUiState(
    val carts: List<ShoppingCart>    = emptyList(),
    val isLoading: Boolean           = true,
    val isSaving: Boolean            = false,
    val showAddCartDialog: Boolean   = false,
    val showDeleteDialog: Boolean    = false,
    val deletingCart: ShoppingCart?  = null,
    val newCartName: String          = "",
    val errorMessage: String?        = null,
    val successMessage: String?      = null
)

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val repo: ShoppingRepository,
    private val auth: FirebaseAuth
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ShoppingUiState())
    val uiState = _uiState.asStateFlow()
    private val userId get() = auth.currentUser?.uid ?: ""

    init {
        repo.getCartsFlow(userId)
            .onEach { carts -> _uiState.value = _uiState.value.copy(carts = carts, isLoading = false) }
            .catch { _uiState.value = _uiState.value.copy(isLoading = false) }
            .launchIn(viewModelScope)
    }

    fun showAddDialog()  { _uiState.value = _uiState.value.copy(showAddCartDialog = true, newCartName = "") }
    fun dismissDialog()  { _uiState.value = _uiState.value.copy(showAddCartDialog = false, errorMessage = null) }
    fun showDelete(c: ShoppingCart) { _uiState.value = _uiState.value.copy(showDeleteDialog = true, deletingCart = c) }
    fun dismissDelete()  { _uiState.value = _uiState.value.copy(showDeleteDialog = false, deletingCart = null) }
    fun onCartNameChange(v: String) { _uiState.value = _uiState.value.copy(newCartName = v) }
    fun clearMessages()  { _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null) }

    fun addCart() {
        val name = _uiState.value.newCartName.trim()
        if (name.isBlank()) { _uiState.value = _uiState.value.copy(errorMessage = "Enter list name"); return }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            when (val r = repo.addCart(userId, name)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(isSaving = false, showAddCartDialog = false, successMessage = "\"$name\" created")
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = r.message)
                else -> Unit
            }
        }
    }

    fun deleteCart() {
        val cart = _uiState.value.deletingCart ?: return
        viewModelScope.launch {
            when (val r = repo.deleteCart(userId, cart.id)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(showDeleteDialog = false, deletingCart = null, successMessage = "${cart.name} deleted")
                is Result.Error   -> _uiState.value = _uiState.value.copy(errorMessage = r.message)
                else -> Unit
            }
        }
    }
}

// Cart detail ViewModel
data class CartDetailUiState(
    val cart: ShoppingCart?     = null,
    val items: List<ShoppingItem> = emptyList(),
    val isLoading: Boolean      = true,
    val showAddItemSheet: Boolean = false,
    val editingItem: ShoppingItem? = null,
    val formName: String        = "",
    val formQty: String         = "1",
    val formUnit: String        = "pcs",
    val formPrice: String       = "",
    val formNotes: String       = "",
    val isSaving: Boolean       = false,
    val errorMessage: String?   = null,
    val successMessage: String? = null
)

@HiltViewModel
class CartDetailViewModel @Inject constructor(
    private val repo: ShoppingRepository,
    private val auth: FirebaseAuth
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(CartDetailUiState())
    val uiState = _uiState.asStateFlow()
    private val userId get() = auth.currentUser?.uid ?: ""
    private var cartId: String = ""

    fun init(id: String) {
        if (cartId == id) return
        cartId = id
        repo.getItemsFlow(userId, id)
            .onEach { items -> _uiState.value = _uiState.value.copy(items = items, isLoading = false) }
            .launchIn(viewModelScope)
    }

    fun showAddSheet()  { _uiState.value = _uiState.value.copy(showAddItemSheet = true, editingItem = null, formName = "", formQty = "1", formUnit = "pcs", formPrice = "", formNotes = "", errorMessage = null) }
    fun showEditSheet(i: ShoppingItem) { _uiState.value = _uiState.value.copy(showAddItemSheet = true, editingItem = i, formName = i.name, formQty = i.quantity.toString(), formUnit = i.unit, formPrice = if (i.estimatedPrice > 0) i.estimatedPrice.toString() else "", formNotes = i.notes) }
    fun dismissSheet()  { _uiState.value = _uiState.value.copy(showAddItemSheet = false, editingItem = null, errorMessage = null) }
    fun clearMessages() { _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null) }

    fun onNameChange(v: String)  { _uiState.value = _uiState.value.copy(formName = v) }
    fun onQtyChange(v: String)   { _uiState.value = _uiState.value.copy(formQty = v) }
    fun onUnitChange(v: String)  { _uiState.value = _uiState.value.copy(formUnit = v) }
    fun onPriceChange(v: String) { _uiState.value = _uiState.value.copy(formPrice = v) }
    fun onNotesChange(v: String) { _uiState.value = _uiState.value.copy(formNotes = v) }

    fun saveItem() {
        val s = _uiState.value
        if (s.formName.isBlank()) { _uiState.value = s.copy(errorMessage = "Item name required"); return }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val item = ShoppingItem(name = s.formName.trim(), quantity = s.formQty.toDoubleOrNull() ?: 1.0,
                unit = s.formUnit, estimatedPrice = s.formPrice.toDoubleOrNull() ?: 0.0, notes = s.formNotes.trim())
            val result = if (s.editingItem == null) repo.addItem(userId, cartId, item)
                .let { if (it is Result.Success) Result.Success(Unit) else it as Result<Unit> }
            else repo.updateItem(userId, cartId, s.editingItem.id, item)
            when (result) {
                is Result.Success -> _uiState.value = _uiState.value.copy(isSaving = false, showAddItemSheet = false, successMessage = "Item saved")
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
                else -> Unit
            }
        }
    }

    fun toggleItemStatus(item: ShoppingItem) {
        val newStatus = when (item.status) {
            "pending"   -> "confirmed"
            "confirmed" -> "pending"
            else        -> "pending"
        }
        viewModelScope.launch { repo.updateItemStatus(userId, cartId, item.id, newStatus) }
    }

    fun deleteItem(item: ShoppingItem) {
        viewModelScope.launch { repo.deleteItem(userId, cartId, item.id) }
    }
}

// ── Shopping list screen ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(
    navController: NavController,
    viewModel: ShoppingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Shopping Lists", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showAddDialog, containerColor = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Add, "New list", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) { Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }; return@Scaffold }

        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (uiState.carts.isEmpty()) {
                item { EmptyState(Icons.Default.ShoppingCart, "No shopping lists", "Tap + to create your first list", Modifier.padding(top = 48.dp)) }
            } else {
                items(uiState.carts, key = { it.id }) { cart ->
                    Card(
                        modifier  = Modifier.fillMaxWidth().clickable { navController.navigate("shopping_cart/${cart.id}") },
                        shape     = RoundedCornerShape(14.dp),
                        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer), Alignment.Center) {
                                Icon(Icons.Default.ShoppingCart, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(cart.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("${cart.totalItems} items · ${cart.confirmedItems} confirmed",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (cart.totalAmount > 0) Text("Est. ৳${"%,.0f".format(cart.totalAmount)}", style = MaterialTheme.typography.labelSmall, color = Emerald600)
                            }
                            if (cart.status == "completed") {
                                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Emerald600.copy(0.12f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("DONE", style = MaterialTheme.typography.labelSmall, color = Emerald600, fontWeight = FontWeight.Bold)
                                }
                            }
                            IconButton(onClick = { viewModel.showDelete(cart) }, Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, null, Modifier.size(16.dp), tint = Color(0xFFEF4444))
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (uiState.showAddCartDialog) {
        AlertDialog(onDismissRequest = viewModel::dismissDialog,
            title = { Text("New Shopping List", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    OutlinedTextField(value = uiState.newCartName, onValueChange = viewModel::onCartNameChange,
                        label = { Text("List name") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = { Button(onClick = viewModel::addCart) { if (uiState.isSaving) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp) else Text("Create") } },
            dismissButton = { TextButton(onClick = viewModel::dismissDialog) { Text("Cancel") } },
            shape = RoundedCornerShape(16.dp))
    }

    if (uiState.showDeleteDialog) {
        ConfirmDialog("Delete List", "Delete \"${uiState.deletingCart?.name}\" and all its items?", "Delete", isDestructive = true, onConfirm = viewModel::deleteCart, onDismiss = viewModel::dismissDelete)
    }
}

// ── Cart detail screen ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingCartScreen(
    navController: NavController,
    cartId: String = "",
    viewModel: CartDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(cartId) { viewModel.init(cartId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    val confirmed = uiState.items.count { it.status == "confirmed" }
    val total     = uiState.items.filter { it.status != "skipped" }.sumOf { it.total }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Shopping List", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showAddSheet, containerColor = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Add, "Add item", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Progress
            if (uiState.items.isNotEmpty()) {
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Column {
                            Text("$confirmed/${uiState.items.size} items", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            LinearProgressIndicator(progress = { if (uiState.items.isNotEmpty()) confirmed.toFloat() / uiState.items.size else 0f },
                                modifier = Modifier.width(140.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).padding(top = 4.dp),
                                color = Emerald600, trackColor = MaterialTheme.colorScheme.outline.copy(0.2f))
                        }
                        Text("৳${"%,.0f".format(total)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Emerald600)
                    }
                }
            }

            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (uiState.items.isEmpty()) {
                    item { EmptyState(Icons.Default.AddShoppingCart, "No items", "Tap + to add items to your list", Modifier.padding(top = 32.dp)) }
                } else {
                    items(uiState.items, key = { it.id }) { item ->
                        ShoppingItemRow(item, onToggle = { viewModel.toggleItemStatus(item) }, onEdit = { viewModel.showEditSheet(item) }, onDelete = { viewModel.deleteItem(item) })
                    }
                }
            }
        }
    }

    if (uiState.showAddItemSheet) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissSheet, sheetState = sheetState, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(if (uiState.editingItem == null) "Add Item" else "Edit Item", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = viewModel::dismissSheet) { Icon(Icons.Default.Close, "Close") }
                }
                uiState.errorMessage?.let { ErrorCard(it) }
                NisabTextField(uiState.formName, viewModel::onNameChange, "Item name", leadingIcon = Icons.Default.ShoppingBag)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NisabTextField(uiState.formQty, viewModel::onQtyChange, "Qty", modifier = Modifier.weight(1f), leadingIcon = Icons.Default.Numbers, keyboardType = KeyboardType.Decimal)
                    NisabTextField(uiState.formUnit, viewModel::onUnitChange, "Unit", modifier = Modifier.weight(1f), leadingIcon = Icons.Default.Scale)
                }
                NisabTextField(uiState.formPrice, viewModel::onPriceChange, "Est. price (৳)", leadingIcon = Icons.Default.MonetizationOn, keyboardType = KeyboardType.Decimal)
                NisabTextField(uiState.formNotes, viewModel::onNotesChange, "Notes (optional)", leadingIcon = Icons.Default.Notes)
                NisabButton(if (uiState.editingItem == null) "Add Item" else "Update", viewModel::saveItem, isLoading = uiState.isSaving)
            }
        }
    }
}

@Composable
private fun ShoppingItemRow(item: ShoppingItem, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isDone = item.status == "confirmed"
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDone) Emerald600.copy(0.06f) else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.5.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isDone, onCheckedChange = { onToggle() }, colors = CheckboxDefaults.colors(checkedColor = Emerald600))
            Column(Modifier.weight(1f).padding(start = 6.dp)) {
                Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isDone) MaterialTheme.colorScheme.onSurface.copy(0.5f) else MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${item.quantity} ${item.unit}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (item.displayPrice > 0) Text("৳${"%,.0f".format(item.total)}", style = MaterialTheme.typography.labelSmall, color = Emerald600)
                }
            }
            IconButton(onClick = onEdit,   Modifier.size(28.dp)) { Icon(Icons.Default.Edit,   null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = onDelete, Modifier.size(28.dp)) { Icon(Icons.Default.Delete, null, Modifier.size(14.dp), tint = Color(0xFFEF4444)) }
        }
    }
}

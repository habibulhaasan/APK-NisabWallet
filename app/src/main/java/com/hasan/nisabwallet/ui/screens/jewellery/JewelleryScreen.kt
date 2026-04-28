package com.hasan.nisabwallet.ui.screens.jewellery

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.firebase.FirestorePaths
import com.hasan.nisabwallet.data.firebase.snapshotFlow
import com.hasan.nisabwallet.data.model.Jewellery
import com.hasan.nisabwallet.data.repository.JewelleryRepository
import com.hasan.nisabwallet.data.safeCall
import com.hasan.nisabwallet.ui.components.*
import com.hasan.nisabwallet.ui.components.SimpleDropdown
import com.hasan.nisabwallet.ui.theme.Emerald600
import com.hasan.nisabwallet.ui.viewmodel.BaseViewModel
import com.hasan.nisabwallet.utils.WeightConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────
data class JewelleryUiState(
    val items: List<Jewellery>       = emptyList(),
    val isLoading: Boolean           = true,
    val isSaving: Boolean            = false,
    val showFormSheet: Boolean       = false,
    val showDeleteDialog: Boolean    = false,
    val editingItem: Jewellery?      = null,
    val deletingItem: Jewellery?     = null,
    val errorMessage: String?        = null,
    val successMessage: String?      = null,
    val totalGoldGrams: Double       = 0.0,
    val totalSilverGrams: Double     = 0.0,
    val totalMarketValue: Double     = 0.0,
    // Form
    val formName: String             = "",
    val formMetal: String            = "Gold",
    val formKarat: String            = "22K",
    val formVori: String             = "0",
    val formAna: String              = "0",
    val formRoti: String             = "0",
    val formPoint: String            = "0",
    val formAcquisition: String      = "purchased",
    val formPurchasePrice: String    = "",
    val formPurchaseDate: String     = LocalDate.now().toString(),
    val formDescription: String      = "",
    val formIncludeZakat: Boolean    = true
) {
    val formGrams: Double get() = WeightConverter.toGrams(
        formVori.toIntOrNull() ?: 0, formAna.toIntOrNull() ?: 0,
        formRoti.toIntOrNull() ?: 0, formPoint.toIntOrNull() ?: 0
    )
}

@HiltViewModel
class JewelleryViewModel @Inject constructor(
    private val jewelleryRepo: JewelleryRepository,
    private val auth: FirebaseAuth
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(JewelleryUiState())
    val uiState = _uiState.asStateFlow()
    private val userId get() = auth.currentUser?.uid ?: ""

    init { observeJewellery() }

    private fun observeJewellery() {
        jewelleryRepo.getJewelleryFlow(userId)
            .onEach { items ->
                _uiState.value = _uiState.value.copy(
                    items            = items,
                    totalGoldGrams   = items.filter { it.metal == "Gold" }.sumOf { it.grams },
                    totalSilverGrams = items.filter { it.metal == "Silver" }.sumOf { it.grams },
                    totalMarketValue = items.sumOf { it.marketValue },
                    isLoading        = false
                )
            }
            .catch { _uiState.value = _uiState.value.copy(isLoading = false) }
            .launchIn(viewModelScope)
    }

    fun showAddSheet() {
        _uiState.value = _uiState.value.copy(showFormSheet = true, editingItem = null,
            formName = "", formMetal = "Gold", formKarat = "22K",
            formVori = "0", formAna = "0", formRoti = "0", formPoint = "0",
            formAcquisition = "purchased", formPurchasePrice = "",
            formPurchaseDate = LocalDate.now().toString(), formDescription = "",
            formIncludeZakat = true, errorMessage = null)
    }

    fun showEditSheet(item: Jewellery) {
        _uiState.value = _uiState.value.copy(showFormSheet = true, editingItem = item,
            formName = item.name, formMetal = item.metal, formKarat = item.karat,
            formVori = item.vori.toString(), formAna = item.ana.toString(),
            formRoti = item.roti.toString(), formPoint = item.point.toString(),
            formAcquisition = item.acquisitionType, formPurchasePrice = if (item.purchasePrice > 0) item.purchasePrice.toLong().toString() else "",
            formPurchaseDate = item.purchaseDate, formDescription = item.description,
            formIncludeZakat = item.includeInZakat, errorMessage = null)
    }

    fun dismissSheet()    { _uiState.value = _uiState.value.copy(showFormSheet = false, editingItem = null, errorMessage = null) }
    fun showDeleteDialog(i: Jewellery) { _uiState.value = _uiState.value.copy(showDeleteDialog = true, deletingItem = i) }
    fun dismissDeleteDialog() { _uiState.value = _uiState.value.copy(showDeleteDialog = false, deletingItem = null) }
    fun clearMessages()   { _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null) }

    fun onNameChange(v: String)         { _uiState.value = _uiState.value.copy(formName = v) }
    fun onMetalChange(v: String)        { _uiState.value = _uiState.value.copy(formMetal = v) }
    fun onKaratChange(v: String)        { _uiState.value = _uiState.value.copy(formKarat = v) }
    fun onVoriChange(v: String)         { _uiState.value = _uiState.value.copy(formVori = v.filter { it.isDigit() }) }
    fun onAnaChange(v: String)          { _uiState.value = _uiState.value.copy(formAna = v.filter { it.isDigit() }) }
    fun onRotiChange(v: String)         { _uiState.value = _uiState.value.copy(formRoti = v.filter { it.isDigit() }) }
    fun onPointChange(v: String)        { _uiState.value = _uiState.value.copy(formPoint = v.filter { it.isDigit() }) }
    fun onAcquisitionChange(v: String)  { _uiState.value = _uiState.value.copy(formAcquisition = v) }
    fun onPurchasePriceChange(v: String){ _uiState.value = _uiState.value.copy(formPurchasePrice = v) }
    fun onPurchaseDateChange(v: String) { _uiState.value = _uiState.value.copy(formPurchaseDate = v) }
    fun onDescriptionChange(v: String)  { _uiState.value = _uiState.value.copy(formDescription = v) }
    fun onZakatChange(v: Boolean)       { _uiState.value = _uiState.value.copy(formIncludeZakat = v) }

    fun save() {
        val s = _uiState.value
        if (s.formName.isBlank()) { _uiState.value = s.copy(errorMessage = "Name required"); return }
        val totalWeight = (s.formVori.toIntOrNull() ?: 0) + (s.formAna.toIntOrNull() ?: 0) +
                          (s.formRoti.toIntOrNull() ?: 0) + (s.formPoint.toIntOrNull() ?: 0)
        if (totalWeight == 0) { _uiState.value = s.copy(errorMessage = "Enter weight (at least 1 point)"); return }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            val item = Jewellery(
                name            = s.formName.trim(),
                metal           = s.formMetal,
                karat           = s.formKarat,
                vori            = s.formVori.toIntOrNull() ?: 0,
                ana             = s.formAna.toIntOrNull() ?: 0,
                roti            = s.formRoti.toIntOrNull() ?: 0,
                point           = s.formPoint.toIntOrNull() ?: 0,
                acquisitionType = s.formAcquisition,
                purchasePrice   = s.formPurchasePrice.toDoubleOrNull() ?: 0.0,
                purchaseDate    = s.formPurchaseDate,
                description     = s.formDescription.trim(),
                includeInZakat  = s.formIncludeZakat
            )
            val result = if (s.editingItem == null)
                jewelleryRepo.addJewellery(userId, item)
                    .let { if (it is Result.Success) Result.Success(Unit) else it as Result<Unit> }
            else jewelleryRepo.updateJewellery(userId, s.editingItem.id, item)

            when (result) {
                is Result.Success -> _uiState.value = _uiState.value.copy(isSaving = false, showFormSheet = false,
                    successMessage = if (s.editingItem == null) "Jewellery added" else "Jewellery updated")
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
                else -> Unit
            }
        }
    }

    fun delete() {
        val item = _uiState.value.deletingItem ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            when (val r = jewelleryRepo.deleteJewellery(userId, item.id)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(isSaving = false, showDeleteDialog = false, deletingItem = null, successMessage = "${item.name} deleted")
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = r.message)
                else -> Unit
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JewelleryScreen(
    navController: NavController,
    viewModel: JewelleryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Jewellery", fontWeight = FontWeight.SemiBold) },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        floatingActionButton = { FloatingActionButton(onClick = viewModel::showAddSheet, containerColor = Color(0xFFFBBF24), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.Add, "Add", tint = Color.White) } },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // ── Summary ───────────────────────────────────────────────────
            if (uiState.items.isNotEmpty()) {
                Card(Modifier.fillMaxWidth().padding(16.dp), RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFBBF24).copy(alpha = 0.12f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.4f))) {
                    Row(Modifier.padding(16.dp), Arrangement.SpaceAround) {
                        JewelStat("Gold", "${"%,.2f".format(uiState.totalGoldGrams)}g", Color(0xFFFBBF24))
                        JewelStat("Silver", "${"%,.2f".format(uiState.totalSilverGrams)}g", Color(0xFF9CA3AF))
                        JewelStat("Est. Value", "৳${"%,.0f".format(uiState.totalMarketValue)}", Color(0xFFFBBF24))
                    }
                }
            }

            if (uiState.isLoading) { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }; return@Column }

            if (uiState.items.isEmpty()) {
                EmptyState(Icons.Default.Diamond, "No jewellery recorded", "Tap + to add gold or silver jewellery", Modifier.weight(1f))
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(uiState.items, key = { it.id }) { item ->
                        JewelleryCard(item, onEdit = { viewModel.showEditSheet(item) }, onDelete = { viewModel.showDeleteDialog(item) })
                    }
                }
            }
        }
    }

    if (uiState.showFormSheet) {
        ModalBottomSheet(onDismissRequest = viewModel::dismissSheet, sheetState = sheetState, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            JewelleryFormSheet(uiState, viewModel, viewModel::dismissSheet)
        }
    }
    if (uiState.showDeleteDialog) {
        ConfirmDialog("Delete Jewellery", "Delete \"${uiState.deletingItem?.name}\"?", "Delete", isDestructive = true, onConfirm = viewModel::delete, onDismiss = viewModel::dismissDeleteDialog)
    }
}

@Composable
private fun JewelStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun JewelleryCard(item: Jewellery, onEdit: () -> Unit, onDelete: () -> Unit) {
    val metalColor = if (item.metal == "Gold") Color(0xFFFBBF24) else Color(0xFF9CA3AF)
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(metalColor.copy(0.15f)), Alignment.Center) {
                    Icon(Icons.Default.Diamond, null, Modifier.size(22.dp), tint = metalColor)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MetalBadge("${item.metal} ${item.karat}", metalColor)
                        MetalBadge(item.acquisitionLabel, Color(0xFF6B7280))
                        if (item.includeInZakat) MetalBadge("ZAKAT", Color(0xFF8B5CF6))
                    }
                }
                IconButton(onClick = onEdit, Modifier.size(28.dp)) { Icon(Icons.Default.Edit, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onDelete, Modifier.size(28.dp)) { Icon(Icons.Default.Delete, null, Modifier.size(14.dp), tint = Color(0xFFEF4444)) }
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column {
                    Text("Weight", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(item.weightDisplay, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("${"%,.4f".format(item.grams)}g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (item.marketValue > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Est. Market Value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("৳${"%,.0f".format(item.marketValue)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = metalColor)
                    }
                }
                if (item.purchasePrice > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Purchase Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("৳${"%,.0f".format(item.purchasePrice)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetalBadge(label: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(color.copy(0.12f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun JewelleryFormSheet(uiState: JewelleryUiState, viewModel: JewelleryViewModel, onCancel: () -> Unit) {
    val isEdit = uiState.editingItem != null
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(if (isEdit) "Edit Jewellery" else "Add Jewellery", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Close") }
        }
        uiState.errorMessage?.let { ErrorCard(it) }
        NisabTextField(uiState.formName, viewModel::onNameChange, "Item name / description", leadingIcon = Icons.Default.Diamond)

        // Metal selector
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WeightConverter.METAL_OPTIONS.forEach { metal ->
                val selected = uiState.formMetal == metal
                val color = if (metal == "Gold") Color(0xFFFBBF24) else Color(0xFF9CA3AF)
                OutlinedButton(onClick = { viewModel.onMetalChange(metal) }, Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected) color.copy(0.12f) else Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, if (selected) color else MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(12.dp)) {
                    Text(metal, color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }

        SimpleDropdown("Karat / Purity", uiState.formKarat, WeightConverter.KARAT_OPTIONS.map { it to it }, viewModel::onKaratChange, Icons.Default.Star)

        // Weight input — Vori / Ana / Roti / Point
        Text("Weight", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Triple("Vori", uiState.formVori, viewModel::onVoriChange),
                   Triple("Ana",  uiState.formAna,  viewModel::onAnaChange),
                   Triple("Roti", uiState.formRoti, viewModel::onRotiChange),
                   Triple("Point",uiState.formPoint,viewModel::onPointChange)).forEach { (label, value, onChange) ->
                OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label, fontSize = 11.sp) },
                    modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp))
            }
        }
        if (uiState.formGrams > 0) {
            Text("= ${"%,.4f".format(uiState.formGrams)} grams", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        }

        SimpleDropdown("Acquisition type", uiState.formAcquisition, WeightConverter.ACQUISITION_OPTIONS, viewModel::onAcquisitionChange, Icons.Default.ShoppingBag)
        if (uiState.formAcquisition == "purchased") {
            NisabTextField(uiState.formPurchasePrice, viewModel::onPurchasePriceChange, "Purchase price (৳, optional)", leadingIcon = Icons.Default.MonetizationOn, keyboardType = KeyboardType.Decimal)
            NisabTextField(uiState.formPurchaseDate, viewModel::onPurchaseDateChange, "Purchase date (yyyy-MM-dd)", leadingIcon = Icons.Default.CalendarToday)
        }
        NisabTextField(uiState.formDescription, viewModel::onDescriptionChange, "Notes (optional)", leadingIcon = Icons.Default.Notes, singleLine = false)
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text("Include in Zakat calculation", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("Worn jewellery may be exempt — consult your scholar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = uiState.formIncludeZakat, onCheckedChange = viewModel::onZakatChange)
        }
        NisabButton(if (isEdit) "Update" else "Add Jewellery", viewModel::save, isLoading = uiState.isSaving)
    }
}

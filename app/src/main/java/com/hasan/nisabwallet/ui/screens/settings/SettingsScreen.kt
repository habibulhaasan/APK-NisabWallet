package com.hasan.nisabwallet.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.firebase.FirestorePaths
import com.hasan.nisabwallet.data.safeCall
import com.hasan.nisabwallet.navigation.Screen
import com.hasan.nisabwallet.ui.components.ConfirmDialog
import com.hasan.nisabwallet.ui.components.NisabButton
import com.hasan.nisabwallet.ui.theme.Emerald600
import com.hasan.nisabwallet.ui.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class SettingsUiState(
    val displayName: String    = "",
    val email: String          = "",
    val theme: String          = "system",
    val currency: String       = "BDT",
    val language: String       = "en",
    val dateFormat: String     = "DD/MM/YYYY",
    val isSaving: Boolean      = false,
    val showSignOutDialog: Boolean  = false,
    val showDeleteDialog: Boolean   = false,
    val deleteConfirmText: String   = "",
    val successMessage: String?     = null,
    val errorMessage: String?       = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val user = auth.currentUser
        _uiState.value = _uiState.value.copy(
            displayName = user?.displayName ?: "",
            email       = user?.email ?: ""
        )
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            try {
                val snap = FirestorePaths.settings(db, auth.currentUser?.uid ?: "").get().await()
                snap.documents.firstOrNull()?.let { doc ->
                    _uiState.value = _uiState.value.copy(
                        currency   = doc.getString("currency")   ?: "BDT",
                        language   = doc.getString("language")   ?: "en",
                        theme      = doc.getString("theme")      ?: "system",
                        dateFormat = doc.getString("dateFormat") ?: "DD/MM/YYYY"
                    )
                }
            } catch (e: Exception) { /* use defaults */ }
        }
    }

    fun onThemeChange(v: String)      { _uiState.value = _uiState.value.copy(theme = v);      savePreferences() }
    fun onCurrencyChange(v: String)   { _uiState.value = _uiState.value.copy(currency = v);   savePreferences() }
    fun onLanguageChange(v: String)   { _uiState.value = _uiState.value.copy(language = v);   savePreferences() }
    fun onDateFormatChange(v: String) { _uiState.value = _uiState.value.copy(dateFormat = v); savePreferences() }
    fun onDeleteConfirmChange(v: String) { _uiState.value = _uiState.value.copy(deleteConfirmText = v) }
    fun showSignOutDialog()  { _uiState.value = _uiState.value.copy(showSignOutDialog = true) }
    fun dismissSignOut()     { _uiState.value = _uiState.value.copy(showSignOutDialog = false) }
    fun showDeleteDialog()   { _uiState.value = _uiState.value.copy(showDeleteDialog = true, deleteConfirmText = "") }
    fun dismissDelete()      { _uiState.value = _uiState.value.copy(showDeleteDialog = false) }
    fun clearMessages()      { _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null) }

    fun signOut() { auth.signOut() }

    private fun savePreferences() {
        val s   = _uiState.value
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val colRef = FirestorePaths.settings(db, uid)
            val snap   = colRef.get().await()
            val data   = mapOf("currency" to s.currency, "language" to s.language,
                "theme" to s.theme, "dateFormat" to s.dateFormat)
            if (snap.isEmpty) colRef.add(data).await()
            else snap.documents.first().reference.update(data).await()
        }
    }

    fun deleteAccount() {
        if (_uiState.value.deleteConfirmText != "DELETE") {
            _uiState.value = _uiState.value.copy(errorMessage = "Type DELETE to confirm"); return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val result: Result<Unit> = safeCall {
                auth.currentUser?.delete()?.await() ?: error("Not logged in")
            }
            when (result) {
                is Result.Success -> auth.signOut()
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
                else -> Unit
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle sign-out navigation
    LaunchedEffect(Unit) {
        // Firebase auth listener in AuthViewModel handles navigation automatically
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {

            // ── Profile card ──────────────────────────────────────────────
            Card(Modifier.fillMaxWidth().padding(16.dp), RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(52.dp).clip(CircleShape).then(Modifier.let { _ ->
                        Modifier.then(Modifier) }), Alignment.Center) {
                        Surface(Modifier.size(52.dp), shape = CircleShape, color = Emerald600) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(uiState.displayName.take(1).uppercase().ifBlank { "U" }, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Column {
                        Text(uiState.displayName.ifBlank { "User" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(uiState.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── Preferences ───────────────────────────────────────────────
            SettingsSection("Preferences") {
                SettingsDropdownRow("Currency", uiState.currency,
                    listOf("BDT" to "BDT (৳)", "USD" to "USD ($)", "EUR" to "EUR (€)", "GBP" to "GBP (£)", "INR" to "INR (₹)", "SAR" to "SAR", "AED" to "AED"),
                    Icons.Default.CurrencyExchange, viewModel::onCurrencyChange)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.1f))
                SettingsDropdownRow("Language", uiState.language,
                    listOf("en" to "English", "bn" to "বাংলা", "ar" to "العربية"),
                    Icons.Default.Language, viewModel::onLanguageChange)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.1f))
                SettingsDropdownRow("Date format", uiState.dateFormat,
                    listOf("DD/MM/YYYY" to "DD/MM/YYYY", "MM/DD/YYYY" to "MM/DD/YYYY", "YYYY-MM-DD" to "YYYY-MM-DD"),
                    Icons.Default.DateRange, viewModel::onDateFormatChange)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.1f))
                SettingsDropdownRow("Theme", uiState.theme,
                    listOf("system" to "System default", "light" to "Light", "dark" to "Dark"),
                    Icons.Default.Palette, viewModel::onThemeChange)
            }

            // ── About ─────────────────────────────────────────────────────
            SettingsSection("About") {
                SettingsInfoRow("App Version", "1.0.0", Icons.Default.Info)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.1f))
                SettingsInfoRow("Package", "com.hasan.nisabwallet", Icons.Default.Android)
            }

            // ── Account actions ───────────────────────────────────────────
            SettingsSection("Account") {
                SettingsActionRow("Sign out", Icons.AutoMirrored.Filled.ExitToApp, Color(0xFFF59E0B), viewModel::showSignOutDialog)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.1f))
                SettingsActionRow("Delete account", Icons.Default.DeleteForever, Color(0xFFEF4444), viewModel::showDeleteDialog)
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (uiState.showSignOutDialog) {
        ConfirmDialog("Sign Out", "Are you sure you want to sign out?", "Sign Out",
            isDestructive = false, onConfirm = viewModel::signOut, onDismiss = viewModel::dismissSignOut)
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(onDismissRequest = viewModel::dismissDelete,
            title = { Text("Delete Account", fontWeight = FontWeight.SemiBold, color = Color(0xFFEF4444)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("This permanently deletes your account and all data. This action cannot be undone.", style = MaterialTheme.typography.bodyMedium)
                    uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    OutlinedTextField(value = uiState.deleteConfirmText, onValueChange = viewModel::onDeleteConfirmChange,
                        label = { Text("Type DELETE to confirm") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                Button(onClick = viewModel::deleteAccount,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    enabled = uiState.deleteConfirmText == "DELETE" && !uiState.isSaving) {
                    if (uiState.isSaving) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("Delete Account")
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissDelete) { Text("Cancel") } },
            shape = RoundedCornerShape(16.dp))
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
            Column { content() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDropdownRow(label: String, selected: String, options: List<Pair<String, String>>, icon: ImageVector, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == selected }?.second ?: selected
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        Row(Modifier.fillMaxWidth().menuAnchor().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(selectedLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, lbl) ->
                DropdownMenuItem(text = { Text(lbl) }, onClick = { onSelect(key); expanded = false },
                    trailingIcon = if (key == selected) { { Icon(Icons.Default.Check, null, Modifier.size(14.dp), tint = Emerald600) } } else null)
            }
        }
    }
}

@Composable
private fun SettingsInfoRow(label: String, value: String, icon: ImageVector) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsActionRow(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(18.dp), tint = color)
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.weight(1f))
        }
    }
}

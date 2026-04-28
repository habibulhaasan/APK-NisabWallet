package com.hasan.nisabwallet.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// ── Primary button ────────────────────────────────────────────────────────────
@Composable
fun NisabButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary
) {
    Button(
        onClick  = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled && !isLoading,
        shape   = RoundedCornerShape(12.dp),
        colors  = ButtonDefaults.buttonColors(containerColor = containerColor)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color    = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Outlined text field ───────────────────────────────────────────────────────
@Composable
fun NisabTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: androidx.compose.ui.text.input.ImeAction = androidx.compose.ui.text.input.ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    placeholder: String = ""
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label) },
        modifier      = modifier.fillMaxWidth(),
        singleLine    = singleLine,
        readOnly      = readOnly,
        isError       = isError,
        shape         = RoundedCornerShape(12.dp),
        leadingIcon   = leadingIcon?.let { icon ->
            { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) }
        },
        trailingIcon  = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility
                                      else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else trailingIcon,
        visualTransformation = if (isPassword && !passwordVisible)
            PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
            imeAction    = imeAction
        ),
        keyboardActions = keyboardActions,
        placeholder     = if (placeholder.isNotEmpty()) { { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) } } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            errorBorderColor     = MaterialTheme.colorScheme.error
        )
    )
}

// ── Error message card ────────────────────────────────────────────────────────
@Composable
fun ErrorCard(message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text     = message,
            color    = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp),
            style    = MaterialTheme.typography.bodyMedium
        )
    }
}

// ── Loading overlay ───────────────────────────────────────────────────────────
@Composable
fun LoadingOverlay() {
    Box(
        modifier        = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

// ── Confirm dialog ────────────────────────────────────────────────────────────
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text(title, fontWeight = FontWeight.SemiBold) },
        text    = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors  = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) MaterialTheme.colorScheme.error
                                     else MaterialTheme.colorScheme.primary
                )
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText) }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

// ── Screen top bar ────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NisabTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        },
        navigationIcon = onBack?.let {
            {
                IconButton(onClick = it) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        } ?: {},
        actions = actions,
        colors  = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

// ── Amount display ────────────────────────────────────────────────────────────
@Composable
fun AmountText(
    amount: Double,
    modifier: Modifier = Modifier,
    currencySymbol: String = "৳",
    positive: Boolean? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.SemiBold
) {
    val color = when (positive) {
        true  -> Color(0xFF10B981)
        false -> Color(0xFFEF4444)
        null  -> MaterialTheme.colorScheme.onSurface
    }
    Text(
        text       = "$currencySymbol${"%,.0f".format(amount)}",
        color      = color,
        fontSize   = fontSize,
        fontWeight = fontWeight,
        modifier   = modifier
    )
}

// ── Section header ────────────────────────────────────────────────────────────
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.titleMedium,
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

// ── Empty state ───────────────────────────────────────────────────────────────
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier         = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector  = icon,
            contentDescription = null,
            modifier     = Modifier.size(64.dp),
            tint         = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Text(title,    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium,  color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        action?.invoke()
    }
}

// Add this to the BOTTOM of NisabComponents.kt
// (keep the existing private one in TransactionsScreen.kt temporarily, 
//  or remove it — both work since Kotlin allows same name in different scopes)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDropdown(
    label: String,
    selectedId: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == selectedId }?.second ?: ""

    ExposedDropdownMenuBox(
        expanded        = expanded,
        onExpandedChange = { expanded = it },
        modifier        = modifier
    ) {
        OutlinedTextField(
            value         = selectedLabel,
            onValueChange = {},
            readOnly      = true,
            label         = { Text(label) },
            leadingIcon   = leadingIcon?.let { { Icon(it, null, Modifier.size(20.dp)) } },
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier      = Modifier.fillMaxWidth().menuAnchor(),
            shape         = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded        = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text    = { Text("No options available",
                        color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = {}
                )
            } else {
                options.forEach { (id, name) ->
                    DropdownMenuItem(
                        text         = { Text(name) },
                        onClick      = { onSelected(id); expanded = false },
                        trailingIcon = if (id == selectedId) {
                            { Icon(Icons.Default.Check, null, Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary) }
                        } else null
                    )
                }
            }
        }
    }
}
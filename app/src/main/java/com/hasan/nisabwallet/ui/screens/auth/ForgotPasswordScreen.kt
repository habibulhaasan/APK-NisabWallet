package com.hasan.nisabwallet.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hasan.nisabwallet.ui.components.*
import com.hasan.nisabwallet.ui.theme.Emerald600

@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            NisabTopBar(title = "Reset Password", onBack = { navController.popBackStack() })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (uiState.emailSent) {
                // ── Success state ─────────────────────────────────────────
                Icon(
                    imageVector  = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint     = Emerald600,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Check your email",
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "We've sent a password reset link to ${uiState.email}",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(32.dp))
                NisabButton(
                    text    = "Back to Sign In",
                    onClick = { navController.popBackStack() }
                )
            } else {
                // ── Input state ───────────────────────────────────────────
                Text(
                    "Enter the email address associated with your account and we'll send you a link to reset your password.",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))

                uiState.errorMessage?.let { ErrorCard(it) }

                if (uiState.errorMessage != null) Spacer(Modifier.height(12.dp))

                NisabTextField(
                    value         = uiState.email,
                    onValueChange = viewModel::onEmailChange,
                    label         = "Email address",
                    leadingIcon   = Icons.Default.Email,
                    keyboardType  = KeyboardType.Email,
                    imeAction     = ImeAction.Done
                )
                Spacer(Modifier.height(20.dp))
                NisabButton(
                    text      = "Send Reset Link",
                    onClick   = viewModel::sendReset,
                    isLoading = uiState.isLoading
                )
            }
        }
    }
}

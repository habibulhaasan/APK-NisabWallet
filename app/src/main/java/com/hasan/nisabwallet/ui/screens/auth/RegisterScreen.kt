package com.hasan.nisabwallet.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hasan.nisabwallet.navigation.Screen
import com.hasan.nisabwallet.ui.components.*
import com.hasan.nisabwallet.ui.theme.Emerald600

@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.registerSuccess) {
        if (uiState.registerSuccess) {
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Emerald600),
                contentAlignment = Alignment.Center
            ) {
                Text("৳", color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Create Account",
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Start managing your finances the halal way",
                style  = MaterialTheme.typography.bodyMedium,
                color  = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(20.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    uiState.errorMessage?.let { ErrorCard(it) }

                    NisabTextField(
                        value         = uiState.displayName,
                        onValueChange = viewModel::onDisplayNameChange,
                        label         = "Full name",
                        leadingIcon   = Icons.Default.Person,
                        imeAction     = ImeAction.Next
                    )
                    NisabTextField(
                        value         = uiState.email,
                        onValueChange = viewModel::onEmailChange,
                        label         = "Email address",
                        leadingIcon   = Icons.Default.Email,
                        keyboardType  = KeyboardType.Email,
                        imeAction     = ImeAction.Next
                    )
                    NisabTextField(
                        value         = uiState.password,
                        onValueChange = viewModel::onPasswordChange,
                        label         = "Password (min 6 characters)",
                        leadingIcon   = Icons.Default.Lock,
                        isPassword    = true,
                        imeAction     = ImeAction.Next
                    )
                    NisabTextField(
                        value         = uiState.confirmPassword,
                        onValueChange = viewModel::onConfirmPasswordChange,
                        label         = "Confirm password",
                        leadingIcon   = Icons.Default.Lock,
                        isPassword    = true,
                        imeAction     = ImeAction.Done
                    )

                    NisabButton(
                        text      = "Create Account",
                        onClick   = viewModel::register,
                        isLoading = uiState.isLoading
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text       = "Sign In",
                    color      = Emerald600,
                    fontWeight = FontWeight.SemiBold,
                    style      = MaterialTheme.typography.bodyMedium,
                    modifier   = Modifier.clickable { navController.popBackStack() }
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

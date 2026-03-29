package com.hasan.nisabwallet.ui.screens.tax

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.hasan.nisabwallet.ui.components.NisabTopBar

@Composable
fun TaxScreen(navController: NavController) {
    Scaffold(
        topBar = { NisabTopBar(title = "Tax File", onBack = { navController.popBackStack() }) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("Tax File — coming soon")
        }
    }
}

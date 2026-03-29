package com.hasan.nisabwallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.hasan.nisabwallet.navigation.NisabNavGraph
import com.hasan.nisabwallet.ui.theme.NisabWalletTheme
import com.hasan.nisabwallet.ui.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val authViewModel: AuthViewModel = hiltViewModel()
            val authState by authViewModel.authState.collectAsState()

            // Keep splash visible while auth state is loading
            splashScreen.setKeepOnScreenCondition {
                authState.isLoading
            }

            NisabWalletTheme {
                NisabNavGraph(authState = authState)
            }
        }
    }
}

package com.hasan.nisabwallet.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Brand colours ────────────────────────────────────────────────────────────
// Primary: Emerald green (Islamic finance theme)
val Emerald50  = Color(0xFFECFDF5)
val Emerald100 = Color(0xFFD1FAE5)
val Emerald500 = Color(0xFF10B981)
val Emerald600 = Color(0xFF059669)
val Emerald700 = Color(0xFF047857)
val Emerald900 = Color(0xFF064E3B)

// Accent: Warm gold (wealth / Zakat)
val Gold400    = Color(0xFFFBBF24)
val Gold600    = Color(0xFFD97706)

// Semantic
val IncomeGreen  = Color(0xFF10B981)
val ExpenseRed   = Color(0xFFEF4444)
val RibaAmber    = Color(0xFFF59E0B)
val ZakatEmerald = Color(0xFF059669)

// Neutrals
val Gray50  = Color(0xFFF9FAFB)
val Gray100 = Color(0xFFF3F4F6)
val Gray200 = Color(0xFFE5E7EB)
val Gray400 = Color(0xFF9CA3AF)
val Gray600 = Color(0xFF4B5563)
val Gray700 = Color(0xFF374151)
val Gray800 = Color(0xFF1F2937)
val Gray900 = Color(0xFF111827)

// ── Light colour scheme ───────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary          = Emerald600,
    onPrimary        = Color.White,
    primaryContainer = Emerald50,
    onPrimaryContainer = Emerald900,
    secondary        = Gold600,
    onSecondary      = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    background       = Gray50,
    onBackground     = Gray900,
    surface          = Color.White,
    onSurface        = Gray900,
    surfaceVariant   = Gray100,
    onSurfaceVariant = Gray600,
    outline          = Gray200,
    error            = ExpenseRed,
    onError          = Color.White
)

// ── Dark colour scheme ────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary          = Emerald500,
    onPrimary        = Emerald900,
    primaryContainer = Emerald700,
    onPrimaryContainer = Emerald50,
    secondary        = Gold400,
    onSecondary      = Color(0xFF78350F),
    secondaryContainer = Color(0xFF92400E),
    onSecondaryContainer = Color(0xFFFDE68A),
    background       = Gray900,
    onBackground     = Gray100,
    surface          = Gray800,
    onSurface        = Gray100,
    surfaceVariant   = Gray700,
    onSurfaceVariant = Gray400,
    outline          = Gray600,
    error            = Color(0xFFFCA5A5),
    onError          = Color(0xFF7F1D1D)
)

@Composable
fun NisabWalletTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled — use brand colors consistently
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    // Set status bar color
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = NisabTypography,
        content     = content
    )
}

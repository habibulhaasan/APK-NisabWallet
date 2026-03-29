package com.hasan.nisabwallet.utils

import java.text.NumberFormat
import java.util.Locale

/**
 * Central currency formatting utility.
 * Primary currency: BDT (Bangladeshi Taka) with ৳ symbol.
 * Mirrors the web app's formatAmount() from useSettings.js.
 */
object CurrencyFormatter {

    /** Format as ৳1,23,456 (BDT style) */
    fun formatBDT(amount: Double): String =
        "৳${"%,.0f".format(amount)}"

    /** Format with explicit symbol */
    fun format(amount: Double, symbol: String = "৳"): String =
        "$symbol${"%,.0f".format(amount)}"

    /** Format with 2 decimal places */
    fun formatDetailed(amount: Double, symbol: String = "৳"): String =
        "$symbol${"%,.2f".format(amount)}"

    /** Format as signed — +৳500 or -৳200 */
    fun formatSigned(amount: Double, symbol: String = "৳"): String =
        "${if (amount >= 0) "+" else ""}$symbol${"%,.0f".format(kotlin.math.abs(amount))}"

    /** Short form: ৳1.2K, ৳1.5M */
    fun formatShort(amount: Double, symbol: String = "৳"): String = when {
        amount >= 1_000_000 -> "$symbol${"%.1f".format(amount / 1_000_000)}M"
        amount >= 1_000     -> "$symbol${"%.1f".format(amount / 1_000)}K"
        else                -> "$symbol${"%,.0f".format(amount)}"
    }
}

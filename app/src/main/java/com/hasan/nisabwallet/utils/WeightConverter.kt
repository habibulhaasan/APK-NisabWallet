package com.hasan.nisabwallet.utils

/**
 * Bangladeshi jewellery weight system.
 * Android port of src/lib/jewelleryCollections.js
 *
 * 1 Vori = 16 Ana = 96 Roti = 960 Point = 11.664 grams
 */
object WeightConverter {
    const val GRAMS_PER_VORI  = 11.664
    const val ANA_PER_VORI    = 16
    const val ROTI_PER_ANA    = 6
    const val POINT_PER_ROTI  = 10
    const val ROTI_PER_VORI   = 96
    const val POINT_PER_VORI  = 960

    val KARAT_PURITY = mapOf(
        "24K"         to 1.0000,
        "22K"         to 0.9167,
        "21K"         to 0.8750,
        "18K"         to 0.7500,
        "Traditional" to 0.7083
    )

    val KARAT_OPTIONS = listOf("22K", "21K", "18K", "Traditional", "24K")
    val METAL_OPTIONS = listOf("Gold", "Silver")
    val ACQUISITION_OPTIONS = listOf("purchased" to "Purchased", "gift" to "Gift",
        "inherited" to "Inherited", "other" to "Other")

    fun toGrams(vori: Int, ana: Int, roti: Int, point: Int): Double {
        val totalPoints = vori.toLong() * POINT_PER_VORI +
                          ana.toLong()  * ROTI_PER_ANA  * POINT_PER_ROTI +
                          roti.toLong() * POINT_PER_ROTI +
                          point.toLong()
        return (totalPoints.toDouble() / POINT_PER_VORI * GRAMS_PER_VORI)
            .let { "%.4f".format(it).toDouble() }
    }

    data class Weight(val vori: Int, val ana: Int, val roti: Int, val point: Int)

    fun fromGrams(grams: Double): Weight {
        val totalPoints = Math.round(grams / GRAMS_PER_VORI * POINT_PER_VORI)
        val vori  = (totalPoints / POINT_PER_VORI).toInt()
        val rem1  = totalPoints % POINT_PER_VORI
        val ana   = (rem1 / (POINT_PER_ROTI * ROTI_PER_ANA)).toInt()
        val rem2  = rem1 % (POINT_PER_ROTI * ROTI_PER_ANA)
        val roti  = (rem2 / POINT_PER_ROTI).toInt()
        val point = (rem2 % POINT_PER_ROTI).toInt()
        return Weight(vori, ana, roti, point)
    }

    /**
     * Calculate market value given grams, karat, metal, and live prices.
     * prices map: "gold_22k_per_gram", "gold_21k_per_gram", etc.
     */
    fun calcMarketValue(grams: Double, karat: String, metal: String, pricePerGram: Double): Double {
        if (grams <= 0 || pricePerGram <= 0) return 0.0
        return Math.round(pricePerGram * grams).toDouble()
    }

    /** Apply maker/resale deduction (default 15%) */
    fun applyDeduction(value: Double, deductionPct: Double = 15.0): Double =
        Math.round(value * (1 - deductionPct / 100)).toDouble()
}

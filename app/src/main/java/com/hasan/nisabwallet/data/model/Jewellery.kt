package com.hasan.nisabwallet.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Mirrors web app jewelleryCollections.js
 * Weight system: 1 Vori = 16 Ana = 96 Roti = 960 Point = 11.664 grams
 */
data class Jewellery(
    @DocumentId
    val id: String              = "",
    val jewelleryId: String     = "",
    val name: String            = "",
    val metal: String           = "Gold",     // "Gold" | "Silver"
    val karat: String           = "22K",      // "22K"|"21K"|"18K"|"Traditional"|"24K"
    val vori: Int               = 0,
    val ana: Int                = 0,
    val roti: Int               = 0,
    val point: Int              = 0,
    val grams: Double           = 0.0,        // computed from vori/ana/roti/point
    val acquisitionType: String = "purchased",// "purchased"|"gift"|"inherited"|"other"
    val purchasePrice: Double   = 0.0,
    val purchaseDate: String    = "",
    val marketValue: Double     = 0.0,        // last calculated market value
    val description: String     = "",
    val includeInZakat: Boolean = true,
    @ServerTimestamp
    val createdAt: Date?        = null,
    @ServerTimestamp
    val updatedAt: Date?        = null
) {
    val weightDisplay: String get() {
        val parts = mutableListOf<String>()
        if (vori > 0)  parts.add("$vori Vori")
        if (ana > 0)   parts.add("$ana Ana")
        if (roti > 0)  parts.add("$roti Roti")
        if (point > 0) parts.add("$point Point")
        return if (parts.isEmpty()) "0" else parts.joinToString(" ")
    }

    val acquisitionLabel: String get() = when (acquisitionType) {
        "purchased" -> "Purchased"
        "gift"      -> "Gift"
        "inherited" -> "Inherited"
        else        -> "Other"
    }
}

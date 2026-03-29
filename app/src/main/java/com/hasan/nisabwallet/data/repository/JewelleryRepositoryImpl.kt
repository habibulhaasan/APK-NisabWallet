package com.hasan.nisabwallet.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.firebase.FirestorePaths
import com.hasan.nisabwallet.data.firebase.snapshotFlow
import com.hasan.nisabwallet.data.model.Jewellery
import com.hasan.nisabwallet.data.safeCall
import com.hasan.nisabwallet.utils.WeightConverter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

interface JewelleryRepository {
    fun getJewelleryFlow(userId: String): Flow<List<Jewellery>>
    suspend fun addJewellery(userId: String, item: Jewellery): Result<String>
    suspend fun updateJewellery(userId: String, docId: String, item: Jewellery): Result<Unit>
    suspend fun deleteJewellery(userId: String, docId: String): Result<Unit>
    suspend fun updateMarketValue(userId: String, docId: String, marketValue: Double): Result<Unit>
}

class JewelleryRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : JewelleryRepository {

    override fun getJewelleryFlow(userId: String): Flow<List<Jewellery>> =
        FirestorePaths.jewellery(db, userId)
            .snapshotFlow()
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(Jewellery::class.java)?.copy(id = it.id) }
                    .sortedByDescending { it.createdAt }
            }

    override suspend fun addJewellery(userId: String, item: Jewellery): Result<String> = safeCall {
        val grams = WeightConverter.toGrams(item.vori, item.ana, item.roti, item.point)
        FirestorePaths.jewellery(db, userId).add(mapOf(
            "jewelleryId"    to UUID.randomUUID().toString(),
            "name"           to item.name,
            "metal"          to item.metal,
            "karat"          to item.karat,
            "vori"           to item.vori,
            "ana"            to item.ana,
            "roti"           to item.roti,
            "point"          to item.point,
            "grams"          to grams,
            "acquisitionType" to item.acquisitionType,
            "purchasePrice"  to item.purchasePrice,
            "purchaseDate"   to item.purchaseDate,
            "marketValue"    to item.marketValue,
            "description"    to item.description,
            "includeInZakat" to item.includeInZakat,
            "createdAt"      to FieldValue.serverTimestamp(),
            "updatedAt"      to FieldValue.serverTimestamp()
        )).await().id
    }

    override suspend fun updateJewellery(userId: String, docId: String, item: Jewellery): Result<Unit> = safeCall {
        val grams = WeightConverter.toGrams(item.vori, item.ana, item.roti, item.point)
        FirestorePaths.jewellery(db, userId).document(docId).update(mapOf(
            "name"            to item.name,
            "metal"           to item.metal,
            "karat"           to item.karat,
            "vori"            to item.vori,
            "ana"             to item.ana,
            "roti"            to item.roti,
            "point"           to item.point,
            "grams"           to grams,
            "acquisitionType" to item.acquisitionType,
            "purchasePrice"   to item.purchasePrice,
            "purchaseDate"    to item.purchaseDate,
            "description"     to item.description,
            "includeInZakat"  to item.includeInZakat,
            "updatedAt"       to FieldValue.serverTimestamp()
        )).await()
    }

    override suspend fun deleteJewellery(userId: String, docId: String): Result<Unit> = safeCall {
        FirestorePaths.jewellery(db, userId).document(docId).delete().await()
    }

    override suspend fun updateMarketValue(userId: String, docId: String, marketValue: Double): Result<Unit> = safeCall {
        FirestorePaths.jewellery(db, userId).document(docId)
            .update("marketValue", marketValue, "updatedAt", FieldValue.serverTimestamp()).await()
    }
}

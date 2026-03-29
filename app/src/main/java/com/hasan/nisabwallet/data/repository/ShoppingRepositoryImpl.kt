package com.hasan.nisabwallet.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.firebase.FirestorePaths
import com.hasan.nisabwallet.data.firebase.snapshotFlow
import com.hasan.nisabwallet.data.model.ShoppingCart
import com.hasan.nisabwallet.data.model.ShoppingItem
import com.hasan.nisabwallet.data.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

interface ShoppingRepository {
    fun getCartsFlow(userId: String): Flow<List<ShoppingCart>>
    fun getItemsFlow(userId: String, cartId: String): Flow<List<ShoppingItem>>
    suspend fun addCart(userId: String, name: String): Result<String>
    suspend fun updateCart(userId: String, cartId: String, name: String): Result<Unit>
    suspend fun deleteCart(userId: String, cartId: String): Result<Unit>
    suspend fun addItem(userId: String, cartId: String, item: ShoppingItem): Result<String>
    suspend fun updateItem(userId: String, cartId: String, itemId: String, item: ShoppingItem): Result<Unit>
    suspend fun updateItemStatus(userId: String, cartId: String, itemId: String, status: String): Result<Unit>
    suspend fun deleteItem(userId: String, cartId: String, itemId: String): Result<Unit>
    suspend fun completeCart(userId: String, cartId: String): Result<Unit>
}

class ShoppingRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : ShoppingRepository {

    override fun getCartsFlow(userId: String): Flow<List<ShoppingCart>> =
        FirestorePaths.shoppingCarts(db, userId).snapshotFlow()
            .map { snap -> snap.documents.mapNotNull { it.toObject(ShoppingCart::class.java)?.copy(id = it.id) }.sortedByDescending { it.createdAt } }

    override fun getItemsFlow(userId: String, cartId: String): Flow<List<ShoppingItem>> =
        FirestorePaths.shoppingItems(db, userId, cartId).snapshotFlow()
            .map { snap -> snap.documents.mapNotNull { it.toObject(ShoppingItem::class.java)?.copy(id = it.id) }.sortedBy { it.status } }

    override suspend fun addCart(userId: String, name: String): Result<String> = safeCall {
        FirestorePaths.shoppingCarts(db, userId).add(mapOf(
            "cartId" to UUID.randomUUID().toString(), "name" to name,
            "status" to "draft", "totalAmount" to 0.0, "totalItems" to 0,
            "confirmedItems" to 0, "pendingItems" to 0,
            "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
        )).await().id
    }

    override suspend fun updateCart(userId: String, cartId: String, name: String): Result<Unit> = safeCall {
        FirestorePaths.shoppingCarts(db, userId).document(cartId)
            .update("name", name, "updatedAt", FieldValue.serverTimestamp()).await()
    }

    override suspend fun deleteCart(userId: String, cartId: String): Result<Unit> = safeCall {
        // Delete all items first
        val items = FirestorePaths.shoppingItems(db, userId, cartId).get().await()
        val batch = db.batch()
        items.documents.forEach { batch.delete(it.reference) }
        batch.delete(FirestorePaths.shoppingCarts(db, userId).document(cartId))
        batch.commit().await()
    }

    override suspend fun addItem(userId: String, cartId: String, item: ShoppingItem): Result<String> = safeCall {
        val ref = FirestorePaths.shoppingItems(db, userId, cartId).add(mapOf(
            "name" to item.name, "quantity" to item.quantity, "unit" to item.unit,
            "estimatedPrice" to item.estimatedPrice, "actualPrice" to 0.0,
            "category" to item.category, "status" to "pending", "notes" to item.notes,
            "createdAt" to FieldValue.serverTimestamp()
        )).await()
        updateCartStats(userId, cartId)
        ref.id
    }

    override suspend fun updateItem(userId: String, cartId: String, itemId: String, item: ShoppingItem): Result<Unit> = safeCall {
        FirestorePaths.shoppingItems(db, userId, cartId).document(itemId).update(mapOf(
            "name" to item.name, "quantity" to item.quantity, "unit" to item.unit,
            "estimatedPrice" to item.estimatedPrice, "actualPrice" to item.actualPrice,
            "category" to item.category, "notes" to item.notes
        )).await()
        updateCartStats(userId, cartId)
    }

    override suspend fun updateItemStatus(userId: String, cartId: String, itemId: String, status: String): Result<Unit> = safeCall {
        FirestorePaths.shoppingItems(db, userId, cartId).document(itemId).update("status", status).await()
        updateCartStats(userId, cartId)
    }

    override suspend fun deleteItem(userId: String, cartId: String, itemId: String): Result<Unit> = safeCall {
        FirestorePaths.shoppingItems(db, userId, cartId).document(itemId).delete().await()
        updateCartStats(userId, cartId)
    }

    override suspend fun completeCart(userId: String, cartId: String): Result<Unit> = safeCall {
        FirestorePaths.shoppingCarts(db, userId).document(cartId)
            .update("status", "completed", "updatedAt", FieldValue.serverTimestamp()).await()
    }

    private suspend fun updateCartStats(userId: String, cartId: String) {
        val items = FirestorePaths.shoppingItems(db, userId, cartId).get().await()
        val all   = items.documents.mapNotNull { it.toObject(ShoppingItem::class.java) }
        val confirmed = all.count { it.status == "confirmed" }
        val pending   = all.count { it.status == "pending" }
        val total     = all.filter { it.status != "skipped" }.sumOf { it.total }
        FirestorePaths.shoppingCarts(db, userId).document(cartId).update(mapOf(
            "totalItems" to all.size, "confirmedItems" to confirmed,
            "pendingItems" to pending, "totalAmount" to total,
            "updatedAt" to FieldValue.serverTimestamp()
        )).await()
    }
}

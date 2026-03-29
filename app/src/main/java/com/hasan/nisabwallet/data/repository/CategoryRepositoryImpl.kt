package com.hasan.nisabwallet.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.firebase.FirestorePaths
import com.hasan.nisabwallet.data.firebase.snapshotFlow
import com.hasan.nisabwallet.data.model.Category
import com.hasan.nisabwallet.data.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : CategoryRepository {

    override fun getCategoriesFlow(userId: String): Flow<List<Category>> =
        FirestorePaths.categories(db, userId)
            .snapshotFlow()
            .map { snapshot ->
                snapshot.documents
                    .mapNotNull { doc -> doc.toObject(Category::class.java)?.copy(id = doc.id) }
                    .sortedWith(compareBy({ it.type }, { it.name }))
            }

    override suspend fun getCategories(userId: String): Result<List<Category>> = safeCall {
        FirestorePaths.categories(db, userId).get().await()
            .documents
            .mapNotNull { doc -> doc.toObject(Category::class.java)?.copy(id = doc.id) }
            .sortedWith(compareBy({ it.type }, { it.name }))
    }

    override suspend fun addCategory(userId: String, category: Category): Result<String> =
        safeCall {
            val data = mapOf(
                "categoryId" to UUID.randomUUID().toString(),
                "name"       to category.name,
                "type"       to category.type,
                "color"      to category.color,
                "isSystem"   to false,
                "isDefault"  to false,
                "isRiba"     to false,
                "createdAt"  to FieldValue.serverTimestamp()
            )
            FirestorePaths.categories(db, userId).add(data).await().id
        }

    override suspend fun updateCategory(
        userId: String,
        docId: String,
        category: Category
    ): Result<Unit> = safeCall {
        FirestorePaths.categories(db, userId).document(docId).update(
            mapOf(
                "name"  to category.name,
                "color" to category.color
                // type and isSystem cannot be changed after creation
            )
        ).await()
    }

    override suspend fun deleteCategory(userId: String, docId: String): Result<Unit> = safeCall {
        FirestorePaths.categories(db, userId).document(docId).delete().await()
    }
}

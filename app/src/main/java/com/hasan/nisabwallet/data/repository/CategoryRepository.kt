package com.hasan.nisabwallet.data.repository

import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategoriesFlow(userId: String): Flow<List<Category>>
    suspend fun getCategories(userId: String): Result<List<Category>>
    suspend fun addCategory(userId: String, category: Category): Result<String>
    suspend fun updateCategory(userId: String, docId: String, category: Category): Result<Unit>
    suspend fun deleteCategory(userId: String, docId: String): Result<Unit>
}

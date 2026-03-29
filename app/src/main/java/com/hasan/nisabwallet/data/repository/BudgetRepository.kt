package com.hasan.nisabwallet.data.repository

import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudgetsFlow(userId: String, year: Int, month: Int): Flow<List<Budget>>
    suspend fun getBudgets(userId: String, year: Int, month: Int): Result<List<Budget>>
    suspend fun addBudget(userId: String, budget: Budget): Result<String>
    suspend fun updateBudget(userId: String, docId: String, budget: Budget): Result<Unit>
    suspend fun deleteBudget(userId: String, docId: String): Result<Unit>
    suspend fun copyBudgetsToNextMonth(
        userId: String,
        fromYear: Int, fromMonth: Int,
        toYear: Int,   toMonth: Int
    ): Result<Int>
    /** Returns total spent per categoryId for a given month from transactions */
    suspend fun getSpentPerCategory(userId: String, year: Int, month: Int): Map<String, Double>
}

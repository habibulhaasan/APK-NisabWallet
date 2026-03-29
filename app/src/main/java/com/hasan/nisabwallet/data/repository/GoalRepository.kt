package com.hasan.nisabwallet.data.repository

import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.model.Goal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getGoalsFlow(userId: String): Flow<List<Goal>>
    suspend fun addGoal(userId: String, goal: Goal): Result<String>
    suspend fun updateGoal(userId: String, docId: String, goal: Goal): Result<Unit>
    suspend fun deleteGoal(userId: String, docId: String): Result<Unit>
    /** Deposit amount into goal — deducted from no account (it's already in the linked account) */
    suspend fun deposit(userId: String, docId: String, amount: Double, currentAmount: Double): Result<Unit>
    /** Withdraw amount from goal — released back to available balance */
    suspend fun withdraw(userId: String, docId: String, amount: Double, currentAmount: Double): Result<Unit>
    suspend fun markCompleted(userId: String, docId: String): Result<Unit>
    /** Returns total allocated across all active goals for a given account */
    suspend fun getTotalAllocated(userId: String, accountId: String): Double
}

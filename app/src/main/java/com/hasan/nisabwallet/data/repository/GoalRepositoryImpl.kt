package com.hasan.nisabwallet.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.firebase.FirestorePaths
import com.hasan.nisabwallet.data.firebase.snapshotFlow
import com.hasan.nisabwallet.data.model.Goal
import com.hasan.nisabwallet.data.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class GoalRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : GoalRepository {

    override fun getGoalsFlow(userId: String): Flow<List<Goal>> =
        FirestorePaths.financialGoals(db, userId)
            .snapshotFlow()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(Goal::class.java)?.copy(id = doc.id)
                }.sortedWith(compareBy({ it.status != "active" }, { it.priority == "low" }))
            }

    override suspend fun addGoal(userId: String, goal: Goal): Result<String> = safeCall {
        val data = mapOf(
            "goalId"               to UUID.randomUUID().toString(),
            "goalName"             to goal.goalName,
            "category"             to goal.category,
            "targetAmount"         to goal.targetAmount,
            "currentAmount"        to goal.currentAmount,
            "targetDate"           to goal.targetDate,
            "monthlyContribution"  to goal.monthlyContribution,
            "linkedAccountId"      to goal.linkedAccountId,
            "linkedAccountName"    to goal.linkedAccountName,
            "priority"             to goal.priority,
            "description"          to goal.description,
            "status"               to "active",
            "enableNotifications"  to goal.enableNotifications,
            "createdAt"            to FieldValue.serverTimestamp(),
            "updatedAt"            to FieldValue.serverTimestamp()
        )
        FirestorePaths.financialGoals(db, userId).add(data).await().id
    }

    override suspend fun updateGoal(userId: String, docId: String, goal: Goal): Result<Unit> = safeCall {
        FirestorePaths.financialGoals(db, userId).document(docId).update(mapOf(
            "goalName"            to goal.goalName,
            "category"            to goal.category,
            "targetAmount"        to goal.targetAmount,
            "targetDate"          to goal.targetDate,
            "monthlyContribution" to goal.monthlyContribution,
            "linkedAccountId"     to goal.linkedAccountId,
            "linkedAccountName"   to goal.linkedAccountName,
            "priority"            to goal.priority,
            "description"         to goal.description,
            "enableNotifications" to goal.enableNotifications,
            "updatedAt"           to FieldValue.serverTimestamp()
        )).await()
    }

    override suspend fun deleteGoal(userId: String, docId: String): Result<Unit> = safeCall {
        FirestorePaths.financialGoals(db, userId).document(docId).delete().await()
    }

    override suspend fun deposit(
        userId: String, docId: String, amount: Double, currentAmount: Double
    ): Result<Unit> = safeCall {
        val newAmount = currentAmount + amount
        val ref       = FirestorePaths.financialGoals(db, userId).document(docId)
        val updates   = mutableMapOf<String, Any>(
            "currentAmount" to newAmount,
            "updatedAt"     to FieldValue.serverTimestamp()
        )
        // Auto-complete if target reached
        val goal = ref.get().await().toObject(Goal::class.java)
        if (goal != null && newAmount >= goal.targetAmount) {
            updates["status"] = "completed"
        }
        ref.update(updates).await()
    }

    override suspend fun withdraw(
        userId: String, docId: String, amount: Double, currentAmount: Double
    ): Result<Unit> = safeCall {
        val newAmount = maxOf(0.0, currentAmount - amount)
        FirestorePaths.financialGoals(db, userId).document(docId).update(
            "currentAmount", newAmount,
            "updatedAt",     FieldValue.serverTimestamp()
        ).await()
    }

    override suspend fun markCompleted(userId: String, docId: String): Result<Unit> = safeCall {
        FirestorePaths.financialGoals(db, userId).document(docId).update(
            "status", "completed", "updatedAt", FieldValue.serverTimestamp()
        ).await()
    }

    override suspend fun getTotalAllocated(userId: String, accountId: String): Double {
        val snap = FirestorePaths.financialGoals(db, userId)
            .whereEqualTo("linkedAccountId", accountId)
            .whereEqualTo("status", "active")
            .get().await()
        return snap.documents.sumOf { it.getDouble("currentAmount") ?: 0.0 }
    }
}

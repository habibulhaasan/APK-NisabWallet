package com.hasan.nisabwallet.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.firebase.FirestorePaths
import com.hasan.nisabwallet.data.firebase.snapshotFlow
import com.hasan.nisabwallet.data.model.Budget
import com.hasan.nisabwallet.data.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : BudgetRepository {

    override fun getBudgetsFlow(userId: String, year: Int, month: Int): Flow<List<Budget>> =
        FirestorePaths.budgets(db, userId)
            .whereEqualTo("year", year)
            .whereEqualTo("month", month)
            .snapshotFlow()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(Budget::class.java)?.copy(id = doc.id)
                }.sortedBy { it.categoryName }
            }

    override suspend fun getBudgets(userId: String, year: Int, month: Int): Result<List<Budget>> =
        safeCall {
            FirestorePaths.budgets(db, userId)
                .whereEqualTo("year", year)
                .whereEqualTo("month", month)
                .get().await()
                .documents
                .mapNotNull { it.toObject(Budget::class.java)?.copy(id = it.id) }
        }

    override suspend fun addBudget(userId: String, budget: Budget): Result<String> = safeCall {
        val data = mapOf(
            "budgetId"      to UUID.randomUUID().toString(),
            "categoryId"    to budget.categoryId,
            "categoryName"  to budget.categoryName,
            "categoryColor" to budget.categoryColor,
            "amount"        to budget.amount,
            "year"          to budget.year,
            "month"         to budget.month,
            "rollover"      to budget.rollover,
            "notes"         to budget.notes,
            "createdAt"     to FieldValue.serverTimestamp(),
            "updatedAt"     to FieldValue.serverTimestamp()
        )
        FirestorePaths.budgets(db, userId).add(data).await().id
    }

    override suspend fun updateBudget(
        userId: String, docId: String, budget: Budget
    ): Result<Unit> = safeCall {
        FirestorePaths.budgets(db, userId).document(docId).update(
            mapOf(
                "amount"    to budget.amount,
                "rollover"  to budget.rollover,
                "notes"     to budget.notes,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    override suspend fun deleteBudget(userId: String, docId: String): Result<Unit> = safeCall {
        FirestorePaths.budgets(db, userId).document(docId).delete().await()
    }

    override suspend fun copyBudgetsToNextMonth(
        userId: String,
        fromYear: Int, fromMonth: Int,
        toYear: Int,   toMonth: Int
    ): Result<Int> = safeCall {
        val source = getBudgets(userId, fromYear, fromMonth).getOrDefault(emptyList())
        if (source.isEmpty()) error("No budgets found for $fromYear-$fromMonth")

        // Check if destination already has budgets — avoid duplicates
        val existing = getBudgets(userId, toYear, toMonth).getOrDefault(emptyList())
        val existingCategoryIds = existing.map { it.categoryId }.toSet()

        val batch = db.batch()
        var copied = 0
        source.forEach { budget ->
            if (budget.categoryId !in existingCategoryIds) {
                val docRef = FirestorePaths.budgets(db, userId).document()
                batch.set(docRef, mapOf(
                    "budgetId"      to UUID.randomUUID().toString(),
                    "categoryId"    to budget.categoryId,
                    "categoryName"  to budget.categoryName,
                    "categoryColor" to budget.categoryColor,
                    "amount"        to budget.amount,
                    "year"          to toYear,
                    "month"         to toMonth,
                    "rollover"      to budget.rollover,
                    "notes"         to budget.notes,
                    "createdAt"     to FieldValue.serverTimestamp(),
                    "updatedAt"     to FieldValue.serverTimestamp()
                ))
                copied++
            }
        }
        if (copied > 0) batch.commit().await()
        copied
    }

    override suspend fun getSpentPerCategory(
        userId: String, year: Int, month: Int
    ): Map<String, Double> {
        val start = "%04d-%02d-01".format(year, month)
        val end   = "%04d-%02d-31".format(year, month)
        val snap  = FirestorePaths.transactions(db, userId)
            .whereEqualTo("type", "Expense")
            .whereGreaterThanOrEqualTo("date", start)
            .whereLessThanOrEqualTo("date", end)
            .get().await()

        val map = mutableMapOf<String, Double>()
        snap.documents.forEach { doc ->
            val catId  = doc.getString("categoryId") ?: return@forEach
            val amount = doc.getDouble("amount") ?: 0.0
            map[catId] = (map[catId] ?: 0.0) + amount
        }
        return map
    }
}

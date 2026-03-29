package com.hasan.nisabwallet.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.firebase.FirestorePaths
import com.hasan.nisabwallet.data.firebase.snapshotFlow
import com.hasan.nisabwallet.data.model.DividendEntry
import com.hasan.nisabwallet.data.model.Investment
import com.hasan.nisabwallet.data.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

interface InvestmentRepository {
    fun getInvestmentsFlow(userId: String): Flow<List<Investment>>
    suspend fun addInvestment(userId: String, inv: Investment, accountBalance: Double): Result<String>
    suspend fun updateInvestment(userId: String, docId: String, inv: Investment): Result<Unit>
    suspend fun updateCurrentValue(userId: String, docId: String, newValue: Double): Result<Unit>
    suspend fun addDividend(userId: String, docId: String, dividend: DividendEntry, currentTotal: Double): Result<Unit>
    suspend fun sellInvestment(userId: String, docId: String, saleValue: Double, inv: Investment, accountBalance: Double): Result<Unit>
    suspend fun deleteInvestment(userId: String, docId: String): Result<Unit>
}

class InvestmentRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : InvestmentRepository {

    override fun getInvestmentsFlow(userId: String): Flow<List<Investment>> =
        FirestorePaths.investments(db, userId)
            .orderBy("purchaseDate", Query.Direction.DESCENDING)
            .snapshotFlow()
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(Investment::class.java)?.copy(id = it.id) }
            }

    override suspend fun addInvestment(
        userId: String, inv: Investment, accountBalance: Double
    ): Result<String> = safeCall {
        val batch   = db.batch()
        val invRef  = FirestorePaths.investments(db, userId).document()

        batch.set(invRef, mapOf(
            "investmentId"  to UUID.randomUUID().toString(),
            "name"          to inv.name,
            "type"          to inv.type,
            "purchasePrice" to inv.purchasePrice,
            "currentValue"  to inv.currentValue,
            "quantity"      to inv.quantity,
            "purchaseDate"  to inv.purchaseDate,
            "maturityDate"  to inv.maturityDate,
            "status"        to "active",
            "notes"         to inv.notes,
            "dividends"     to emptyList<Map<String, Any>>(),
            "totalDividends" to 0.0,
            "broker"        to inv.broker,
            "accountId"     to inv.accountId,
            "accountName"   to inv.accountName,
            "createdAt"     to FieldValue.serverTimestamp(),
            "updatedAt"     to FieldValue.serverTimestamp()
        ))

        // Debit account if accountId provided
        if (inv.accountId.isNotBlank()) {
            val accRef = FirestorePaths.accounts(db, userId).document(inv.accountId)
            batch.update(accRef, "balance", accountBalance - inv.totalInvested, "updatedAt", FieldValue.serverTimestamp())
            // Record expense transaction
            val txnRef = FirestorePaths.transactions(db, userId).document()
            batch.set(txnRef, mapOf(
                "transactionId" to UUID.randomUUID().toString(),
                "type" to "Expense", "amount" to inv.totalInvested,
                "accountId" to inv.accountId, "accountName" to inv.accountName,
                "categoryName" to "Investment Purchase",
                "description" to "Invested in ${inv.name} (${inv.typeLabel})",
                "date" to inv.purchaseDate, "isRiba" to false,
                "createdAt" to FieldValue.serverTimestamp()
            ))
        }
        batch.commit().await()
        invRef.id
    }

    override suspend fun updateInvestment(userId: String, docId: String, inv: Investment): Result<Unit> = safeCall {
        FirestorePaths.investments(db, userId).document(docId).update(mapOf(
            "name" to inv.name, "type" to inv.type,
            "currentValue" to inv.currentValue, "quantity" to inv.quantity,
            "maturityDate" to inv.maturityDate, "notes" to inv.notes, "broker" to inv.broker,
            "updatedAt" to FieldValue.serverTimestamp()
        )).await()
    }

    override suspend fun updateCurrentValue(userId: String, docId: String, newValue: Double): Result<Unit> = safeCall {
        FirestorePaths.investments(db, userId).document(docId)
            .update("currentValue", newValue, "updatedAt", FieldValue.serverTimestamp()).await()
    }

    override suspend fun addDividend(
        userId: String, docId: String, dividend: DividendEntry, currentTotal: Double
    ): Result<Unit> = safeCall {
        val ref  = FirestorePaths.investments(db, userId).document(docId)
        val snap = ref.get().await()
        val existing = snap.get("dividends") as? List<*> ?: emptyList<Any>()
        val updated  = existing + mapOf("amount" to dividend.amount, "date" to dividend.date,
            "notes" to dividend.notes, "recordedAt" to java.time.LocalDate.now().toString())
        ref.update("dividends", updated, "totalDividends", currentTotal + dividend.amount,
            "updatedAt", FieldValue.serverTimestamp()).await()
    }

    override suspend fun sellInvestment(
        userId: String, docId: String, saleValue: Double, inv: Investment, accountBalance: Double
    ): Result<Unit> = safeCall {
        val batch  = db.batch()
        val invRef = FirestorePaths.investments(db, userId).document(docId)
        batch.update(invRef, mapOf("status" to "sold", "updatedAt" to FieldValue.serverTimestamp()))

        if (inv.accountId.isNotBlank()) {
            val accRef = FirestorePaths.accounts(db, userId).document(inv.accountId)
            batch.update(accRef, "balance", accountBalance + saleValue, "updatedAt", FieldValue.serverTimestamp())
            val txnRef = FirestorePaths.transactions(db, userId).document()
            batch.set(txnRef, mapOf(
                "transactionId" to UUID.randomUUID().toString(),
                "type" to "Income", "amount" to saleValue,
                "accountId" to inv.accountId, "accountName" to inv.accountName,
                "categoryName" to "Investment Return",
                "description" to "Sold ${inv.name}",
                "date" to java.time.LocalDate.now().toString(), "isRiba" to false,
                "createdAt" to FieldValue.serverTimestamp()
            ))
        }
        batch.commit().await()
    }

    override suspend fun deleteInvestment(userId: String, docId: String): Result<Unit> = safeCall {
        FirestorePaths.investments(db, userId).document(docId).delete().await()
    }
}

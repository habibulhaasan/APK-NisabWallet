package com.hasan.nisabwallet.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.firebase.FirestorePaths
import com.hasan.nisabwallet.data.firebase.snapshotFlow
import com.hasan.nisabwallet.data.model.NisabSettings
import com.hasan.nisabwallet.data.model.ZakatCycle
import com.hasan.nisabwallet.data.model.ZakatPayment
import com.hasan.nisabwallet.data.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

interface ZakatRepository {
    fun getCyclesFlow(userId: String): Flow<List<ZakatCycle>>
    fun getPaymentsFlow(userId: String): Flow<List<ZakatPayment>>
    suspend fun getActiveCycle(userId: String): ZakatCycle?
    suspend fun startCycle(userId: String, startDate: String, hijriStartDate: String, wealth: Double, nisab: Double): Result<String>
    suspend fun updateCycleStartDate(userId: String, cycleId: String, newStartDate: String, newHijri: String): Result<Unit>
    suspend fun recordPayment(userId: String, payment: ZakatPayment, accountBalance: Double): Result<String>
    suspend fun completeCycle(userId: String, cycleId: String): Result<Unit>
    suspend fun getNisabSettings(userId: String): NisabSettings
    suspend fun saveNisabSettings(userId: String, settings: NisabSettings): Result<Unit>
}

class ZakatRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : ZakatRepository {

    override fun getCyclesFlow(userId: String): Flow<List<ZakatCycle>> =
        FirestorePaths.zakatCycles(db, userId)
            .snapshotFlow()
            .map { snap -> snap.documents.mapNotNull { it.toObject(ZakatCycle::class.java)?.copy(id = it.id) }
                .sortedByDescending { it.createdAt } }

    override fun getPaymentsFlow(userId: String): Flow<List<ZakatPayment>> =
        FirestorePaths.zakatPayments(db, userId)
            .snapshotFlow()
            .map { snap -> snap.documents.mapNotNull { it.toObject(ZakatPayment::class.java)?.copy(id = it.id) }
                .sortedByDescending { it.createdAt } }

    override suspend fun getActiveCycle(userId: String): ZakatCycle? {
        val snap = FirestorePaths.zakatCycles(db, userId)
            .whereEqualTo("status", "active").limit(1).get().await()
        if (snap.isEmpty) return null
        return snap.documents.first().let { it.toObject(ZakatCycle::class.java)?.copy(id = it.id) }
    }

    override suspend fun startCycle(
        userId: String, startDate: String, hijriStartDate: String, wealth: Double, nisab: Double
    ): Result<String> = safeCall {
        // Deactivate any existing active cycles first
        val existing = FirestorePaths.zakatCycles(db, userId).whereEqualTo("status", "active").get().await()
        val batch = db.batch()
        existing.documents.forEach { batch.update(it.reference, "status", "completed") }

        val newRef = FirestorePaths.zakatCycles(db, userId).document()
        batch.set(newRef, mapOf(
            "cycleId"        to UUID.randomUUID().toString(),
            "startDate"      to startDate,
            "hijriStartDate" to hijriStartDate,
            "status"         to "active",
            "startWealth"    to wealth,
            "nisabThreshold" to nisab,
            "zakatAmount"    to 0.0,
            "paidAmount"     to 0.0,
            "createdAt"      to FieldValue.serverTimestamp(),
            "updatedAt"      to FieldValue.serverTimestamp()
        ))
        batch.commit().await()
        newRef.id
    }

    override suspend fun updateCycleStartDate(
        userId: String, cycleId: String, newStartDate: String, newHijri: String
    ): Result<Unit> = safeCall {
        FirestorePaths.zakatCycles(db, userId).document(cycleId).update(
            "startDate", newStartDate,
            "hijriStartDate", newHijri,
            "updatedAt", FieldValue.serverTimestamp()
        ).await()
    }

    override suspend fun recordPayment(
        userId: String, payment: ZakatPayment, accountBalance: Double
    ): Result<String> = safeCall {
        val batch   = db.batch()
        val payRef  = FirestorePaths.zakatPayments(db, userId).document()
        val accRef  = FirestorePaths.accounts(db, userId).document(payment.accountId)
        val txnRef  = FirestorePaths.transactions(db, userId).document()

        batch.set(payRef, mapOf(
            "paymentId"   to UUID.randomUUID().toString(),
            "cycleId"     to payment.cycleId,
            "amount"      to payment.amount,
            "accountId"   to payment.accountId,
            "accountName" to payment.accountName,
            "paymentDate" to payment.paymentDate,
            "notes"       to payment.notes,
            "createdAt"   to FieldValue.serverTimestamp()
        ))

        batch.update(accRef, "balance", accountBalance - payment.amount, "updatedAt", FieldValue.serverTimestamp())

        batch.set(txnRef, mapOf(
            "transactionId" to UUID.randomUUID().toString(),
            "type" to "Expense", "amount" to payment.amount,
            "accountId" to payment.accountId, "accountName" to payment.accountName,
            "categoryName" to "Zakat Payment",
            "description" to "Zakat payment${if (payment.notes.isNotBlank()) " — ${payment.notes}" else ""}",
            "date" to payment.paymentDate, "isRiba" to false,
            "createdAt" to FieldValue.serverTimestamp()
        ))

        // Update cycle paidAmount
        if (payment.cycleId.isNotBlank()) {
            val cycleRef = FirestorePaths.zakatCycles(db, userId).document(payment.cycleId)
            batch.update(cycleRef, "paidAmount", FieldValue.increment(payment.amount), "updatedAt", FieldValue.serverTimestamp())
        }

        batch.commit().await()
        payRef.id
    }

    override suspend fun completeCycle(userId: String, cycleId: String): Result<Unit> = safeCall {
        FirestorePaths.zakatCycles(db, userId).document(cycleId)
            .update("status", "paid", "updatedAt", FieldValue.serverTimestamp()).await()
    }

    override suspend fun getNisabSettings(userId: String): NisabSettings {
        val snap = FirestorePaths.settings(db, userId).get().await()
        val doc  = snap.documents.firstOrNull { it.contains("nisabThreshold") } ?: return NisabSettings()
        return NisabSettings(
            nisabThreshold = doc.getDouble("nisabThreshold") ?: 0.0,
            silverPerGram  = doc.getDouble("silverPerGram") ?: 0.0,
            goldPerGram    = doc.getDouble("goldPerGram") ?: 0.0,
            lastUpdated    = doc.getString("lastUpdated") ?: ""
        )
    }

    override suspend fun saveNisabSettings(userId: String, settings: NisabSettings): Result<Unit> = safeCall {
        val colRef = FirestorePaths.settings(db, userId)
        val snap   = colRef.get().await()
        val data   = mapOf(
            "nisabThreshold" to settings.nisabThreshold,
            "silverPerGram"  to settings.silverPerGram,
            "goldPerGram"    to settings.goldPerGram,
            "lastUpdated"    to settings.lastUpdated
        )
        if (snap.isEmpty) colRef.add(data).await()
        else snap.documents.first().reference.update(data).await()
    }
}

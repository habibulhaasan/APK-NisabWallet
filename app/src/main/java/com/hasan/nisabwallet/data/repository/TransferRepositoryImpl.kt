package com.hasan.nisabwallet.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.firebase.FirestorePaths
import com.hasan.nisabwallet.data.firebase.snapshotFlow
import com.hasan.nisabwallet.data.model.Transfer
import com.hasan.nisabwallet.data.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class TransferRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : TransferRepository {

    override fun getTransfersFlow(userId: String): Flow<List<Transfer>> =
        FirestorePaths.transfers(db, userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .snapshotFlow()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(Transfer::class.java)?.copy(id = doc.id)
                }
            }

    override suspend fun addTransfer(
        userId: String,
        transfer: Transfer,
        fromBalance: Double,
        toBalance: Double
    ): Result<String> = safeCall {
        val batch   = db.batch()
        val txnRef  = FirestorePaths.transfers(db, userId).document()
        val fromRef = FirestorePaths.accounts(db, userId).document(transfer.fromAccountId)
        val toRef   = FirestorePaths.accounts(db, userId).document(transfer.toAccountId)

        // 1. Write transfer record
        batch.set(txnRef, mapOf(
            "transferId"      to UUID.randomUUID().toString(),
            "fromAccountId"   to transfer.fromAccountId,
            "fromAccountName" to transfer.fromAccountName,
            "toAccountId"     to transfer.toAccountId,
            "toAccountName"   to transfer.toAccountName,
            "amount"          to transfer.amount,
            "description"     to transfer.description,
            "date"            to transfer.date,
            "createdAt"       to FieldValue.serverTimestamp()
        ))

        // 2. Deduct from source account
        batch.update(fromRef, mapOf(
            "balance"   to (fromBalance - transfer.amount),
            "updatedAt" to FieldValue.serverTimestamp()
        ))

        // 3. Credit destination account
        batch.update(toRef, mapOf(
            "balance"   to (toBalance + transfer.amount),
            "updatedAt" to FieldValue.serverTimestamp()
        ))

        batch.commit().await()
        txnRef.id
    }

    override suspend fun deleteTransfer(
        userId: String,
        transfer: Transfer,
        fromBalance: Double,
        toBalance: Double
    ): Result<Unit> = safeCall {
        val batch   = db.batch()
        val txnRef  = FirestorePaths.transfers(db, userId).document(transfer.id)
        val fromRef = FirestorePaths.accounts(db, userId).document(transfer.fromAccountId)
        val toRef   = FirestorePaths.accounts(db, userId).document(transfer.toAccountId)

        batch.delete(txnRef)

        // Reverse: add back to source, deduct from destination
        batch.update(fromRef, mapOf(
            "balance"   to (fromBalance + transfer.amount),
            "updatedAt" to FieldValue.serverTimestamp()
        ))
        batch.update(toRef, mapOf(
            "balance"   to (toBalance - transfer.amount),
            "updatedAt" to FieldValue.serverTimestamp()
        ))

        batch.commit().await()
    }
}

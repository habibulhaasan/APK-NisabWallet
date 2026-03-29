package com.hasan.nisabwallet.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.firebase.FirestorePaths
import com.hasan.nisabwallet.data.firebase.snapshotFlow
import com.hasan.nisabwallet.data.model.Account
import com.hasan.nisabwallet.data.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : AccountRepository {

    // ── Real-time Flow (offline-first) ────────────────────────────────────────

    override fun getAccountsFlow(userId: String): Flow<List<Account>> =
        FirestorePaths.accounts(db, userId)
            .snapshotFlow()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Account::class.java)?.copy(id = doc.id)
                }.sortedBy { it.name }
            }

    // ── One-shot fetches ──────────────────────────────────────────────────────

    override suspend fun getAccounts(userId: String): Result<List<Account>> = safeCall {
        FirestorePaths.accounts(db, userId)
            .get().await()
            .documents
            .mapNotNull { doc -> doc.toObject(Account::class.java)?.copy(id = doc.id) }
            .sortedBy { it.name }
    }

    override suspend fun getAccount(userId: String, docId: String): Result<Account> = safeCall {
        val doc = FirestorePaths.accounts(db, userId).document(docId).get().await()
        doc.toObject(Account::class.java)?.copy(id = doc.id)
            ?: error("Account not found")
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    override suspend fun addAccount(userId: String, account: Account): Result<String> = safeCall {
        val colRef = FirestorePaths.accounts(db, userId)
        val data = mapOf(
            "accountId"   to UUID.randomUUID().toString(),
            "name"        to account.name,
            "type"        to account.type,
            "balance"     to account.balance,
            "currency"    to account.currency,
            "color"       to account.color,
            "icon"        to account.icon,
            "isDefault"   to account.isDefault,
            "ribaBalance" to 0.0,
            "description" to account.description,
            "createdAt"   to FieldValue.serverTimestamp(),
            "updatedAt"   to FieldValue.serverTimestamp()
        )
        val docRef = colRef.add(data).await()
        docRef.id
    }

    override suspend fun updateAccount(
        userId: String,
        docId: String,
        account: Account
    ): Result<Unit> = safeCall {
        val updates = mapOf(
            "name"        to account.name,
            "type"        to account.type,
            "color"       to account.color,
            "icon"        to account.icon,
            "description" to account.description,
            "updatedAt"   to FieldValue.serverTimestamp()
            // balance NOT updated here — use updateBalance() instead
        )
        FirestorePaths.accounts(db, userId).document(docId).update(updates).await()
    }

    override suspend fun deleteAccount(userId: String, docId: String): Result<Unit> = safeCall {
        FirestorePaths.accounts(db, userId).document(docId).delete().await()
    }

    // ── Balance helpers (called by Transaction / Transfer / Loan repos) ───────

    override suspend fun updateBalance(
        userId: String,
        docId: String,
        newBalance: Double
    ): Result<Unit> = safeCall {
        FirestorePaths.accounts(db, userId).document(docId)
            .update(
                "balance",   newBalance,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
    }

    override suspend fun updateRibaBalance(
        userId: String,
        docId: String,
        delta: Double
    ): Result<Unit> = safeCall {
        // Uses Firestore increment so concurrent updates are safe
        FirestorePaths.accounts(db, userId).document(docId)
            .update("ribaBalance", FieldValue.increment(delta)).await()
    }
}

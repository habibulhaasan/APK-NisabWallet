package com.hasan.nisabwallet.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.firebase.FirestorePaths
import com.hasan.nisabwallet.data.model.DefaultCategories
import com.hasan.nisabwallet.data.safeCall
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<Unit> =
        safeCall {
            auth.signInWithEmailAndPassword(email, password).await()
        }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String
    ): Result<Unit> = safeCall {
        // 1. Create Firebase Auth user
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user   = result.user ?: error("Registration failed")

        // 2. Update display name
        val profileUpdate = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()
        user.updateProfile(profileUpdate).await()

        // 3. Create user document in Firestore
        val userDoc = mapOf(
            "uid"         to user.uid,
            "email"       to email,
            "displayName" to displayName,
            "currency"    to "BDT",
            "language"    to "en",
            "createdAt"   to System.currentTimeMillis()
        )
        FirestorePaths.userDocument(db, user.uid)
            .set(userDoc, SetOptions.merge())
            .await()

        // 4. Seed default categories (mirrors web app defaultData.js)
        seedDefaultCategories(user.uid)
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        safeCall {
            auth.sendPasswordResetEmail(email).await()
        }

    override suspend fun signOut() {
        auth.signOut()
    }

    override fun getCurrentUserId(): String? = auth.currentUser?.uid
    override fun isLoggedIn(): Boolean       = auth.currentUser != null

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * Seeds the 21 default categories for a new user.
     * Uses batched writes for atomicity.
     * Checks if categories already exist first (idempotent).
     */
    private suspend fun seedDefaultCategories(userId: String) {
        val colRef    = FirestorePaths.categories(db, userId)
        val existing  = colRef.get().await()

        // Already seeded — skip
        if (!existing.isEmpty) return

        val batch = db.batch()
        DefaultCategories.ALL.forEach { category ->
            val docRef = colRef.document()
            val data   = mapOf(
                "categoryId" to UUID.randomUUID().toString(),
                "name"       to category.name,
                "type"       to category.type,
                "color"      to category.color,
                "isSystem"   to category.isSystem,
                "isDefault"  to category.isDefault,
                "isRiba"     to category.isRiba,
                "createdAt"  to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            batch.set(docRef, data)
        }
        batch.commit().await()
    }
}

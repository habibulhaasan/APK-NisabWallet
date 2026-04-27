package com.hasan.nisabwallet.data.firebase

import com.google.firebase.firestore.FirebaseFirestore

/**
 * Single source of truth for all Firestore collection paths.
 * Mirrors the web app's firestoreCollections.js exactly.
 *
 * All user data lives under:  users/{userId}/{subcollection}
 */
object FirestorePaths {

    // ── User sub-collections ─────────────────────────────────────────────
    fun accounts(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId).collection("accounts")

    fun transactions(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId).collection("transactions")

    fun categories(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId).collection("categories")

    fun transfers(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId).collection("transfers")

    fun budgets(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId).collection("budgets")

    fun billReminders(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId).collection("billReminders")

    fun loans(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId).collection("loans")

    fun lendings(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId).collection("lendings")

    fun financialGoals(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId).collection("financialGoals")

    fun investments(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId).collection("investments")

    fun jewellery(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId).collection("jewellery")

    fun shoppingCarts(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId).collection("shoppingCarts")

    fun shoppingItems(db: FirebaseFirestore, userId: String, cartId: String) =
        db.collection("users").document(userId)
            .collection("shoppingCarts").document(cartId)
            .collection("items")

    fun recurringTransactions(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId).collection("recurringTransactions")

    fun recurringLogs(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId).collection("recurringLogs")

    fun zakatCycles(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId).collection("zakatCycles")

    fun zakatPayments(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId).collection("zakatPayments")

    fun settings(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId).collection("settings")

    fun taxYears(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId).collection("taxYears")

    fun taxMappings(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId).collection("taxMappings")

    fun feedback(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId).collection("feedback")

    // ── Top-level collections ────────────────────────────────────────────
    fun metalPrices(db: FirebaseFirestore) =
        db.collection("metalPrices")

    fun userDocument(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId)

    // In FirestorePaths.kt — add these two functions

    fun expenseTrackerTabs(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId).collection("expenseTrackerTabs")

    fun expenseTrackerData(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId).collection("expenseTrackerData")

    fun groceryItems(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId).collection("groceryItems")

    fun groceryMonths(db: FirebaseFirestore, userId: String) =
        db.collection("users").document(userId).collection("groceryMonths")
}

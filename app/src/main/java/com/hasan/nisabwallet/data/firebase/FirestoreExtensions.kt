package com.hasan.nisabwallet.data.firebase

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Converts a Firestore CollectionReference into a Flow<QuerySnapshot>.
 * Emits every time the collection changes (online or from local cache offline).
 * This is the core of our offline-first reactive data layer.
 */
fun CollectionReference.snapshotFlow(): Flow<QuerySnapshot> = callbackFlow {
    val listener = addSnapshotListener { snapshot, error ->
        if (error != null) {
            cancel(error.message ?: "Firestore error", error)
            return@addSnapshotListener
        }
        snapshot?.let { trySend(it) }
    }
    awaitClose { listener.remove() }
}

/**
 * Converts a Firestore Query into a Flow<QuerySnapshot>.
 */
fun Query.snapshotFlow(): Flow<QuerySnapshot> = callbackFlow {
    val listener = addSnapshotListener { snapshot, error ->
        if (error != null) {
            cancel(error.message ?: "Firestore query error", error)
            return@addSnapshotListener
        }
        snapshot?.let { trySend(it) }
    }
    awaitClose { listener.remove() }
}

/**
 * Converts a DocumentReference into a Flow.
 */
fun DocumentReference.snapshotFlow() = callbackFlow {
    val listener = addSnapshotListener { snapshot, error ->
        if (error != null) {
            cancel(error.message ?: "Firestore doc error", error)
            return@addSnapshotListener
        }
        snapshot?.let { trySend(it) }
    }
    awaitClose { listener.remove() }
}

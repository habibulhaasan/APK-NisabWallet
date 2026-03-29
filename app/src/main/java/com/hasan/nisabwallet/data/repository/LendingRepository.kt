package com.hasan.nisabwallet.data.repository

import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.model.Lending
import com.hasan.nisabwallet.data.model.LendingRepayment
import kotlinx.coroutines.flow.Flow

interface LendingRepository {
    fun getLendingsFlow(userId: String): Flow<List<Lending>>
    suspend fun addLending(userId: String, lending: Lending, accountBalance: Double): Result<String>
    suspend fun updateLending(userId: String, docId: String, lending: Lending): Result<Unit>
    suspend fun deleteLending(userId: String, docId: String): Result<Unit>
    suspend fun recordRepayment(
        userId: String,
        lendingDocId: String,
        repayment: LendingRepayment,
        currentLending: Lending,
        accountBalance: Double
    ): Result<Unit>
    suspend fun markAsReturned(userId: String, docId: String): Result<Unit>
}

package com.hasan.nisabwallet.data.repository

import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.model.Transfer
import kotlinx.coroutines.flow.Flow

interface TransferRepository {
    fun getTransfersFlow(userId: String): Flow<List<Transfer>>
    suspend fun addTransfer(
        userId: String,
        transfer: Transfer,
        fromBalance: Double,
        toBalance: Double
    ): Result<String>
    suspend fun deleteTransfer(
        userId: String,
        transfer: Transfer,
        fromBalance: Double,
        toBalance: Double
    ): Result<Unit>
}

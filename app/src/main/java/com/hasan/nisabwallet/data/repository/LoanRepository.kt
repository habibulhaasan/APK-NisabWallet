package com.hasan.nisabwallet.data.repository

import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.model.Loan
import com.hasan.nisabwallet.data.model.LoanPayment
import kotlinx.coroutines.flow.Flow

interface LoanRepository {
    fun getLoansFlow(userId: String): Flow<List<Loan>>
    suspend fun getLoan(userId: String, docId: String): Result<Loan>
    suspend fun addLoan(userId: String, loan: Loan, accountBalance: Double): Result<String>
    suspend fun updateLoan(userId: String, docId: String, loan: Loan): Result<Unit>
    suspend fun deleteLoan(userId: String, docId: String): Result<Unit>
    suspend fun recordPayment(
        userId: String,
        loanDocId: String,
        payment: LoanPayment,
        currentLoan: Loan,
        accountBalance: Double
    ): Result<Unit>
    suspend fun markAsPaid(userId: String, docId: String): Result<Unit>
}

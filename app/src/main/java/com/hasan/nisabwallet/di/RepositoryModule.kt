package com.hasan.nisabwallet.di

import com.hasan.nisabwallet.data.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

// Stub impls for modules without full repository implementations yet
class TaxRepositoryImpl @Inject constructor() : TaxRepository
class FeedbackRepositoryImpl @Inject constructor() : FeedbackRepository
class SettingsRepositoryImpl @Inject constructor() : SettingsRepository
class AnalyticsRepositoryImpl @Inject constructor() : AnalyticsRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
    @Binds @Singleton abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository
    @Binds @Singleton abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository
    @Binds @Singleton abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository
    @Binds @Singleton abstract fun bindTransferRepository(impl: TransferRepositoryImpl): TransferRepository
    @Binds @Singleton abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository
    @Binds @Singleton abstract fun bindLoanRepository(impl: LoanRepositoryImpl): LoanRepository
    @Binds @Singleton abstract fun bindLendingRepository(impl: LendingRepositoryImpl): LendingRepository
    @Binds @Singleton abstract fun bindGoalRepository(impl: GoalRepositoryImpl): GoalRepository
    @Binds @Singleton abstract fun bindInvestmentRepository(impl: InvestmentRepositoryImpl): InvestmentRepository
    @Binds @Singleton abstract fun bindJewelleryRepository(impl: JewelleryRepositoryImpl): JewelleryRepository
    @Binds @Singleton abstract fun bindZakatRepository(impl: ZakatRepositoryImpl): ZakatRepository
    @Binds @Singleton abstract fun bindRecurringRepository(impl: RecurringRepositoryImpl): RecurringRepository
    @Binds @Singleton abstract fun bindShoppingRepository(impl: ShoppingRepositoryImpl): ShoppingRepository
    @Binds @Singleton abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository
    @Binds @Singleton abstract fun bindTaxRepository(impl: TaxRepositoryImpl): TaxRepository
    @Binds @Singleton abstract fun bindFeedbackRepository(impl: FeedbackRepositoryImpl): FeedbackRepository
    @Binds @Singleton abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds @Singleton abstract fun bindAnalyticsRepository(impl: AnalyticsRepositoryImpl): AnalyticsRepository
}

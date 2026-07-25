package com.moneymanager.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FinanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: UserSettingsEntity)

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getSettings(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAccount(account: AccountEntity): Long

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccount(id: Long)

    @Query("SELECT * FROM accounts ORDER BY id ASC")
    suspend fun getAccounts(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seedCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id AND isDefault = 0")
    suspend fun deleteCustomCategory(id: Long)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: Long)

    @Query("UPDATE transactions SET categoryId = :toCategoryId WHERE categoryId = :fromCategoryId")
    suspend fun moveTransactionsToCategory(fromCategoryId: Long, toCategoryId: Long)

    @Query("UPDATE transactions SET secondaryCategoryId = :toCategoryId WHERE secondaryCategoryId = :fromCategoryId")
    suspend fun moveSecondaryCategory(fromCategoryId: Long, toCategoryId: Long)

    /** Untags CC rows whose second category was deleted, so no row points at a missing category. */
    @Query("UPDATE transactions SET secondaryCategoryId = NULL WHERE secondaryCategoryId = :categoryId")
    suspend fun clearSecondaryCategory(categoryId: Long)

    @Query("SELECT * FROM categories ORDER BY isDefault DESC, id ASC")
    suspend fun getCategories(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTransaction(transaction: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("SELECT * FROM transactions ORDER BY timestampMillis DESC")
    suspend fun getTransactions(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBudget(budget: BudgetEntity): Long

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudget(id: Long)

    @Query("SELECT * FROM budgets ORDER BY month DESC, id DESC")
    suspend fun getBudgets(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: DetectedDraftEntity): Long

    @Query("DELETE FROM detected_drafts WHERE id = :id")
    suspend fun deleteDraft(id: Long)

    @Query("SELECT * FROM detected_drafts ORDER BY detectedAtMillis DESC")
    suspend fun getDrafts(): List<DetectedDraftEntity>

    @Query("DELETE FROM user_settings")
    suspend fun deleteSettings()

    @Query("DELETE FROM accounts")
    suspend fun deleteAccounts()

    @Query("DELETE FROM transactions")
    suspend fun deleteTransactions()

    @Query("DELETE FROM budgets")
    suspend fun deleteBudgets()

    @Query("DELETE FROM detected_drafts")
    suspend fun deleteDrafts()

    @Query("DELETE FROM categories WHERE isDefault = 0")
    suspend fun deleteCustomCategories()
}

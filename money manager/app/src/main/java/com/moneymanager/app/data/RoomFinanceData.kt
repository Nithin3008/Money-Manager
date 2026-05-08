package com.moneymanager.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.moneymanager.app.model.BankAccount
import com.moneymanager.app.model.BudgetPlan
import com.moneymanager.app.model.CategoryItem
import com.moneymanager.app.model.CurrencyOption
import com.moneymanager.app.model.DefaultCategories
import com.moneymanager.app.model.DetectedTransactionDraft
import com.moneymanager.app.model.FinanceUiState
import com.moneymanager.app.model.LedgerTransaction
import com.moneymanager.app.model.MoneyIcons
import com.moneymanager.app.model.ThemeMode
import com.moneymanager.app.model.TransactionType
import java.time.YearMonth

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Long = 1,
    val userName: String,
    val currencyCode: String,
    val themeMode: String
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val balance: Double
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val iconKey: String,
    val isDefault: Boolean
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val type: String,
    val categoryId: Long,
    val accountId: Long?,
    val timestampMillis: Long,
    val isAutoDetected: Boolean,
    val rawMessage: String?
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val limitAmount: Double,
    val categoryIdsCsv: String,
    val month: String
)

@Entity(tableName = "detected_drafts")
data class DetectedDraftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bankName: String,
    val name: String,
    val amount: Double,
    val type: String,
    val counterparty: String,
    val rawMessage: String,
    val suggestedCategoryId: Long?,
    val detectedAtMillis: Long
)

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

    @Query("DELETE FROM categories WHERE id = :id AND isDefault = 0")
    suspend fun deleteCustomCategory(id: Long)

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
}

@Database(
    entities = [
        UserSettingsEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        DetectedDraftEntity::class
    ],
    version = 2
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun dao(): FinanceDao

    companion object {
        @Volatile private var instance: FinanceDatabase? = null

        fun get(context: Context): FinanceDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "money_manager.db"
                )
                    .addMigrations(Migration1To2)
                    .build()
                    .also { instance = it }
            }
        }

        private val Migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN themeMode TEXT NOT NULL DEFAULT 'Dark'")
            }
        }
    }
}

class FinanceRepository(private val dao: FinanceDao) {
    suspend fun loadState(current: FinanceUiState = FinanceUiState()): FinanceUiState {
        seedDefaultCategories()
        val settings = dao.getSettings()
        return current.copy(
            userName = settings?.userName.orEmpty(),
            currency = settings?.currencyCode?.let { code ->
                CurrencyOption.entries.firstOrNull { it.currencyCode == code }
            } ?: CurrencyOption.INR,
            themeMode = settings?.themeMode?.let { mode ->
                ThemeMode.entries.firstOrNull { it.name == mode }
            } ?: ThemeMode.Dark,
            accounts = dao.getAccounts().map { it.toModel() },
            categories = dao.getCategories().map { it.toModel() },
            transactions = dao.getTransactions().map { it.toModel() },
            budgets = dao.getBudgets().map { it.toModel() },
            detectedDrafts = dao.getDrafts().map { it.toModel() }
        )
    }

    suspend fun saveSettings(name: String, currency: CurrencyOption, themeMode: ThemeMode) {
        dao.saveSettings(
            UserSettingsEntity(
                userName = name,
                currencyCode = currency.currencyCode,
                themeMode = themeMode.name
            )
        )
    }

    suspend fun addAccount(name: String, balance: Double) {
        dao.saveAccount(AccountEntity(name = name, balance = balance))
    }

    suspend fun addCategory(category: CategoryItem) {
        dao.saveCategory(category.toEntity())
    }

    suspend fun addTransaction(transaction: LedgerTransaction): Long {
        return dao.saveTransaction(transaction.toEntity(id = 0))
    }

    suspend fun addBudget(budget: BudgetPlan): Long {
        return dao.saveBudget(budget.toEntity(id = 0))
    }

    suspend fun saveDraft(draft: DetectedTransactionDraft): Long {
        return dao.saveDraft(draft.toEntity(id = 0))
    }

    suspend fun deleteDraft(id: Long) = dao.deleteDraft(id)
    suspend fun deleteTransaction(id: Long) = dao.deleteTransaction(id)
    suspend fun deleteBudget(id: Long) = dao.deleteBudget(id)
    suspend fun deleteAccount(id: Long) = dao.deleteAccount(id)
    suspend fun deleteCustomCategory(id: Long) = dao.deleteCustomCategory(id)

    private suspend fun seedDefaultCategories() {
        DefaultCategories.items.forEach { dao.saveCategory(it.toEntity()) }
    }
}

private fun AccountEntity.toModel() = BankAccount(id = id, name = name, balance = balance)

private fun CategoryEntity.toModel() = CategoryItem(
    id = id,
    name = name,
    iconKey = iconKey,
    icon = MoneyIcons.resolveCategoryIcon(iconKey),
    isDefault = isDefault
)

private fun CategoryItem.toEntity() = CategoryEntity(
    id = id,
    name = name,
    iconKey = iconKey,
    isDefault = isDefault
)

private fun TransactionEntity.toModel() = LedgerTransaction(
    id = id,
    name = name,
    amount = amount,
    type = TransactionType.valueOf(type),
    categoryId = categoryId,
    accountId = accountId,
    timestampMillis = timestampMillis,
    isAutoDetected = isAutoDetected,
    rawMessage = rawMessage
)

private fun LedgerTransaction.toEntity(id: Long = this.id) = TransactionEntity(
    id = id,
    name = name,
    amount = amount,
    type = type.name,
    categoryId = categoryId,
    accountId = accountId,
    timestampMillis = timestampMillis,
    isAutoDetected = isAutoDetected,
    rawMessage = rawMessage
)

private fun BudgetEntity.toModel() = BudgetPlan(
    id = id,
    name = name,
    limitAmount = limitAmount,
    categoryIds = categoryIdsCsv.split(",").mapNotNull { it.toLongOrNull() }.toSet(),
    month = YearMonth.parse(month)
)

private fun BudgetPlan.toEntity(id: Long = this.id) = BudgetEntity(
    id = id,
    name = name,
    limitAmount = limitAmount,
    categoryIdsCsv = categoryIds.joinToString(","),
    month = month.toString()
)

private fun DetectedDraftEntity.toModel() = DetectedTransactionDraft(
    id = id,
    bankName = bankName,
    name = name,
    amount = amount,
    type = TransactionType.valueOf(type),
    counterparty = counterparty,
    rawMessage = rawMessage,
    suggestedCategoryId = suggestedCategoryId,
    detectedAtMillis = detectedAtMillis
)

private fun DetectedTransactionDraft.toEntity(id: Long = this.id) = DetectedDraftEntity(
    id = id,
    bankName = bankName,
    name = name,
    amount = amount,
    type = type.name,
    counterparty = counterparty,
    rawMessage = rawMessage,
    suggestedCategoryId = suggestedCategoryId,
    detectedAtMillis = detectedAtMillis
)

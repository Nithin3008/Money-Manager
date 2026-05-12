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
import com.moneymanager.app.model.UiAccent
import com.moneymanager.app.model.UiSurface
import java.time.YearMonth

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Long = 1,
    val userName: String,
    val currencyCode: String,
    val themeMode: String,
    val salaryShiftIncomeEnabled: Boolean = false,
    val salaryShiftWindowDays: Int = 5,
    val salaryCategoryId: Long? = null,
    val salaryKeywordsForUncategorized: Boolean = true,
    val bankSmsSetupCompleted: Boolean = false,
    val summaryAccountFilterIdsCsv: String = "",
    val uiAccent: String = "Sky",
    val uiSurface: String = "Midnight",
    val defaultAccountId: Long? = null
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val balance: Double,
    val smsMatchKey: String? = null
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val iconKey: String,
    val isDefault: Boolean,
    val colorHex: String
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
    val rawMessage: String?,
    val smsBankLabel: String? = null,
    val excludeFromSummary: Boolean = false
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
    val detectedAtMillis: Long,
    val transactionTimestampMillis: Long
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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seedCategory(category: CategoryEntity)

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

@Database(
    entities = [
        UserSettingsEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        DetectedDraftEntity::class
    ],
    version = 10
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
                    .addMigrations(
                        Migration1To2,
                        Migration2To3,
                        Migration3To4,
                        Migration4To5,
                        Migration5To6,
                        Migration6To7,
                        Migration7To8,
                        Migration8To9,
                        Migration9To10
                    )
                    .build()
                    .also { instance = it }
            }
        }

        private val Migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN themeMode TEXT NOT NULL DEFAULT 'Dark'")
            }
        }

        private val Migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE detected_drafts ADD COLUMN transactionTimestampMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE detected_drafts SET transactionTimestampMillis = detectedAtMillis WHERE transactionTimestampMillis = 0")
            }
        }

        private val Migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN colorHex TEXT NOT NULL DEFAULT '#8F95A3'")
                db.execSQL("UPDATE categories SET colorHex = '#38E68B' WHERE name = 'Grocery'")
                db.execSQL("UPDATE categories SET colorHex = '#FFC857' WHERE name = 'Food'")
                db.execSQL("UPDATE categories SET colorHex = '#FF4FB8' WHERE name = 'Shopping'")
                db.execSQL("UPDATE categories SET colorHex = '#FF8A3D' WHERE name = 'Fuel'")
                db.execSQL("UPDATE categories SET colorHex = '#FF6B7A' WHERE name = 'Rent'")
            }
        }

        private val Migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE transactions SET categoryId = 0 WHERE isAutoDetected = 1")
            }
        }

        private val Migration5To6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE user_settings ADD COLUMN salaryShiftIncomeEnabled INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE user_settings ADD COLUMN salaryShiftWindowDays INTEGER NOT NULL DEFAULT 5"
                )
            }
        }

        private val Migration6To7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN smsBankLabel TEXT")
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN excludeFromSummary INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL("ALTER TABLE accounts ADD COLUMN smsMatchKey TEXT")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN salaryCategoryId INTEGER")
                db.execSQL(
                    "ALTER TABLE user_settings ADD COLUMN salaryKeywordsForUncategorized INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE user_settings ADD COLUMN bankSmsSetupCompleted INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE user_settings ADD COLUMN summaryAccountFilterIdsCsv TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val Migration7To8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN uiAccent TEXT NOT NULL DEFAULT 'Sky'")
            }
        }

        private val Migration8To9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN uiSurface TEXT NOT NULL DEFAULT 'Midnight'")
            }
        }

        private val Migration9To10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN defaultAccountId INTEGER")
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
            uiAccent = settings?.uiAccent?.let { accent ->
                UiAccent.entries.firstOrNull { it.name == accent }
            } ?: UiAccent.Sky,
            uiSurface = settings?.uiSurface?.let { surface ->
                UiSurface.entries.firstOrNull { it.name == surface }
            } ?: UiSurface.Midnight,
            salaryShiftIncomeEnabled = settings?.salaryShiftIncomeEnabled ?: false,
            salaryShiftWindowDays = settings?.salaryShiftWindowDays?.coerceIn(1, 14) ?: 5,
            salaryCategoryId = settings?.salaryCategoryId,
            salaryKeywordsForUncategorized = settings?.salaryKeywordsForUncategorized ?: true,
            bankSmsSetupCompleted = settings?.bankSmsSetupCompleted ?: false,
            defaultAccountId = settings?.defaultAccountId,
            summarySelectedAccountIds = settings?.summaryAccountFilterIdsCsv
                ?.split(",")
                ?.mapNotNull { it.trim().toLongOrNull() }
                ?.toSet()
                ?: emptySet(),
            accounts = dao.getAccounts().map { it.toModel() },
            categories = dao.getCategories().map { it.toModel() },
            transactions = dao.getTransactions().map { it.toModel() },
            budgets = dao.getBudgets().map { it.toModel() },
            detectedDrafts = dao.getDrafts().map { it.toModel() }
        )
    }

    suspend fun persistUserSettings(state: FinanceUiState) {
        dao.saveSettings(state.toSettingsEntity())
    }

    suspend fun addAccount(name: String, balance: Double, smsMatchKey: String? = null): Long {
        return dao.saveAccount(
            AccountEntity(
                name = name,
                balance = balance,
                smsMatchKey = smsMatchKey?.trim()?.takeIf { it.isNotEmpty() }
            )
        )
    }

    suspend fun updateAccount(account: BankAccount) {
        dao.saveAccount(
            AccountEntity(
                id = account.id,
                name = account.name,
                balance = account.balance,
                smsMatchKey = account.smsMatchKey?.trim()?.takeIf { it.isNotEmpty() }
            )
        )
    }

    suspend fun remapTransactionAccountsFromSmsLabels() {
        val accounts = dao.getAccounts().map { it.toModel() }
        if (accounts.isEmpty()) return
        for (entity in dao.getTransactions()) {
            val tx = entity.toModel()
            val parsedLabel = tx.rawMessage
                ?.let { TransactionMessageParser.parse(it, tx.timestampMillis)?.bankName }
            val label = parsedLabel ?: tx.smsBankLabel ?: continue
            val resolved = SmsBankKeys.resolveAccountId(label, accounts)
            if (resolved != null) {
                accounts.firstOrNull { it.id == resolved && it.smsMatchKey.isNullOrBlank() }
                    ?.let { updateAccount(it.copy(smsMatchKey = SmsBankKeys.normalize(label))) }
            }
            val next = tx.copy(
                smsBankLabel = label,
                accountId = resolved ?: tx.accountId
            )
            if (next.accountId != tx.accountId || next.smsBankLabel != tx.smsBankLabel) {
                dao.saveTransaction(next.toEntity(tx.id))
            }
        }
    }

    suspend fun addCategory(category: CategoryItem) {
        dao.saveCategory(category.toEntity())
    }

    suspend fun addTransaction(transaction: LedgerTransaction): Long {
        return dao.saveTransaction(transaction.toEntity(id = 0))
    }

    suspend fun updateTransaction(transaction: LedgerTransaction): Long {
        return dao.saveTransaction(transaction.toEntity(id = transaction.id))
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

    suspend fun clearAllSavedData() {
        dao.deleteDrafts()
        dao.deleteTransactions()
        dao.deleteBudgets()
        dao.deleteAccounts()
        dao.deleteCustomCategories()
        dao.deleteSettings()
        seedDefaultCategories()
    }

    suspend fun cleanupCreditCardRepaymentArtifacts() {
        dao.getTransactions().forEach { entity ->
            val tx = entity.toModel()
            if (SmsTransactionNormalizer.isNonLedgerTransactionArtifact(tx.rawMessage, tx.type)) {
                dao.deleteTransaction(tx.id)
            }
        }
    }

    suspend fun exportData(): String {
        val data = com.moneymanager.app.model.AppBackupData(
            settings = dao.getSettings(),
            accounts = dao.getAccounts(),
            categories = dao.getCategories(),
            transactions = dao.getTransactions(),
            budgets = dao.getBudgets()
        )
        return com.google.gson.Gson().toJson(data)
    }

    suspend fun importData(jsonString: String) {
        val data = com.google.gson.Gson().fromJson(jsonString, com.moneymanager.app.model.AppBackupData::class.java)
        
        // delete all current
        dao.deleteTransactions()
        dao.deleteBudgets()
        dao.deleteAccounts()
        dao.deleteCustomCategories()
        dao.deleteSettings()

        // insert all
        data.settings?.let { dao.saveSettings(it) }
        data.accounts.forEach { dao.saveAccount(it) }
        data.categories.forEach { dao.saveCategory(it) }
        data.transactions.forEach { dao.saveTransaction(it) }
        data.budgets.forEach { dao.saveBudget(it) }
    }

    private suspend fun seedDefaultCategories() {
        DefaultCategories.items.forEach { dao.seedCategory(it.toEntity()) }
    }
}

private fun FinanceUiState.toSettingsEntity(): UserSettingsEntity = UserSettingsEntity(
    userName = userName,
    currencyCode = currency.currencyCode,
    themeMode = themeMode.name,
    uiAccent = uiAccent.name,
    uiSurface = uiSurface.name,
    salaryShiftIncomeEnabled = salaryShiftIncomeEnabled,
    salaryShiftWindowDays = salaryShiftWindowDays.coerceIn(1, 14),
    salaryCategoryId = salaryCategoryId,
    salaryKeywordsForUncategorized = salaryKeywordsForUncategorized,
    bankSmsSetupCompleted = bankSmsSetupCompleted,
    defaultAccountId = defaultAccountId,
    summaryAccountFilterIdsCsv = summarySelectedAccountIds.joinToString(",")
)

private fun AccountEntity.toModel() = BankAccount(
    id = id,
    name = name,
    balance = balance,
    smsMatchKey = smsMatchKey
)

private fun CategoryEntity.toModel() = CategoryItem(
    id = id,
    name = name,
    iconKey = iconKey,
    icon = MoneyIcons.resolveCategoryIcon(iconKey),
    isDefault = isDefault,
    colorHex = colorHex
)

private fun CategoryItem.toEntity() = CategoryEntity(
    id = id,
    name = name,
    iconKey = iconKey,
    isDefault = isDefault,
    colorHex = colorHex
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
    rawMessage = rawMessage,
    smsBankLabel = smsBankLabel,
    excludeFromSummary = excludeFromSummary
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
    rawMessage = rawMessage,
    smsBankLabel = smsBankLabel,
    excludeFromSummary = excludeFromSummary
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
    detectedAtMillis = detectedAtMillis,
    transactionTimestampMillis = if (transactionTimestampMillis == 0L) detectedAtMillis else transactionTimestampMillis
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
    detectedAtMillis = detectedAtMillis,
    transactionTimestampMillis = transactionTimestampMillis
)

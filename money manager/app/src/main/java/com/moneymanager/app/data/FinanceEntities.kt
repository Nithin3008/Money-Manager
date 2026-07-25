package com.moneymanager.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Long = 1,
    val userName: String,
    val currencyCode: String,
    val themeMode: String,
    val salaryShiftIncomeEnabled: Boolean = false,
    val salaryShiftWindowDays: Int = 5,
    val salaryCategoryId: Long? = null,
    // Legacy keyword toggle; superseded by salaryCounterpartyKey but kept for schema stability.
    val salaryKeywordsForUncategorized: Boolean = true,
    val salaryCounterpartyKey: String? = null,
    val dismissedSalaryKeysCsv: String = "",
    val bankSmsSetupCompleted: Boolean = false,
    val offlineLlmParsingEnabled: Boolean = false,
    val offlineLlmModelDownloaded: Boolean = false,
    val onboardedAtMillis: Long = 0L,
    val lastSuccessfulScanMillis: Long = 0L,
    val summaryAccountFilterIdsCsv: String = "",
    // Newline-delimited: SMS keys contain commas, but normalization strips newlines.
    val dismissedSmsKeys: String = "",
    val uiAccent: String = "Lime",
    val uiSurface: String = "Midnight",
    val defaultAccountId: Long? = null,
    /** Shared color library, comma-separated "#RRGGBB"; empty until first seeded. */
    val paletteHexCsv: String = ""
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val balance: Double,
    val smsMatchKey: String? = null,
    val accountType: String = "Bank",
    val balanceAnchorAtMillis: Long = 0L,
    // Comma-separated extra card last-4s billed under this account (consolidated statement).
    val linkedCardsCsv: String = ""
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
    val excludeFromSummary: Boolean = false,
    val isCreditCardTransaction: Boolean = false,
    val description: String? = null,
    val fromAccountId: Long? = null,
    val toAccountId: Long? = null,
    // Second tag on CC rows only ("CC + Shopping"); null everywhere else. Max one, so a
    // transaction never carries more than two categories.
    val secondaryCategoryId: Long? = null
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
    val transactionTimestampMillis: Long,
    val fromAccountId: Long? = null,
    val toAccountId: Long? = null
)

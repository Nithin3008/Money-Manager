package com.moneymanager.app.model

import com.moneymanager.app.data.AccountEntity
import com.moneymanager.app.data.BudgetEntity
import com.moneymanager.app.data.CategoryEntity
import com.moneymanager.app.data.DetectedDraftEntity
import com.moneymanager.app.data.TransactionEntity
import com.moneymanager.app.data.UserSettingsEntity

// All fields carry defaults so Kotlin emits a no-arg constructor: Gson then applies these
// defaults for fields missing from older backup files instead of leaving them null.
data class AppBackupData(
    val version: Int = 2,
    val settings: UserSettingsEntity? = null,
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    // Since version 2: pending SMS-detected suggestions.
    val drafts: List<DetectedDraftEntity> = emptyList()
)
